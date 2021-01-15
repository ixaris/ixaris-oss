/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.value.validation;

import java.time.ZoneOffset;
import java.time.ZonedDateTime;

import com.ixaris.commons.dimensions.config.value.AbstractDateValue;
import com.ixaris.commons.dimensions.lib.context.ConfigValidationException;

/**
 * Range validation on integer values
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
public class FutureDateValidation extends SimpleValidation<AbstractDateValue> {
    
    /**
     * Checks whether an integer satisfies the defined range
     *
     * @param value The value to check to check.
     * @throws ConfigValidationException if the value is invalid
     */
    @Override
    public void validate(final AbstractDateValue value) throws ConfigValidationException {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        
        if (value.getValue().isBefore(ZonedDateTime.now(ZoneOffset.UTC))) {
            throw new ConfigValidationException("Date should be in the future");
        }
    }
    
    @Override
    public String toString() {
        return "FutureDate";
    }
    
}
