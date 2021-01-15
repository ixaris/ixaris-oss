/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.lib.dimension;

import java.util.SortedMap;
import java.util.TreeMap;

import com.ixaris.commons.dimensions.lib.context.PartialContext;

/**
 * dimensions with an enum value
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
public abstract class EnumBasedDimensionDef<E extends Enum<E>> extends StringHierarchyDimensionDef<E> {
    
    public EnumBasedDimensionDef() {
        super();
    }
    
    public EnumBasedDimensionDef(final String key) {
        super(key);
    }
    
    protected abstract Class<E> getEnumType();
    
    protected abstract E[] getEnumValues();
    
    @Override
    protected String convertNonNullValueToString(final E value) {
        return value.name();
    }
    
    @Override
    protected E getValueFromSpecificString(final String str, final PartialContext context) {
        return Enum.valueOf(getEnumType(), str);
    }
    
    @Override
    public boolean isMatchAnySupported() {
        return false;
    }
    
    @Override
    public boolean isHierarchical() {
        return false;
    }
    
    @Override
    public final boolean isCacheable() {
        return true;
    }
    
    @Override
    public final boolean isFixedValue() {
        return true;
    }
    
    @Override
    public SortedMap<String, String> getFixedValues() {
        final SortedMap<String, String> result = new TreeMap<>();
        for (final E enumConst : getEnumValues()) {
            if (!enumConst.name().equals("UNRECOGNIZED")) {
                result.put(enumConst.name(), enumConst.toString());
            }
        }
        return result;
    }
    
}
