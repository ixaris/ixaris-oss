package com.ixaris.commons.collections.lib;

import java.util.Collection;
import java.util.Set;

public final class SetBuilder<T, S extends Set<T>> {
    
    public static <U, V extends Set<U>> SetBuilder<U, V> with(final V set) {
        return new SetBuilder<U, V>(set);
    }
    
    private S set;
    
    public SetBuilder(final S set) {
        this.set = set;
    }
    
    @SafeVarargs
    public final SetBuilder<T, S> add(final T... items) {
        for (final T t : items) {
            set.add(t);
        }
        return this;
    }
    
    public SetBuilder<T, S> addAll(final Collection<T> items) {
        set.addAll(items);
        return this;
    }
    
    @SafeVarargs
    public final SetBuilder<T, S> remove(final T... items) {
        for (final T t : items) {
            set.remove(t);
        }
        return this;
    }
    
    public S build() {
        return set;
    }
    
}
