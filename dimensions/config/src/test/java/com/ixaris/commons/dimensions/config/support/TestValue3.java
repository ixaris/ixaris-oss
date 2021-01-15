/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.support;

import com.ixaris.commons.dimensions.config.AbstractValueDef;
import com.ixaris.commons.dimensions.config.value.LongValue;
import com.ixaris.commons.dimensions.config.value.validation.MaxCascadeValueValidation;
import com.ixaris.commons.dimensions.lib.context.ContextDef;

public final class TestValue3 extends AbstractValueDef<LongValue> {
    
    private static final TestValue3 INSTANCE = new TestValue3();
    
    public static TestValue3 getInstance() {
        return INSTANCE;
    }
    
    public static final String KEY = "TEST_PROP3";
    
    private TestValue3() {
        super("Test Long Property", new ContextDef(PDimDef.getInstance()), new MaxCascadeValueValidation<>());
    }
    
    @Override
    public String getKey() {
        return KEY;
    }
    
}
