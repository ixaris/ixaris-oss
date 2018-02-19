package com.ixaris.commons.async.reactive;

import java.util.TimerTask;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import org.reactivestreams.Subscriber;

import com.ixaris.commons.async.lib.AsyncExecutor;
import com.ixaris.commons.async.lib.executor.AsyncExecutorServiceWrapper;
import com.ixaris.commons.async.lib.executor.AsyncScheduledExecutorServiceWrapper;
import com.ixaris.commons.async.lib.scheduler.AsyncSchedulerWrapper;
import com.ixaris.commons.async.lib.scheduler.Scheduler;
import com.ixaris.commons.async.lib.thread.NamedThreadFactory;
import com.ixaris.commons.misc.lib.object.Wrapper;

/**
 * The executor for this factory should be wrapped in an {@link AsyncExecutor} wrapper
 */
public final class ExecutorPublisherSupportFactory implements PublisherSupportFactory, SchedulingSupport, AutoCloseable {
    
    public static ExecutorPublisherSupportFactory common() {
        return new ExecutorPublisherSupportFactory(ForkJoinPool.commonPool(), Scheduler.commonScheduler());
    }
    
    private static ScheduledExecutorService createScheduledExecutorService(int size) {
        return Executors.newScheduledThreadPool(size, new NamedThreadFactory("ExecutorPublisherSupportFactory-"));
    }
    
    private final ExecutorService executor;
    private final Scheduler scheduler;
    
    public ExecutorPublisherSupportFactory(final int size) {
        this(createScheduledExecutorService(size));
    }
    
    public ExecutorPublisherSupportFactory(final ScheduledExecutorService executor) {
        if (executor == null) {
            throw new IllegalArgumentException("executor is null");
        }
        
        this.executor = Wrapper.isWrappedBy(executor, AsyncScheduledExecutorServiceWrapper.class)
            ? executor : new AsyncScheduledExecutorServiceWrapper<>(executor);
        this.scheduler = null;
    }
    
    public ExecutorPublisherSupportFactory(final ExecutorService executor, final Scheduler scheduler) {
        if (executor == null) {
            throw new IllegalArgumentException("executor is null");
        }
        if (scheduler == null) {
            throw new IllegalArgumentException("scheduler is null");
        }
        
        this.executor = Wrapper.isWrappedBy(executor, AsyncExecutorServiceWrapper.class)
            ? executor : new AsyncExecutorServiceWrapper<>(executor);
        this.scheduler = Wrapper.isWrappedBy(scheduler, AsyncSchedulerWrapper.class)
            ? scheduler : new AsyncSchedulerWrapper<>(scheduler, executor);
    }
    
    @Override
    public <T> PublisherSupport<T> create() {
        return new ExecutorPublisherSupport<>();
    }
    
    @Override
    public ScheduledTask schedule(final Runnable runnable, final long delay, final TimeUnit unit) {
        if (scheduler != null) {
            final TimerTask task = scheduler.schedule(runnable, delay, unit);
            return task::cancel;
        } else {
            final ScheduledFuture<?> schedule = ((ScheduledExecutorService) executor).schedule(runnable, delay, unit);
            return () -> schedule.cancel(false);
        }
    }
    
    @Override
    public void close() {
        executor.shutdown();
        if (scheduler != null) {
            scheduler.shutdown();
        }
    }
    
    private final class ExecutorPublisherSupport<T> extends AbstractSingleSubscriberPublisherSupport<T> {
        
        @Override
        protected void next(final Subscriber<? super T> s, final T t) {
            executor.execute(() -> s.onNext(t));
        }
        
        @Override
        protected void complete(final Subscriber<? super T> s) {
            executor.execute(s::onComplete);
        }
        
        @Override
        protected void error(final Subscriber<? super T> s, final Throwable t) {
            executor.execute(() -> s.onError(t));
        }
        
    }
    
}
