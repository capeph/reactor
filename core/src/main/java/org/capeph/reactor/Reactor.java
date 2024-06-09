/*
 * Copyright 2024 Peter Danielsson
 */
package org.capeph.reactor;


import io.aeron.Aeron;
import io.aeron.Publication;
import io.aeron.Subscription;
import io.aeron.driver.MediaDriver;
import io.aeron.logbuffer.BufferClaim;
import org.agrona.MutableDirectBuffer;
import org.agrona.concurrent.Agent;
import org.agrona.concurrent.AgentRunner;
import org.agrona.concurrent.IdleStrategy;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.capeph.lookup.dto.ReactorInfo;
import org.capeph.messages.codec.Codec;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.function.Consumer;

public class Reactor {

    private final Logger log = LogManager.getLogger(Reactor.class);
    MediaDriver driver;
    Aeron aeron;
    private final Map<String, Publication> publications = new HashMap<>();
    private final MessagePool messagePool = new MessagePool();
    private final Dispatcher dispatcher;
    private final Consumer<String> logConsumer = (s) -> log.info("Media Driver check: {}", s);
    private final IdleStrategy reactorIdleStrategy;
    private final ICodec codec;
    private final Registrar registrar;

    /**
     * @param name      name of the reactor. used to look up
     * @param endpoint  the local endpoint
     * @param lookupUrl url used for looking up other reactors
     * @param overrideCodec optional codec class
     */
    public Reactor(String name, String endpoint, String lookupUrl, boolean inProcess, ICodec overrideCodec)  {
        registrar = new Registrar(lookupUrl);
        if (!verifyMediaDriver()) {
            throw new IllegalStateException("Could not get or start a media driver");
        }
        log.info("Media driver is running at {}", MediaDriver.Context.getAeronDirectoryName());

        dispatcher = new Dispatcher(inProcess);
        this.codec = overrideCodec == null ? new Codec() :  overrideCodec;
        aeron = Aeron.connect();

        ReactorInfo info = registrar.register(name, endpoint);
        Subscription subscription = aeron.addSubscription(buildUri(endpoint), info.getStreamid());

        reactorIdleStrategy = aeron.context().idleStrategy();
        // start agent  TODO: setup errorCounter
        String description = "Reactor(" + name + "," + endpoint + ")";
        Agent agent = new ReactorAgent(subscription, codec, messagePool, dispatcher, description);
        final var runner = new AgentRunner(reactorIdleStrategy, this::errorHandler,null, agent);
        AgentRunner.startOnThread(runner);
    }

    private void errorHandler(Throwable throwable) {
        log.error("Caught exception: ", throwable);
    }

    private String buildUri(String endpoint) {
        return "aeron:udp?endpoint=" + endpoint;
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
            ReactorInfo info = registrar.lookupReactor(target);
            String channel = buildUri(info.getEndpoint());
            return aeron.addExclusivePublication(channel, info.getStreamid());
        });
    }

    public  boolean signal(ReusableMessage message, String targetReactor) {

        Publication publication = getPublication(targetReactor);
        BufferClaim bufferClaim = new BufferClaim();  // can be reused!

        int encodedLength = codec.encodedLength(message);
        while (publication.tryClaim(encodedLength, bufferClaim) <= 0L) {
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
            log.error("Sending message threw exception ", e);
            bufferClaim.abort();
        }
        return false;
    }


    /**
     * register a message handler for a message type
     * @param messageClass class of message
     * @param messageConsumer handler for message type
     */
    public void react(Class<? extends ReusableMessage> messageClass, Consumer<ReusableMessage> messageConsumer) {
        messagePool.addMessagePool(messageClass);
        dispatcher.addMessageHandler(messageClass, messageConsumer);
    }






}
