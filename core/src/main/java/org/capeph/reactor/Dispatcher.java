package org.capeph.reactor;

import org.agrona.collections.Object2ObjectHashMap;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class Dispatcher implements Consumer<ReusableMessage> {

    private final Map<Class<? extends ReusableMessage>, List<Consumer<ReusableMessage>>> reactions = new Object2ObjectHashMap<>();
    private final ExecutorService messageHandler;

    public Dispatcher(boolean inProcess) {
        this.messageHandler = inProcess ? null : Executors.newSingleThreadExecutor();
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

    @Override
    public void accept(ReusableMessage message) {
        List<Consumer<ReusableMessage>> consumers = reactions.get(message.getClass());
       if (consumers != null) {
           consumers.forEach(consumer -> {
               dispatchTask(() -> consumer.accept(message));
           });
       } else {
           throw new IllegalStateException("No handler for message of type " + message.getClass());
       }
    }

    public void dispatchTask(Runnable task) {
        if (messageHandler == null) {
            task.run();
        } else {
            // TODO: this breaks the contract that handlers should be executed in order
            messageHandler.submit(task);
        }

    }

    public void stop() {
        if (messageHandler != null) {
            messageHandler.shutdownNow();
        }
    }
}
