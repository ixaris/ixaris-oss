/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.limits;

import com.ixaris.commons.dimensions.lib.dimension.EnumBasedDimensionDef;

/**
 * Dimension used for {@link Definer}
 */
public class DefinerDimensionDef extends EnumBasedDimensionDef<Definer> {
    
    private static final DefinerDimensionDef INSTANCE = new DefinerDimensionDef();
    
    public static DefinerDimensionDef getInstance() {
        return INSTANCE;
    }
    
    private DefinerDimensionDef() {}
    
    @Override
    protected Class<Definer> getEnumType() {
        return Definer.class;
    }
    
    @Override
    protected Definer[] getEnumValues() {
        return Definer.values();
    }
    
    @Override
    public boolean isHierarchical() {
        return true;
    }
    
    @Override
    public byte getMaxDepth() {
        return 1;
    }
    
}
