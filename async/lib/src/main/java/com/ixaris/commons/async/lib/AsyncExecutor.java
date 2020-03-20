package com.ixaris.commons.async.lib;

import static com.ixaris.commons.async.lib.CompletableFutureUtil.complete;
import static com.ixaris.commons.misc.lib.exception.ExceptionUtil.sneakyThrow;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixaris.commons.async.lib.executor.AsyncExecutorWrapper;
import com.ixaris.commons.async.lib.executor.AsyncScheduledExecutorServiceWrapper;
import com.ixaris.commons.async.lib.scheduler.ScheduledExecutorServiceWrapper;
import com.ixaris.commons.async.lib.scheduler.Scheduler;
import com.ixaris.commons.async.lib.thread.ThreadLocalHelper;
import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.misc.lib.function.RunnableThrows;

/**
 * This class allows an executor to keep a thread local reference to itself in the tasks it executes, primarily for allowing asynchronous tasks
 * to relay execution back to the original executor (or default to relaying to common pool). E.g. if a database or network operation is
 * performed, the execution is relayed back once the result is obtained. It is the responsibility of implementors that switch executors to relay
 * execution back and to preserve async trace and async locals across tasks.
 *
 * <pre>
 * ...
 * final Result result = await(relay(() -&gt; exec(otherExecutor, () -&gt; transaction(tx, persistenceCallable))));
 * doSomethingWith(result);
 * ...
 * </pre>
 */
public final class AsyncExecutor {
    
    public static final AsyncScheduledExecutorServiceWrapper<?> DEFAULT = new AsyncScheduledExecutorServiceWrapper<>(
        true, new ScheduledExecutorServiceWrapper<>(ForkJoinPool.commonPool(), Scheduler.commonScheduler()));
    
    private static final Logger LOG = LoggerFactory.getLogger(AsyncExecutor.class);
    private static final ThreadLocal<AsyncExecutorWrapper<?>> ASYNC_CONTEXT = new ThreadLocal<>();
    
    @SuppressWarnings("squid:S1181")
    public static Runnable wrap(final AsyncExecutorWrapper<?> executor, final Runnable task) {
        return () -> {
            try {
                ThreadLocalHelper.exec(ASYNC_CONTEXT, executor, RunnableThrows.from(task));
            } catch (final Throwable t) {
                LOG.error("Unhandled error", AsyncTrace.join(t));
                throw t;
            }
        };
    }
    
    @SuppressWarnings("squid:S1181")
    public static <T> Callable<T> wrap(final AsyncExecutorWrapper<?> executor, final Callable<T> task) {
        return () -> {
            try {
                return ThreadLocalHelper.exec(ASYNC_CONTEXT, executor, CallableThrows.from(task));
            } catch (final Throwable t) {
                LOG.error("Unhandled error", AsyncTrace.join(t));
                throw t;
            }
        };
    }
    
    @SuppressWarnings("squid:S1181")
    public static <T> Collection<Callable<T>> wrap(final AsyncExecutorWrapper<?> executor, final Collection<? extends Callable<T>> tasks) {
        final Collection<Callable<T>> coll = new ArrayList<>(tasks.size());
        for (final Callable<T> task : tasks) {
            coll.add(() -> {
                try {
                    return ThreadLocalHelper.exec(ASYNC_CONTEXT, executor, CallableThrows.from(task));
                } catch (final Throwable t) {
                    LOG.error("Unhandled error", AsyncTrace.join(t));
                    throw t;
                }
            });
        }
        return coll;
    }
    
    @SuppressWarnings("squid:S1452")
    public static AsyncExecutorWrapper<?> get() {
        final AsyncExecutorWrapper<?> executor = ASYNC_CONTEXT.get();
        return executor != null ? executor : DEFAULT;
    }
    
    /**
     * Execute a synchronous task (the runnable) in the same executor as the current thread and return a future that is
     * completed when the task completes
     */
    public static <E extends Exception> FutureAsync<Void> exec(final RunnableThrows<E> runnable) throws E {
        return exec(get(), runnable);
    }
    
    /**
     * Execute a synchronous task (the runnable) in an executor and return a future that is completed when the task
     * completes
     */
    public static <E extends Exception> FutureAsync<Void> exec(final Executor executor, final RunnableThrows<E> runnable) throws E {
        final FutureAsync<Void> future = new FutureAsync<>();
        final RunnableThrows<E> wrapped = AsyncLocal.wrapThrows(AsyncTrace.wrapThrows(runnable));
        executor.execute(() -> complete(future, wrapped));
        return future;
    }
    
