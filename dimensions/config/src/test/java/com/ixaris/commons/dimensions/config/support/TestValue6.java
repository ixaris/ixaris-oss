/*
 * Copyright 2002, 2014 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.support;

import com.ixaris.commons.dimensions.config.AbstractValueDef;
import com.ixaris.commons.dimensions.config.value.LongValue;
import com.ixaris.commons.dimensions.lib.base.DimensionalDef;
import com.ixaris.commons.dimensions.lib.context.ContextDef;

public final class TestValue6 extends AbstractValueDef<LongValue> {
    
    private static final TestValue6 INSTANCE = new TestValue6();
    
    public static TestValue6 getInstance() {
        return INSTANCE;
    }
    
    public static final String KEY = "TEST_PROP6";
    
    public TestValue6() {
        super("Test Long Property with LARGER_THAN dimension", new ContextDef(EDimDef.getInstance()), null);
    }
    
    /**
     * @see DimensionalDef#getKey()
     */
    @Override
    public String getKey() {
        return KEY;
    }
    
}
