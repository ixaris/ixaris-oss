/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.support;

import com.ixaris.commons.dimensions.config.AbstractValueDef;
import com.ixaris.commons.dimensions.config.value.StringValue;
import com.ixaris.commons.dimensions.lib.context.ContextDef;

public final class TestValue2 extends AbstractValueDef<StringValue> {
    
    private static final TestValue2 INSTANCE = new TestValue2();
    
    public static TestValue2 getInstance() {
        return INSTANCE;
    }
    
    public static final String KEY = "TEST_PROP2";
    
    private TestValue2() {
        super("Test String Property", new ContextDef(HDimDef.getInstance()), null);
    }
    
    @Override
    public String getKey() {
        return KEY;
    }
    
}