    /**
     * Execute a synchronous task (the callable) in the same executor as the current thread and return a future that is
     * fulfilled from the executed task's result
     */
    public static <T, E extends Exception> FutureAsync<T> exec(final CallableThrows<T, E> callable) throws E {
        return exec(get(), callable);
    }
    
    /**
     * Execute a synchronous task (the callable) in an executor and return a future that is fulfilled from the executed
     * task's result
     */
    public static <T, E extends Exception> FutureAsync<T> exec(final Executor executor, final CallableThrows<T, E> callable) throws E {
        final FutureAsync<T> future = new FutureAsync<>();
        final CallableThrows<T, E> wrapped = AsyncLocal.wrapThrows(AsyncTrace.wrapThrows(callable));
        executor.execute(() -> complete(future, wrapped));
        return future;
    }
    
    /**
     * Execute an asynchronous task in the same executor as the current thread and return a future that is fulfilled
     * from the executed task's result
     */
    public static <T, E extends Exception> FutureAsync<T> exec(final CompletionStageCallableThrows<T, E> callable) throws E {
        return exec(get(), callable);
    }
    
    /**
     * Execute an asynchronous task in an executor and return a future that is fulfilled from the executed task's result
     */
    public static <T, E extends Exception> FutureAsync<T> exec(final Executor executor, final CompletionStageCallableThrows<T, E> callable) throws E {
        final FutureAsync<T> future = new FutureAsync<>();
        final CompletionStageCallableThrows<T, E> wrapped = AsyncLocal.wrapThrows(AsyncTrace.wrapThrows(callable));
        executor.execute(() -> complete(future, wrapped));
        return future;
    }
    
    /**
     * Schedule a task and return a future that is completed when the scheduled task completes
     */
    public static <E extends Exception> FutureAsyncWithTimeout<Void> schedule(final long delay, final TimeUnit timeUnit, final RunnableThrows<E> runnable) throws E {
        final AsyncTrace trace = AsyncTrace.get();
        final RunnableThrows<E> wrapped = AsyncLocal.wrapThrows(() -> AsyncTrace.exec(trace, runnable));
        
        final FutureAsyncWithTimeout<Void> future = new FutureAsyncWithTimeout<>();
        final Runnable command = () -> complete(future, wrapped);
        future.setScheduledFutureAsync(schedule(command, trace, delay, timeUnit));
        return future;
    }
    
    /**
     * Schedule a task and return a future that is fulfilled on the given executor from the scheduled task's result
     */
    public static <T, E extends Exception> FutureAsyncWithTimeout<T> schedule(final long delay, final TimeUnit timeUnit, final CallableThrows<T, E> callable) throws E {
        final AsyncTrace trace = AsyncTrace.get();
        final CallableThrows<T, E> wrapped = AsyncLocal.wrapThrows(() -> AsyncTrace.exec(trace, callable));
        
        final FutureAsyncWithTimeout<T> future = new FutureAsyncWithTimeout<>();
        final Runnable command = () -> complete(future, wrapped);
        future.setScheduledFutureAsync(schedule(command, trace, delay, timeUnit));
        return future;
    }
    
    /**
     * Schedule a task and return a future that is fulfilled on the given executor from the scheduled task's future
     */
    public static <T, E extends Exception> FutureAsyncWithTimeout<T> schedule(final long delay, final TimeUnit timeUnit, final CompletionStageCallableThrows<T, E> callable) throws E {
        final AsyncTrace trace = AsyncTrace.get();
        final CompletionStageCallableThrows<T, E> wrapped = AsyncLocal.wrapThrows(() -> AsyncTrace.exec(trace, callable));
        
        final FutureAsyncWithTimeout<T> future = new FutureAsyncWithTimeout<>();
        final Runnable command = () -> complete(future, wrapped);
        future.setScheduledFutureAsync(schedule(command, trace, delay, timeUnit));
        return future;
    }
    
    @SuppressWarnings("squid:S1181")
    private static ScheduledFuture<?> schedule(final Runnable command, final AsyncTrace trace, final long delay, final TimeUnit timeUnit) {
        final Executor executor = get();
        return (executor instanceof ScheduledExecutorService)
            ? ((ScheduledExecutorService) executor).schedule(command, delay, timeUnit)
            : Scheduler
                .commonScheduler()
                .schedule(
                    () -> {
                        try {
                            executor.execute(command);
                        } catch (final Throwable tt) {
                            throw sneakyThrow(AsyncTrace.join(tt, trace));
                        }
                    },
                    delay,
                    timeUnit);
    }
    
