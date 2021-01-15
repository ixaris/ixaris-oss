/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.lib.dimension;

public final class HDimDef extends EnumBasedDimensionDef<HEnum> {
    
    private static final HDimDef INSTANCE = new HDimDef();
    
    public static HDimDef getInstance() {
        return INSTANCE;
    }
    
    private HDimDef() {}
    
    @Override
    protected Class<HEnum> getEnumType() {
        return HEnum.class;
    }
    
    @Override
    protected HEnum[] getEnumValues() {
        return HEnum.values();
    }
    
    @Override
    public boolean isHierarchical() {
        return true;
    }
    
    @Override
    public byte getMaxDepth() {
        return 2;
    }
    
}
