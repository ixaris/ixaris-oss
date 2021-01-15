/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config;

import com.ixaris.commons.dimensions.config.value.Value;
import com.ixaris.commons.dimensions.lib.base.DimensionalDef;
import com.ixaris.commons.misc.lib.registry.NoRegistry;

/**
 * Interface to be defined by property definitions
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
@NoRegistry
public interface ConfigDef<T extends Value> extends DimensionalDef {
    
    /**
     * The value type for this property, subclass of {@link Value}
     *
     * @return the value type of the property. Never null
     */
    Class<T> getType();
    
    /**
     * @return the builder for the value type
     */
    Value.Builder<T> getValueBuilder();
    
    /**
     * Returning false will cause an IllegalStateException to be thrown if a property is not defined for a context
     *
     * @return true if an unconfigured value is expected, false otherwise
     */
    boolean isNullExpected();
    
}
