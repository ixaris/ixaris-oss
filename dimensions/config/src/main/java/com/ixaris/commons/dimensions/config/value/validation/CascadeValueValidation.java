package com.ixaris.commons.dimensions.config.value.validation;

import com.ixaris.commons.dimensions.config.value.Value;
import com.ixaris.commons.dimensions.lib.context.ConfigValidationException;

public abstract class CascadeValueValidation<T extends Value> implements ValueValidation<T> {
    
    /**
     * Validates the value of a context property value.
     *
     * <p>Should not assume that value is of the required type. Type checking is a must
     *
     * @param value the value to check
     * @param nextMatchingValue may be null if no next matching value
     * @throws ConfigValidationException if the value is invalid
     */
    public abstract <V extends T> void validate(V value, V nextMatchingValue) throws ConfigValidationException;
    
    /**
     * Validates the value of a context property value.
     *
     * <p>Should not assume that value is of the required type. Type checking is a must
     *
     * @param value the value to update
     * @param rootValue the value that triggered the cascade update
     * @return return the updated value or null if no change
     * @throws ConfigValidationException if the value is invalid
     */
    public abstract <V extends T> V cascadeUpdate(V value, V rootValue);
    
    @Override
    public final Type getType() {
        return Type.CASCADE;
    }
    
}
