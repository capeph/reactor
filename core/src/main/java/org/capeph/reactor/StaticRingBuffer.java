package org.capeph.reactor;

public class StaticRingBuffer<T> {

    private static final boolean CLEAR_VALUES_ON_READ = true;

    private final int capacity;
    private final int mask;
    private final T[] store;
    private volatile int write, read;

    @SuppressWarnings("unchecked")
    public StaticRingBuffer(int sizeFactor) {
        this.capacity = 1 << sizeFactor;
        this.mask = capacity - 1;
        this.store = (T[]) new Object[this.capacity];
        this.read = 0;
        this.write = 0;
    }

    public boolean offer(T element) {
        if (isFull()) {
            return false;
        }
        store[write & mask] = element;
        write++;
        return true;
    }

    public T poll() {

        if (isEmpty()) {
            return null;
        }

        T nextValue = store[read & mask];

        if (CLEAR_VALUES_ON_READ) {
            if (nextValue == null) {
                throw new RuntimeException("Reading unset value");
            }
            store[read & mask] = null;
        }
        read++;
        return nextValue;
    }

    public int size() {
        return write - read;
    }

    public boolean isEmpty() {
        return write <= read;
    }

    public boolean isFull() {
        return size() >= capacity;
    }


}