/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.value;

import static com.ixaris.commons.misc.lib.object.Tuple.tuple;

import com.ixaris.commons.misc.lib.object.EqualsUtil;
import com.ixaris.commons.misc.lib.object.Tuple2;

/**
 * Ordered String value object
 *
 * @author <a href="mailtoolivia-ann.grech@ixaris.com">Olivia Grech</a>
 */
public final class OrderedStringValue extends Value implements Comparable<OrderedStringValue> {
    
    public static Builder newBuilder() {
        return new Builder();
    }
    
    public static final class Builder implements Value.Builder<OrderedStringValue> {
        
        @Override
        public final int getNumberOfParts() {
            return 2;
        }
        
        @Override
        public final OrderedStringValue buildFromPersisted(final PersistedValue persistedValue) {
            if (persistedValue == null) {
                throw new IllegalArgumentException("persistedValue is null");
            }
            if (persistedValue.getStringValue() == null) {
                throw new IllegalArgumentException("String part is null");
            }
            if (persistedValue.getLongValue() == null) {
                throw new IllegalArgumentException("Long part is null");
            }
            
            return new OrderedStringValue(persistedValue.getStringValue(), persistedValue.getLongValue());
        }
        
        @Override
        public final OrderedStringValue buildFromStringParts(final String... parts) {
            if (parts == null) {
                throw new IllegalArgumentException("parts is null");
            }
            if (parts.length != 2) {
                throw new IllegalArgumentException("Should have 2 parts. Found " + parts.length);
            }
            if (parts[0] == null || parts[1] == null) {
                throw new IllegalArgumentException("a part is null");
            }
            
            return new OrderedStringValue(parts[0], Long.parseLong(parts[1]));
        }
    }
    
    private final String value;
    private final long order;
    
    public OrderedStringValue(final String value, final long order) {
        this.value = value;
        this.order = order;
    }
    
    @Override
    public final String[] getStringParts() {
        return new String[] { value, Long.toString(order) };
    }
    
    @Override
    public final PersistedValue getPersistedValue() {
        return new PersistedValue(order, value);
    }
    
    public final long getOrder() {
        return order;
    }
    
    public final Tuple2<String, Long> getValue() {
        return tuple(value, order);
    }
    
    @Override
    public final boolean equals(final Object o) {
        return EqualsUtil.equals(this, o, other -> value.equals(other.value) && (order == other.order));
    }
    
    @Override
    public int hashCode() {
        return value.hashCode();
    }
    
    @Override
    public final String toString() {
        return "[ " + Long.toString(order) + " ] " + value;
    }
    
    @Override
    public final int compareTo(final OrderedStringValue value) {
        long tmp = order - value.getOrder();
        
        return (tmp == 0) ? 0 : (tmp > 0) ? 1 : -1;
    }
}
