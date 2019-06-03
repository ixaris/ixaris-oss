package com.ixaris.commons.misc.lib.function;

import com.ixaris.commons.misc.lib.exception.ExceptionUtil;

public interface ConditionThrows<T, E extends Throwable> {
    
    static <T> ConditionThrows<T, RuntimeException> from(final Condition<T> c) {
        return c::isTrue;
    }
    
    static <T> Condition<T> asCondition(final ConditionThrows<T, ?> consumer) {
        return t -> {
            try {
                return consumer.isTrue(t);
            } catch (final Throwable e) { // NOSONAR catch throwable to rethrow without declaring
                throw ExceptionUtil.sneakyThrow(e);
            }
        };
    }
    
    boolean isTrue(T t) throws E;
    
}
