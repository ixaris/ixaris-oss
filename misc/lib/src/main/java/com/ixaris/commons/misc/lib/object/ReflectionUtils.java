package com.ixaris.commons.misc.lib.object;

import java.lang.reflect.Method;

public final class ReflectionUtils {
    
    private ReflectionUtils() {}
    
    /**
     * Determine whether the given method is an "equals" method.
     */
    public static boolean isEqualsMethod(final Method method) {
        if ((method == null) || !"equals".equals(method.getName())) {
            return false;
        }
        final Class<?>[] paramTypes = method.getParameterTypes();
        return (paramTypes.length == 1) && (paramTypes[0] == Object.class);
    }
    
    /**
     * Determine whether the given method is a "hashCode" method.
     */
    public static boolean isHashCodeMethod(final Method method) {
        return (method != null) && "hashCode".equals(method.getName()) && (method.getParameterTypes().length == 0);
    }
    
    /**
     * Determine whether the given method is a "toString" method.
     */
    public static boolean isToStringMethod(final Method method) {
        return (method != null) && "toString".equals(method.getName()) && (method.getParameterTypes().length == 0);
    }
    
}
