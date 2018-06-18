package com.ixaris.commons.misc.lib.object;

import java.lang.reflect.Field;

public final class Unsafe {

    public static final sun.misc.Unsafe UNSAFE;

    static {
        try {
            final Field f = sun.misc.Unsafe.class.getDeclaredField("theUnsafe");
            f.setAccessible(true);
            UNSAFE = (sun.misc.Unsafe)f.get(null);
        } catch (final Exception e) {
            throw new Error(e);
        }
    }

}
