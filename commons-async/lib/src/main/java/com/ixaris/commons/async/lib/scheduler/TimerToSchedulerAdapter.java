package com.ixaris.commons.async.lib.scheduler;

import static com.ixaris.commons.async.lib.CompletableFutureUtil.complete;
import static com.ixaris.commons.async.lib.CompletableFutureUtil.completeFrom;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.Delayed;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.misc.lib.function.RunnableThrows;

/**
 * Adapter that exposes a {@link Timer} as a {@link Scheduler}
 */
final class TimerToSchedulerAdapter implements Scheduler {
    
    static final Scheduler COMMON = new TimerToSchedulerAdapter(new Timer("CommonScheduler", true));
    
    private static final class ScheduledFutureImpl<T> implements ScheduledFuture<T> {
        
        static ScheduledFutureImpl<Void> from(final Runnable runnable, long msPeriod) {
            final CompletableFuture<Void> future = new CompletableFuture<>();
            final TimerTask task = new TimerTask() {
                
                @Override
                public void run() {
                    complete(future, RunnableThrows.from(runnable));
                }
                
            };
            
            return new ScheduledFutureImpl<>(future, task, msPeriod);
        }
        
        static <V> ScheduledFutureImpl<V> from(final Callable<V> callable) {
            final CompletableFuture<V> future = new CompletableFuture<>();
            final TimerTask task = new TimerTask() {
                
                @Override
                public void run() {
                    complete(future, CallableThrows.from(callable));
                }
                
            };
            
            return new ScheduledFutureImpl<>(future, task, 0L);
        }
        
        static <V> ScheduledFutureImpl<V> from(final CallableThrows<CompletionStage<V>, ?> callable) {
            final CompletableFuture<V> future = new CompletableFuture<>();
            final TimerTask task = new TimerTask() {
                
                @Override
                public void run() {
                    completeFrom(future, callable);
                }
                
            };
            
            return new ScheduledFutureImpl<>(future, task, 0L);
        }
        
        private final CompletableFuture<T> future;
        private final TimerTask task;
        private final long period;
        
        private ScheduledFutureImpl(final CompletableFuture<T> future, final TimerTask task, final long period) {
            this.future = future;
            this.task = task;
            if (Math.abs(period) > (Long.MAX_VALUE >> 1)) {
                this.period = period >> 1; // copied from java.util.Timer
            } else {
                this.period = period;
            }
        }
        
        @Override
        public long getDelay(final TimeUnit unit) {
            final long delay = task.scheduledExecutionTime() - System.currentTimeMillis() + period;
            return unit.convert(delay, TimeUnit.MILLISECONDS);
        }
        
        @Override
        public int compareTo(final Delayed other) {
            long d = (getDelay(TimeUnit.MILLISECONDS) - other.getDelay(TimeUnit.MILLISECONDS));
            return (d < 0) ? -1 : (d > 0) ? 1 : 0;
        }
        
        @Override
        public boolean isCancelled() {
            return future.isCancelled();
        }
        
        @Override
        public boolean cancel(final boolean mayInterruptIfRunning) {
            task.cancel();
            return future.cancel(mayInterruptIfRunning);
        }
        
        @Override
        public boolean isDone() {
            return future.isDone();
        }
        
        @Override
        public T get() throws InterruptedException, ExecutionException {
            return future.get();
        }
        
        @Override
        public T get(final long timeout, final TimeUnit unit) throws InterruptedException, ExecutionException, TimeoutException {
            return future.get(timeout, unit);
        }
        
    }
    
    private final Timer timer;
    
    TimerToSchedulerAdapter(final Timer timer) {
        this.timer = timer;
    }
    
    @Override
    public ScheduledFuture<?> schedule(final Runnable runnable, final long delay, final TimeUnit unit) {
        final ScheduledFutureImpl<?> task = ScheduledFutureImpl.from(runnable, 0L);
        timer.schedule(task.task, unit.toMillis(delay));
        return task;
    }
    
    @Override
    public <V> ScheduledFuture<V> schedule(final Callable<V> callable, final long delay, final TimeUnit unit) {
        final ScheduledFutureImpl<V> task = ScheduledFutureImpl.from(callable);
        timer.schedule(task.task, unit.toMillis(delay));
        return task;
    }
    
    @Override
    public <V> ScheduledFuture<V> schedule(final CallableThrows<CompletionStage<V>, ?> callable, final long delay, final TimeUnit unit) {
        final ScheduledFutureImpl<V> task = ScheduledFutureImpl.from(callable);
        timer.schedule(task.task, unit.toMillis(delay));
        return task;
    }
    
    @Override
    public ScheduledFuture<?> schedule(final Runnable runnable, final Date time) {
        final ScheduledFutureImpl<?> task = ScheduledFutureImpl.from(runnable, 0L);
        timer.schedule(task.task, time);
        return task;
    }
    
    @Override
    public ScheduledFuture<?> scheduleAtFixedRate(final Runnable runnable, final long initialDelay, final long period, final TimeUnit unit) {
        final long msPeriod = unit.toMillis(period);
        final ScheduledFutureImpl<?> task = ScheduledFutureImpl.from(runnable, msPeriod);
        timer.scheduleAtFixedRate(task.task, unit.toMillis(initialDelay), msPeriod);
        return task;
    }
    
    @Override
    public ScheduledFuture<?> scheduleWithFixedDelay(final Runnable runnable, final long initialDelay, final long delay, final TimeUnit unit) {
        final long msPeriod = unit.toMillis(delay);
        final ScheduledFutureImpl<?> task = ScheduledFutureImpl.from(runnable, msPeriod);
        timer.schedule(task.task, unit.toMillis(initialDelay), msPeriod);
        return task;
    }
    
    @Override
    public void shutdown() {
        if (this != COMMON) {
            timer.cancel();
        }
    }
    
}
