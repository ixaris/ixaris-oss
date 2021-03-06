package com.ixaris.commons.dimensions.lib.dimension;

import java.util.EnumSet;

import com.ixaris.commons.dimensions.lib.context.hierarchy.HierarchyChildNodeSet;
import com.ixaris.commons.dimensions.lib.context.hierarchy.HierarchyNode;
import com.ixaris.commons.dimensions.lib.context.hierarchy.HierarchySubtreeNodes;

public enum HEnum implements HierarchyNode<HEnum> {
    
    ROOT,
    I1(ROOT),
    I1_1(I1);
    
    private final byte depth;
    private final HEnum parent;
    
    HEnum() {
        this.parent = null;
        this.depth = 0;
    }
    
    HEnum(final HEnum parent) {
        this.parent = parent;
        this.depth = (byte) (parent.depth + 1);
    }
    
    @Override
    public String getKey() {
        return name();
    }
    
    @Override
    public HEnum getParent() {
        return parent;
    }
    
    @Override
    public HierarchySubtreeNodes<HEnum> getSubtree() {
        final EnumSet<HEnum> subtree = EnumSet.noneOf(HEnum.class);
        getSubtreeInternal(this, subtree);
        if (subtree.size() == 1) {
            return null;
        } else {
            return new HierarchyChildNodeSet<>(subtree);
        }
    }
    
    private void getSubtreeInternal(final HEnum parent, final EnumSet<HEnum> children) {
        children.add(parent);
        for (HEnum value : values()) {
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
