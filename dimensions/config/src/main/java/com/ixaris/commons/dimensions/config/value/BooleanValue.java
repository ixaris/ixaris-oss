/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.value;

import java.util.Collections;
import java.util.Map;
import java.util.TreeMap;

import com.ixaris.commons.misc.lib.object.EqualsUtil;

/**
 * Boolean value object
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
public final class BooleanValue extends Value {
    
    public static Builder newBuilder() {
        return new Builder();
    }
    
    public static final class Builder implements Value.Builder<BooleanValue> {
        
        /**
         * static list of enumerated values
         */
        public static final Map<String, String> BOOLEAN_ENUMERATED_VALUES;
        
        static {
            final Map<String, String> tmpMap = new TreeMap<>();
            tmpMap.put(Boolean.toString(true), "Yes");
            tmpMap.put(Boolean.toString(false), "No");
            BOOLEAN_ENUMERATED_VALUES = Collections.unmodifiableMap(tmpMap);
        }
        
        /**
         * Boolean values are enumerable: true/false
         *
         * @param part
         * @return true
         */
        @Override
        public boolean isPartFixedValue(final int part) {
            return part == 0;
        }
        
        /**
         * Get the enumerated list of possible values for this value - true/false
         *
         * @param
         * @return the enumerated list
         */
        @Override
        public Map<String, String> getPartFixedValues(final int part) {
            if (part == 0) {
                return BOOLEAN_ENUMERATED_VALUES;
            } else {
                throw new UnsupportedOperationException();
            }
        }
        
        @Override
        public final BooleanValue buildFromPersisted(final PersistedValue persistedValue) {
            if (persistedValue == null) {
                throw new IllegalArgumentException("persistedValue is null");
            }
            if (persistedValue.getStringValue() == null) {
                throw new IllegalArgumentException("String part is null");
            }
            
            return new BooleanValue(Boolean.valueOf(persistedValue.getStringValue()));
        }
        
        @Override
        public final BooleanValue buildFromStringParts(final String... parts) {
            if (parts == null) {
                throw new IllegalArgumentException("parts is null");
            }
            if (parts.length != 1) {
                throw new IllegalArgumentException("Should have 1 part. Found " + parts.length);
            }
            if (parts[0] == null) {
                throw new IllegalArgumentException("Part 0 is null");
            }
            
            return new BooleanValue(Boolean.valueOf(parts[0]));
        }
    }
    
    private final boolean value;
    
    public BooleanValue(final boolean value) {
        this.value = value;
    }
    
    @Override
    public PersistedValue getPersistedValue() {
        return new PersistedValue(null, Boolean.toString(value));
    }
    
    public boolean getValue() {
        return value;
    }
    
    @Override
    public String[] getStringParts() {
        return new String[] { Boolean.toString(value) };
    }
    
    @Override
    public boolean equals(final Object o) {
        return EqualsUtil.equals(this, o, other -> value == other.value);
    }
    
    @Override
    public int hashCode() {
        return Boolean.hashCode(value);
    }
    
    @Override
    public String toString() {
        return Boolean.toString(value);
    }
}
