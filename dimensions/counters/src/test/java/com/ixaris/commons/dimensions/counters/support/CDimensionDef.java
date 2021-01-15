/*
 * Copyright 2002, 2011 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.counters.support;

import com.ixaris.commons.dimensions.lib.context.PartialContext;
import com.ixaris.commons.dimensions.lib.dimension.LongDimensionDef;

public final class CDimensionDef extends LongDimensionDef<Long> {
    
    private static final CDimensionDef INSTANCE = new CDimensionDef();
    
    public static CDimensionDef getInstance() {
        return INSTANCE;
    }
    
    private CDimensionDef() {}
    
    @Override
    public long convertNonNullValueToLong(final Long value) {
        return value;
    }
    
    @Override
    public Long getValueFromSpecificLong(final long l, final PartialContext context) {
        return l;
    }
    
    @Override
    public String getStringValue(final Long value) {
        return value.toString();
    }
    
    @Override
    public boolean isCacheable() {
        return false;
    }
    
}
