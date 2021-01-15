/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.support;

import com.ixaris.commons.dimensions.config.AbstractSetDef;
import com.ixaris.commons.dimensions.config.value.StringValue;
import com.ixaris.commons.dimensions.config.value.validation.SubsetValidation;
import com.ixaris.commons.dimensions.lib.context.ContextDef;

public final class TestSet3 extends AbstractSetDef<StringValue> {
    
    public static final String KEY = "TEST_SET3";
    
    private static final TestSet3 INSTANCE = new TestSet3();
    
    public static TestSet3 getInstance() {
        return INSTANCE;
    }
    
    private TestSet3() {
        super("Test String Property", new ContextDef(PDimDef.getInstance()), new SubsetValidation<>(), true);
    }
    
    @Override
    public String getKey() {
        return KEY;
    }
    
}
