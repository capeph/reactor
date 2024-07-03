package org.capeph.reactor;

import org.agrona.collections.Object2ObjectHashMap;
import org.agrona.concurrent.*;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.capeph.config.Config;
import org.capeph.pool.MessagePool;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;


public class Dispatcher implements Consumer<ReusableMessage>, Agent {

    private final Logger log = LogManager.getLogger(Dispatcher.class);
    private final MessagePool pool;
    private final boolean inProcess;
    private final IdleStrategy waitStrategy;
    private StaticRingBuffer<ReusableMessage> ringBuffer;
    private final Map<Class<? extends ReusableMessage>, List<Consumer<ReusableMessage>>> reactions = new Object2ObjectHashMap<>();
    private AgentRunner runner;

    public Dispatcher(IdleStrategy idleStrategy, MessagePool pool, boolean inProcess) {
        this.pool = pool;
        ringBuffer = new StaticRingBuffer<>(Config.maxPoolSize.get()); // max needed buffer size
        this.inProcess = inProcess;
        this.waitStrategy = new BackoffIdleStrategy();  // waiting for the buffer to be available
        if (!inProcess) {
            runner = new AgentRunner(idleStrategy, this::errorHandler, null, this);
            AgentRunner.startOnThread(runner);
        }
    }

    private void errorHandler(Throwable throwable) {
        log.error("Caught exception ", throwable);
    }


    /**
     * register a message handler for a message type
     * @param messageClass - type of message
     * @param messageConsumer - handler for the message type
     */

    public  void addMessageHandler(Class<? extends ReusableMessage> messageClass, Consumer<ReusableMessage> messageConsumer) {
        List<Consumer<ReusableMessage>> consumers = reactions.computeIfAbsent(messageClass, r -> new ArrayList<>());
        consumers.add(messageConsumer);
    }

    private long longestWait = 0;

    @Override
    public void accept(ReusableMessage message) {
        if (inProcess) {
            process(message);
        }
        else {
            while (!ringBuffer.offer(message)) {
                waitStrategy.idle();
            }
        }
    }

    public void process(ReusableMessage message) {
        List<Consumer<ReusableMessage>> consumers = reactions.get(message.getClass());
        if (consumers != null) {
            consumers.forEach(consumer -> consumer.accept(message));
            pool.reuseMessage(message);
        } else {
            pool.reuseMessage(message);
            throw new IllegalStateException("No handler for message of type " + message.getClass());
        }
    }

    public void stop() {  // TODO: rename to close
        runner.close();
    }

    @Override
    public int doWork() throws Exception {
        ReusableMessage msg = ringBuffer.poll();
        if (msg == null) {
            return 0;
        }
        process(msg);
        return 1;
    }

    @Override
    public String roleName() {
        return "";
    }
}
