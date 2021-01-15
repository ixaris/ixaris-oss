/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.support;

import com.ixaris.commons.dimensions.lib.dimension.LongBasedDimensionDef;

public final class EDimDef extends LongBasedDimensionDef {
    
    private static final EDimDef INSTANCE = new EDimDef();
    
    public static EDimDef getInstance() {
        return INSTANCE;
    }
    
    private EDimDef() {}
    
    @Override
    public final boolean isCacheable() {
        return false;
    }
    
    @Override
    public Range getRange() {
        return Range.LARGER;
    }
    
}
