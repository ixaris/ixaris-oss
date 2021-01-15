/*
 * Copyright 2002, 2015 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.value.validation;

import com.ixaris.commons.dimensions.config.value.IntegerRangeValue;
import com.ixaris.commons.dimensions.lib.context.ConfigValidationException;

/**
 * Validates an integer range against both it lower and upper limits
 *
 * @author <a href="mailto:matthias.portelli@ixaris.com">matthias.portelli</a>
 */
public class IntegerRangeValidation extends SimpleValidation<IntegerRangeValue> {
    
    /**
     * Lower and upper limits
     */
    private final int min, max;
    
    /**
     * Constructor
     *
     * <p>The min value must not be greater than the max value.
     *
     * @param min The lower limit value
     * @param max The upper limit value
     */
    public IntegerRangeValidation(final int min, final int max) {
        // the max value must be >= min value
        if (min > max) {
            throw new IllegalArgumentException("Invalid range: min > max. min: " + min + ", max: " + max);
        }
        
        this.min = min;
        this.max = max;
    }
    
    @Override
    public void validate(final IntegerRangeValue value) throws ConfigValidationException {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        // Check from against min
        if (value.getValueFrom() < min) {
            throw new ConfigValidationException("Range start [" + value + "] should be >= " + min);
        }
        // check from against max
        if (value.getValueFrom() > max) {
            throw new ConfigValidationException("Range start [" + value + "] should be <= " + max);
        }
        // Check to against min
        if (value.getValueTo() < min) {
            throw new ConfigValidationException("Range end [" + value + "] should be >= " + min);
        }
        // Check to against max
        if (value.getValueTo() > max) {
            throw new ConfigValidationException("Range end [" + value + "] should be <= " + max);
        }
    }
    
    @Override
    public String toString() {
        return "Integer Range (" + min + " - " + max + ")";
    }
    
}
