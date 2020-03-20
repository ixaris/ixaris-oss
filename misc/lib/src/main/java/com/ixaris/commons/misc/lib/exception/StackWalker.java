package com.ixaris.commons.misc.lib.exception;

import java.util.Objects;
import java.util.function.Predicate;

public class StackWalker {
    
    private StackWalker() {}
    
    /**
     * Returns the stack trace element of the immediate caller of the specified class.
     *
     * @param target the target class whose callers we are looking for.
     * @return the stack trace element representing the immediate caller of the specified class, or null if no caller
     * was found (due to incorrect target, wrong skip count or use of JNI).
     */
    public static String findCaller(final Predicate<String> target, final int skip) {
        Objects.requireNonNull(target);
        return java.lang.StackWalker.getInstance().walk(s -> s
            .skip((long) skip + 1L)
            .filter(f -> target.test(f.getClassName()))
            .findFirst()
            .map(f -> f.getClassName() + '#' + f.getMethodName())
            .orElse(null));
    }
}
