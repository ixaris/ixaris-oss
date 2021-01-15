package com.ixaris.commons.dimensions.config.value.validation;

import com.ixaris.commons.dimensions.config.value.Value;
import com.ixaris.commons.dimensions.lib.context.ConfigValidationException;

public class MaxCascadeValueValidation<T extends Value> extends CascadeValueValidation<T> {
    
    @Override
    public <V extends T> void validate(final V value, final V nextMatchingValue) throws ConfigValidationException {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        if (!(value instanceof Comparable<?>)) {
            throw new IllegalArgumentException("MaxCascadeValueValidation is only applicable to Comparable values. Value given " + value);
        }
        
        @SuppressWarnings("unchecked")
        final Comparable<V> cv = (Comparable<V>) value;
        
        // Specific overrides of validate must specify what validations to take place if no matching values exist.
        if (nextMatchingValue == null) {
            validate(value);
        } else if (cv.compareTo(nextMatchingValue) > 0) {
            throw new ConfigValidationException("Value [" + value + "] should be smaller than [" + nextMatchingValue + "]");
        }
    }
    
    @Override
    public <V extends T> V cascadeUpdate(final V value, final V rootValue) {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        if (!(value instanceof Comparable<?>)) {
            throw new IllegalArgumentException("MaxCascadeValueValidation is only applicable to Comparable values. Value given " + value);
        }
        
        @SuppressWarnings("unchecked")
        final Comparable<V> cv = (Comparable<V>) value;
        
        // the actual cascade update - if value exceeds limit set by root value, then set value to the limit
        if (cv.compareTo(rootValue) > 0) {
            return rootValue;
        }
        
        return null;
    }
    
    /**
     * Called whenever validate method is called and a next matching value is not defined. Specific overrides must specify what validations to
     * take place if no matching values exist. By default no checks are performed.
     *
     * @param value Value to be validated
     * @throws ConfigValidationException
     */
    public <V extends T> void validate(final V value) throws ConfigValidationException {
        // no validation performed by default if there is no matching value
        // override as necessary
    }
    
}
