/*
 * Copyright 2002, 2009 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.lib.dimension;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.ixaris.commons.dimensions.lib.context.Dimension;
import com.ixaris.commons.dimensions.lib.context.PartialContext;
import com.ixaris.commons.dimensions.lib.context.PersistedDimensionValue;

/**
 * dimensions with a boolean value
 *
 * @author <a href="mailto:jimmy.borg@ixaris.com">JB</a>
 */
public abstract class BooleanDimensionDef extends AbstractDimensionDef<Boolean> {
    
    /**
     * static list of enumerated values
     */
    public static final Map<String, String> BOOLEAN_ENUMERATED_VALUES;
    
    static {
        final Map<String, String> tmpMap = new TreeMap<String, String>();
        tmpMap.put(Boolean.toString(true), "Yes");
        tmpMap.put(Boolean.toString(false), "No");
        BOOLEAN_ENUMERATED_VALUES = Collections.unmodifiableMap(tmpMap);
    }
    
    /**
     * @param value
     * @return the new boolean dimension instance
     */
    @Override
    public final Dimension<Boolean> create(final Boolean value) {
        return new Dimension<Boolean>(value, this);
    }
    
    /**
     * Creates this dimension from a persisted value
     *
     * @param persistedValue
     * @param context
     */
    @Override
    public final Dimension<Boolean> createFromPersistedValue(final PersistedDimensionValue persistedValue, final PartialContext context) {
        if (persistedValue == null) {
            throw new IllegalArgumentException("persistedValue is null");
        }
        
        return createFromString(persistedValue.getStringValue(), context);
    }
    
    /**
     * Creates this dimension from a string
     *
     * @param stringValue
     * @param context
     */
    @Override
    public final Dimension<Boolean> createFromString(final String stringValue, final PartialContext context) {
        if (stringValue == null) {
            throw new IllegalArgumentException("stringValue is null");
        }
        
        return create(Boolean.valueOf(stringValue));
    }
    
    @Override
    public final PersistedDimensionValue getPersistedValue(final Boolean value) {
        return new PersistedDimensionValue(Boolean.toString(value));
    }
    
    @Override
    public final String getStringValue(final Boolean value) {
        return Boolean.toString(value);
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
    public final Map<String, String> getFixedValues() {
        return BOOLEAN_ENUMERATED_VALUES;
    }
    
    @Override
    public final ValueMatch getValueMatch() {
        return ValueMatch.STRING;
    }
    
    /**
     * Final - cannot be overridden as CATCH ALL
     */
    @Override
    public final boolean isMatchAnySupported() {
        return false;
    }
    
}
