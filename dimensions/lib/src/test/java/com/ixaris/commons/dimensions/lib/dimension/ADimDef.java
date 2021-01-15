/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.lib.dimension;

import com.ixaris.commons.dimensions.lib.context.PartialContext;

public final class ADimDef extends StringDimensionDef<String> {
    
    private static final ADimDef INSTANCE = new ADimDef();
    
    public static ADimDef getInstance() {
        return INSTANCE;
    }
    
    private ADimDef() {}
    
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
        return true;
    }
    
}
