package com.ixaris.commons.misc.lib.object;

import java.util.Objects;
import java.util.function.BiFunction;

/**
 * Example use case:
 *
 * <pre>
 * public boolean equals(final Object o) {
 *     return Equals.equals(this, o, that -> EqualsUtil.nullableEquals(attribute, that.attribute));
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
        if (o == t) {
            return true;
        } else if ((o == null) || !Objects.equals(t.getClass(), o.getClass())) {
            return false;
        } else {
            return eq.apply((T) o);
        }
    }
    
    @SuppressWarnings("unchecked")
    public static <T> boolean assignableTypeEquals(final T t, final Object o, final BiFunction<T, T, Boolean> eq) {
        if (o == t) {
            return true;
        } else if ((o == null) || !t.getClass().isAssignableFrom(o.getClass())) {
            return false;
        } else {
            return eq.apply(t, (T) o);
        }
    }
    
    @SuppressWarnings("unchecked")
    public static boolean nullableEquals(final Object o1, final Object o2) {
        return Objects.equals(o1, o2);
    }
    
    private EqualsUtil() {}
    
}
