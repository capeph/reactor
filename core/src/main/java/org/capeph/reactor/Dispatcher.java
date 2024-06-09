package org.capeph.reactor;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.function.Consumer;

public class Dispatcher implements Consumer<ReusableMessage> {

    private final Logger log = LogManager.getLogger(Dispatcher.class);
    // TODO: better map.. Agrona?
    private final Map<Class<? extends ReusableMessage>, List<Consumer<ReusableMessage>>> reactions = new HashMap<>();
    private final ExecutorService messageHandler;

    Dispatcher(boolean inProcess) {
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
        try {
            if (consumers != null) {
                consumers.forEach(consumer -> {
                    if (messageHandler == null) {
                        consumer.accept(message);
                    } else {
                        messageHandler.submit(() -> consumer.accept(message));
                    }
                });
            } else {
                throw new IllegalStateException("No handler for message of type " + message.getClass());
            }
        } catch (Throwable th) {
            log.error("Problem processing message {}", message, th);
        }
    }
}
