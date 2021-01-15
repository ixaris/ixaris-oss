package com.ixaris.commons.dimensions.config.value.validation;

import java.util.Set;

import com.ixaris.commons.dimensions.config.SetUpdates;
import com.ixaris.commons.dimensions.config.value.Value;
import com.ixaris.commons.dimensions.lib.context.ConfigValidationException;

public abstract class CascadeSetValidation<T extends Value> implements SetValidation<T> {
    
    /**
     * Validates the value of a context property value.
     *
     * <p>Should not assume that value is of the required type. Type checking is a must
     *
     * @param set value to check
     * @param nextMatchingSet may be null if no next matching set
     * @throws ConfigValidationException ERR_VALUE_VALIDATION_FAILED value failed validation
     */
    public abstract <V extends T> void validate(Set<V> set, Set<V> nextMatchingSet) throws ConfigValidationException;
    
    /**
     * Validates the value of a context property value.
     *
     * <p>Should not assume that value is of the required type. Type checking is a must
     *
     * @param set the set to update
     * @param rootSet the set that triggered the cascade update
     * @param rootUpdates the updates to the root set, may be empty
     * @return return the update to the set (toAdd, toRemove) or null if no change
     * @throws ConfigValidationException if the value is invalid
     */
    public abstract <V extends T> SetUpdates<V> cascadeUpdate(Set<V> set, Set<V> rootSet, SetUpdates<V> rootUpdates);
    
    @Override
    public final Type getType() {
        return Type.CASCADE;
    }
    
}
