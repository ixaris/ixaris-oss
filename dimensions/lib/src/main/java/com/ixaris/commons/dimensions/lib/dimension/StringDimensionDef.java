/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.lib.dimension;

public abstract class StringDimensionDef<T> extends StringHierarchyDimensionDef<T> {
    
    @Override
    public boolean isMatchAnySupported() {
        return false;
    }
    
    @Override
    public boolean isHierarchical() {
        return false;
    }
    
}
