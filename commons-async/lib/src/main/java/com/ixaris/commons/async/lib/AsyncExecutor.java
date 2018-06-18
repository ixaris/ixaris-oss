package com.ixaris.commons.async.lib;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.CompletableFutureUtil.complete;
import static com.ixaris.commons.async.lib.CompletableFutureUtil.completeFrom;

import java.util.ArrayList;
import java.util.Collection;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixaris.commons.async.lib.scheduler.ScheduledExecutorServiceWrapper;
import com.ixaris.commons.async.lib.scheduler.Scheduler;
import com.ixaris.commons.async.lib.thread.ThreadLocalHelper;
import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.misc.lib.function.RunnableThrows;
import com.ixaris.commons.misc.lib.object.Wrapper;

/**
 * This class allows an executor to keep a thread local reference to itself in the tasks it executes, primarily for
 * allowing asynchronous tasks to relay execution back to the original executor (or default to relaying to common pool).
 * E.g. if a database or network operation is performed, the execution is relayed back once the result is obtained.
 * It is the responsibility of implementors that switch executors to relay execution back and to preserve async trace
 * and async locals across tasks.
 *
 * <pre>
 * ...
 * final Result result = await(relay(() -&gt; exec(otherExecutor, () -&gt; transaction(tx, persistenceCallable))));
 * doSomethingWith(result);
 * ...
 * </pre>
 */
public final class AsyncExecutor {
    
    public static final ScheduledExecutorService DEFAULT =
        new ScheduledExecutorServiceWrapper<>(ForkJoinPool.commonPool(), Scheduler.commonScheduler());
    
    private static final ThreadLocal<Executor> ASYNC_CONTEXT = new ThreadLocal<>();
    private static final Logger LOG = LoggerFactory.getLogger(AsyncExecutor.class);
    
    public static Runnable wrap(final Executor executor, final Runnable task) {
        return () -> {
            try {
                ThreadLocalHelper.exec(ASYNC_CONTEXT, executor, RunnableThrows.from(task));
            } catch (final Throwable t) { // NOSONAR required to join trace
                LOG.error("Unhandled error", AsyncTrace.join(t));
                throw t;
            }
        };
    }
    
    public static <T> Callable<T> wrap(final Executor executor, final Callable<T> task) {
        return () -> {
            try {
                return ThreadLocalHelper.exec(ASYNC_CONTEXT, executor, CallableThrows.from(task));
            } catch (final Throwable t) { // NOSONAR required to join trace
                LOG.error("Unhandled error", AsyncTrace.join(t));
                throw t;
            }
        };
    }
    
