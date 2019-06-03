package com.ixaris.commons.collections.lib;

import com.ixaris.commons.misc.lib.object.Tuple2;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

public final class ArrayListMultiMap<K, V> extends AbstractMultiMap<K, V, List<V>> {
    
    private static final long serialVersionUID = 4214071197300093677L;
    
    public ArrayListMultiMap(final int initialCapacity, final float loadFactor) {
        super(new HashMap<>(initialCapacity, loadFactor));
    }
    
    public ArrayListMultiMap(final int initialCapacity) {
        super(new HashMap<>(initialCapacity));
    }
    
    public ArrayListMultiMap() {
        super(new HashMap<>());
    }
    
    public ArrayListMultiMap(final MultiMap<? extends K, ? extends V> map) {
        this(Math.max((int) (map.size() / 0.75f) + 1, 16));
        putAll(map);
    }
    
    public ArrayListMultiMap(final Collection<? extends Tuple2<? extends K, ? extends V>> collection) {
        this(Math.max((int) (collection.size() / 0.75f) + 1, 16));
        putAll(collection);
    }
    
    @Override
    protected final List<V> createCollection() {
        return new ArrayList<V>();
    }
    
    @Override
    protected final List<V> createCollection(final Collection<V> collection) {
        return new ArrayList<V>(collection);
    }
    
    @Override
    protected final List<V> immutableCollection(final List<V> collection) {
        return Collections.unmodifiableList(collection);
    }
    
}
