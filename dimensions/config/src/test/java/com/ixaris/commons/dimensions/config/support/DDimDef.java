/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.support;

import com.ixaris.commons.dimensions.lib.dimension.LongBasedDimensionDef;

public final class DDimDef extends LongBasedDimensionDef {
    
    private static final DDimDef INSTANCE = new DDimDef();
    
    public static DDimDef getInstance() {
        return INSTANCE;
    }
    
    private DDimDef() {}
    
    @Override
    public final boolean isCacheable() {
        return false;
    }
    
    @Override
    public Range getRange() {
        return Range.MATCH_ANY;
    }
    
}
