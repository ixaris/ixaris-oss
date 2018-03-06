package com.ixaris.commons.misc.lib.object;

import java.util.Objects;

/**
 * Example use case:
 *
 * <pre>
 * public boolean equals(final Object o) {
 *     return Equals.equals(this, o, that -> Objects.equals(attribute, that.attribute));
 * }
 * </pre>
 */
public final class EqualsUtil {
    
    @FunctionalInterface
    public interface EqFunction<T> {
        
        boolean apply(T t);
        
    }
    
    @SuppressWarnings("unchecked")
    public static <T> boolean equals(final T t, final Object o, final EqFunction<T> eq) {
        return o == t || (o != null) && Objects.equals(t.getClass(), o.getClass()) && eq.apply((T) o);
    }
    
    @SuppressWarnings("unchecked")
    public static <T> boolean assignableTypeEquals(final T t, final Object o, final EqFunction<T> eq) {
        return o == t || (o != null) && t.getClass().isAssignableFrom(o.getClass()) && eq.apply((T) o);
    }
    
    private EqualsUtil() {}
    
}
