package com.ixaris.commons.collections.lib;

import java.util.Set;

public abstract class Vertex<T, V extends Vertex<T, V>> {
    
    private final T item;
    
    Vertex(final T item) {
        this.item = item;
    }
    
    public final T get() {
        return item;
    }
    
    public abstract boolean isRoot();
    
    public abstract boolean isLeaf();
    
    abstract Set<V> getParents();
    
    abstract Set<V> getChildren();
    
    abstract void removeParent(final V parent);
    
    abstract void removeChild(final V child);
    
    @Override
    public final int hashCode() {
        return item.hashCode();
    }
    
    @Override
    public final boolean equals(final Object o) {
        if (o == null) {
            return false;
        } else if (o == this) {
            return true;
        } else if (!(o instanceof Vertex)) {
            return false;
        } else {
            return item.equals(((Vertex<?, ?>) o).item);
        }
    }
    
    @Override
    public final String toString() {
        return item.toString();
    }
}
