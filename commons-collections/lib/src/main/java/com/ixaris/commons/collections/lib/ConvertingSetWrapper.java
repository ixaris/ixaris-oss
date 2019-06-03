package com.ixaris.commons.collections.lib;

import java.util.Set;

public abstract class ConvertingSetWrapper<X, E, C extends Set<X>> extends ConvertingCollectionWrapper<X, E, C>
implements Set<E> {
    
    public ConvertingSetWrapper(final C wrapped) {
        super(wrapped);
    }
    
}
