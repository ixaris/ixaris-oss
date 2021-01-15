/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.support;

import com.ixaris.commons.dimensions.config.AbstractValueDef;
import com.ixaris.commons.dimensions.config.value.StringValue;
import com.ixaris.commons.dimensions.lib.context.ContextDef;

public final class TestValue1 extends AbstractValueDef<StringValue> {
    
    private static final TestValue1 INSTANCE = new TestValue1();
    
    public static TestValue1 getInstance() {
        return INSTANCE;
    }
    
    public static final String KEY = "TEST_PROP1";
    
    private TestValue1() {
        super("Test String Property", new ContextDef(ADimDef.getInstance(), BDimDef.getInstance(), CDimDef.getInstance()), null);
    }
    
    @Override
    public boolean isNullExpected() {
        return true;
    }
    
    @Override
    public String getKey() {
        return KEY;
    }
    
}
