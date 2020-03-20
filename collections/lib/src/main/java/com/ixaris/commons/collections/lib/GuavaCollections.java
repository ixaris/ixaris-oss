package com.ixaris.commons.collections.lib;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.Map;
import java.util.Map.Entry;
import java.util.NoSuchElementException;
import java.util.Set;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public final class GuavaCollections {
    
    public static <T> ImmutableSet<T> copyOfSetAdding(final ImmutableSet<T> set, final T toAdd) {
        if (!set.contains(toAdd)) {
            return copyOfSetAdding(set, Collections.singleton(toAdd));
        } else {
            return set;
        }
    }
    
    @SafeVarargs
    public static <T> ImmutableSet<T> copyOfSetAdding(final ImmutableSet<T> set, final T... toAdd) {
        return copyOfSetAdding(set, Arrays.asList(toAdd));
    }
    
    public static <T> ImmutableSet<T> copyOfSetAdding(final ImmutableSet<T> set, final Collection<T> toAdd) {
        final Set<T> copy = new HashSet<>(set);
        if (copy.addAll(toAdd)) {
            return ImmutableSet.copyOf(copy);
        } else {
            return set;
        }
    }
    
    public static <T> ImmutableSet<T> copyOfSetRemoving(final ImmutableSet<T> set, final T toRemove) {
        if (set.contains(toRemove)) {
            return copyOfSetRemoving(set, Collections.singleton(toRemove));
        } else {
            return set;
        }
    }
    
    @SafeVarargs
    public static <T> ImmutableSet<T> copyOfSetRemoving(final ImmutableSet<T> set, final T... toRemove) {
        return copyOfSetRemoving(set, Arrays.asList(toRemove));
    }
    
    public static <T> ImmutableSet<T> copyOfSetRemoving(final ImmutableSet<T> set, final Collection<T> toRemove) {
        final ImmutableSet<T> newSet = ImmutableSet.copyOf(iteratorWithoutValues(set, toRemove));
        if (newSet.size() < set.size()) {
            return newSet;
        } else {
            return set;
        }
    }
    
    private static <T> Iterator<T> iteratorWithoutValues(final Iterable<T> iterable, final Collection<T> toRemove) {
        final Set<T> setToRemove = new HashSet<>(toRemove);
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
    }
    
    public static <K, V> ImmutableMap<K, V> copyOfMapAdding(final ImmutableMap<K, V> map, final K key, final V value) {
        final Map<K, V> copy = new HashMap<>(map);
        if (copy.put(key, value) != value) {
            return ImmutableMap.copyOf(copy);
        } else {
            return map;
        }
    }
    
    public static <K, V> ImmutableMap<K, V> copyOfMapAdding(final ImmutableMap<K, V> map, final Map<K, V> toAdd) {
        final Map<K, V> copy = new HashMap<>(map);
        boolean modified = false;
        for (final Entry<K, V> e : toAdd.entrySet()) {
            final V value = e.getValue();
            modified |= copy.put(e.getKey(), value) != value;
        }
        if (modified) {
            return ImmutableMap.copyOf(copy);
        } else {
            return map;
        }
    }
    
    public static <K, V> ImmutableMap<K, V> copyOfMapRemoving(final ImmutableMap<K, V> map, final K keyToRemove) {
        if (map.containsKey(keyToRemove)) {
            return copyOfMapRemoving(map, Collections.singleton(keyToRemove));
        } else {
            return map;
        }
    }
    
    @SafeVarargs
    public static <K, V> ImmutableMap<K, V> copyOfMapRemoving(final ImmutableMap<K, V> map, final K... keysToRemove) {
        return copyOfMapRemoving(map, Arrays.asList(keysToRemove));
    }
    
    public static <K, V> ImmutableMap<K, V> copyOfMapRemoving(final ImmutableMap<K, V> map, final Collection<K> keysToRemove) {
        final ImmutableMap<K, V> newMap = ImmutableMap.copyOf(iterableWithoutKeys(map.entrySet(), keysToRemove));
        if (newMap.size() < map.size()) {
            return newMap;
        } else {
            return map;
        }
    }
    
    private static <K, V> Iterable<Entry<K, V>> iterableWithoutKeys(final Iterable<Entry<K, V>> iterable, final Collection<K> toRemove) {
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
