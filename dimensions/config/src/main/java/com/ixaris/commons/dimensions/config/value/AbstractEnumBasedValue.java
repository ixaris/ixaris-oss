/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.value;

import java.util.Map;
import java.util.TreeMap;

import com.ixaris.commons.misc.lib.object.EqualsUtil;

/**
 * ContextDef dimensions with a string value
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
public abstract class AbstractEnumBasedValue<E extends Enum<E>> extends Value {
    
    public abstract static class Builder<E extends Enum<E>, T extends AbstractEnumBasedValue<E>> implements Value.Builder<T> {
        
        protected abstract T build(E value);
        
        protected abstract Class<E> getEnumType();
        
        protected abstract E[] getEnumValues();
        
        @Override
        public final boolean isPartFixedValue(final int part) {
            return part == 0;
        }
        
        @Override
        public final Map<String, String> getPartFixedValues(final int part) {
            if (part == 0) {
                final Map<String, String> result = new TreeMap<>();
                for (final E item : getEnumValues()) {
                    result.put(item.name(), item.toString());
                }
                return result;
            } else {
                throw new UnsupportedOperationException();
            }
        }
        
        @Override
        public final T buildFromPersisted(final PersistedValue persistedValue) {
            if (persistedValue == null) {
                throw new IllegalArgumentException("persistedValue is null");
            }
            if (persistedValue.getStringValue() == null) {
                throw new IllegalArgumentException("String part is null");
            }
            
            return build(Enum.valueOf(getEnumType(), persistedValue.getStringValue()));
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
            
            return build(Enum.valueOf(getEnumType(), parts[0]));
        }
    }
    
    private final E value;
    
    public AbstractEnumBasedValue(final E value) {
        this.value = value;
    }
    
    public final E getValue() {
        return value;
    }
    
    @Override
    public final String[] getStringParts() {
        return new String[] { value.name() };
    }
    
    @Override
    public final PersistedValue getPersistedValue() {
        return new PersistedValue(null, value.name());
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
    public final String toString() {
        return value.toString();
    }
    
}
