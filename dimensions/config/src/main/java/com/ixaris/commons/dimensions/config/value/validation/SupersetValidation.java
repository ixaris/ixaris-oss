package com.ixaris.commons.dimensions.config.value.validation;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;

import com.ixaris.commons.dimensions.config.SetUpdates;
import com.ixaris.commons.dimensions.config.value.Value;
import com.ixaris.commons.dimensions.lib.context.ConfigValidationException;

public class SupersetValidation<T extends Value> extends CascadeSetValidation<T> {
    
    @Override
    public <V extends T> void validate(final Set<V> set, final Set<V> nextMatchingSet) throws ConfigValidationException {
        // Specific overrides of validate must specify what validations to take place if no matching sets exist.
        if (nextMatchingSet == null) {
            validate(set);
        } else if (!set.containsAll(nextMatchingSet)) {
            throw new ConfigValidationException("Set " + set + " should be a superset of " + nextMatchingSet);
        }
    }
    
    @Override
    public <V extends T> SetUpdates<V> cascadeUpdate(final Set<V> set, final Set<V> rootSet, final SetUpdates<V> rootUpdates) {
        final Set<V> toAdd = new HashSet<>();
        if (!rootUpdates.getAdded().isEmpty()) {
            for (V item : rootUpdates.getAdded()) {
                if (!set.contains(item)) {
                    toAdd.add(item);
                }
            }
        }
        
        if (toAdd.isEmpty()) {
            return null;
        } else {
            return new SetUpdates<>(toAdd, Collections.emptySet());
        }
    }
    
    /**
     * Called whenever validate method is called and a next matching value is not defined. Specific overrides must specify what validations to
     * take place if no matching values exist. By default no checks are performed.
     *
     * @param set Value to be validated
     * @throws ConfigValidationException
     */
    public <V extends T> void validate(final Set<V> set) throws ConfigValidationException {
        // no validation performed by default if there is no matching value
        // override as necessary
    }
    
}
