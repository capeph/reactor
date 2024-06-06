/*
 * Copyright 2024 Peter Danielsson
 */
package org.capeph.reactor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class MessagePool {

    private final int initialPoolSize = 8;

    private final Map<Class<? extends ReusableMessage>, List<ReusableMessage>> templates = new HashMap<>();

    private List<ReusableMessage> createMessagePool(Class<? extends ReusableMessage> msgClazz) {
        List<ReusableMessage> pool = new ArrayList<>();
        try {
            for (int i = 0; i < initialPoolSize; i++) {
                pool.add(msgClazz.getConstructor().newInstance());
            }
        } catch (Exception e) {
            throw new IllegalArgumentException("Failed to instantiate new message", e);
        }
        return pool;
    }

    public void addMessagePool(Class<? extends ReusableMessage> msgClazz) {
        templates.computeIfAbsent(msgClazz, this::createMessagePool);
    }

    public ReusableMessage getMessageTemplate(Class<? extends ReusableMessage> msgClazz) {
        List<ReusableMessage> pool = templates.computeIfAbsent(msgClazz, this::createMessagePool);
        if (pool.isEmpty()) {
            try {
                return msgClazz.getConstructor().newInstance();
            } catch (Exception e) {
                throw new IllegalStateException("Failed to instantiate new message ", e);
            }
        }
        else {
            return pool.removeLast();
        }
    }

    public void reuseMessage(ReusableMessage message) {
        message.clear();
        List<ReusableMessage> pool = templates.computeIfAbsent(message.getClass(), this::createMessagePool);
        pool.add(message);
    }

}
