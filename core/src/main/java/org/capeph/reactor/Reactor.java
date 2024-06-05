package org.capeph.reactor;


import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.aeron.Aeron;
import io.aeron.FragmentAssembler;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.BufferClaim;
import io.aeron.logbuffer.FragmentHandler;

import org.agrona.DirectBuffer;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.IdleStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.capeph.lookup.dto.ReactorInfo;
import org.capeph.messages.codec.Codec;

import java.io.File;
import java.io.IOException;
import java.net.HttpURLConnection;
import java.net.URI;
import java.net.URISyntaxException;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;
import static java.net.http.HttpRequest.BodyPublishers.ofString;

public class Reactor implements Agent {


    //TODO:
     // - register name and host
    // - register callback
    // - send message to other channel
    // - set up listener

    private final Logger log = LogManager.getLogger(Reactor.class);
    MediaDriver driver;
    Aeron aeron;
    private final String localUri;
    private final int streamId;
    private final String lookupUrl;
    private final String reactorDescription;
    private final Subscription subscription;
    private final Map<String, Publication> publications = new HashMap<>();
    private final MessagePool messagePool = new MessagePool();

    private final Map<Class<? extends ReusableMessage>, List<Consumer<ReusableMessage>>> reactions = new HashMap<>();
    private final ExecutorService messageHandler;
    private final Consumer<String> logConsumer = (s) -> log.info("Media Driver check: {}", s);
    private final IdleStrategy reactorIdleStrategy;
    private final ICodec codec;
    /**
     * @param name      name of the reactor. used to look up
     * @param endpoint  the local endpoint
     * @param lookupUrl url used for looking up other reactors
     * @param codec optional codec class
     * @throws URISyntaxException
     * @throws IOException
     * @throws InterruptedException
     */
    public Reactor(String name, String endpoint, String lookupUrl, boolean inProcess, ICodec codec) throws URISyntaxException, IOException, InterruptedException {
        this.reactorDescription = "Reactor(" + name + "," + endpoint +")";
        this.lookupUrl = lookupUrl;
        if (!verifyMediaDriver()) {
            throw new IllegalStateException("Could not get or start a media driver");
        }
        messageHandler = inProcess ? null : Executors.newSingleThreadExecutor();
        log.info("Media driver is running at {}", MediaDriver.Context.getAeronDirectoryName());
        aeron = Aeron.connect();
        this.codec = codec == null ? new Codec() :  codec;

        // register name and local host name / IP
        // query name to get unique connection details
        // build aeron url
        ReactorInfo info = register(name, endpoint);
        localUri = buildUri(endpoint);
        streamId = info.getStreamid();
        subscription = aeron.addSubscription(localUri, streamId);

        reactorIdleStrategy = aeron.context().idleStrategy();
        // start agent  TODO: setup errorCounter
        final var runner = new AgentRunner(reactorIdleStrategy, this::errorHandler,null, this);
        AgentRunner.startOnThread(runner);
    }

    private void errorHandler(Throwable throwable) {
        log.error("Caught exception: ", throwable);
    }

    private String buildUri(String endpoint) {
        return "aeron:udp?endpoint=" + endpoint;
    }

    public ReactorInfo register(String name, String endpoint)  {
        try (HttpClient httpClient = HttpClient.newHttpClient()) {
            ObjectMapper objectMapper = new ObjectMapper();
            // TODO: move DTOs to separate module
            String body = objectMapper.writeValueAsString(new ReactorInfo(name, endpoint, 0));
            HttpRequest update = HttpRequest.newBuilder()
                    .uri(new URI(lookupUrl + "/lookup"))  //TODO:  make path a constant
                    .header("Content-Type", "application/json")
                    .header("Accept", "application/json")
                    .POST(ofString(body))
                    .build();
            HttpResponse<String> response = httpClient.send(update, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("lookup endpoint returned " + response.statusCode());
            }
            String jsonData = response.body();

            return objectMapper.readValue(jsonData,
                    new TypeReference<ReactorInfo>() {});

        }
        catch (Exception e) {
            log.error("Registration of reactor failed {} ", e.getMessage());
        }
        throw new IllegalStateException("Failed to set up reactor");
    }

