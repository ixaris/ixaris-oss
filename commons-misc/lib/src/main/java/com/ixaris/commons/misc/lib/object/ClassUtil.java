package com.ixaris.commons.misc.lib.object;

public class ClassUtil {
    
    public static boolean isInstanceOf(final Object instance, final Class<?>... matchClasses) {
        if (instance == null) {
            return false;
        }
        return isSameOrSubtypeOf(instance.getClass(), matchClasses);
    }
    
    public static boolean isSameOrSubtypeOf(final Class<?> maybeSubclass, final Class<?>... matchClasses) {
        if (matchClasses.length == 0) {
            return true;
        }
        for (final Class<?> cls : matchClasses) {
            if (cls.isAssignableFrom(maybeSubclass)) {
                return true;
            }
        }
        return false;
    }
    
}
