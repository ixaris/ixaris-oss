/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.value;

import com.ixaris.commons.misc.lib.object.EqualsUtil;

public final class LongValue extends Value implements Comparable<LongValue> {
    
    public static Builder newBuilder() {
        return new Builder();
    }
    
    public static final class Builder implements Value.Builder<LongValue> {
        
        @Override
        public final LongValue buildFromPersisted(final PersistedValue persistedValue) {
            if (persistedValue == null) {
                throw new IllegalArgumentException("persistedValue is null");
            }
            if (persistedValue.getLongValue() == null) {
                throw new IllegalArgumentException("Long part is null");
            }
            
            return new LongValue(persistedValue.getLongValue());
        }
        
        @Override
        public final LongValue buildFromStringParts(final String... parts) {
            if (parts == null) {
                throw new IllegalArgumentException("parts is null");
            }
            if (parts.length != 1) {
                throw new IllegalArgumentException("Should have 1 part. Found " + parts.length);
            }
            if (parts[0] == null) {
                throw new IllegalArgumentException("Part 0 is null");
            }
            
            return new LongValue(Long.valueOf(parts[0]));
        }
    }
    
    private final long value;
    
    public LongValue(final long value) {
        this.value = value;
    }
    
    @Override
    public String[] getStringParts() {
        return new String[] { Long.toString(value) };
    }
    
    @Override
    public PersistedValue getPersistedValue() {
        return new PersistedValue(value, null);
    }
    
    public Long getValue() {
        return value;
    }
    
    @Override
    public boolean equals(final Object o) {
        return EqualsUtil.equals(this, o, other -> value == other.value);
    }
    
    @Override
    public int hashCode() {
        return Long.hashCode(value);
    }
    
    @Override
    public String toString() {
        return Long.toString(value);
    }
    
    @Override
    public int compareTo(final LongValue o) {
        return Long.compare(value, o.value);
    }
}
