/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.value;

import com.ixaris.commons.misc.lib.object.EqualsUtil;

/**
 * Integer range value object. Uses high 20 bits for FROM, low 12 bits for TO
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
public final class IntegerRangeValue extends Value {
    
    public static Builder newBuilder() {
        return new Builder();
    }
    
    public static final class Builder implements Value.Builder<IntegerRangeValue> {
        
        /**
         * @return 2, FROM and TO
         */
        @Override
        public int getNumberOfParts() {
            return 2;
        }
        
        @Override
        public final IntegerRangeValue buildFromPersisted(final PersistedValue persistedValue) {
            if (persistedValue == null) {
                throw new IllegalArgumentException("persistedValue is null");
            }
            if (persistedValue.getStringValue() == null) {
                throw new IllegalArgumentException("String part is null");
            }
            
            return new IntegerRangeValue((int) (persistedValue.getLongValue() >>> 32), (int) (persistedValue.getLongValue() & 0xFFFFFFFFL));
        }
        
        @Override
        public final IntegerRangeValue buildFromStringParts(final String... parts) {
            if (parts == null) {
                throw new IllegalArgumentException("parts is null");
            }
            if (parts.length != 2) {
                throw new IllegalArgumentException("Should have 2 parts. Found " + parts.length);
            }
            if ((parts[0] == null) || (parts[1] == null)) {
                throw new IllegalArgumentException("A part is null");
            }
            
            return new IntegerRangeValue(Integer.parseInt(parts[0]), Integer.parseInt(parts[1]));
        }
    }
    
    private final int valueFrom;
    private final int valueTo;
    
    public IntegerRangeValue(final int valueFrom, final int valueTo) {
        validate(valueFrom, valueTo);
        this.valueFrom = valueFrom;
        this.valueTo = valueTo;
    }
    
    @Override
    public String[] getStringParts() {
        return new String[] { Integer.toString(getValueFrom()), Integer.toString(getValueTo()) };
    }
    
    @Override
    public PersistedValue getPersistedValue() {
        return new PersistedValue(toLong(), null);
    }
    
    public int getValueFrom() {
        return valueFrom;
    }
    
    public int getValueTo() {
        return valueTo;
    }
    
    @Override
    public final boolean equals(final Object o) {
        return EqualsUtil.equals(this, o, other -> (valueFrom == other.valueFrom) && (valueTo == other.valueTo));
    }
    
    @Override
    public final int hashCode() {
        return Long.hashCode(toLong());
    }
    
    @Override
    public String toString() {
        return getValueFrom() + " - " + getValueTo();
    }
    
    private long toLong() {
        return (long) valueFrom << 32 | valueTo;
    }
    
    /**
     * @param valueFrom
     * @param valueTo
     */
    private void validate(final int valueFrom, final int valueTo) {
        if (valueFrom > valueTo) {
            throw new IllegalArgumentException("From [" + valueFrom + "] should be smaller than to [" + valueTo + "]");
        }
    }
}
