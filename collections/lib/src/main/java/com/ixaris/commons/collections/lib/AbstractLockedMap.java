package com.ixaris.commons.collections.lib;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.concurrent.locks.StampedLock;

import com.ixaris.commons.misc.lib.lock.LockUtil;

public abstract class AbstractLockedMap<K, V> {
    
    protected final Map<K, V> map;
    protected final StampedLock lock = new StampedLock();
    
    public AbstractLockedMap() {
        this(new HashMap<>());
    }
    
    public AbstractLockedMap(final Map<K, V> map) {
        this.map = map;
    }
    
    public final V get(final K key) {
        return LockUtil.read(lock, true, () -> map.get(key));
    }
    
    public final V put(final K key, final V value) {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        return LockUtil.write(lock, () -> map.put(key, value));
    }
    
    public final V remove(final K key) {
        return LockUtil.write(lock, () -> map.remove(key));
    }
    
}
