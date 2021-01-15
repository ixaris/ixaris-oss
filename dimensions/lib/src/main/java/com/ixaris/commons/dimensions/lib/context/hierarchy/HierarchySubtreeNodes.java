package com.ixaris.commons.dimensions.lib.context.hierarchy;

/**
 * Children of a {@link HierarchyNode}.
 */
public interface HierarchySubtreeNodes<T extends HierarchyNode<T>> {
    
    enum Resolution {
        SET, // explicitly return a set of nodes including the root of the subtree and all the children
        PREFIX // return a pattern to use in the query WHERE value like "<pattern>"
    }
    
    Resolution getResolution();
    
}
