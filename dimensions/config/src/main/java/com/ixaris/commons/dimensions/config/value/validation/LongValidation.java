/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.value.validation;

import com.ixaris.commons.dimensions.config.value.LongValue;
import com.ixaris.commons.dimensions.lib.context.ConfigValidationException;

/**
 * Range validation on long values
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
public class LongValidation extends SimpleValidation<LongValue> {
    
    /**
     * The lower and upper limit values
     */
    private final long min, max;
    
    /**
     * Constructor
     *
     * <p>The min value must not be greater than the max value.
     *
     * @param min The lower limit value
     * @param max The upper limit value
     */
    public LongValidation(final long min, final long max) {
        // the max value must be >= min value
        if (min > max) {
            throw new IllegalArgumentException("Invalid range: min > max. min: " + min + ", max: " + max);
        }
        
        this.min = min;
        this.max = max;
    }
    
    /**
     * Checks whether an integer satisfies the defined range
     *
     * @param value The value to check to check.
     * @throws ConfigValidationException if the value is invalid
     */
    @Override
    public void validate(final LongValue value) throws ConfigValidationException {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        // check min violation
        if (value.getValue() < min) {
            throw new ConfigValidationException("value [" + value + "] should be >= " + min);
        }
        // check max violation
        if (value.getValue() > max) {
            throw new ConfigValidationException("value [" + value + "] should be <= " + max);
        }
    }
    
    @Override
    public String toString() {
        return "Long validation. Range: (" + min + " - " + max + ")";
    }
    
}
