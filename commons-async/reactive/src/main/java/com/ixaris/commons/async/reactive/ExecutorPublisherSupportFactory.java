package com.ixaris.commons.async.reactive;

import com.ixaris.commons.async.lib.AsyncExecutor;
import com.ixaris.commons.async.lib.executor.AsyncExecutorServiceWrapper;
import com.ixaris.commons.async.lib.executor.AsyncExecutorWrapper;
import com.ixaris.commons.async.lib.executor.AsyncScheduledExecutorServiceWrapper;
import com.ixaris.commons.async.lib.scheduler.ScheduledExecutorServiceWrapper;
import com.ixaris.commons.async.lib.scheduler.ScheduledExecutorWrapper;
import com.ixaris.commons.async.lib.scheduler.Scheduler;
import com.ixaris.commons.async.lib.thread.NamedThreadFactory;
import com.ixaris.commons.misc.lib.object.Wrapper;
import java.util.concurrent.Executor;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import org.reactivestreams.Subscriber;

/**
 * The executor for this factory should be wrapped in an {@link AsyncExecutor} wrapper
 */
public final class ExecutorPublisherSupportFactory
implements PublisherSupportFactory, SchedulingSupport, AutoCloseable {
    
    public static ExecutorPublisherSupportFactory common() {
        return new ExecutorPublisherSupportFactory(AsyncExecutor.DEFAULT);
    }
    
    private static ScheduledExecutorService createScheduledExecutorService(int size) {
        return Executors.newScheduledThreadPool(size, new NamedThreadFactory("ExecutorPublisherSupportFactory-"));
    }
    
    private final ScheduledExecutorService executor;
    
    public ExecutorPublisherSupportFactory(final int size) {
        this(createScheduledExecutorService(size));
    }
    
    public ExecutorPublisherSupportFactory(final ScheduledExecutorService executor) {
        if (executor == null) {
            throw new IllegalArgumentException("executor is null");
        }
        
        this.executor = Wrapper.isWrappedBy(executor, AsyncScheduledExecutorServiceWrapper.class)
            ? executor : new AsyncScheduledExecutorServiceWrapper<>(executor);
    }
    
    public ExecutorPublisherSupportFactory(final ExecutorService executor, final Scheduler scheduler) {
        if (executor == null) {
            throw new IllegalArgumentException("executor is null");
        }
        if (scheduler == null) {
            throw new IllegalArgumentException("executor is null");
        }
        
        if (Wrapper.isWrappedBy(executor, AsyncExecutorServiceWrapper.class)) {
            this.executor = new ScheduledExecutorServiceWrapper<>(executor, scheduler);
        } else {
            this.executor = new AsyncScheduledExecutorServiceWrapper<>(
                new ScheduledExecutorServiceWrapper<>(executor, scheduler)
            );
        }
    }
    
    public ExecutorPublisherSupportFactory(final Executor executor, final Scheduler scheduler) {
        if (executor == null) {
            throw new IllegalArgumentException("executor is null");
        }
        if (scheduler == null) {
            throw new IllegalArgumentException("executor is null");
        }
        
        if (Wrapper.isWrappedBy(executor, AsyncExecutorWrapper.class)) {
            this.executor = new ScheduledExecutorWrapper<>(executor, scheduler);
        } else {
            this.executor = new AsyncScheduledExecutorServiceWrapper<>(
                new ScheduledExecutorWrapper<>(executor, scheduler)
            );
        }
    }
    
    @Override
    public <T> PublisherSupport<T> create() {
        return new ExecutorPublisherSupport<>();
    }
    
    @Override
    public ScheduledFuture<?> schedule(final Runnable runnable, final long delay, final TimeUnit unit) {
        return executor.schedule(runnable, delay, unit);
    }
    
    @Override
    public void close() {
        executor.shutdown();
    }
    
    private final class ExecutorPublisherSupport<T> extends AbstractSingleSubscriberPublisherSupport<T> {
        
        @Override
        protected void next(final Subscriber<? super T> s, final T t) {
            executor.execute(() -> s.onNext(t));
        }
        
    }
    
}
