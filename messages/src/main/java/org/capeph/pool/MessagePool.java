/*
 * Copyright 2024 Peter Danielsson
 */
package org.capeph.pool;

import org.agrona.collections.Object2ObjectHashMap;
import org.capeph.config.Config;

import java.util.Map;
import java.util.function.Consumer;
import java.util.function.Supplier;

public class MessagePool {

    private final Map<Class<? extends Object>, ObjectPool<Object>> templates = new Object2ObjectHashMap<>();
    private final Consumer<Object> clearFunc;

    public MessagePool(Consumer<Object> clearFunc) {
        this.clearFunc = clearFunc;
    }

    private ObjectPool<Object> createPool(final Class<? extends Object> msgClazz) {
        Supplier<Object> factory = () -> {
            try {
                return msgClazz.getConstructor().newInstance();
            } catch (Exception e) {
                throw new RuntimeException(e); // TODO: better exceptions
            }
        };
        return new SmoothObjectPool<>(factory, Config.minPoolSize.get(), Config.maxPoolSize.get());
    }

    @SuppressWarnings("unchecked")
    public <K> K getMessageTemplate(Class<K> msgClazz) {
//        ObjectPool<K> pool = // TODO: error if clazz not supported
//                (ObjectPool<K>) templates.computeIfAbsent(msgClazz, this::createPool);
        ObjectPool<K> pool = (ObjectPool<K>) templates.get(msgClazz);
        return pool.get();
    }

    public void reuseMessage(Object message) {
        clearFunc.accept(message);
        ObjectPool<Object> pool = templates.get(message.getClass());
        pool.put(message);
    }

    public void addMessagePool(Class<?> msgClazz) {
        templates.computeIfAbsent(msgClazz,  this::createPool);
    }
}
