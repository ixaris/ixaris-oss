/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config;

import java.util.Set;

import com.ixaris.commons.dimensions.config.value.Value;
import com.ixaris.commons.dimensions.lib.context.Context;

/**
 * Helper class to represent a default context set
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
public class ContextSet<T extends Value> {
    
    private final Context context;
    private final Set<T> set;
    
    public ContextSet(final Context context, final Set<T> set) {
        if (context == null) {
            throw new IllegalArgumentException("context is null");
        }
        
        this.context = context;
        this.set = set;
    }
    
    public Context getContext() {
        return context;
    }
    
    public Set<T> getSet() {
        return set;
    }
    
}
