/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.value;

import java.math.BigDecimal;

import com.ixaris.commons.misc.lib.object.EqualsUtil;

/**
 * Timestamp value object
 *
 * @author <a href="mailto:olivia-ann.grech@ixaris.com">Olivia Grech</a>
 */
public abstract class AbstractDecimalValue extends Value implements Comparable<AbstractDecimalValue> {
    
    public abstract static class Builder<T extends AbstractDecimalValue> implements Value.Builder<T> {
        
        protected abstract T build(BigDecimal value);
        
        protected abstract int getLeftShift();
        
        @Override
        public final T buildFromPersisted(final PersistedValue persistedValue) {
            if (persistedValue == null) {
                throw new IllegalArgumentException("persistedValue is null");
            }
            if (persistedValue.getLongValue() == null) {
                throw new IllegalArgumentException("Long part is null");
            }
            
            return build(BigDecimal.valueOf(persistedValue.getLongValue(), getLeftShift()));
        }
        
        @Override
        public final T buildFromStringParts(final String... parts) {
            if (parts == null) {
                throw new IllegalArgumentException("parts is null");
            }
            if (parts.length != 1) {
                throw new IllegalArgumentException("Should have 1 part. Found " + parts.length);
            }
            if (parts[0] == null) {
                throw new IllegalArgumentException("Part 0 is null");
            }
            
            return build(BigDecimal.valueOf(Long.valueOf(parts[0]), getLeftShift()));
        }
    }
    
    protected final BigDecimal value;
    
    public AbstractDecimalValue(final BigDecimal value) {
        this.value = value;
    }
    
    protected abstract int getLeftShift();
    
    @Override
    public final String[] getStringParts() {
        return new String[] { value.toString() };
    }
    
    public final BigDecimal getValue() {
        return value.setScale(getLeftShift(), BigDecimal.ROUND_UNNECESSARY);
    }
    
    @Override
    public final int compareTo(final AbstractDecimalValue o) {
        return value.compareTo(o.value);
    }
    
    @Override
    public final PersistedValue getPersistedValue() {
        return new PersistedValue(this.value.movePointRight(getLeftShift()).longValue(), null);
    }
    
    @Override
    public final boolean equals(final Object o) {
        return EqualsUtil.equals(this, o, other -> value.compareTo(other.value) == 0);
    }
    
    @Override
    public final int hashCode() {
        return value.hashCode();
    }
    
    @Override
    public final String toString() {
        return value.toString();
    }
    
}
