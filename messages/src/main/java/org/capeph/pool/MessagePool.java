/*
 * Copyright 2024 Peter Danielsson
 */
package org.capeph.pool;

import org.agrona.collections.Object2ObjectHashMap;
import org.capeph.config.Config;
import org.capeph.reactor.ReusableMessage;

import java.util.Map;
import java.util.function.Supplier;

public class MessagePool {

    private final Map<Class<? extends ReusableMessage>, ObjectPool<ReusableMessage>> templates = new Object2ObjectHashMap<>();


    private ObjectPool<ReusableMessage> createPool(final Class<? extends ReusableMessage> msgClazz) {
        Supplier<ReusableMessage> factory = () -> {
            try {
                return msgClazz.getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e); // TODO: better exceptions
            }
        };
//        return new GrowingObjectPool<>(factory, Config.minPoolSize.get(), Config.maxPoolSize.get());
        return new SmoothObjectPool<>(factory, Config.minPoolSize.get(), Config.maxPoolSize.get());
    }

    public ReusableMessage getMessageTemplate(Class<? extends ReusableMessage> msgClazz) {
        ObjectPool<? extends ReusableMessage> pool =
                templates.computeIfAbsent(msgClazz, this::createPool);
        return pool.get();
    }

    public void reuseMessage(ReusableMessage message) {
        message.clear();
        ObjectPool<ReusableMessage> pool = templates.computeIfAbsent(message.getClass(),  this::createPool);
        pool.put(message);
    }

    public void addMessagePool(Class<? extends ReusableMessage> msgClazz) {
        templates.computeIfAbsent(msgClazz,  this::createPool);
    }
}
