package com.ixaris.commons.collections.lib;

import java.util.Map;

public final class LockedMap<K, V> extends AbstractLockedMap<K, V> {
    
    public LockedMap() {
        super();
    }
    
    public LockedMap(final Map<K, V> map) {
        super(map);
    }
    
}
