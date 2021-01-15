/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.lib.dimension;

import com.ixaris.commons.dimensions.lib.context.PartialContext;

public abstract class LongBasedDimensionDef extends LongDimensionDef<Long> {
    
    @Override
    protected final Long getValueFromSpecificLong(final long l, final PartialContext context) {
        return l;
    }
    
    @Override
    protected final long convertNonNullValueToLong(final Long value) {
        return value;
    }
    
    @Override
    public final String getStringValue(final Long value) {
        return value.toString();
    }
    
}