    public static <T> BiConsumer<? super T, ? super Throwable> relayConsumer(final CompletableFuture<T> future) {
        return relayConsumer(future, AsyncTrace.get());
    }
    
    /**
     * optimisation to avoid the cost of creating the trace twice when executing and relaying a result
     */
    @SuppressWarnings({ "squid:S1181", "squid:S1452" })
    private static <T> BiConsumer<? super T, ? super Throwable> relayConsumer(final CompletableFuture<T> future, final AsyncTrace trace) {
        final BiConsumer<? super T, ? super Throwable> wrapped = AsyncLocal.wrap((r, t) -> AsyncTrace.exec(trace, () -> complete(future, r, t)));
        final Executor executor = get();
        return (r, t) -> {
            final Executor thisExecutor = ASYNC_CONTEXT.get();
            if ((thisExecutor == null) || (thisExecutor != executor)) {
                try {
                    executor.execute(() -> wrapped.accept(r, t));
                } catch (final Throwable tt) {
                    throw sneakyThrow(AsyncTrace.join(tt, trace));
                }
            } else {
                wrapped.accept(r, t);
            }
        };
    }
    
    public static <T, E extends Exception> FutureAsync<T> execAndRelay(final Executor executor, final CallableThrows<T, E> callable) throws E {
        final AsyncTrace trace = AsyncTrace.get();
        final CallableThrows<T, E> wrapped = AsyncLocal.wrapThrows(() -> AsyncTrace.exec(trace, callable));
        
        final FutureAsync<T> future = new FutureAsync<>();
        final BiConsumer<? super T, ? super Throwable> relayConsumer = relayConsumer(future, trace);
        executor.execute(() -> {
            final T result;
            try {
                result = wrapped.call();
            } catch (final Throwable t) {
                relayConsumer.accept(null, t);
                return;
            }
            relayConsumer.accept(result, null);
        });
        return future;
    }
    
    public static <T, E extends Exception> FutureAsync<T> execAndRelay(final Executor executor, final CompletionStageCallableThrows<T, E> callable) throws E {
        final AsyncTrace trace = AsyncTrace.get();
        final CompletionStageCallableThrows<T, E> wrapped = AsyncLocal.wrapThrows(() -> AsyncTrace.exec(trace, callable));
        
        final FutureAsync<T> future = new FutureAsync<>();
        final BiConsumer<? super T, ? super Throwable> relayConsumer = relayConsumer(future, trace);
        executor.execute(() -> {
            final CompletionStage<T> stage;
            try {
                stage = wrapped.call();
            } catch (final Throwable t) {
                relayConsumer.accept(null, t);
                return;
            }
            CompletionStageUtil.whenDone(stage, relayConsumer);
        });
        return future;
    }
    
    /**
     * Relay execution back to the executor associated with the current thread (defaulting to common pool). This is
     * achieved by completing a future on the executor from the given future.
     */
    public static <T> FutureAsync<T> relay(final CompletionStage<T> stage) {
        final FutureAsync<T> future = new FutureAsync<>();
        CompletionStageUtil.whenDone(stage, relayConsumer(future, AsyncTrace.get()));
        return future;
    }
    
    /**
     * Async processes need to await the sleep, i.e. await(sleep(10, SECONDS))
     */
    @SuppressWarnings("squid:S1602")
    public static FutureAsyncWithTimeout<Void> sleep(final long interval, final TimeUnit timeUnit) {
        final AsyncTrace trace = AsyncTrace.get();
        final FutureAsyncWithTimeout<Void> future = new FutureAsyncWithTimeout<>();
        final Runnable command = AsyncLocal.wrap(() -> AsyncTrace.exec(trace, () -> {
            future.complete(null);
        }));
        
        future.setScheduledFutureAsync(schedule(command, trace, interval, timeUnit));
        return future;
    }
    
    /**
     * Used in co-operative multitasking to relinquish control of the computation thread. Used to break lengthy
     * computation which would not otherwise be broken by I/O or other blocking operations.
     *
     * <p>Async processes need to await the yield, i.e. await(yield())
     */
    @SuppressWarnings("squid:S1602")
    public static FutureAsync<Void> yield() {
        final FutureAsync<Void> future = new FutureAsync<>();
        get().execute(AsyncLocal.wrap(AsyncTrace.wrap(() -> {
            future.complete(null);
        })));
        return future;
    }
    
    private AsyncExecutor() {}
    
}
