/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.lib.dimension;

import com.ixaris.commons.dimensions.lib.context.PartialContext;

public final class BDimDef extends StringDimensionDef<String> {
    
    private static final BDimDef INSTANCE = new BDimDef();
    
    public static BDimDef getInstance() {
        return INSTANCE;
    }
    
    private BDimDef() {}
    
    @Override
    protected String convertNonNullValueToString(final String value) {
        return value;
    }
    
    @Override
    protected String getValueFromSpecificString(final String str, final PartialContext context) {
        return str;
    }
    
    @Override
    public boolean isCacheable() {
        return false;
    }
    
}
