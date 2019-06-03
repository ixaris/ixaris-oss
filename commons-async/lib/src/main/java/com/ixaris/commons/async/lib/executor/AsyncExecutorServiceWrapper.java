package com.ixaris.commons.async.lib.executor;

import com.ixaris.commons.async.lib.AsyncExecutor;
import java.util.Collection;
import java.util.List;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Future;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

/**
 * Wrapper for an {@link ExecutorService} to wrap jobs using {@link AsyncExecutor}
 */
public class AsyncExecutorServiceWrapper<E extends ExecutorService> extends AsyncExecutorWrapper<E>
implements ExecutorService {
    
    public AsyncExecutorServiceWrapper(final E wrapped) {
        super(wrapped);
    }
    
    @Override
    public void shutdown() {
        wrapped.shutdown();
    }
    
    @Override
    public List<Runnable> shutdownNow() {
        return wrapped.shutdownNow();
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
        return wrapped.submit(AsyncExecutor.wrap(this, task));
    }
    
    @Override
    public <T> Future<T> submit(final Runnable task, final T result) {
        return wrapped.submit(AsyncExecutor.wrap(this, task), result);
    }
    
    @Override
    public Future<?> submit(final Runnable task) {
        return wrapped.submit(AsyncExecutor.wrap(this, task));
    }
    
    @Override
    public <T> List<Future<T>> invokeAll(final Collection<? extends Callable<T>> tasks) throws InterruptedException {
        return wrapped.invokeAll(AsyncExecutor.wrap(this, tasks));
    }
    
    @Override
    public <T> List<Future<T>> invokeAll(
        final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit
    ) throws InterruptedException {
        return wrapped.invokeAll(AsyncExecutor.wrap(this, tasks), timeout, unit);
    }
    
    @Override
    public <T> T invokeAny(
        final Collection<? extends Callable<T>> tasks
    ) throws InterruptedException, ExecutionException {
        return wrapped.invokeAny(AsyncExecutor.wrap(this, tasks));
    }
    
    @Override
    public <T> T invokeAny(
        final Collection<? extends Callable<T>> tasks, final long timeout, final TimeUnit unit
    ) throws InterruptedException, ExecutionException, TimeoutException {
        return wrapped.invokeAny(AsyncExecutor.wrap(this, tasks), timeout, unit);
    }
    
}