    public ReactorInfo lookupReactor(String name) {
        try(HttpClient httpClient = HttpClient.newHttpClient()) {
            HttpRequest query = HttpRequest.newBuilder()
                    .uri(new URI(lookupUrl + "/lookup/" + name))
                    .header("Accept", "application/json")
                    .GET()
                    .build();
            HttpResponse<String> response = httpClient.send(query, HttpResponse.BodyHandlers.ofString());
            if (response.statusCode() != HttpURLConnection.HTTP_OK) {
                throw new IllegalStateException("lookup endpoint returned " + response.statusCode());
            }
            String jsonData = response.body();
            ObjectMapper objectMapper = new ObjectMapper();
            return objectMapper.readValue(jsonData,
                    new TypeReference<ReactorInfo>() {});

        } catch (Exception e) {
            throw new IllegalStateException("Lookup failed:", e);
        }
    }



    public boolean verifyMediaDriver() {
        // get the directory name
        long timeout = MediaDriver.Context.DRIVER_TIMEOUT_MS;
        String aeronDirName = MediaDriver.Context.getAeronDirectoryName();
        File aeronDir = new File(aeronDirName);
        log.info("Scanning for Media Driver at {}", aeronDirName);
        if (MediaDriver.Context.isDriverActive(aeronDir, timeout, logConsumer)) {
            return true;
        }
        log.warn("No Media Driver found - launching local media driver");
        if (driver != null) {
            log.warn("Media Driver instance found - replacing with new");
            driver.close();
        }
        // launch media driver with default context  // TODO: customize context
        driver = MediaDriver.launch();
        return MediaDriver.Context.isDriverActive(aeronDir, timeout, logConsumer);
    }

    private Publication getPublication(String targetReactor) {
        return publications.computeIfAbsent(targetReactor, target -> {
            ReactorInfo info = lookupReactor(target);
            String channel = buildUri(info.getEndpoint());
            return aeron.addExclusivePublication(channel, info.getStreamid());
        });
    }

    public <T extends ReusableMessage> boolean signal(T message, String targetReactor) {

        Publication publication = getPublication(targetReactor);
        BufferClaim bufferClaim = new BufferClaim();  // can be reused!

        int encodedLength = codec.encodedLength(message);
        long result;
        while ((result = publication.tryClaim(encodedLength, bufferClaim)) <= 0L) {
            reactorIdleStrategy.idle();  // TODO: add timeout
        }
        try {
            final MutableDirectBuffer buffer = bufferClaim.buffer();
            int offset = bufferClaim.offset();           // Work with buffer directly or wrap with a flyweight
            codec.encode(message, buffer, offset);
            bufferClaim.commit();
            log.info("sent message");
            return true;
        }
        catch (Exception e) {
            bufferClaim.abort();
        }

        return false;
    }


    /**
     * register a message handler for a message type
     * @param messageClass
     * @param messageConsumer
     * @param <T>
     */
    public <T extends ReusableMessage> void react(Class<? extends ReusableMessage> messageClass, Consumer<T> messageConsumer) {
        List<Consumer<ReusableMessage>> consumers = reactions.computeIfAbsent(messageClass, r -> new ArrayList<>());
        consumers.add((Consumer<ReusableMessage>) messageConsumer);
    }

    private void reactTo(ReusableMessage message) {
        List<Consumer<ReusableMessage>> consumers = reactions.get(message.getClass());
        try {
            if (consumers != null) {
                if (messageHandler == null) {
                    consumers.forEach(c -> c.accept(message));
                } else {
                    consumers.forEach(c -> messageHandler.submit(() -> c.accept(message)));
                }
            } else {
                throw new IllegalStateException("No handler for message of type " + message.getClass());
            }
        } catch (Throwable th) {
            log.error("Problem processing message {}", message, th);
        }
    }

    private MessagePool getMessagePool() {
        return messagePool;
    }

    FragmentAssembler assembler = new FragmentAssembler(new MessageHandler(this));


    private record MessageHandler(Reactor reactor) implements FragmentHandler {

        @Override
        public void onFragment(DirectBuffer buffer, int offset, int length, io.aeron.logbuffer.Header header) {
            ReusableMessage msg = reactor.getMessage(buffer, offset);
            if (msg != null) {
                reactor.reactTo(msg);
                reactor.getMessagePool().reuseMessage(msg);
            }
        }
    }


    public ReusableMessage getMessage(DirectBuffer buffer, int offset) {
        ReusableMessage message = codec.decode(buffer, offset, messagePool);
        return message;
    }


    @Override
    public int doWork() throws Exception {
        return subscription.poll(assembler, 100);
    }

    @Override
    public String roleName() {
        return reactorDescription;
    }
}
