/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.support;

import com.ixaris.commons.dimensions.lib.context.PartialContext;
import com.ixaris.commons.dimensions.lib.dimension.LongDimensionDef;

public final class FDimDef extends LongDimensionDef<Long> {
    
    private static final FDimDef INSTANCE = new FDimDef();
    
    public static FDimDef getInstance() {
        return INSTANCE;
    }
    
    private FDimDef() {}
    
    @Override
    public final Long getValueFromSpecificLong(final long l, final PartialContext context) {
        return l;
    }
    
    @Override
    public long convertNonNullValueToLong(final Long value) {
        return value == null ? MATCH_ANY : value;
    }
    
    @Override
    public String getStringValue(final Long value) {
        return value.toString();
    }
    
    @Override
    public final boolean isCacheable() {
        return false;
    }
    
    @Override
    public Range getRange() {
        return Range.SMALLER;
    }
    
}
