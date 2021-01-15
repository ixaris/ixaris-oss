/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config;

import com.ixaris.commons.dimensions.config.value.Value;
import com.ixaris.commons.dimensions.lib.context.Context;

/**
 * Helper class to represent a default context value
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
public class ContextValue<T extends Value> {
    
    private final Context context;
    private final T value;
    
    public ContextValue(final Context context, T value) {
        if (context == null) {
            throw new IllegalArgumentException("context is null");
        }
        
        this.context = context;
        this.value = value;
    }
    
    public Context getContext() {
        return context;
    }
    
    public T getValue() {
        return value;
    }
    
}
