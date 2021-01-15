package com.ixaris.commons.dimensions.lib.context.hierarchy;

import java.util.Set;

public final class HierarchyChildNodeSet<T extends HierarchyNode<T>> implements HierarchySubtreeNodes<T> {
    
    private Set<T> childSet;
    
    public HierarchyChildNodeSet(final Set<T> childSet) {
        if (childSet == null) {
            throw new IllegalArgumentException("childSet is null");
        }
        if (childSet.isEmpty()) {
            throw new IllegalArgumentException("childSet should not be empty");
        }
        this.childSet = childSet;
    }
    
    @Override
    public Resolution getResolution() {
        return Resolution.SET;
    }
    
    public Set<T> getChildNodeSet() {
        return childSet;
    }
    
}
