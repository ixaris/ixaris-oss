package com.ixaris.commons.collections.lib;

import static com.ixaris.commons.misc.lib.object.Tuple.tuple;

import com.ixaris.commons.misc.lib.lock.LockUtil;
import com.ixaris.commons.misc.lib.object.Tuple2;
import it.unimi.dsi.fastutil.longs.Long2ObjectMap;
import it.unimi.dsi.fastutil.longs.Long2ObjectOpenHashMap;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class AbstractLazyReadWriteLockedLongMap<V, T, GET> extends ReadWriteLockedLongMap<V> {
    
    private final Long2ObjectMap<V> map = new Long2ObjectOpenHashMap<>();
    private final ReadWriteLock lock = new ReentrantReadWriteLock();
    
    public final Tuple2<V, GET> get(final long key, final T t) {
        return LockUtil.exec(lock.readLock(), () -> {
            final V value = map.get(key);
            if (value != null) {
                return tuple(value, existing(value, t));
            } else {
                return null;
            }
        });
    }
    
    public final Tuple2<V, GET> getOrCreate(final long key, final T t) {
        final Tuple2<V, GET> tuple = get(key, t);
        if (tuple != null) {
            return tuple;
        } else {
            return LockUtil.exec(lock.writeLock(), () -> {
                final V value = map.get(key);
                if (value != null) {
                    return tuple(value, existing(value, t));
                } else {
                    final Tuple2<V, GET> tt = create(t);
                    map.put(key, tt.get1());
                    return tt;
                }
            });
        }
    }
    
    public final boolean tryRemove(final long key) {
        return LockUtil.exec(lock.writeLock(), () -> {
            final V removed = map.remove(key);
            if (!shouldRemove(removed)) {
                map.put(key, removed);
                return false;
            } else {
                return true;
            }
        });
    }
    
    protected abstract Tuple2<V, GET> create(T t);
    
    protected GET existing(final V value, T t) {
        return null;
    }
    
    protected boolean shouldRemove(final V removed) {
        return true;
    }
    
}
