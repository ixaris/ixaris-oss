package com.ixaris.commons.collections.lib;

import java.util.Map;
import java.util.Objects;

import com.ixaris.commons.misc.lib.lock.LockUtil;

public abstract class AbstractLazyLockedMap<K, V, T> extends AbstractLockedMap<K, V> {
    
    public AbstractLazyLockedMap() {
        super();
    }
    
    public AbstractLazyLockedMap(final Map<K, V> map) {
        super(map);
    }
    
    public final V getOrCreate(final K key, final T t) {
        return LockUtil.readMaybeWrite(lock, true, () -> map.get(key), Objects::nonNull, () -> map.computeIfAbsent(key, k -> create(t)));
    }
    
    public final boolean tryRemove(final K key) {
        return LockUtil.write(lock, () -> {
            final V removed = map.remove(key);
            if ((removed != null) && !shouldRemove(removed)) {
                map.put(key, removed);
                return false;
            } else {
                return true;
            }
        });
    }
    
    protected abstract V create(T t);
    
    protected boolean shouldRemove(final V removed) {
        return true;
    }
    
}
