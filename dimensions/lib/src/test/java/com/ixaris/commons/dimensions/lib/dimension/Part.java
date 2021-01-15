package com.ixaris.commons.dimensions.lib.dimension;

import java.util.Objects;

import com.ixaris.commons.dimensions.lib.context.hierarchy.HierarchyNode;
import com.ixaris.commons.dimensions.lib.context.hierarchy.HierarchySubtreeNodePrefix;
import com.ixaris.commons.misc.lib.object.EqualsUtil;

public class Part implements HierarchyNode<Part> {
    
    public static final String DELIMITER = ",";
    
    public static Part fromString(final String s) {
        final String[] parts = s.split(DELIMITER, 2);
        
        if (parts.length == 1) {
            return new Part(parts[0]);
        } else {
            return new Part(parts[0], parts[1]);
        }
    }
    
    private final String s1;
    private final String s2;
    
    public Part(final String s) {
        this.s1 = s;
        this.s2 = null;
    }
    
    public Part(final String s1, final String s2) {
        this.s1 = s1;
        this.s2 = s2;
    }
    
    public String getS1() {
        return s1;
    }
    
    public String getS2() {
        return s2;
    }
    
    @Override
    public boolean equals(final Object o) {
        return EqualsUtil.equals(this, o, other -> s1.equals(other.s1) && Objects.equals(s2, other.s2));
    }
    
    /**
     * This should respect the contract that if a == b, a.hashCode() == b.hashCode(). The inverse is not required
     *
     * @return the hashcode
     */
    @Override
    public int hashCode() {
        return Objects.hash(s1, s2);
    }
    
    @Override
    public Part getParent() {
        if (s2 == null) {
            return null;
        } else {
            return new Part(s1);
        }
    }
    
    @Override
    public HierarchySubtreeNodePrefix<Part> getSubtree() {
        if (s2 == null) {
            return new HierarchySubtreeNodePrefix<Part>(s1 + DELIMITER);
        } else {
            return null;
        }
    }
    
    @Override
    public String getKey() {
        if (s2 == null) {
            return s1;
        } else {
            return s1 + DELIMITER + s2;
        }
    }
    
    @Override
    public byte getDepth() {
        return (byte) (s2 == null ? 0 : 1);
    }
    
}
