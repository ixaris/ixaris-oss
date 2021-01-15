package com.ixaris.commons.dimensions.limits;

import java.util.EnumSet;

import com.ixaris.commons.dimensions.lib.context.hierarchy.HierarchyChildNodeSet;
import com.ixaris.commons.dimensions.lib.context.hierarchy.HierarchyNode;
import com.ixaris.commons.dimensions.lib.context.hierarchy.HierarchySubtreeNodes;

/**
 * Used to identify who declared the Limit.
 *
 * @author <a href="mailto:aaron.axisa@ixaris.com">Aaron Axisa</a>
 */
public enum Definer implements HierarchyNode<Definer> {
    
    /**
     * To be used when a User is defining or querying for a Limit Users are effected by both SYSTEM and USER limits
     */
    USER,
    /**
     * To be used when the System is defining or querying for a Limit System is only effected by SYSTEM limits. The SYSTEM should have preference
     * over USER defined limits in case more than one limit is equally specific.
     */
    SYSTEM(USER);
    
    private final byte depth;
    private final Definer parent;
    
    Definer() {
        this.parent = null;
        this.depth = 0;
    }
    
    Definer(final Definer parent) {
        this.parent = parent;
        this.depth = (byte) (parent.depth + 1);
    }
    
    @Override
    public String getKey() {
        return name();
    }
    
    @Override
    public Definer getParent() {
        return parent;
    }
    
    @Override
    public HierarchySubtreeNodes<Definer> getSubtree() {
        final EnumSet<Definer> subtree = EnumSet.noneOf(Definer.class);
        getSubtreeInternal(this, subtree);
        if (subtree.size() == 1) {
            return null;
        } else {
            return new HierarchyChildNodeSet<>(subtree);
        }
    }
    
    private void getSubtreeInternal(final Definer parent, final EnumSet<Definer> children) {
        children.add(parent);
        for (Definer value : values()) {
            if ((value.getParent() != null) && value.getParent().equals(parent)) {
                getSubtreeInternal(value, children);
            }
        }
    }
    
    @Override
    public byte getDepth() {
        return depth;
    }
    
}
