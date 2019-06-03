package com.ixaris.commons.collections.lib;

import java.util.Collection;
import java.util.HashSet;
import java.util.Map.Entry;

public class MultiMapUpdates<K, V> {
    
    private HashSetMultiMap<K, V> toAdd;
    private HashSetMultiMap<K, V> toRemove;
    private HashSetMultiMap<K, V> toReplace;
    private HashSet<K> keysToRemove;
    
    public MultiMapUpdates() {}
    
    /**
     * Sets the update to set the values as indicated by the values of the multi-value map.
     *
     * @param map
     */
    public MultiMapUpdates(final MultiMap<K, V> map) {
        
        if (map == null) {
            throw new IllegalArgumentException("map is null");
        }
        
        for (Entry<K, ? extends Collection<V>> entry : map.asMap().entrySet()) {
            for (V value : entry.getValue()) {
                if (value != null) {
                    add(entry.getKey(), value);
                }
            }
        }
    }
    
    public final HashSetMultiMap<K, V> getToAdd() {
        return toAdd;
    }
    
    public void add(final K name, final V value) {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        if (toAdd == null) {
            toAdd = new HashSetMultiMap<K, V>();
        }
        toAdd.put(name, value);
    }
    
    public final HashSetMultiMap<K, V> getToRemove() {
        return toRemove;
    }
    
    public void remove(final K name, final V value) {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        if (toRemove == null) {
            toRemove = new HashSetMultiMap<K, V>();
        }
        toRemove.put(name, value);
    }
    
    public final HashSetMultiMap<K, V> getToReplace() {
        return toReplace;
    }
    
    public void replace(final K name, final V value) {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        if (toReplace == null) {
            toReplace = new HashSetMultiMap<K, V>();
        }
        toReplace.put(name, value);
    }
    
    public final HashSet<K> getKeysToRemove() {
        return keysToRemove;
    }
    
    public void remove(final K name) {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        if (keysToRemove == null) {
            keysToRemove = new HashSet<K>();
        }
        keysToRemove.add(name);
    }
    
}
