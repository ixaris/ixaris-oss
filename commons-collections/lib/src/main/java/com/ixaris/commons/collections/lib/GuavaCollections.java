package com.ixaris.commons.collections.lib;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;
import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

public final class GuavaCollections {
    
    @SafeVarargs
    public static <T> ImmutableSet<T> copyOfSetAdding(final Set<T> set, final T... toAdd) {
        return copyOfSetAdding(set, Arrays.asList(toAdd));
    }
    
    public static <T> ImmutableSet<T> copyOfSetAdding(final Set<T> set, final Collection<T> toAdd) {
        final Set<T> copy = new HashSet<>(set);
        copy.addAll(toAdd);
        return ImmutableSet.copyOf(copy);
    }
    
    @SafeVarargs
    public static <T> ImmutableSet<T> copyOfSetRemoving(final Set<T> set, final T... toRemove) {
        return copyOfSetRemoving(set, Arrays.asList(toRemove));
    }
    
    public static <T> ImmutableSet<T> copyOfSetRemoving(final Set<T> set, final Collection<T> toRemove) {
        return ImmutableSet.copyOf(iterableWithoutValues(set, toRemove));
    }
    
    private static <T> Iterable<T> iterableWithoutValues(final Iterable<T> iterable, final Collection<T> toRemove) {
        final Set<T> setToRemove = new HashSet<>(toRemove);
        return () -> {
            final Iterator<T> iterator = iterable.iterator();
            return new Iterator<T>() {
                
                private T next = findNext();
                
                @Override
                public boolean hasNext() {
                    return next != null;
                }
                
                @Override
                public T next() {
                    if (next == null) {
                        throw new NoSuchElementException();
                    }
                    final T toReturn = next;
                    next = findNext();
                    return toReturn;
                }
                
                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
                
                private T findNext() {
                    while (iterator.hasNext()) {
                        final T n = iterator.next();
                        if (!setToRemove.contains(n)) {
                            return n;
                        }
                    }
                    return null;
                }
                
            };
        };
    }
    
    public static <K, V> ImmutableMap<K, V> copyOfMapAdding(final Map<K, V> map, final K key, final V value) {
        final Map<K, V> copy = new HashMap<>(map);
        copy.put(key, value);
        return ImmutableMap.copyOf(copy);
    }
    
    public static <K, V> ImmutableMap<K, V> copyOfMapAdding(final Map<K, V> map, final Map<K, V> newMap) {
        final Map<K, V> copy = new HashMap<>(map);
        copy.putAll(newMap);
        return ImmutableMap.copyOf(copy);
    }
    
    @SafeVarargs
    public static <K, V> ImmutableMap<K, V> copyOfMapRemoving(final Map<K, V> map, final K... keysToRemove) {
        return copyOfMapRemoving(map, Arrays.asList(keysToRemove));
    }
    
    public static <K, V> ImmutableMap<K, V> copyOfMapRemoving(final Map<K, V> map, final Collection<K> keysToRemove) {
        return ImmutableMap.copyOf(iterableWithoutKeys(map.entrySet(), keysToRemove));
    }
    
    private static <K, V> Iterable<Entry<K, V>> iterableWithoutKeys(
        final Iterable<Entry<K, V>> iterable, final Collection<K> toRemove
    ) {
        final Set<K> setToRemove = new HashSet<>(toRemove);
        return () -> {
            final Iterator<Entry<K, V>> iterator = iterable.iterator();
            return new Iterator<Entry<K, V>>() {
                
                private Entry<K, V> next = findNext();
                
                @Override
                public boolean hasNext() {
                    return next != null;
                }
                
                @Override
                public Entry<K, V> next() {
                    if (next == null) {
                        throw new NoSuchElementException();
                    }
                    final Entry<K, V> toReturn = next;
                    next = findNext();
                    return toReturn;
                }
                
                @Override
                public void remove() {
                    throw new UnsupportedOperationException();
                }
                
                private Entry<K, V> findNext() {
                    while (iterator.hasNext()) {
                        final Entry<K, V> n = iterator.next();
                        if (!setToRemove.contains(n.getKey())) {
                            return n;
                        }
                    }
                    return null;
                }
                
            };
        };
    }
    
    private GuavaCollections() {}
    
}
