/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.value.validation;

import com.ixaris.commons.dimensions.config.value.AbstractStringValue;
import com.ixaris.commons.dimensions.lib.context.ConfigValidationException;

/**
 * Length validation on string values
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
public class StringLengthValidation extends SimpleValidation<AbstractStringValue> {
    
    /**
     * The lower and upper limit length
     */
    private final int min, max;
    
    /**
     * Constructor
     *
     * <p>The min value must not be greater than the max value.
     *
     * @param min The lower length limit
     * @param max The upper length limit
     */
    public StringLengthValidation(final int min, final int max) {
        // the minimum length should be positive or 0
        if (min < 0) {
            throw new IllegalArgumentException("Negative min length: " + min);
        }
        // the maximum length should be at least 1
        if (max < 1) {
            throw new IllegalArgumentException("Non-positive max length: " + max);
        }
        // the max value must be >= min value
        if (min > max) {
            throw new IllegalArgumentException("Invalid range: min > max. min: " + min + ", max: " + max);
        }
        
        this.min = min;
        this.max = max;
    }
    
    /**
     * Checks whether an string satisfies the defined range
     *
     * @param value The value to check to check.
     * @throws ConfigValidationException if the value is invalid
     */
    @Override
    public void validate(final AbstractStringValue value) throws ConfigValidationException {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        
        // check min length violation
        if (value.getValue().length() < min) {
            throw new ConfigValidationException("The length of [" + value + "] should be greater than, or equal to " + min + " characters");
        }
        
        // check max length violation
        if (value.getValue().length() > max) {
            throw new ConfigValidationException("The length of [" + value + "] should be lesser than, or equal to " + max + " characters");
        }
    }
    
    @Override
    public String toString() {
        return "StringLength (" + min + " - " + max + ")";
    }
    
}
