package com.ixaris.commons.collections.lib;

import com.ixaris.commons.misc.lib.object.Tuple2;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Set;

public class LinkedHashSetMultiMap<K, V> extends AbstractMultiMap<K, V, Set<V>> {
    
    private static final long serialVersionUID = 4214071197300093677L;
    
    public LinkedHashSetMultiMap(final int initialCapacity, final float loadFactor) {
        super(new HashMap<>(initialCapacity, loadFactor));
    }
    
    public LinkedHashSetMultiMap(final int initialCapacity) {
        super(new HashMap<>(initialCapacity));
    }
    
    public LinkedHashSetMultiMap() {
        super(new HashMap<>());
    }
    
    public LinkedHashSetMultiMap(final MultiMap<? extends K, ? extends V> map) {
        this(Math.max((int) (map.size() / 0.75f) + 1, 16));
        putAll(map);
    }
    
    public LinkedHashSetMultiMap(final Collection<? extends Tuple2<? extends K, ? extends V>> collection) {
        this(Math.max((int) (collection.size() / 0.75f) + 1, 16));
        putAll(collection);
    }
    
    @Override
    protected final LinkedHashSet<V> createCollection() {
        return new LinkedHashSet<>();
    }
    
    @Override
    protected final LinkedHashSet<V> createCollection(final Collection<V> collection) {
        return new LinkedHashSet<V>(collection);
    }
    
    @Override
    protected final Set<V> immutableCollection(final Set<V> collection) {
        return Collections.unmodifiableSet(collection);
    }
    
}
