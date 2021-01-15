/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config;

import com.ixaris.commons.dimensions.config.CommonsDimensionsConfig.ConfigValueDef;
import com.ixaris.commons.dimensions.config.value.Value;
import com.ixaris.commons.dimensions.config.value.validation.ValueValidation;
import com.ixaris.commons.misc.lib.registry.Registerable;

/**
 * Interface to be defined by property definitions
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
public interface ValueDef<T extends Value> extends ConfigDef<T>, Registerable {
    
    /**
     * Note: type checking is performed by default, i.e. a StringValue can never be applied to an INTEGER property.
     *
     * @return an object that validates the property value. May be null if not validation is required.
     */
    ValueValidation<? super T> getValidation();
    
    /**
     * @return True if the property definition may be deleted. False otherwise.
     */
    boolean isDeletable();
    
    ConfigValueDef toProtobuf();
    
}
