package com.ixaris.commons.async.lib;

import java.lang.reflect.Field;
import java.util.Arrays;
import java.util.concurrent.Callable;
import java.util.function.BiConsumer;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;

import com.ixaris.commons.async.lib.thread.ThreadLocalHelper;
import com.ixaris.commons.collections.lib.GuavaCollections;
import com.ixaris.commons.misc.lib.exception.ExceptionUtil;
import com.ixaris.commons.misc.lib.exception.StackWalker;
import com.ixaris.commons.misc.lib.function.BiFunctionThrows;
import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.misc.lib.function.ConsumerThrows;
import com.ixaris.commons.misc.lib.function.FunctionThrows;
import com.ixaris.commons.misc.lib.function.RunnableThrows;

/**
 * Maintains stack trace of separate parts of an asynchronous process
 */
public final class AsyncTrace {
    
    private static ImmutableSet<String> IGNORED_PACKAGES = ImmutableSet.of();
    
    static {
        ignorePackages("java", "sun", "com.ixaris.commons");
    }
    
    public static synchronized void ignorePackages(final String... toIgnore) {
        IGNORED_PACKAGES = GuavaCollections.copyOfSetAdding(IGNORED_PACKAGES,
            Arrays.stream(toIgnore).map(p -> p.endsWith(".") ? p : (p + ".")).collect(Collectors.toSet()));
    }
    
    public static final String ASYNC_TRACE_SKIP_PROPERTY = "async.trace.skip";
    
    private static final Logger LOG = LoggerFactory.getLogger(AsyncTrace.class);
    public static final AsyncLocal<Token> NEW_RELIC = new AsyncLocal<>("new_relic_token");
    private static final ThreadLocal<AsyncTrace> TRACE = ThreadLocal.withInitial(AsyncTrace::new);
    private static final int MAX_TRACE_DEPTH = 24;
    
    private static final boolean RECORD_TRACE = !Boolean.parseBoolean(System.getProperty(ASYNC_TRACE_SKIP_PROPERTY, "false"));
    
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
    public static <T, R, E extends Exception> FunctionThrows<T, R, E> wrapThrows(final FunctionThrows<T, R, E> function) {
        final AsyncTrace trace = get();
        return (trace != null) ? t -> exec(trace, () -> function.apply(t)) : function;
    }
    
    @SuppressWarnings("squid:S864")
    public static <T, U, R, E extends Exception> BiFunctionThrows<T, U, R, E> wrapThrows(final BiFunctionThrows<T, U, R, E> biFunction) {
        final AsyncTrace trace = get();
        return (trace != null) ? (t, u) -> exec(trace, () -> biFunction.apply(t, u)) : biFunction;
    }
    
    @SuppressWarnings("squid:S864")
    public static <T, E extends Exception> CompletionStageCallableThrows<T, E> wrapThrows(final CompletionStageCallableThrows<T, E> callable) {
        final AsyncTrace trace = get();
        return (trace != null) ? () -> exec(trace, callable) : callable;
    }
    
    @SuppressWarnings("squid:S864")
    public static AsyncTrace get() {
        if (!RECORD_TRACE) {
            return null;
        }
        
        final String caller = StackWalker.findCaller(c -> IGNORED_PACKAGES.stream().noneMatch(c::startsWith), 1);
        final AsyncTrace parent = TRACE.get();
        
        // attempt to match a previous trace to the caller. If matched, trace is trimmed to that point
        AsyncStep matchedStep;
        if (caller == null) {
            matchedStep = null;
        } else {
            matchedStep = parent.step;
            while (matchedStep != null) {
                if (caller.equals(matchedStep.caller)) {
                    break;
                }
                matchedStep = matchedStep.getCause();
            }
        }
        
        final AsyncStep step;
        if (matchedStep != null) {
            // check 1 level deeper. For loops we keep the first iteration (which shows the proper trace to the
            // loop) and the last iteration with the iteration count
            final AsyncStep matchedStepCause = matchedStep.getCause();
            if ((matchedStepCause != null) && (matchedStep.caller.equals(matchedStepCause.caller))) {
                step = new AsyncStep(caller, matchedStepCause, matchedStep.depth, matchedStep.iterations + 1);
            } else {
                step = new AsyncStep(caller, matchedStep, matchedStep.depth, matchedStep.iterations + 1);
            }
        } else if (parent.step != null) {
            step = parent.step.depth < MAX_TRACE_DEPTH
                ? new AsyncStep(caller, parent.step, parent.step.depth + 1, 1) : parent.step;
        } else {
            step = new AsyncStep(caller);
        }
        
        return new AsyncTrace(step);
    }
    
