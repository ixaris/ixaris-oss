package com.ixaris.commons.dimensions.config;

import java.util.Set;

import com.ixaris.commons.dimensions.config.value.Value;

public final class SetUpdates<T extends Value> {
    
    private final Set<T> added;
    private final Set<T> removed;
    
    public SetUpdates(final Set<T> added, final Set<T> removed) {
        if (added == null) {
            throw new IllegalArgumentException("added is null");
        }
        if (removed == null) {
            throw new IllegalArgumentException("removed is null");
        }
        
        this.added = added;
        this.removed = removed;
    }
    
    public Set<T> getAdded() {
        return added;
    }
    
    public Set<T> getRemoved() {
        return removed;
    }
    
}
