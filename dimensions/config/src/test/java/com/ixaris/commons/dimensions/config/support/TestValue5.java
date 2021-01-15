/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.support;

import com.ixaris.commons.dimensions.config.AbstractValueDef;
import com.ixaris.commons.dimensions.config.value.LongValue;
import com.ixaris.commons.dimensions.lib.context.ContextDef;

public final class TestValue5 extends AbstractValueDef<LongValue> {
    
    private static final TestValue5 INSTANCE = new TestValue5();
    
    public static TestValue5 getInstance() {
        return INSTANCE;
    }
    
    public static final String KEY = "TEST_PROP5";
    
    private TestValue5() {
        super("Test Long Property with catch all dimension", new ContextDef(DDimDef.getInstance()), null);
    }
    
    @Override
    public String getKey() {
        return KEY;
    }
    
}
