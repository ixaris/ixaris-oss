/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.value;

/**
 * String value object
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
public final class StringValue extends AbstractStringValue {
    
    public static Builder newBuilder() {
        return new Builder();
    }
    
    public static final class Builder extends AbstractStringValue.Builder<StringValue> {
        
        @Override
        protected StringValue build(final String value) {
            return new StringValue(value);
        }
        
    }
    
    public StringValue(final String value) {
        super(value);
    }
    
    @Override
    public String getValue() {
        return value;
    }
    
}
