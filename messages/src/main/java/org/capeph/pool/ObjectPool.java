package org.capeph.pool;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.capeph.reactor.ReusableMessage;

import java.lang.reflect.Constructor;
import java.util.concurrent.atomic.AtomicInteger;

public class ObjectPool<T> {

    private final Constructor<T> constructor;
    private volatile int mask;
    private volatile T[] store;
    private final AtomicInteger read;
    private final AtomicInteger write;
    private final AtomicInteger elements;

    private final Logger log = LogManager.getLogger(ObjectPool.class);
    private final int maxSize;

    public ObjectPool(Class<T> clazz, int sizeFactor) {
        this(clazz, sizeFactor, Math.min(24, sizeFactor*2));
    }

    @SuppressWarnings("unchecked")
    public ObjectPool(Class<T> clazz, int sizeFactor, int maxSizeFactor) {
        int size = 1 << sizeFactor;
        maxSize = 1 << maxSizeFactor;
        mask = size - 1;
        store = (T[]) new Object[size];
        try {
            constructor = clazz.getConstructor();
            for(int i = 0 ; i < size; i++) {
                store[i] = constructor.newInstance();
            }
            elements = new AtomicInteger(size);
            read = new AtomicInteger(0);
            write = new AtomicInteger(size);
            log.info("Pool initialized: read {}  write {}", read.get(), write.get());
        } catch (Exception e) {
            throw new IllegalArgumentException("Provided class can't be used in object pool: ", e);
        }
    }

    public T get() {
        while (emptyPool()) {
            // backoff to allow in flight objects to trickle in before allocating
            // with real life loads the backoff should move down
            if (elements.get() < store.length && elements.get() < maxSize) {
                try {
                    if (elements.incrementAndGet() == maxSize) {
                        log.info("ObjectPool has reached limit for growing: {} elements", maxSize);
                    }
                    return constructor.newInstance();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
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

    private boolean emptyPool() {
        return read.get() >= write.get();
    }

    @SuppressWarnings("unchecked")
    public void put(ReusableMessage obj) {
        if (emptyPool() && elements.get() == store.length && store.length < maxSize) {
            log.info("Expanding pool to {}, to fit {}", store.length * 2, elements.get());
            T[] newStore = (T[]) new Object[store.length * 2];
            mask = newStore.length - 1;
            int idx = write.get() & mask;
            newStore[idx] = (T) obj;
            store = newStore;
        }
        else {
            int idx = write.get() & mask;
            store[idx] = (T) obj;
        }
        write.getAndIncrement();
    }
}
