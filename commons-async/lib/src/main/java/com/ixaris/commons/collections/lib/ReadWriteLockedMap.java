package com.ixaris.commons.collections.lib;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReadWriteLockedMap<K, V> {
    
    private final Map<K, V> map;
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    public ReadWriteLockedMap() {
        this(new HashMap<>());
    }
    
    public ReadWriteLockedMap(final Map<K, V> map) {
        this.map = map;
    }
    
    public final V get(final K key) {
        lock.readLock().lock();
        try {
            final V value = map.get(key);
            if (value != null) {
                return value;
            }
        } finally {
            lock.readLock().unlock();
        }
        
        return null;
    }
    
    public final V put(final K key, final V value) {
        lock.writeLock().lock();
        try {
            return map.put(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public final V remove(final K key) {
        lock.writeLock().lock();
        try {
            return map.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
}
