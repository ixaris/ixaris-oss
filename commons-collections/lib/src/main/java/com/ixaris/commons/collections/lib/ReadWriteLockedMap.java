package com.ixaris.commons.collections.lib;

import com.ixaris.commons.misc.lib.lock.LockUtil;
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
        return LockUtil.exec(lock.readLock(), () -> map.get(key));
    }
    
    public final V put(final K key, final V value) {
        return LockUtil.exec(lock.writeLock(), () -> map.put(key, value));
    }
    
    public final V remove(final K key) {
        return LockUtil.exec(lock.writeLock(), () -> map.remove(key));
    }
    
}
