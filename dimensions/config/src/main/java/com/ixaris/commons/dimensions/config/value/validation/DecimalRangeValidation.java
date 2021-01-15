/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.value.validation;

import java.math.BigDecimal;

import com.ixaris.commons.dimensions.config.value.AbstractDecimalValue;
import com.ixaris.commons.dimensions.lib.context.ConfigValidationException;

/**
 * Range validation on decimal values
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
public class DecimalRangeValidation extends SimpleValidation<AbstractDecimalValue> {
    
    /**
     * The lower and upper limit values
     */
    private final BigDecimal min, max;
    
    /**
     * Constructor
     *
     * <p>The min value must not be greater than the max value.
     *
     * @param min The lower limit value
     * @param max The upper limit value
     */
    public DecimalRangeValidation(final BigDecimal min, final BigDecimal max) {
        // the max value must be >= min value
        if (min == null) {
            throw new IllegalArgumentException("min is null");
        }
        if (max == null) {
            throw new IllegalArgumentException("max is null");
        }
        if (min.compareTo(max) > 0) {
            throw new IllegalArgumentException("Invalid range: min > max. min: " + min + ", max: " + max);
        }
        
        this.min = min;
        this.max = max;
    }
    
    /**
     * Checks whether a decimal satisfies the defined range
     *
     * @param value The value to check to check.
     * @throws ConfigValidationException if the value is invalid
     */
    @Override
    public void validate(final AbstractDecimalValue value) throws ConfigValidationException {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        // check min violation
        if (value.getValue().compareTo(min) < 0) {
            throw new ConfigValidationException("value [" + value + "] should be >= " + min);
        }
        // check max violation
        if (value.getValue().compareTo(max) > 0) {
            throw new ConfigValidationException("value [" + value + "] should be <= " + max);
        }
    }
    
    @Override
    public String toString() {
        return "DecimalRange (" + min + " - " + max + ")";
    }
    
}
