package com.ixaris.commons.misc.lib.function;

import com.ixaris.commons.misc.lib.exception.ExceptionUtil;

@FunctionalInterface
public interface RunnableThrows<E extends Throwable> {
    
    @SuppressWarnings("unchecked")
    static RunnableThrows<RuntimeException> from(final Runnable r) {
        return r::run;
    }
    
    static Runnable asRunnable(final RunnableThrows<?> runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (final Throwable e) { // NOSONAR catch throwable to rethrow without declaring
                throw ExceptionUtil.sneakyThrow(e);
            }
        };
    }
    
    void run() throws E;
    
}
