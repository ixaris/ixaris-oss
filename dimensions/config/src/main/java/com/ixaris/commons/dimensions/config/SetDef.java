/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config;

import com.ixaris.commons.dimensions.config.CommonsDimensionsConfig.ConfigSetDef;
import com.ixaris.commons.dimensions.config.value.Value;
import com.ixaris.commons.dimensions.config.value.validation.SetValidation;
import com.ixaris.commons.misc.lib.registry.Registerable;

/**
 * Interface to be defined by set definitions
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
public interface SetDef<T extends Value> extends ConfigDef<T>, Registerable {
    
    /**
     * Note: type checking is performed by default, i.e. a StringValue can never be applied to a LongValue property.
     *
     * @return an object that validates the property value. May be null if no validation is required.
     */
    SetValidation<? super T> getValidation();
    
    /**
     * Not all sets should be cached.
     *
     * <p>Some sets are large with a low read frequency, and should not be cached.
     *
     * <p>If true, the set values will NOT be fetched eagerly from db, and will NOT be displayed in admin console. [Value-Parts] Search,
     * AddOrUpdate, Remove will be displayed instead of all values.
     *
     * @return true if this set can be cached, false otherwise
     */
    boolean isCacheable();
    
    /**
     * Some sets are incremental, i.e. buildList up incrementally from all matching sets.
     *
     * @return true if this set is incremental, false otherwise
     */
    boolean isIncremental();
    
    ConfigSetDef toProtobuf();
    
}
