package com.ixaris.commons.collections.lib;

import com.ixaris.commons.misc.lib.object.Tuple2;
import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;

/**
 * Abstract multimap implementation
 *
 * @author brian.vella
 * @param <K> The generic type of the Key
 * @param <V> The generic type of the Value
 * @param <C> The generic type of the Collection
 */
public abstract class AbstractMultiMap<K, V, C extends Collection<V>> implements MultiMap<K, V> {
    
    private static final long serialVersionUID = 4214071197300093677L;
    
    private final Map<K, C> backingMap;
    
    protected AbstractMultiMap(final Map<K, C> backingMap) {
        this.backingMap = backingMap;
    }
    
    protected abstract C createCollection();
    
    protected abstract C createCollection(Collection<V> collection);
    
    protected C immutableCollection(final C collection) {
        return collection;
    }
    
    @Override
    public final int size() {
        return backingMap.size();
    }
    
    @Override
    public final boolean isEmpty() {
        return backingMap.isEmpty();
    }
    
    @Override
    public final boolean containsKey(final K key) {
        return backingMap.containsKey(key);
    }
    
    @Override
    public final boolean containsEntry(final K key, final V value) {
        final C collection = backingMap.get(key);
        return collection != null && collection.contains(value);
    }
    
    @Override
    public final C get(final K key) {
        final C collection = backingMap.get(key);
        return collection != null ? immutableCollection(collection) : null;
    }
    
    @Override
    public final boolean put(final K key, final V value) {
        C collection = backingMap.computeIfAbsent(key, k -> createCollection());
        return collection.add(value);
    }
    
    @Override
    public final boolean remove(final K key, final V value) {
        final C collection = backingMap.get(key);
        if (collection == null) {
            return false;
        } else if (collection.remove(value)) {
            if (collection.isEmpty()) {
                backingMap.remove(key);
            }
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    public final C remove(final K key) {
        return backingMap.remove(key);
    }
    
    @Override
    public final boolean putAll(final K key, final Collection<V> values) {
        if (values.isEmpty()) {
            return false;
        }
        
        final boolean modified;
        C collection = backingMap.get(key);
        if (collection == null) {
            collection = createCollection(values);
            backingMap.put(key, collection);
            modified = true;
        } else {
            modified = collection.addAll(values);
        }
        
        return modified;
    }
    
    @Override
    public final boolean putAll(final K key, final Iterable<? extends V> values) {
        C collection = backingMap.computeIfAbsent(key, k -> createCollection());
        boolean modified = false;
        for (V value : values) {
            modified |= collection.add(value);
        }
        
        return modified;
    }
    
    @Override
    public final boolean putAll(final MultiMap<? extends K, ? extends V> map) {
        boolean modified = false;
        for (final Entry<? extends K, ? extends Collection<? extends V>> entry : map.asMap().entrySet()) {
            modified |= putAll(entry.getKey(), entry.getValue());
        }
        return modified;
    }
    
    @Override
    public final boolean putAll(final Collection<? extends Tuple2<? extends K, ? extends V>> collection) {
        boolean modified = false;
        for (final Tuple2<? extends K, ? extends V> tuple : collection) {
            modified |= put(tuple.get1(), tuple.get2());
        }
        return modified;
    }
    
    @Override
    public final C replace(final K key, final Collection<V> values) {
        final C collection = createCollection(values);
        return backingMap.put(key, collection);
    }
    
    @Override
    public final C replace(final K key, final Iterable<? extends V> values) {
        final C collection = createCollection();
        for (V value : values) {
            collection.add(value);
        }
        return backingMap.put(key, collection);
    }
    
    @Override
    public final boolean removeAll(final K key, final Collection<V> values) {
        if (values.isEmpty()) {
            return false;
        }
        
        final C collection = backingMap.get(key);
        if (collection == null) {
            return false;
        } else if (collection.removeAll(values)) {
            if (collection.isEmpty()) {
                backingMap.remove(key);
            }
            return true;
        } else {
            return false;
        }
    }
    
    @Override
    public final boolean removeAll(final K key, final Iterable<? extends V> values) {
        final C collection = backingMap.get(key);
        boolean modified = false;
        if (collection != null) {
            for (V value : values) {
                modified |= collection.remove(value);
            }
            if (modified && collection.isEmpty()) {
                backingMap.remove(key);
            }
        }
        return modified;
    }
    
    @Override
    public final boolean removeAll(final MultiMap<? extends K, ? extends V> map) {
        boolean modified = false;
        for (final Entry<? extends K, ? extends Collection<? extends V>> entry : map.asMap().entrySet()) {
            modified |= removeAll(entry.getKey(), entry.getValue());
        }
        return modified;
    }
    
    @Override
    public final boolean removeAll(final Collection<? extends Tuple2<? extends K, ? extends V>> collection) {
        boolean modified = false;
        for (final Tuple2<? extends K, ? extends V> tuple : collection) {
            modified |= remove(tuple.get1(), tuple.get2());
        }
        return modified;
    }
    
    @Override
    public final void clear() {
        backingMap.clear();
    }
    
    @Override
    public final Iterator<Entry<K, ? extends Collection<V>>> iterator() {
        return new Iterator<Entry<K, ? extends Collection<V>>>() {
            
            private final Iterator<Entry<K, C>> i = backingMap.entrySet().iterator();
            
            @Override
            public boolean hasNext() {
                return i.hasNext();
            }
            
            @Override
            public Entry<K, ? extends Collection<V>> next() {
                return new Entry<K, Collection<V>>() {
                    
                    private final Entry<K, C> e = i.next();
                    
                    @Override
                    public K getKey() {
                        return e.getKey();
                    }
                    
                    @Override
                    public Collection<V> getValue() {
                        return immutableCollection(e.getValue());
                    }
                    
                    @Override
                    public Collection<V> setValue(final Collection<V> value) {
                        return e.setValue(createCollection(value));
                    }
                    
                    public int hashCode() {
                        return e.hashCode();
                    }
                    
                    public boolean equals(final Object o) {
                        if (this == o) {
                            return true;
                        }
                        if (!(o instanceof Entry)) {
                            return false;
                        }
                        final Entry<?, ?> t = (Entry<?, ?>) o;
                        return e.getKey().equals(t.getKey()) && e.getValue().equals(t.getValue());
                    }
                    
                    public String toString() {
                        return e.toString();
                    }
                    
                };
            }
            
        };
    }
    
    @Override
    public final Set<K> keySet() {
        return Collections.unmodifiableSet(backingMap.keySet());
    }
    
    @Override
    public final Map<K, ? extends Collection<V>> asMap() {
        return ExtendedCollections.unmodifiableMapOfCollection(backingMap);
    }
    
    @Override
    public final void apply(final MultiMapUpdates<K, V> updates) {
        if (updates.getToAdd() != null) {
            putAll(updates.getToAdd());
        }
        if (updates.getToRemove() != null) {
            removeAll(updates.getToRemove());
        }
        if (updates.getToReplace() != null) {
            for (final Entry<K, ? extends Collection<V>> entry : updates.getToReplace().asMap().entrySet()) {
                replace(entry.getKey(), entry.getValue());
            }
        }
        if (updates.getKeysToRemove() != null) {
            for (final K key : updates.getKeysToRemove()) {
                remove(key);
            }
        }
    }
    
}
