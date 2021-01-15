/*
 * Copyright 2002, 2011 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.counters.support;

import com.ixaris.commons.dimensions.lib.dimension.EnumBasedDimensionDef;

public final class ADimensionDef extends EnumBasedDimensionDef<AEnum> {
    
    private static final ADimensionDef INSTANCE = new ADimensionDef();
    
    public static ADimensionDef getInstance() {
        return INSTANCE;
    }
    
    private ADimensionDef() {}
    
    @Override
    protected Class<AEnum> getEnumType() {
        return AEnum.class;
    }
    
    @Override
    protected AEnum[] getEnumValues() {
        return AEnum.values();
    }
    
}
