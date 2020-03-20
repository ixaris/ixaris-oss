package com.ixaris.commons.microservices.defaults.test;

import java.util.Collection;
import java.util.Collections;
import java.util.Objects;
import java.util.Set;

import com.google.common.collect.ImmutableSet;
import com.google.protobuf.GeneratedMessageV3;

public final class MessageLiteEqualityHelper {
    
    public static <T extends GeneratedMessageV3> boolean isEqualToExcludingFields(final T actual, final T other, final String... exclusions) {
        final Set<String> excludedFields = exclusions == null ? Collections.emptySet() : ImmutableSet.copyOf(exclusions);
        return actual
            .getAllFields()
            .keySet()
            .stream()
            .filter(fieldDescriptor -> !excludedFields.contains(fieldDescriptor.getName()))
            .allMatch(fieldDescriptor -> Objects.equals(actual.getField(fieldDescriptor), other.getField(fieldDescriptor)));
    }
    
    public static <T extends GeneratedMessageV3> boolean collectionElementsEqualExcludingFields(final Collection<T> actual,
                                                                                                final Collection<T> other,
                                                                                                final String... exclusions) {
        final boolean everyActualHasMatchingElementInOther = actual.stream()
            .map(actualElement -> other.stream().anyMatch(otherElement -> isEqualToExcludingFields(actualElement, otherElement, exclusions)))
            .reduce(true, Boolean::logicalAnd);
        return actual.size() == other.size() && everyActualHasMatchingElementInOther;
    }
    
    private MessageLiteEqualityHelper() {}
    
}
