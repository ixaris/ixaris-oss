package com.ixaris.commons.async.lib.scheduler;

import static com.ixaris.commons.async.lib.CompletableFutureUtil.complete;

import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.misc.lib.object.Wrapper;

/**
 * Wrapper for a {@link Scheduler} to execute scheduled jobs on the given executor
 */
public class ScheduledExecutorServiceWrapper<E extends ExecutorService> implements ScheduledExecutorService, Wrapper<E> {
    
    protected final E wrapped;
    protected final Scheduler scheduler;
    
    public ScheduledExecutorServiceWrapper(final E wrapped, final Scheduler scheduler) {
        if (wrapped == null) {
            throw new IllegalArgumentException("wrapped is null");
        }
        if (scheduler == null) {
            throw new IllegalArgumentException("scheduler is null");
        }
        
        this.wrapped = wrapped;
        this.scheduler = scheduler;
    }
    
    @Override
    public void shutdown() {
        scheduler.shutdown();
        wrapped.shutdown();
    }
    
    @Override
    public List<Runnable> shutdownNow() {
        scheduler.shutdown();
        return wrapped.shutdownNow();
    }
    
    @Override
    public void execute(Runnable command) {
        wrapped.execute(command);
    }
    
    @Override
    public ScheduledFuture<?> schedule(final Runnable runnable, final long delay, final TimeUnit unit) {
        return scheduler.schedule(() -> wrapped.execute(runnable), delay, unit);
    }
    
    @Override
    public <V> ScheduledFuture<V> schedule(final Callable<V> callable, final long delay, final TimeUnit unit) {
        return scheduler.schedule(
            () -> {
                final CompletableFuture<V> future = new CompletableFuture<>();
                wrapped.execute(() -> complete(future, CallableThrows.from(callable)));
                return future;
            },
            delay,
            unit);
    }
    
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable runnable, final long initialDelay, final long period, final TimeUnit unit) {
        return scheduler.scheduleAtFixedRate(() -> wrapped.execute(runnable), initialDelay, period, unit);
    }
    
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable runnable, final long initialDelay, final long delay, final TimeUnit unit) {
        return scheduler.scheduleWithFixedDelay(() -> wrapped.execute(runnable), initialDelay, delay, unit);
    }
    
    @Override
    public boolean isShutdown() {
        return wrapped.isShutdown();
    }
    
    @Override
    public boolean isTerminated() {
        return wrapped.isTerminated();
    }
    
    @Override
    public boolean awaitTermination(final long timeout, final TimeUnit unit) throws InterruptedException {
        return wrapped.awaitTermination(timeout, unit);
    }
    
    @Override
    public <T> Future<T> submit(final Callable<T> task) {
        return wrapped.submit(task);
    }
    
    @Override
    public <T> Future<T> submit(final Runnable task, final T result) {
        return wrapped.submit(task, result);
    }
    
    @Override
    public Future<?> submit(final Runnable task) {
        return wrapped.submit(task);
    }
    
    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return wrapped.invokeAll(tasks);
    }
    
    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException {
        return wrapped.invokeAll(tasks, timeout, unit);
    }
    
    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks) throws InterruptedException, ExecutionException {
        return wrapped.invokeAny(tasks);
    }
    
    @Override
    public <T> T invokeAny(final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
        return wrapped.invokeAny(tasks, timeout, unit);
    }
    
    @Override
    public E unwrap() {
        return wrapped;
    }
    
}
