package com.ixaris.commons.collections.lib;

import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

import gnu.trove.map.hash.TLongObjectHashMap;

public class ReadWriteLockedLongMap<V> {
    
    private final TLongObjectHashMap<V> map = new TLongObjectHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    public final V get(final long key) {
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
    
    public final V put(final long key, final V value) {
        lock.writeLock().lock();
        try {
            return map.put(key, value);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
    public final V remove(final long key) {
        lock.writeLock().lock();
        try {
            return map.remove(key);
        } finally {
            lock.writeLock().unlock();
        }
    }
    
}
