/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.support;

import com.ixaris.commons.dimensions.config.AbstractSetDef;
import com.ixaris.commons.dimensions.config.value.StringValue;
import com.ixaris.commons.dimensions.lib.context.ContextDef;

public final class TestSet6 extends AbstractSetDef<StringValue> {
    
    public static final String KEY = "TEST_SET6";
    
    private static final TestSet6 INSTANCE = new TestSet6();
    
    public static TestSet6 getInstance() {
        return INSTANCE;
    }
    
    private TestSet6() {
        super("Test String Property", new ContextDef(ADimDef.getInstance(), BDimDef.getInstance(), CDimDef.getInstance()), null, true);
    }
    
    @Override
    public String getKey() {
        return KEY;
    }
    
    @Override
    public boolean isNullExpected() {
        return true;
    }
    
}
