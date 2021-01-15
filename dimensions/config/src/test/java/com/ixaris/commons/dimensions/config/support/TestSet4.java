/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.support;

import com.ixaris.commons.dimensions.config.AbstractSetDef;
import com.ixaris.commons.dimensions.config.value.StringValue;
import com.ixaris.commons.dimensions.config.value.validation.SupersetValidation;
import com.ixaris.commons.dimensions.lib.context.ContextDef;

public final class TestSet4 extends AbstractSetDef<StringValue> {
    
    public static final String KEY = "TEST_SET4";
    
    private static final TestSet4 INSTANCE = new TestSet4();
    
    public static TestSet4 getInstance() {
        return INSTANCE;
    }
    
    private TestSet4() {
        super("Test String Property", new ContextDef(PDimDef.getInstance()), new SupersetValidation<>(), true);
    }
    
    @Override
    public String getKey() {
        return KEY;
    }
    
}
