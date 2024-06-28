package org.capeph.pool;


import org.junit.jupiter.api.Test;

import java.util.ArrayDeque;
import java.util.Deque;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;

class GrowingObjectPoolTest {



    @Test
    public void testObjectPool() {
        AtomicInteger counter = new AtomicInteger(0);
        GrowingObjectPool<String> stringPool = new GrowingObjectPool<>(
                () -> String.format("%d", counter.getAndIncrement()), 2, 3);
        Deque<String> fifo = new ArrayDeque<>();
        assertEquals(4, stringPool.size());
        assertEquals(4, stringPool.capacity());
        for (int i = 0; i < 4; i++) {
            String obj = stringPool.get();
            fifo.offer(obj);
            assertEquals(String.format("%d", i), obj);
        }
        for (int i = 0; i < 4; i++) {
            stringPool.put(fifo.poll());
        }
        assertEquals(4, stringPool.size());
        assertEquals(8, stringPool.capacity());
        for (int i = 0; i < 8; i++) {
            String obj = stringPool.get();
            fifo.offer(obj);
            assertEquals(String.format("%d", i), obj);
        }
        assertEquals(8, stringPool.size());
        assertEquals(8, stringPool.capacity());
    }
}