/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.lib.dimension;

import com.ixaris.commons.dimensions.lib.context.PartialContext;

public final class PDimDef extends StringDimensionDef<Part> {
    
    private static final PDimDef INSTANCE = new PDimDef();
    
    public static PDimDef getInstance() {
        return INSTANCE;
    }
    
    private PDimDef() {}
    
    @Override
    protected String convertNonNullValueToString(final Part value) {
        return value.getKey();
    }
    
    @Override
    protected Part getValueFromSpecificString(final String str, final PartialContext context) {
        return Part.fromString(str);
    }
    
    @Override
    public boolean isCacheable() {
        return true;
    }
    
    @Override
    public boolean isHierarchical() {
        return true;
    }
    
    @Override
    public byte getMaxDepth() {
        return 1;
    }
    
}
