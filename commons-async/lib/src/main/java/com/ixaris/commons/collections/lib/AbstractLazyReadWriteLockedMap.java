package com.ixaris.commons.collections.lib;

import static com.ixaris.commons.misc.lib.object.Tuple.tuple;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import com.ixaris.commons.misc.lib.object.Tuple2;

public abstract class AbstractLazyReadWriteLockedMap<K, V, T, GET> extends ReadWriteLockedMap<K, V> {
    
    private final Map<K, V> map;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    public AbstractLazyReadWriteLockedMap() {
        this(new HashMap<>());
    }
    
    public AbstractLazyReadWriteLockedMap(final Map<K, V> map) {
        this.map = map;
    }
    
    public final Tuple2<V, GET> get(final K key, final T t) {
        lock.readLock().lock();
        try {
            final V value = map.get(key);
            if (value != null) {
                return tuple(value, existing(value, t));
            }
        } finally {
            lock.readLock().unlock();
        }
        
        return null;
    }
    
    public final Tuple2<V, GET> getOrCreate(final K key, final T t) {
        Tuple2<V, GET> tuple = get(key, t);
        if (tuple != null) {
            return tuple;
        }
        
        lock.writeLock().lock();
        try {
            V value = map.get(key);
            if (value != null) {
                tuple = tuple(value, existing(value, t));
            } else {
                tuple = create(t);
                map.put(key, tuple.get1());
            }
            return tuple;
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public final boolean tryRemove(final K key) {
        lock.writeLock().lock();
        try {
            final V removed = map.remove(key);
            if (!shouldRemove(removed)) {
                map.put(key, removed);
                return false;
            } else {
                return true;
            }
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    protected abstract Tuple2<V, GET> create(T t);
    
    protected GET existing(final V value, T t) {
        return null;
    }
    
    protected boolean shouldRemove(final V removed) {
        return true;
    }
    
}
