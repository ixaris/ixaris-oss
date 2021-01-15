/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.lib.dimension;

import java.util.Map;
import java.util.TreeMap;

import com.ixaris.commons.dimensions.lib.context.PartialContext;
import com.ixaris.commons.misc.lib.registry.Registerable;
import com.ixaris.commons.misc.lib.registry.Registry;

/**
 * dimensions with a registry item value
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
public abstract class RegistryBasedDimensionDef<R extends Registerable> extends StringHierarchyDimensionDef<R> {
    
    protected abstract Registry<? super R> getRegistry();
    
    @Override
    protected String convertNonNullValueToString(final R value) {
        return value.getKey();
    }
    
    @Override
    @SuppressWarnings("unchecked")
    protected R getValueFromSpecificString(final String str, final PartialContext context) {
        return (R) getRegistry().resolve(str);
    }
    
    @Override
    public boolean isMatchAnySupported() {
        return false;
    }
    
    @Override
    public boolean isHierarchical() {
        return false;
    }
    
    @Override
    public final boolean isCacheable() {
        return true;
    }
    
    @Override
    public final boolean isFixedValue() {
        return true;
    }
    
    @Override
    public Map<String, String> getFixedValues() {
        final Map<String, String> result = new TreeMap<>();
        for (final Registerable item : getRegistry().getRegisteredValues()) {
            result.put(item.getKey(), item.toString());
        }
        return result;
    }
    
}
