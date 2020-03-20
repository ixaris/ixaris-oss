package com.ixaris.commons.multitenancy.lib.collection;

import java.util.Collection;
import java.util.Map;
import java.util.Set;
import java.util.function.Supplier;

import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.lib.object.LazyMultiTenantObject;

/**
 * See {@link LazyMultiTenantObject}
 *
 * @author <a href="mailto:matthias.portelli@ixaris.com">matthias.portelli</a>
 */
public final class MultiTenantMap<K, V, M extends Map<K, V>> extends LazyMultiTenantObject<M> implements Map<K, V> {
    
    public MultiTenantMap(final MultiTenancy multiTenancy, final Supplier<M> newInstanceSupplier) {
        super(multiTenancy, newInstanceSupplier);
    }
    
    @Override
    public int size() {
        return get().size();
    }
    
    @Override
    public boolean isEmpty() {
        return get().isEmpty();
    }
    
    @Override
    public boolean containsKey(final Object key) {
        return get().containsKey(key);
    }
    
    @Override
    public boolean containsValue(final Object value) {
        return get().containsValue(value);
    }
    
    @Override
    public V get(final Object key) {
        return get().get(key);
    }
    
    @Override
    public V put(final K key, final V value) {
        return get().put(key, value);
    }
    
    @Override
    public V remove(final Object key) {
        return get().remove(key);
    }
    
    @Override
    public void putAll(Map<? extends K, ? extends V> m) {
        get().putAll(m);
    }
    
    @Override
    public void clear() {
        get().clear();
    }
    
    @Override
    public Set<K> keySet() {
        return get().keySet();
    }
    
    @Override
    public Collection<V> values() {
        return get().values();
    }
    
    @Override
    public Set<Entry<K, V>> entrySet() {
        return get().entrySet();
    }
    
}
