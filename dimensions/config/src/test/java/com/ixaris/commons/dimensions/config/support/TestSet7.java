/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.support;

import com.ixaris.commons.dimensions.config.AbstractSetDef;
import com.ixaris.commons.dimensions.config.value.StringValue;
import com.ixaris.commons.dimensions.lib.context.ContextDef;

public final class TestSet7 extends AbstractSetDef<StringValue> {
    
    public static final String KEY = "TEST_SET7";
    
    private static final TestSet7 INSTANCE = new TestSet7();
    
    public static TestSet7 getInstance() {
        return INSTANCE;
    }
    
    private TestSet7() {
        super("Test Not set Property", new ContextDef(), null, true);
    }
    
    @Override
    public String getKey() {
        return KEY;
    }
    
}
