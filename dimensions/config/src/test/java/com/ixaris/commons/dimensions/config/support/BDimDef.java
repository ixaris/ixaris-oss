/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.support;

import com.ixaris.commons.dimensions.lib.dimension.StringBasedDimensionDef;

public final class BDimDef extends StringBasedDimensionDef {
    
    private static final BDimDef INSTANCE = new BDimDef();
    
    public static BDimDef getInstance() {
        return INSTANCE;
    }
    
    private BDimDef() {}
    
    @Override
    public boolean isCacheable() {
        return false;
    }
    
}
