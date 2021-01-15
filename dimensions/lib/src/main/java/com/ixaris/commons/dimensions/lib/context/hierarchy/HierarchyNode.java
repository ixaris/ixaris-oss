package com.ixaris.commons.dimensions.lib.context.hierarchy;

import java.io.Serializable;

/**
 * Interface to be implemented by each node in a hierarchy - example the hierarchy of instruments
 *
 * @author <a href="mailto:brian.vella@ixaris.com">BV</a>
 */
public interface HierarchyNode<T extends HierarchyNode<T>> extends Serializable {
    
    /**
     * Should be positive (root is at depth 0). Match the maximum with the dimension definition
     *
     * <p>When calculating context hash, this depth will be transposed to allow for match any and wildcard
     *
     * @return the depth of the node
     */
    byte getDepth();
    
    /**
     * @return the parent of this node, or null if this is the root
     */
    T getParent();
    
    /**
     * @return the subtree, including this node
     */
    HierarchySubtreeNodes<T> getSubtree();
    
    /**
     * @return the string representation of the node
     */
    String getKey();
    
}
