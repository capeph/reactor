/*
 * Copyright 2024 Peter Danielsson
 */
package org.capeph.pool;

import java.util.Map;

import org.agrona.collections.Object2ObjectHashMap;
import org.capeph.config.Config;
import org.capeph.config.Loader;
import org.capeph.reactor.ReusableMessage;

public class MessagePool {

    private final Map<Class<? extends ReusableMessage>, ObjectPool<? extends ReusableMessage>> templates = new Object2ObjectHashMap<>();

    private ObjectPool<? extends ReusableMessage> createPool(Class<? extends ReusableMessage> msgClazz) {
        return new ObjectPool<>(msgClazz, Config.minPoolSize.get(), Config.maxPoolSize.get());
    }

    public ReusableMessage getMessageTemplate(Class<? extends ReusableMessage> msgClazz) {
        ObjectPool<? extends ReusableMessage> pool =
                templates.computeIfAbsent(msgClazz, this::createPool);
        return pool.get();
    }

    public void reuseMessage(ReusableMessage message) {
        message.clear();
        ObjectPool<? extends ReusableMessage> pool =
                templates.computeIfAbsent(message.getClass(),  m -> new ObjectPool<>(m, 1));
        pool.put(message);
    }

    public void addMessagePool(Class<? extends ReusableMessage> msgClazz) {
        templates.computeIfAbsent(msgClazz,  this::createPool);
    }
}
