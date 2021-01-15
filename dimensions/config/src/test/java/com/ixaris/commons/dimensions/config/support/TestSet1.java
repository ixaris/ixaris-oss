/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.support;

import com.ixaris.commons.dimensions.config.AbstractSetDef;
import com.ixaris.commons.dimensions.config.value.StringValue;
import com.ixaris.commons.dimensions.lib.context.ContextDef;

public final class TestSet1 extends AbstractSetDef<StringValue> {
    
    public static final String KEY = "TEST_SET1";
    
    private static final TestSet1 INSTANCE = new TestSet1();
    
    public static TestSet1 getInstance() {
        return INSTANCE;
    }
    
    private TestSet1() {
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