    public static <T> Collection<? extends Callable<T>> wrap(final Executor executor, final Collection<? extends Callable<T>> tasks) {
        final Collection<Callable<T>> coll = new ArrayList<>(tasks.size());
        for (final Callable<T> task : tasks) {
            coll.add(() -> {
                try {
                    return ThreadLocalHelper.exec(ASYNC_CONTEXT, executor, CallableThrows.from(task));
                } catch (final Throwable t) { // NOSONAR required to join trace
                    LOG.error("Unhandled error", AsyncTrace.join(t));
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
     * Execute a synchronous task (the runnable) in the same executor as the current thread and return a future that is completed
     * when the task completes
     *
     * @param runnable
     * @param <E>
     * @return
     * @throws E
     */
    public static <E extends Exception> Async<Void> exec(final RunnableThrows<E> runnable) throws E {
        return exec(get(), runnable);
    }
    
    /**
     * Execute a synchronous task (the runnable) in an executor and return a future that is completed when the task completes
     *
     * @param executor
     * @param runnable
     * @param <E>
     * @return
     * @throws E
     */
    public static <E extends Exception> Async<Void> exec(final Executor executor, final RunnableThrows<E> runnable) throws E {
        final FutureAsync<Void> future = new FutureAsync<>();
        executor.execute(AsyncTrace.wrap(AsyncLocal.wrap(() -> complete(future, runnable))));
        return future;
    }
    
    /**
     * Execute a synchronous task (the callable) in the same executor as the current thread and return a future that is fulfilled from
     * the executed task's result
     *
     * @param callable
     * @param <T>
     * @param <E>
     * @return
     * @throws E
     */
    public static <T, E extends Exception> Async<T> exec(final CallableThrows<T, E> callable) throws E {
        return exec(get(), callable);
    }
    
    /**
     * Execute a synchronous task (the callable) in an executor and return a future that is fulfilled from the executed task's result
     *
     * @param executor
     * @param callable
     * @param <T>
     * @param <E>
     * @return
     * @throws E
     */
    public static <T, E extends Exception> Async<T> exec(final Executor executor, final CallableThrows<T, E> callable) throws E {
        final FutureAsync<T> future = new FutureAsync<>();
        executor.execute(AsyncTrace.wrap(AsyncLocal.wrap(() -> complete(future, callable))));
        return future;
    }
    
    /**
     * Execute an asynchronous task in the same executor as the current thread and return a future that is fulfilled from
     * the executed task's result
     *
     * @param callable
     * @param <T>
     * @param <E>
     * @return
     * @throws E
     */
    public static <T, E extends Exception> Async<T> exec(final CompletionStageCallableThrows<T, E> callable) throws E {
        return exec(get(), callable);
    }
    
    /**
     * Execute an asynchronous task in an executor and return a future that is fulfilled from the executed task's result
     *
     * @param executor
     * @param callable
     * @param <T>
     * @param <E>
     * @return
     * @throws E
     */
    public static <T, E extends Exception> Async<T> exec(final Executor executor, final CompletionStageCallableThrows<T, E> callable) throws E {
        final FutureAsync<T> future = new FutureAsync<>();
        executor.execute(AsyncTrace.wrap(AsyncLocal.wrap(() -> completeFrom(future, callable))));
        return future;
    }
    
    public static <E extends Exception> Async<Void> schedule(final long delay,
                                                             final TimeUnit timeUnit,
                                                             final RunnableThrows<E> runnable) throws E {
        return schedule(get(), delay, timeUnit, runnable);
    }
    
    /**
     * Schedule a task and return a future that is completed on the given executor when the scheduled task completes
     *
     * @param executor
     * @param runnable
     * @param <E>
     * @return
     * @throws E
     */
    public static <E extends Exception> Async<Void> schedule(final Executor executor,
                                                             final long delay,
                                                             final TimeUnit timeUnit,
                                                             final RunnableThrows<E> runnable) throws E {
        final FutureAsync<Void> future = new FutureAsync<>();
        final Runnable wrapped = AsyncTrace.wrap(AsyncLocal.wrap(() -> complete(future, runnable)));
        if (executor instanceof ScheduledExecutorService) {
            ((ScheduledExecutorService) executor).schedule(wrapped, delay, timeUnit);
        } else {
            Scheduler.commonScheduler().schedule(() -> executor.execute(wrapped), delay, timeUnit);
        }
        return future;
    }
    
    public static <T, E extends Exception> Async<T> schedule(final long delay,
                                                             final TimeUnit timeUnit,
                                                             final CallableThrows<T, E> callable) throws E {
        return schedule(get(), delay, timeUnit, callable);
    }
    
    /**
     * Schedule a task and return a future that is fulfilled on the given executor from the scheduled task's result
     *
     * @param executor
     * @param callable
     * @param <T>
     * @param <E>
     * @return
     * @throws E
     */
    public static <T, E extends Exception> Async<T> schedule(final Executor executor,
                                                             final long delay,
                                                             final TimeUnit timeUnit,
                                                             final CallableThrows<T, E> callable) throws E {
        final FutureAsync<T> future = new FutureAsync<>();
        final Runnable wrapped = AsyncTrace.wrap(AsyncLocal.wrap(() -> complete(future, callable)));
        if (executor instanceof ScheduledExecutorService) {
            ((ScheduledExecutorService) executor).schedule(wrapped, delay, timeUnit);
        } else {
            Scheduler.commonScheduler().schedule(() -> executor.execute(wrapped), delay, timeUnit);
        }
        return future;
    }
    
    public static <T, E extends Exception> Async<T> schedule(final long delay,
                                                             final TimeUnit timeUnit,
                                                             final CompletionStageCallableThrows<T, E> callable) throws E {
        return schedule(get(), delay, timeUnit, callable);
    }
    
    /**
     * Schedule a task and return a future that is fulfilled on the given executor from the scheduled task's future
     *
     * @param executor
     * @param callable
     * @param <T>
     * @param <E>
     * @return
     * @throws E
     */
    public static <T, E extends Exception> Async<T> schedule(final Executor executor,
                                                             final long delay,
                                                             final TimeUnit timeUnit,
                                                             final CompletionStageCallableThrows<T, E> callable) throws E {
        final FutureAsync<T> future = new FutureAsync<>();
        final Runnable wrapped = AsyncTrace.wrap(AsyncLocal.wrap(() -> completeFrom(future, callable)));
        if (executor instanceof ScheduledExecutorService) {
            ((ScheduledExecutorService) executor).schedule(wrapped, delay, timeUnit);
        } else {
            Scheduler.commonScheduler().schedule(() -> executor.execute(wrapped), delay, timeUnit);
        }
        return future;
    }
    
    /**
     * Relay execution back to the executor associated with the current thread (defaulting to common pool). This is
     * achieved by completing a future on the executor from the given future.
     *
     * @param stage
     * @param <T>
     * @return
     */
    public static <T> Async<T> relay(final CompletionStage<T> stage) {
        return relay(get(), stage);
    }
    
    /**
     * Use {@link #relay(CompletionStage)}
     */
    @Deprecated
    public static <T, E extends Exception> Async<T> relay(final CallableThrows<Async<T>, E> callable) throws E {
        return relay(get(), callable.call());
    }
    
    /**
     * Relay execution back to the given executor. same semantics as {@link #relay(CompletionStage)}
     *
     * @param executor
     * @param stage
     * @param <T>
     * @return
     */
    public static <T> Async<T> relay(final Executor executor, final CompletionStage<T> stage) {
        final FutureAsync<T> future = new FutureAsync<>();
        stage.whenComplete((r, t) -> {
            if (get() != executor) { // NOSONAR check reference
                final Throwable tt = AsyncTrace.join(CompletionStageUtil.extractCause(t));
                executor.execute(AsyncTrace.wrap(AsyncLocal.wrap(() -> {
                    if (t != null) {
                        future.completeExceptionally(tt);
                    } else {
                        future.complete(r);
                    }
                })));
            } else {
                complete(future, r, t);
            }
        });
        return future;
    }
    
    /**
     * Use {@link #relay(Executor, CompletionStage)}
     */
    @Deprecated
    public static <T, E extends Exception> Async<T> relay(final Executor executor,
                                                          final CallableThrows<Async<T>, E> callable) throws E {
        return relay(executor, callable.call());
    }
    
    /**
     * Async processes need to await the sleep, i.e. await(sleep(10, SECONDS))
     */
    public static Async<Void> sleep(final long interval, final TimeUnit timeUnit) {
        return sleep(get(), interval, timeUnit);
    }
    
    public static Async<Void> sleep(final Executor executor, final long interval, final TimeUnit timeUnit) {
        final FutureAsync<Void> future = new FutureAsync<>();
        final Runnable wrapped = AsyncTrace.wrap(AsyncLocal.wrap(() -> {
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
     * Used in co-operative multitasking to relinquish control of the computation thread. Used to break lengthy computation
     * which would not otherwise be broken by I/O or other blocking operations.
     * <p>
     * Async processes need to await the yield, i.e. await(yield())
     */
    public static Async<Void> yield() {
        return yield(get());
    }
    
    public static Async<Void> yield(final Executor executor) {
        final FutureAsync<Void> future = new FutureAsync<>();
        executor.execute(AsyncTrace.wrap(AsyncLocal.wrap(() -> {
            future.complete(null);
        })));
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
            if (++countUntilYield >= yieldEvery) {
                countUntilYield = 0;
                await(yield());
            }
            return wrapped.next();
        }
        
        @Override
        public AsyncIterator<E> unwrap() {
            return wrapped;
        }
        
    }
    
    private AsyncExecutor() {}
    
}
