package com.ixaris.commons.dimensions.lib.context.hierarchy;

public final class HierarchySubtreeNodePrefix<T extends HierarchyNode<T>> implements HierarchySubtreeNodes<T> {
    
    private String prefix;
    
    public HierarchySubtreeNodePrefix(final String prefix) {
        this.prefix = prefix;
    }
    
    @Override
    public Resolution getResolution() {
        return Resolution.PREFIX;
    }
    
    /**
     * @return the database lookup prefix for matching subtree (node and its children), excluding any wildcard character
     */
    public String getSubtreeNodePrefix() {
        return prefix;
    }
    
}
