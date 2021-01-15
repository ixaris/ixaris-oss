/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.value.validation;

/**
 * Base class for value validation
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
public interface Validation {
    
    Type getType();
    
    enum Type {
        SIMPLE, // validation that only needs the value. These validations typically deal with size or format
        CONTEXT, // validation that needs the value and the context. This means that the validation can differ based on
        // the defined context
        CASCADE // Validation that requires the value and the value of the next matching context. This means that less
        // specific values can be
        // used as constraints for more specific values. Also, this type of validation will cascade to all values
        // containing the context
        // to make sure that more specific values still respect the constraints
    }
    
}