    @SuppressWarnings("squid:S1181")
    @Trace(async = true)
    public static <E extends Exception> void exec(final AsyncTrace trace, final RunnableThrows<E> task) throws E {
        linkNewRelic();
        ThreadLocalHelper.exec(TRACE, trace, () -> {
            try {
                task.run();
            } catch (final Throwable t) {
                throw ExceptionUtil.sneakyThrow(AsyncTrace.join(t));
            }
        });
    }
    
    @SuppressWarnings("squid:S1181")
    @Trace(async = true)
    public static <V, E extends Exception> V exec(final AsyncTrace trace, final CallableThrows<V, E> task) throws E {
        linkNewRelic();
        return ThreadLocalHelper.exec(TRACE, trace, () -> {
            try {
                return task.call();
            } catch (final Throwable t) {
                throw ExceptionUtil.sneakyThrow(AsyncTrace.join(t));
            }
        });
    }
    
    @SuppressWarnings("squid:S1181")
    @Trace(async = true)
    public static <V, E extends Exception> Async<V> exec(final AsyncTrace trace, final CompletionStageCallableThrows<V, E> task) throws E {
        linkNewRelic();
        return ThreadLocalHelper.exec(TRACE, trace, () -> {
            try {
                return task.call();
            } catch (final Throwable t) {
                throw ExceptionUtil.sneakyThrow(AsyncTrace.join(t));
            }
        });
    }
    
    public static <T extends Throwable> T join(final T throwable) {
        return join(throwable, TRACE.get());
    }
    
    public static <T extends Throwable> T join(final T throwable, final AsyncTrace trace) {
        if (throwable == null) {
            return null;
        }
        if (throwable instanceof AsyncStep) {
            throw new IllegalArgumentException("Cannot join traces to themselves");
        }
        
        final AsyncStep step = trace.step;
        if (step == null) {
            return throwable;
        }
        Throwable tmp = throwable;
        while (true) {
            final Throwable cause = tmp.getCause();
            if (cause == null) {
                setCause(tmp, step);
                return throwable;
            } else if ((cause == step) || (cause instanceof AsyncStep)) {
                return throwable;
            }
            tmp = cause;
        }
    }
    
    private static void linkNewRelic() {
        final Token token = NEW_RELIC.get();
        if (token != null) {
            token.link();
        }
    }
    
    private static void setCause(final Throwable tmp, final AsyncStep step) {
        try {
            tmp.initCause(step);
        } catch (final IllegalStateException e) {
            // Throwables initialised with null as cause cannot have their cause set using initCause()
            // so we have to revert to using reflection
            try {
                CAUSE_FIELD.set(tmp, step);
                LOG.warn("Setting cause by reflection for " + tmp.getClass());
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
    
    private static final class AsyncStep extends RuntimeException {
        
        private final String caller;
        private final int depth;
        private final int iterations;
        
        private AsyncStep(final String caller, final AsyncStep parent, final int depth, final int iterations) {
            super(String.format("Async Step [%d] %sfrom %s @ %s", depth, ((iterations > 1) ? ("(x" + iterations + ") ") : ""), caller, Thread.currentThread().getName()), parent);
            this.caller = caller;
            this.depth = depth;
            this.iterations = iterations;
        }
        
        private AsyncStep(final String caller) {
            super(String.format("Async Step [0] from %s @ %s", caller, Thread.currentThread().getName()));
            this.caller = caller;
            this.depth = 0;
            this.iterations = 1;
        }
        
        @Override
        public synchronized AsyncStep getCause() {
            return (AsyncStep) super.getCause();
        }
        
    }
    
    private final AsyncStep step;
    
    public AsyncTrace() {
        this.step = null;
    }
    
    public AsyncTrace(final AsyncStep step) {
        this.step = step;
    }
    
}
