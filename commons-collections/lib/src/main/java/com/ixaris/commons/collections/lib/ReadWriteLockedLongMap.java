package com.ixaris.commons.collections.lib;

import com.ixaris.commons.misc.lib.lock.LockUtil;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public class ReadWriteLockedLongMap<V> {
    
    private final Long2ObjectMap<V> map = new Long2ObjectOpenHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    public final V get(final long key) {
        return LockUtil.exec(lock.readLock(), () -> map.get(key));
    }
    
    public final V put(final long key, final V value) {
        return LockUtil.exec(lock.writeLock(), () -> map.put(key, value));
    }
    
    public final V remove(final long key) {
        return LockUtil.exec(lock.writeLock(), () -> map.remove(key));
    }
    
}
