/*
 * Copyright 2024 Peter Danielsson
 */
package org.capeph.reactor;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.agrona.collections.Object2ObjectHashMap;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

public class MessagePool {

    private final int initialPoolSize = 12;

    private final Logger log = LogManager.getLogger(MessagePool.class);
    private final Map<Class<? extends ReusableMessage>, ObjectPool<? extends ReusableMessage>> templates = new Object2ObjectHashMap<>();


    public ReusableMessage getMessageTemplate(Class<? extends ReusableMessage> msgClazz) {
        ObjectPool<? extends ReusableMessage> pool =
                templates.computeIfAbsent(msgClazz,  m -> new ObjectPool<>(m, initialPoolSize));
        return pool.get();
    }

    public void reuseMessage(ReusableMessage message) {
        message.clear();
        ObjectPool<? extends ReusableMessage> pool =
                templates.computeIfAbsent(message.getClass(),  m -> new ObjectPool<>(m, 1));
        pool.put(message);
    }

    public void addMessagePool(Class<? extends ReusableMessage> msgClazz) {
        templates.computeIfAbsent(msgClazz,  m -> new ObjectPool<>(m, initialPoolSize));
    }
}
