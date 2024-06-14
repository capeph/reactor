package org.capeph.pool;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.capeph.reactor.ReusableMessage;

import java.util.concurrent.atomic.AtomicInteger;

public class StaticObjectPool<T> {

    private final int mask;
    private final T[] store;
    private final AtomicInteger read;
    private final AtomicInteger write;

    private final Logger log = LogManager.getLogger(StaticObjectPool.class);

    @SuppressWarnings("unchecked")
    public StaticObjectPool(Class<T> clazz, int sizeFactor) {
        int size = 1 << sizeFactor;
        mask = size - 1;
        store = (T[]) new Object[size];
        try {
            for(int i = 0 ; i < size; i++) {
                store[i] = clazz.getConstructor().newInstance();
            }
            read = new AtomicInteger(0);
            write = new AtomicInteger(size);
            log.info("Pool initialized: read {}  write {}", read.get(), write.get());
        } catch (Exception e) {
            throw new IllegalArgumentException("Provided class can't be used in object pool: ", e);
        }
    }

    public T get() {
        while (emptyPool()) {
            Thread.yield();   // TODO: better backoff strategy?
        }
        int idx = read.getAndIncrement() & mask;
        T result = store[idx];
        // null check and setting the store to null is not needed, just kept for better visuals
        if (result == null) {
            log.error("Empty buffer!");
            throw new IllegalStateException("Trying to return non-initialized value");
        }
        store[idx] = null;
        return result;
    }

    private boolean emptyPool() {
        return read.get() >= write.get();
    }

    @SuppressWarnings("unchecked")
    public void put(ReusableMessage obj) {
        int idx = write.get() & mask;
        store[idx] = (T) obj;
        write.getAndIncrement();
    }
}
