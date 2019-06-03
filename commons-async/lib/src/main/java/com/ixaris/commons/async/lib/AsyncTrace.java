package com.ixaris.commons.async.lib;

import static com.ixaris.commons.async.lib.Async.from;

import com.ixaris.commons.async.lib.thread.ThreadLocalHelper;
import com.ixaris.commons.misc.lib.exception.ExceptionUtil;
import com.ixaris.commons.misc.lib.function.BiFunctionThrows;
import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.misc.lib.function.ConsumerThrows;
import com.ixaris.commons.misc.lib.function.FunctionThrows;
import com.ixaris.commons.misc.lib.function.RunnableThrows;
import com.ixaris.commons.misc.lib.logging.Logger;
import com.ixaris.commons.misc.lib.logging.LoggerFactory;
import java.lang.reflect.Field;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Maintains stack trace of separate parts of an asynchronous process
 */
public final class AsyncTrace extends RuntimeException {
    
    public static final String ASYNC_TRACE_SKIP_PROPERTY = "async.trace.skip";
    
    private static final Logger LOG = LoggerFactory.forEnclosingClass();
    private static final ThreadLocal<AsyncTrace> TRACE = new ThreadLocal<>();
    private static final int MAX_TRACE_DEPTH = 100;
    
    private static final boolean RECORD_TRACE = !Boolean.valueOf(System.getProperty(
        ASYNC_TRACE_SKIP_PROPERTY, "false"
    ));
    
    @SuppressWarnings("squid:S864")
    public static <T> Callable<T> wrap(final Callable<T> callable) {
        final AsyncTrace trace = get();
        return (trace != null) ? () -> exec(trace, CallableThrows.from(callable)) : callable;
    }
    
    @SuppressWarnings("squid:S864")
    public static Runnable wrap(final Runnable runnable) {
        final AsyncTrace trace = get();
        return (trace != null) ? () -> exec(trace, RunnableThrows.from(runnable)) : runnable;
    }
    
    @SuppressWarnings("squid:S864")
    public static <T> Consumer<T> wrap(final Consumer<T> consumer) {
        final AsyncTrace trace = get();
        return (trace != null) ? t -> exec(trace, () -> consumer.accept(t)) : consumer;
    }
    
    @SuppressWarnings("squid:S864")
    public static <T, R> Function<T, R> wrap(final Function<T, R> function) {
        final AsyncTrace trace = get();
        return (trace != null) ? t -> exec(trace, () -> function.apply(t)) : function;
    }
    
    @SuppressWarnings("squid:S864")
    public static <T, U> BiConsumer<T, U> wrap(final BiConsumer<T, U> biConsumer) {
        final AsyncTrace trace = get();
        return (trace != null) ? (t, u) -> exec(trace, () -> biConsumer.accept(t, u)) : biConsumer;
    }
    
    @SuppressWarnings("squid:S864")
    public static <T, E extends Exception> CallableThrows<T, E> wrapThrows(final CallableThrows<T, E> callable) {
        final AsyncTrace trace = get();
        return (trace != null) ? () -> exec(trace, callable) : callable;
    }
    
    @SuppressWarnings("squid:S864")
    public static <E extends Exception> RunnableThrows<E> wrapThrows(final RunnableThrows<E> runnable) {
        final AsyncTrace trace = get();
        return (trace != null) ? () -> exec(trace, runnable) : runnable;
    }
    
    @SuppressWarnings("squid:S864")
    public static <T, E extends Exception> ConsumerThrows<T, E> wrapThrows(final ConsumerThrows<T, E> consumer) {
        final AsyncTrace trace = get();
        return (trace != null) ? t -> exec(trace, () -> consumer.accept(t)) : consumer;
    }
    
    @SuppressWarnings("squid:S864")
    public static <T, R, E extends Exception> FunctionThrows<T, R, E> wrapThrows(
        final FunctionThrows<T, R, E> function
    ) {
        final AsyncTrace trace = get();
        return (trace != null) ? t -> exec(trace, () -> function.apply(t)) : function;
    }
    
    @SuppressWarnings("squid:S864")
    public static <T, U, R, E extends Exception> BiFunctionThrows<T, U, R, E> wrapThrows(
        final BiFunctionThrows<T, U, R, E> biFunction
    ) {
        final AsyncTrace trace = get();
        return (trace != null) ? (t, u) -> exec(trace, () -> biFunction.apply(t, u)) : biFunction;
    }
    
    @SuppressWarnings("squid:S864")
    public static <T, E extends Exception> CompletionStageCallableThrows<T, E> wrapThrows(
        final CompletionStageCallableThrows<T, E> callable
    ) {
        final AsyncTrace trace = get();
        return (trace != null) ? () -> exec(trace, callable) : callable;
    }
    
    @SuppressWarnings("squid:S864")
    public static AsyncTrace get() {
        if (RECORD_TRACE) {
            final AsyncTrace parent = TRACE.get();
            return (parent == null)
                ? new AsyncTrace() : (parent.depth < MAX_TRACE_DEPTH) ? new AsyncTrace(parent) : parent;
        } else {
            return null;
        }
    }
    
    @SuppressWarnings("squid:S1181")
    public static <E extends Exception> void exec(final AsyncTrace trace, final RunnableThrows<E> task) throws E {
        if (trace != null) {
            ThreadLocalHelper.exec(TRACE, trace, () -> {
                try {
                    task.run();
                } catch (final Throwable t) {
                    throw ExceptionUtil.sneakyThrow(AsyncTrace.join(t));
                }
            });
        } else {
            task.run();
        }
    }
    
    @SuppressWarnings("squid:S1181")
    public static <V, E extends Exception> V exec(final AsyncTrace trace, final CallableThrows<V, E> task) throws E {
        if (trace != null) {
            return ThreadLocalHelper.exec(TRACE, trace, () -> {
                try {
                    return task.call();
                } catch (final Throwable t) {
                    throw ExceptionUtil.sneakyThrow(AsyncTrace.join(t));
                }
            });
        } else {
            return task.call();
        }
    }
    
    @SuppressWarnings("squid:S1181")
    public static <V, E extends Exception> Async<V> exec(
        final AsyncTrace trace, final CompletionStageCallableThrows<V, E> task
    ) throws E {
        if (trace != null) {
            return ThreadLocalHelper.exec(TRACE, trace, () -> {
                try {
                    return task.call();
                } catch (final Throwable t) {
                    throw ExceptionUtil.sneakyThrow(AsyncTrace.join(t));
                }
            });
        } else {
            return from(task.call());
        }
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
            if (cause == null) {
                setCause(tmp, trace);
                return throwable;
            } else if ((cause == trace) || (cause instanceof AsyncTrace)) {
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
            LOG.atWarn().log("Setting cause by reflection for " + tmp.getClass());
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
