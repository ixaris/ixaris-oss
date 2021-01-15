/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.value;

import com.ixaris.commons.misc.lib.object.EqualsUtil;

/**
 * Timestamp value object
 *
 * @author <a href="mailto:olivia-ann.grech@ixaris.com">Olivia Grech</a>
 */
public abstract class AbstractStringValue extends Value {
    
    public abstract static class Builder<T extends AbstractStringValue> implements Value.Builder<T> {
        
        protected abstract T build(String value);
        
        @Override
        public final T buildFromPersisted(final PersistedValue persistedValue) {
            if (persistedValue == null) {
                throw new IllegalArgumentException("persistedValue is null");
            }
            if (persistedValue.getStringValue() == null) {
                throw new IllegalArgumentException("String part is null");
            }
            
            return build(persistedValue.getStringValue());
        }
        
        @Override
        public final T buildFromStringParts(final String... parts) {
            if (parts == null) {
                throw new IllegalArgumentException("parts is null");
            }
            if (parts.length != 1) {
                throw new IllegalArgumentException("Should have 1 part. Found " + parts.length);
            }
            if (parts[0] == null || parts[0].length() == 0) {
                throw new IllegalArgumentException("part 0 is null or empty");
            }
            
            return build(parts[0]);
        }
    }
    
    protected final String value;
    
    public AbstractStringValue(final String value) {
        this.value = value;
    }
    
    public abstract String getValue();
    
    @Override
    public final String[] getStringParts() {
        return new String[] { value };
    }
    
    @Override
    public final PersistedValue getPersistedValue() {
        return new PersistedValue(null, value);
    }
    
    @Override
    public final boolean equals(final Object o) {
        return EqualsUtil.equals(this, o, other -> value.equals(other.value));
    }
    
    @Override
    public final int hashCode() {
        return value.hashCode();
    }
    
    @Override
    public String toString() {
        return value;
    }
}
