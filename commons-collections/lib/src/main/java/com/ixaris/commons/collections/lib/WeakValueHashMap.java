package com.ixaris.commons.collections.lib;

import java.lang.ref.ReferenceQueue;
import java.lang.ref.WeakReference;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

public class WeakValueHashMap<K, V> implements Map<K, V> {
    
    private final Map<K, WeakValue<K, V>> backingMap;
    private ReferenceQueue<V> queue = new ReferenceQueue<V>();
    
    public WeakValueHashMap(final int initialCapacity, final float loadFactor) {
        backingMap = new HashMap<>(initialCapacity, loadFactor);
    }
    
    public WeakValueHashMap(final int initialCapacity) {
        backingMap = new HashMap<>(initialCapacity);
    }
    
    public WeakValueHashMap() {
        backingMap = new HashMap<>();
    }
    
    @Override
    public int size() {
        processQueue();
        return backingMap.size();
    }
    
    @Override
    public boolean isEmpty() {
        processQueue();
        return backingMap.isEmpty();
    }
    
    @Override
    public boolean containsKey(final Object key) {
        processQueue();
        return backingMap.containsKey(key);
    }
    
    @Override
    public boolean containsValue(final Object value) {
        processQueue();
        return backingMap.containsValue(value);
    }
    
    @Override
    public V get(final Object key) {
        processQueue();
        final WeakReference<V> ref = backingMap.get(key);
        return (ref != null) ? ref.get() : null;
    }
    
    @Override
    public V put(final K key, final V value) {
        processQueue();
        final WeakReference<V> ref = backingMap.put(key, WeakValue.create(key, value, queue));
        return (ref != null) ? ref.get() : null;
    }
    
    @Override
    public V remove(final Object key) {
        processQueue();
        final WeakReference<V> ref = backingMap.remove(key);
        return (ref != null) ? ref.get() : null;
    }
    
    @Override
    public void putAll(final Map<? extends K, ? extends V> m) {
        throw new UnsupportedOperationException();
    }
    
    @Override
    public void clear() {
        processQueue();
        backingMap.clear();
    }
    
    @Override
    public Set<K> keySet() {
        processQueue();
        return backingMap.keySet();
    }
    
    @Override
    public Collection<V> values() {
        processQueue();
        return new ConvertingCollectionWrapper<WeakValue<K, V>, V, Collection<WeakValue<K, V>>>(backingMap.values()) {
            
            @Override
            protected V convert(final WeakValue<K, V> item) {
                return item != null ? item.get() : null;
            }
            
            @Override
            protected WeakValue<K, V> reverseConvert(final V item) {
                throw new UnsupportedOperationException();
            }
            
            @Override
            protected boolean isOriginalType(final Object item) {
                return item instanceof WeakValue;
            }
            
        };
    }
    
    @Override
    public Set<Entry<K, V>> entrySet() {
        processQueue();
        return new ConvertingSetWrapper<
            Entry<K, WeakValue<K, V>>, Entry<K, V>, Set<Entry<K, WeakValue<K, V>>>
        >(backingMap.entrySet()) {
            
            @Override
            protected Entry<K, V> convert(final Entry<K, WeakValue<K, V>> item) {
                return new ConvertingEntry(item);
            }
            
            @Override
            protected Entry<K, WeakValue<K, V>> reverseConvert(final Entry<K, V> item) {
                throw new UnsupportedOperationException();
            }
            
            @Override
            protected boolean isOriginalType(final Object item) {
                return item instanceof Entry;
            }
            
        };
    }
    
    @Override
    public int hashCode() {
        processQueue();
        return backingMap.hashCode();
    }
    
    @Override
    public boolean equals(final Object o) {
        processQueue();
        return backingMap.equals(o);
    }
    
    @Override
    public String toString() {
        processQueue();
        return backingMap.toString();
    }
    
    @SuppressWarnings("unchecked")
    private void processQueue() {
        
        WeakValue<K, V> garbage;
        while ((garbage = (WeakValue<K, V>) this.queue.poll()) != null) {
            final WeakValue<K, V> removed = backingMap.remove(garbage.key);
            if ((removed != null) && (removed != garbage)) {
                backingMap.put(removed.key, removed);
            }
        }
    }
    
    private static class WeakValue<K, V> extends WeakReference<V> {
        
        private static <K, V> WeakValue<K, V> create(final K key, final V value, final ReferenceQueue<V> queue) {
            return value != null ? new WeakValue<K, V>(key, value, queue) : null;
        }
        
        private K key;
        
        private WeakValue(final K key, final V value, final ReferenceQueue<V> queue) {
            super(value, queue);
            this.key = key;
        }
        
        @Override
        public boolean equals(final Object o) {
            if (o == null) {
                return false;
            } else if (this == o) {
                return true;
            } else if (!(this.getClass().equals(o.getClass()))) {
                return false;
            } else {
                final WeakValue<?, ?> other = (WeakValue<?, ?>) o;
                return Objects.equals(get(), other.get());
            }
        }
        
        @Override
        public int hashCode() {
            final V ref = get();
            return (ref == null) ? 0 : ref.hashCode();
        }
        
        @Override
        public String toString() {
            final V ref = get();
            return (ref == null) ? "[NULL]" : ref.toString();
        }
    }
    
    private class ConvertingEntry implements Entry<K, V> {
        
        private Entry<K, WeakValue<K, V>> wrapped;
        private V value; // Strong reference to value, so that the GC will leave it alone as long as this Entry exists
        
        private ConvertingEntry(final Entry<K, WeakValue<K, V>> wrapped) {
            this.wrapped = wrapped;
            value = wrapped.getValue().get();
        }
        
        @Override
        public K getKey() {
            return wrapped.getKey();
        }
        
        @Override
        public V getValue() {
            return value;
        }
        
        @Override
        public V setValue(final V value) {
            final V oldValue = this.value;
            this.value = value;
            wrapped.setValue(WeakValue.create(wrapped.getKey(), value, queue));
            return oldValue;
        }
        
        @Override
        public boolean equals(final Object o) {
            return wrapped.equals(o);
        }
        
        @Override
        public int hashCode() {
            return wrapped.hashCode();
        }
        
    }
    
}
