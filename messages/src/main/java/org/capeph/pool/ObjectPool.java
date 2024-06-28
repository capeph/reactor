package org.capeph.pool;

public interface ObjectPool<T> {
    T get();

    void  put(T obj);
}
