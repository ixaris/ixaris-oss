package com.ixaris.commons.async.lib;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.CompletableFutureUtil.complete;

import com.ixaris.commons.async.lib.scheduler.ScheduledExecutorServiceWrapper;
import com.ixaris.commons.async.lib.scheduler.Scheduler;
import com.ixaris.commons.async.lib.thread.ThreadLocalHelper;
import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.misc.lib.function.RunnableThrows;
import com.ixaris.commons.misc.lib.logging.Logger;
import com.ixaris.commons.misc.lib.logging.LoggerFactory;
import com.ixaris.commons.misc.lib.object.Wrapper;
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

/**
 * This class allows an executor to keep a thread local reference to itself in the tasks it executes, primarily for
 * allowing asynchronous tasks to relay execution back to the original executor (or default to relaying to common pool).
 * E.g. if a database or network operation is performed, the execution is relayed back once the result is obtained. It
 * is the responsibility of implementors that switch executors to relay execution back and to preserve async trace and
 * async locals across tasks.
 *
 * <pre>
 * ...
 * final Result result = await(relay(() -&gt; exec(otherExecutor, () -&gt; transaction(tx, persistenceCallable))));
 * doSomethingWith(result);
 * ...
 * </pre>
 */
public final class AsyncExecutor {
    
    public static final ScheduledExecutorService DEFAULT = new ScheduledExecutorServiceWrapper<>(
        ForkJoinPool.commonPool(), Scheduler.commonScheduler()
    );
    
    private static final Logger LOG = LoggerFactory.forEnclosingClass();
    private static final ThreadLocal<Executor> ASYNC_CONTEXT = new ThreadLocal<>();
    
    @SuppressWarnings("squid:S1181")
    public static Runnable wrap(final Executor executor, final Runnable task) {
        return () -> {
            try {
                ThreadLocalHelper.exec(ASYNC_CONTEXT, executor, RunnableThrows.from(task));
            } catch (final Throwable t) {
                LOG.atError(AsyncTrace.join(t)).log("Unhandled error");
                throw t;
            }
        };
    }
    
    @SuppressWarnings("squid:S1181")
    public static <T> Callable<T> wrap(final Executor executor, final Callable<T> task) {
        return () -> {
            try {
                return ThreadLocalHelper.exec(ASYNC_CONTEXT, executor, CallableThrows.from(task));
            } catch (final Throwable t) {
                LOG.atError(AsyncTrace.join(t)).log("Unhandled error");
                throw t;
            }
        };
    }
    
    @SuppressWarnings("squid:S1181")
    public static <T> Collection<? extends Callable<T>> wrap(
        final Executor executor, final Collection<? extends Callable<T>> tasks
    ) {
        final Collection<Callable<T>> coll = new ArrayList<>(tasks.size());
        for (final Callable<T> task : tasks) {
            coll.add(() -> {
                try {
                    return ThreadLocalHelper.exec(ASYNC_CONTEXT, executor, CallableThrows.from(task));
                } catch (final Throwable t) {
                    LOG.atError(AsyncTrace.join(t)).log("Unhandled error");
                    throw t;
                }
            });
        }
        return coll;
    }
    
