package org.capeph.pool;

import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;

import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

public class SmoothObjectPool<T> implements ObjectPool<T> {

    private static final boolean CHECK_ACCESS = false;
    private static final int RETRIES = 2;
    private final Supplier<T> factory;
    private volatile Store<T> readStore;
    private volatile Store<T> writeStore;
    private volatile int elements;

    private final Logger log = LogManager.getLogger(SmoothObjectPool.class);
    private final int maxSize;


    private static class Store<T> {
        private final T[] underlying;
        private final AtomicInteger read = new AtomicInteger(0);
        private final AtomicInteger write = new AtomicInteger(0);
        private final int mask;

        @SuppressWarnings("unchecked")
        public Store(int size) {
            mask = size -1;
            underlying = (T[]) new Object[size];
        }

        public int size() {
            return underlying.length;
        }

        public  T get() {
            if (read.get() >= write.get()) {
                return null;
            }
            int offset = read.getAndIncrement() & mask;
            T result = underlying[offset];

            if (CHECK_ACCESS){  // can be removed for better performance / less safety
                if (result == null) {
                    throw new IndexOutOfBoundsException("Trying to read non-initialized value");
                }
                underlying[offset] = null;
            }

            return result;
        }

        private void put(T obj) {
            int offset = write.get() & mask;

            if (CHECK_ACCESS){  // can be removed for better performance
                if (underlying[offset] != null) {
                    throw new IndexOutOfBoundsException("Trying to overwite existing value");
                }
            }

            underlying[offset] = obj;
            write.getAndIncrement();
        }

    }

    public SmoothObjectPool(Supplier<T> factory, int sizeFactor, int maxSizeFactor) {
        int size = 1 << sizeFactor;
        maxSize = 1 << maxSizeFactor;
        readStore = new Store<>(size);
        writeStore = readStore;
        this.factory = factory;
        try {
            for(int i = 0 ; i < size; i++) {
                writeStore.put(factory.get());
            }
            elements = size;
            log.info("Pool initialized, initial size: {}  max size: {}", size, maxSize);
        } catch (Exception e) {
            throw new IllegalArgumentException("Provided class can't be used in object pool: ", e);
        }
    }


    private void grow(int newSize) {
        synchronized (factory) {
            if (writeStore.size() < newSize) {
                writeStore = new Store<>(newSize);
                log.info("Growing pool to {}", newSize);
            }
        }
    }

    @Override
    public T get() {
        T result = readStore.get();
        while (result == null) {
            if (writeStore != readStore) {
                readStore = writeStore;
                return get();
            }
            int writeSize = writeStore.size();
            if (elements == writeSize && writeSize < maxSize) {
                grow(writeSize * 2);
            }
            Thread.yield(); // TODO: better idle strategy
            result = readStore.get();
            if (result == null && elements < writeSize) { // room to grow?
                elements++;
                return factory.get();
            }
        }
        return result;
    }


    public T get2() {
        T result = null;
        int retries = RETRIES;
        while (result == null) {
            result = readStore.get();
            if (result == null && writeStore != readStore) {
                readStore = writeStore;
                result = get();
            }
            if (retries == 0) {
                int writeSize = writeStore.size();
                if (elements == writeSize && writeSize < maxSize) {
                    grow(writeSize * 2);
                }
                if (elements < writeSize) { // room to grow?
                    elements++;
                    result = factory.get();
                }
            } else {
                retries--;
                Thread.yield();
            }
        }
        return result;
    }


    @Override
    public void put(T obj) {
        writeStore.put(obj);
    }
}
