package org.capeph.pool;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class GrowingObjectPool<T> implements ObjectPool<T> {

    private final Supplier<T> factory;
    private volatile int mask;
    private volatile T[] store;
    private final AtomicInteger read;
    private final AtomicInteger write;
    private volatile int elements;

    private final Logger log = LogManager.getLogger(GrowingObjectPool.class);
    private final int maxSize;

    int size() {
        return elements;
    }

    int capacity() {
        return store.length;
    }

    @SuppressWarnings("unchecked")
    public GrowingObjectPool(Supplier<T> factory, int sizeFactor, int maxSizeFactor) {
        int size = 1 << sizeFactor;
        maxSize = 1 << maxSizeFactor;
        mask = size - 1;
        store = (T[]) new Object[size];
        this.factory = factory;
        try {
            for(int i = 0 ; i < size; i++) {
                store[i] = factory.get();
            }
            elements = size;
            read = new AtomicInteger(0);
            write = new AtomicInteger(size);
            log.info("Pool initialized: read {}  write {}", read.get(), write.get());
        } catch (Exception e) {
            throw new IllegalArgumentException("Provided class can't be used in object pool: ", e);
        }
    }

    public boolean emptyPool() {
        return read.get() >= write.get();
    }

    @Override
    public T get() {
        while (emptyPool()) {
            // backoff to allow in flight objects to trickle in before allocating
            // with real life loads the backoff should move down
            if (elements < store.length && elements < maxSize) {
                if (++elements == maxSize) {
                    log.info("ObjectPool has reached limit for growing: {} elements", maxSize);
                }
                return factory.get();
            }
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

    @Override
    @SuppressWarnings("unchecked")
    public void put(T obj) {
        if (emptyPool() && elements == store.length && store.length < maxSize) {
            log.info("Expanding pool to {}, to fit {}", store.length * 2, elements);
            T[] newStore = (T[]) new Object[store.length * 2];
            mask = newStore.length - 1;
            int idx = write.get() & mask;
            newStore[idx] = (T) obj;
            store = newStore;
        }
        else {
            int idx = write.get() & mask;
            store[idx] = obj;
        }
        write.getAndIncrement();
    }
}
