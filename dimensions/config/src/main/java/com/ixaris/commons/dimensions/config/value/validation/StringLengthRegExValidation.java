/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.value.validation;

import java.util.regex.Pattern;

import com.ixaris.commons.dimensions.config.value.AbstractStringValue;
import com.ixaris.commons.dimensions.lib.context.ConfigValidationException;

/**
 * Length and regular expression validation on string values
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
public class StringLengthRegExValidation extends StringLengthValidation {
    
    /**
     * The pattern that the string must follow
     */
    private final Pattern pattern;
    
    /**
     * Constructor
     *
     * <p>The min value must not be greater than the max value.
     *
     * @param min The lower length limit
     * @param max The upper length limit
     * @param regex the Java compliant regular expression that the string must follow
     */
    public StringLengthRegExValidation(final int min, final int max, final String regex) {
        super(min, max);
        
        if (regex == null) {
            throw new IllegalArgumentException("regex is null");
        }
        
        // check if the regular expression is valid
        this.pattern = Pattern.compile(regex);
    }
    
    /**
     * Checks whether an string satisfies the defined range
     *
     * @param value The value to check to check.
     * @throws ConfigValidationException if the value is invalid
     */
    @Override
    public void validate(final AbstractStringValue value) throws ConfigValidationException {
        super.validate(value);
        
        // check regular expression
        if (!pattern.matcher(value.getValue()).matches()) {
            throw new ConfigValidationException("value [" + value + "] should match pattern " + pattern);
        }
    }
    
    @Override
    public String toString() {
        return super.toString() + " + StringRegEx (" + pattern + ")";
    }
    
}
