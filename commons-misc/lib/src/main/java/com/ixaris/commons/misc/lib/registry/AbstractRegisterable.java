package com.ixaris.commons.misc.lib.registry;

import com.ixaris.commons.misc.lib.object.EqualsUtil;

/**
 * Class to be extended by registerable objects to register themselves with appropriate registries
 */
public abstract class AbstractRegisterable implements Registerable {
    
    @Override
    public boolean equals(final Object o) {
        // equality is based on key (these are typically singletons, but could be instances of a class)
        return EqualsUtil.equals(this, o, other -> getKey().equals(other.getKey()));
    }
    
    @Override
    public int hashCode() {
        return getKey().hashCode();
    }
    
    @Override
    public String toString() {
        return getKey();
    }
    
}
