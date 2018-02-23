package com.ixaris.commons.async.lib;

import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.function.BiFunction;
import java.util.function.Consumer;
import java.util.function.Function;

import com.ixaris.commons.async.lib.thread.ThreadLocalHelper;
import com.ixaris.commons.misc.lib.function.BiFunctionThrows;
import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.misc.lib.function.ConsumerThrows;
import com.ixaris.commons.misc.lib.function.FunctionThrows;
import com.ixaris.commons.misc.lib.function.RunnableThrows;

/**
 * Maintains stack trace of separate parts of an asynchronous process
 */
public final class AsyncTrace extends Throwable {
    
    public static final String ASYNC_TRACE_SKIP_PROPERTY = "async.trace.skip";
    
    private static final ThreadLocal<AsyncTrace> TRACE = new ThreadLocal<>();
    private static final boolean RECORD_TRACE = !Boolean.valueOf(System.getProperty(ASYNC_TRACE_SKIP_PROPERTY, "false"));
    
    public static <T> Callable<T> wrap(final Callable<T> callable) {
        final AsyncTrace trace = get();
        return () -> exec(trace, CallableThrows.from(callable));
    }
    
    public static Runnable wrap(final Runnable runnable) {
        final AsyncTrace trace = get();
        return () -> exec(trace, RunnableThrows.from(runnable));
    }
    
    public static <T> Consumer<T> wrap(final Consumer<T> consumer) {
        final AsyncTrace trace = get();
        return v -> exec(trace, () -> consumer.accept(v));
    }
    
    public static <I, V> Function<I, V> wrap(final Function<I, V> function) {
        final AsyncTrace trace = get();
        return i -> exec(trace, () -> function.apply(i));
    }
    
    public static <I1, I2, V> BiFunction<I1, I2, V> wrapThrows(final BiFunction<I1, I2, V> biFunction) {
        final AsyncTrace trace = get();
        return (i1, i2) -> exec(trace, () -> biFunction.apply(i1, i2));
    }
    
    public static <V, E extends Throwable> CallableThrows<V, E> wrapThrows(final CallableThrows<V, E> callable) {
        final AsyncTrace trace = get();
        return () -> exec(trace, callable);
    }
    
    public static <E extends Throwable> RunnableThrows<E> wrapThrows(final RunnableThrows<E> runnable) {
        final AsyncTrace trace = get();
        return () -> exec(trace, runnable);
    }
    
    public static <V, E extends Throwable> ConsumerThrows<V, E> wrapThrows(final ConsumerThrows<V, E> consumer) {
        final AsyncTrace trace = get();
        return v -> exec(trace, () -> consumer.accept(v));
    }
    
    public static <I, V, E extends Throwable> FunctionThrows<I, V, E> wrapThrows(final FunctionThrows<I, V, E> function) {
        final AsyncTrace trace = get();
        return i -> exec(trace, () -> function.apply(i));
    }
    
    public static <I1, I2, V, E extends Throwable> BiFunctionThrows<I1, I2, V, E> wrapThrows(final BiFunctionThrows<I1, I2, V, E> biFunction) {
        final AsyncTrace trace = get();
        return (i1, i2) -> exec(trace, () -> biFunction.apply(i1, i2));
    }
    
    public static AsyncTrace get() {
        if (RECORD_TRACE) {
            final AsyncTrace parent = TRACE.get();
            return parent == null ? new AsyncTrace() : new AsyncTrace(parent);
        } else {
            return null;
        }
    }
    
    public static <E extends Throwable> void exec(final AsyncTrace trace, final RunnableThrows<E> task) throws E {
        ThreadLocalHelper.exec(TRACE, trace, task);
    }
    
    public static <V, E extends Throwable> V exec(final AsyncTrace trace, final CallableThrows<V, E> task) throws E {
        return ThreadLocalHelper.exec(TRACE, trace, task);
    }
    
    public static <T extends Throwable> T join(final T throwable) {
        if (throwable instanceof AsyncTrace) {
            throw new IllegalArgumentException("Cannot join traces to themselves");
        }
        if (throwable == null) {
            return null;
        }
        
        final AsyncTrace trace = TRACE.get();
        if (trace == null) {
            return throwable;
        }
        Throwable tmp = throwable;
        while (true) {
            final Throwable cause = tmp.getCause();
            if ((cause == null) || (cause instanceof AsyncTrace)) {
                setCause(tmp, trace);
                return throwable;
            } else if (cause == trace) {
                return throwable;
            }
            tmp = cause;
        }
    }
    
    private static void setCause(final Throwable tmp, final AsyncTrace trace) {
        try {
            tmp.initCause(trace);
        } catch (final IllegalStateException e) {
            // Throwables initialised with null as cause cannot have their cause set using initCause()
            // so we have to revert to using reflection
            try {
                CAUSE_FIELD.set(tmp, trace);
            } catch (final IllegalAccessException ee) {
                throw new IllegalStateException(ee);
            }
        }
    }
    
    private static final Field CAUSE_FIELD;
    
    static {
        try {
            CAUSE_FIELD = Throwable.class.getDeclaredField("cause");
            CAUSE_FIELD.setAccessible(true);
        } catch (final NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private final int depth;
    
    private AsyncTrace() {
        super("Async Trace [0] @ " + Thread.currentThread().getName());
        this.depth = 1;
    }
    
    private AsyncTrace(final AsyncTrace parent) {
        super("Async Trace [" + parent.depth + "] @ " + Thread.currentThread().getName(), parent);
        this.depth = parent.depth + 1;
    }
    
}
