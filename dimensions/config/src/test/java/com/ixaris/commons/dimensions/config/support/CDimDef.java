/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.support;

import com.ixaris.commons.dimensions.lib.dimension.EnumBasedDimensionDef;

public final class CDimDef extends EnumBasedDimensionDef<CEnum> {
    
    private static final CDimDef INSTANCE = new CDimDef();
    
    public static CDimDef getInstance() {
        return INSTANCE;
    }
    
    private CDimDef() {}
    
    @Override
    protected Class<CEnum> getEnumType() {
        return CEnum.class;
    }
    
    @Override
    protected CEnum[] getEnumValues() {
        return CEnum.values();
    }
    
}
