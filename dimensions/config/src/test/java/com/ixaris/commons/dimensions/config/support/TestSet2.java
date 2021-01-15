/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.support;

import com.ixaris.commons.dimensions.config.AbstractIncrementalSetDef;
import com.ixaris.commons.dimensions.config.value.StringValue;
import com.ixaris.commons.dimensions.lib.context.ContextDef;

public final class TestSet2 extends AbstractIncrementalSetDef<StringValue> {
    
    public static final String KEY = "TEST_SET2";
    
    private static final TestSet2 INSTANCE = new TestSet2();
    
    public static TestSet2 getInstance() {
        return INSTANCE;
    }
    
    private TestSet2() {
        super("Test String Property", new ContextDef(ADimDef.getInstance(), BDimDef.getInstance(), CDimDef.getInstance()), null, true);
    }
    
    @Override
    public String getKey() {
        return KEY;
    }
    
}