    public static Executor get() {
        final Executor executor = ASYNC_CONTEXT.get();
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
    public static <E extends Exception> FutureAsync<Void> exec(
        final Executor executor, final RunnableThrows<E> runnable
    ) throws E {
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
    public static <T, E extends Exception> FutureAsync<T> exec(
        final Executor executor, final CallableThrows<T, E> callable
    ) throws E {
        final FutureAsync<T> future = new FutureAsync<>();
        final CallableThrows<T, E> wrapped = AsyncLocal.wrapThrows(AsyncTrace.wrapThrows(callable));
        executor.execute(() -> complete(future, wrapped));
        return future;
    }
    
    /**
     * Execute an asynchronous task in the same executor as the current thread and return a future that is fulfilled
     * from the executed task's result
     */
    public static <T, E extends Exception> FutureAsync<T> exec(
        final CompletionStageCallableThrows<T, E> callable
    ) throws E {
        return exec(get(), callable);
    }
    
    /**
     * Execute an asynchronous task in an executor and return a future that is fulfilled from the executed task's result
     */
    public static <T, E extends Exception> FutureAsync<T> exec(
        final Executor executor, final CompletionStageCallableThrows<T, E> callable
    ) throws E {
        final FutureAsync<T> future = new FutureAsync<>();
        final CompletionStageCallableThrows<T, E> wrapped = AsyncLocal.wrapThrows(AsyncTrace.wrapThrows(callable));
        executor.execute(() -> complete(future, wrapped));
        return future;
    }
    
    public static <E extends Exception> FutureAsyncWithTimeout<Void> schedule(
        final long delay, final TimeUnit timeUnit, final RunnableThrows<E> runnable
    ) throws E {
        return schedule(get(), delay, timeUnit, runnable);
    }
    
    /**
     * Schedule a task and return a future that is completed on the given executor when the scheduled task completes
     */
    public static <E extends Exception> FutureAsyncWithTimeout<Void> schedule(
        final Executor executor, final long delay, final TimeUnit timeUnit, final RunnableThrows<E> runnable
    ) throws E {
        final FutureAsyncWithTimeout<Void> future = new FutureAsyncWithTimeout<>();
        final RunnableThrows<E> wrapped = AsyncLocal.wrapThrows(AsyncTrace.wrapThrows(runnable));
        final ScheduledFuture<?> scheduledFuture;
        if (executor instanceof ScheduledExecutorService) {
            scheduledFuture = ((ScheduledExecutorService) executor).schedule(
                () -> complete(future, wrapped), delay, timeUnit
            );
        } else {
            scheduledFuture = Scheduler
                .commonScheduler()
                .schedule(() -> executor.execute(() -> complete(future, wrapped)), delay, timeUnit);
        }
        future.setScheduledFutureAsync(scheduledFuture);
        return future;
    }
    
    public static <T, E extends Exception> FutureAsyncWithTimeout<T> schedule(
        final long delay, final TimeUnit timeUnit, final CallableThrows<T, E> callable
    ) throws E {
        return schedule(get(), delay, timeUnit, callable);
    }
    
    /**
     * Schedule a task and return a future that is fulfilled on the given executor from the scheduled task's result
     */
    public static <T, E extends Exception> FutureAsyncWithTimeout<T> schedule(
        final Executor executor, final long delay, final TimeUnit timeUnit, final CallableThrows<T, E> callable
    ) throws E {
        final FutureAsyncWithTimeout<T> future = new FutureAsyncWithTimeout<>();
        final CallableThrows<T, E> wrapped = AsyncLocal.wrapThrows(AsyncTrace.wrapThrows(callable));
        final ScheduledFuture<?> scheduledFuture;
        if (executor instanceof ScheduledExecutorService) {
            scheduledFuture = ((ScheduledExecutorService) executor).schedule(
                () -> complete(future, wrapped), delay, timeUnit
            );
        } else {
            scheduledFuture = Scheduler
                .commonScheduler()
                .schedule(() -> executor.execute(() -> complete(future, wrapped)), delay, timeUnit);
        }
        future.setScheduledFutureAsync(scheduledFuture);
        return future;
    }
    
    public static <T, E extends Exception> FutureAsyncWithTimeout<T> schedule(
        final long delay, final TimeUnit timeUnit, final CompletionStageCallableThrows<T, E> callable
    ) throws E {
        return schedule(get(), delay, timeUnit, callable);
    }
    
    /**
     * Schedule a task and return a future that is fulfilled on the given executor from the scheduled task's future
     */
    public static <T, E extends Exception> FutureAsyncWithTimeout<T> schedule(
        final Executor executor,
        final long delay,
        final TimeUnit timeUnit,
        final CompletionStageCallableThrows<T, E> callable
    ) throws E {
        final FutureAsyncWithTimeout<T> future = new FutureAsyncWithTimeout<>();
        final CompletionStageCallableThrows<T, E> wrapped = AsyncLocal.wrapThrows(AsyncTrace.wrapThrows(callable));
        final ScheduledFuture<?> scheduledFuture;
        if (executor instanceof ScheduledExecutorService) {
            scheduledFuture = ((ScheduledExecutorService) executor).schedule(
                () -> complete(future, wrapped), delay, timeUnit
            );
        } else {
            scheduledFuture = Scheduler
                .commonScheduler()
                .schedule(() -> executor.execute(() -> complete(future, wrapped)), delay, timeUnit);
        }
        future.setScheduledFutureAsync(scheduledFuture);
        return future;
    }
    
    public static <T> BiConsumer<? super T, ? super Throwable> relayConsumer(final CompletableFuture<T> future) {
        return relayConsumer(get(), future);
    }
    
    public static <T> BiConsumer<? super T, ? super Throwable> relayConsumer(
        final Executor executor, final CompletableFuture<T> future
    ) {
        return relayConsumer(executor, future, AsyncTrace.get());
    }
    
    /**
     * optimisation to avoid the cost of creating the trace twice when executing and relaying a result
     */
    public static <T> BiConsumer<? super T, ? super Throwable> relayConsumer(
        final Executor executor, final CompletableFuture<T> future, final AsyncTrace trace
    ) {
        final BiConsumer<? super T, ? super Throwable> wrapped = AsyncLocal.wrap((r, t) ->
            AsyncTrace.exec(trace, () -> complete(future, r, t))
        );
        return (r, t) -> {
            if (get() != executor) {
                executor.execute(() -> wrapped.accept(r, t));
            } else {
                wrapped.accept(r, t);
            }
        };
    }
    
    public static <T, E extends Exception> FutureAsync<T> execAndRelay(
        final Executor executor, final CallableThrows<T, E> callable
    ) throws E {
        return execAndRelay(executor, callable, get());
    }
    
    @SuppressWarnings("squid:S1181")
    public static <T, E extends Exception> FutureAsync<T> execAndRelay(
        final Executor executor, final CallableThrows<T, E> callable, final Executor relayExecutor
    ) throws E {
        final AsyncTrace trace = AsyncTrace.get();
        
        final FutureAsync<T> future = new FutureAsync<>();
        final CallableThrows<T, E> wrapped = AsyncLocal.wrapThrows(() -> AsyncTrace.exec(trace, callable));
        final BiConsumer<? super T, ? super Throwable> relayConsumer = relayConsumer(relayExecutor, future, trace);
        executor.execute(() -> {
            try {
                relayConsumer.accept(wrapped.call(), null);
            } catch (final Throwable t) {
                relayConsumer.accept(null, AsyncTrace.join(t));
            }
        });
        return future;
    }
    
    public static <T, E extends Exception> FutureAsync<T> execAndRelay(
        final Executor executor, final CompletionStageCallableThrows<T, E> callable
    ) throws E {
        return execAndRelay(executor, callable, get());
    }
    
    @SuppressWarnings("squid:S1181")
    public static <T, E extends Exception> FutureAsync<T> execAndRelay(
        final Executor executor, final CompletionStageCallableThrows<T, E> callable, final Executor relayExecutor
    ) throws E {
        final AsyncTrace trace = AsyncTrace.get();
        
        final FutureAsync<T> future = new FutureAsync<>();
        final CompletionStageCallableThrows<T, E> wrapped = AsyncLocal.wrapThrows(() -> AsyncTrace.exec(trace, callable)
        );
        final BiConsumer<? super T, ? super Throwable> relayConsumer = relayConsumer(relayExecutor, future, trace);
        executor.execute(() -> {
            try {
                CompletionStageUtil.whenDone(wrapped.call(), (r, t) ->
                    relayConsumer.accept(r, t == null ? null : AsyncTrace.join(t))
                );
            } catch (final Throwable t) {
                relayConsumer.accept(null, AsyncTrace.join(t));
            }
        });
        return future;
    }
    
    /**
     * Relay execution back to the executor associated with the current thread (defaulting to common pool). This is
     * achieved by completing a future on the executor from the given future.
     */
    public static <T> FutureAsync<T> relay(final CompletionStage<T> stage) {
        return relay(get(), stage);
    }
    
    /**
     * Relay execution back to the given executor. same semantics as {@link #relay(CompletionStage)}
     */
    public static <T> FutureAsync<T> relay(final Executor executor, final CompletionStage<T> stage) {
        final FutureAsync<T> future = new FutureAsync<>();
        CompletionStageUtil.whenDone(stage, relayConsumer(executor, future));
        return future;
    }
    
    /**
     * Async processes need to await the sleep, i.e. await(sleep(10, SECONDS))
     */
    public static FutureAsync<Void> sleep(final long interval, final TimeUnit timeUnit) {
        return sleep(get(), interval, timeUnit);
    }
    
    public static FutureAsync<Void> sleep(final Executor executor, final long interval, final TimeUnit timeUnit) {
        final FutureAsync<Void> future = new FutureAsync<>();
        final Runnable wrapped = AsyncLocal.wrap(AsyncTrace.wrap(() -> {
            future.complete(null);
        }));
        if (executor instanceof ScheduledExecutorService) {
            ((ScheduledExecutorService) executor).schedule(wrapped, interval, timeUnit);
        } else {
            Scheduler.commonScheduler().schedule(() -> executor.execute(wrapped), interval, timeUnit);
        }
        return future;
    }
    
    /**
     * Used in co-operative multitasking to relinquish control of the computation thread. Used to break lengthy
     * computation which would not otherwise be broken by I/O or other blocking operations.
     *
     * <p>Async processes need to await the yield, i.e. await(yield())
     */
    public static FutureAsync<Void> yield() {
        return yield(get());
    }
    
    /**
     * see {yield()}
     *
     * <p>Set preserve trace to false if this process is long running or infinite to avoid filling the heap with an
     * infinite joined trace
     */
    public static FutureAsync<Void> yield(final boolean preserveTrace) {
        return yield(get(), preserveTrace);
    }
    
    public static FutureAsync<Void> yield(final Executor executor) {
        return yield(executor, false);
    }
    
    public static FutureAsync<Void> yield(final Executor executor, final boolean preserveTrace) {
        final FutureAsync<Void> future = new FutureAsync<>();
        final Runnable task = () -> future.complete(null);
        final Runnable wrapped = AsyncLocal.wrap(preserveTrace ? AsyncTrace.wrap(task) : task);
        executor.execute(wrapped);
        return future;
    }
    
    public static final class YieldingAsyncIterator<E> implements AsyncIterator<E>, Wrapper<AsyncIterator<E>> {
        
        private final AsyncIterator<E> wrapped;
        private final int yieldEvery;
        private int countUntilYield;
        
        public YieldingAsyncIterator(final AsyncIterator<E> wrapped, final int yieldEvery) {
            if (wrapped == null) {
                throw new IllegalArgumentException("wrapped is null");
            }
            if (yieldEvery < 1) {
                throw new IllegalArgumentException("yieldEvery is < 1");
            }
            
            this.wrapped = wrapped;
            this.yieldEvery = yieldEvery;
        }
        
        public YieldingAsyncIterator(final AsyncIterator<E> wrapped) {
            this(wrapped, 1);
        }
        
        @Override
        public Async<E> next() throws NoMoreElementsException {
            if (countUntilYield >= yieldEvery) {
                countUntilYield = 0;
                await(yield());
            }
            countUntilYield++;
            return wrapped.next();
        }
        
        @Override
        public AsyncIterator<E> unwrap() {
            return wrapped;
        }
        
    }
    
    private AsyncExecutor() {}
    
}
