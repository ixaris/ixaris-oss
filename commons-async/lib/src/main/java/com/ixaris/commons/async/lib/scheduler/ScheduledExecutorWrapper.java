package com.ixaris.commons.async.lib.scheduler;

import static com.ixaris.commons.async.lib.CompletableFutureUtil.complete;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.AbstractExecutorService;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.RejectedExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.misc.lib.object.Wrapper;

/**
 * Wrapper for a {@link Scheduler} to execute scheduled jobs on the given executor
 */
public class ScheduledExecutorWrapper<E extends Executor> extends AbstractExecutorService implements ScheduledExecutorService, Wrapper<E> {
    
    protected final E wrapped;
    protected final Scheduler scheduler;
    private final AtomicBoolean active = new AtomicBoolean(true);
    
    public ScheduledExecutorWrapper(final E wrapped, final Scheduler scheduler) {
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
        if (active.compareAndSet(true, false)) {
            scheduler.shutdown();
        }
    }
    
    @Override
    public List<Runnable> shutdownNow() {
        shutdown();
        return Collections.emptyList();
    }
    
    @Override
    public boolean isShutdown() {
        return !active.get();
    }
    
    @Override
    public boolean isTerminated() {
        return !active.get();
    }
    
    @Override
    public boolean awaitTermination(final long timeout, final TimeUnit unit) {
        return true;
    }
    
    @Override
    public final void execute(final Runnable command) {
        if (active.get()) {
            wrapped.execute(command);
        } else {
            throw new RejectedExecutionException();
        }
    }
    
    @Override
    public ScheduledFuture<?> schedule(final Runnable runnable, final long delay, final TimeUnit unit) {
        if (active.get()) {
            return scheduler.schedule(() -> wrapped.execute(runnable), delay, unit);
        } else {
            throw new RejectedExecutionException();
        }
    }
    
    @Override
    public <V> ScheduledFuture<V> schedule(final Callable<V> callable, final long delay, final TimeUnit unit) {
        if (active.get()) {
            return scheduler.schedule(() -> {
                final CompletableFuture<V> future = new CompletableFuture<>();
                wrapped.execute(() -> complete(future, CallableThrows.from(callable)));
                return future;
            }, delay, unit);
        } else {
            throw new RejectedExecutionException();
        }
    }
    
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable runnable, final long initialDelay, final long period, final TimeUnit unit) {
        if (active.get()) {
            return scheduler.scheduleAtFixedRate(() -> wrapped.execute(runnable), initialDelay, period, unit);
        } else {
            throw new RejectedExecutionException();
        }
        
    }
    
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable runnable, final long initialDelay, final long delay, final TimeUnit unit) {
        if (active.get()) {
            return scheduler.scheduleWithFixedDelay(() -> wrapped.execute(runnable), initialDelay, delay, unit);
        } else {
            throw new RejectedExecutionException();
        }
    }
    
    @Override
    public final E unwrap() {
        return wrapped;
    }
    
}
