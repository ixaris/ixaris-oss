/*
 * Copyright 2002, 2014 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.support;

import com.ixaris.commons.dimensions.config.AbstractValueDef;
import com.ixaris.commons.dimensions.config.value.LongValue;
import com.ixaris.commons.dimensions.lib.base.DimensionalDef;
import com.ixaris.commons.dimensions.lib.context.ContextDef;

public final class TestValue7 extends AbstractValueDef<LongValue> {
    
    private static final TestValue7 INSTANCE = new TestValue7();
    
    public static TestValue7 getInstance() {
        return INSTANCE;
    }
    
    public static final String KEY = "TEST_PROP7";
    
    public TestValue7() {
        super("Test Long Property with SMALLER THAN dimension", new ContextDef(FDimDef.getInstance()), null);
    }
    
    /**
     * @see DimensionalDef#getKey()
     */
    @Override
    public String getKey() {
        return KEY;
    }
    
}
