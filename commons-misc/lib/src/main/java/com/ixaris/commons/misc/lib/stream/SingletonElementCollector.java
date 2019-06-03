package com.ixaris.commons.misc.lib.stream;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;

/**
 * Collector which expects only a single element to remain in the {@link Stream}, and returns it.
 *
 * @author daniel.grech
 */
public final class SingletonElementCollector {
    
    private SingletonElementCollector() {}
    
    /**
     * Collects the {@link Stream} into an {@link Optional} single element, or else throws an {@link
     * IllegalStateException} if there is more than one element. Note that this uses a {@link List} underlying
     * collector, so it expects the stream to actually contain a <strong>single</strong> element - duplicate elements
     * are considered as separate objects and will cause this collector to throw an exception.
     */
    public static <T> Collector<T, ?, Optional<T>> optionallyOneOrThrowIfMore() {
        return Collectors.collectingAndThen(Collectors.toList(), list -> {
            if (list.size() > 1) {
                throw new IllegalStateException("Expected only one element, instead found: " + list);
            } else if (list.size() == 1) {
                return Optional.of(list.get(0));
            } else {
                return Optional.empty();
            }
        });
    }
    
    /**
     * Collects the {@link Stream} into a single element, or else throws an {@link IllegalStateException} if that
     * element is not present or if more than one is found. Note that this uses a {@link List} underlying collector, so
     * it expects the stream to actually contain a <strong>single</strong> element - duplicate elements are considered
     * as separate objects and will cause this collector to throw an exception.
     */
    public static <T> Collector<T, ?, T> exactlyOne() {
        return Collectors.collectingAndThen(Collectors.toList(), list -> {
            if (list.size() == 1) {
                return list.get(0);
            } else {
                throw new IllegalStateException("Expected only one element, instead found: " + list);
            }
        });
    }
    
}
