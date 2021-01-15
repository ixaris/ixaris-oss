/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.lib.dimension;

import com.ixaris.commons.dimensions.lib.context.PartialContext;

public abstract class StringBasedDimensionDef extends StringDimensionDef<String> {
    
    @Override
    protected final String getValueFromSpecificString(final String str, final PartialContext context) {
        return str;
    }
    
    @Override
    protected final String convertNonNullValueToString(final String value) {
        return value;
    }
    
}
