package com.ixaris.commons.async.lib.scheduler;

import java.util.Date;
import java.util.Timer;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

import com.ixaris.commons.misc.lib.function.CallableThrows;

/**
 * Scheduler api similar to ScheduledExecutorService, for the sake of consistency
 */
public interface Scheduler {
    
    static Scheduler newScheduler(final String name) {
        return newScheduler(new Timer(name));
    }
    
    static Scheduler newScheduler(final Timer timer) {
        return new TimerToSchedulerAdapter(timer);
    }
    
    static Scheduler commonScheduler() {
        return TimerToSchedulerAdapter.COMMON;
    }
    
    /**
     * Schedules the specified task for execution after the specified delay.
     *
     * @param runnable the task to execute
     * @param delay the time to delay execution
     * @param unit the time unit of the initialDelay and period parameters
     */
    ScheduledFuture<?> schedule(Runnable runnable, long delay, TimeUnit unit);
    
    <V> ScheduledFuture<V> schedule(Callable<V> callable, long delay, TimeUnit unit);
    
    <V> ScheduledFuture<V> schedule(CallableThrows<CompletionStage<V>, ?> callable, long delay, TimeUnit unit);
    
    /**
     * Schedules the specified task for execution at the specified time.  If
     * the time is in the past, the task is scheduled for immediate execution.
     *
     * @param runnable the task to execute
     * @param time time at which task is to be executed.
     * @return the scheduled task which can be used to cancel the schedule
     */
    ScheduledFuture<?> schedule(final Runnable runnable, final Date time);
    
    /**
     * @param runnable the task to execute
     * @param initialDelay the time to delay first execution
     * @param period the period between successive executions
     * @param unit the time unit of the initialDelay and period parameters
     * @return the scheduled task which can be used to cancel the schedule
     */
    ScheduledFuture<?> scheduleAtFixedRate(final Runnable runnable, final long initialDelay, final long period, final TimeUnit unit);
    
    /**
     * @param runnable the task to execute
     * @param initialDelay the time to delay first execution
     * @param delay the delay between the termination of one execution and the commencement of the next
     * @param unit the time unit of the initialDelay and delay parameters
     * @return the scheduled task which can be used to cancel the schedule
     */
    ScheduledFuture<?> scheduleWithFixedDelay(final Runnable runnable, final long initialDelay, final long delay, final TimeUnit unit);
    
    void shutdown();
    
}
