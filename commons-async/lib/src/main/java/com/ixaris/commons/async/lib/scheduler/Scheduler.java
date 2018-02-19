package com.ixaris.commons.async.lib.scheduler;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

/**
 * Scheduler api similar to ScheduledExecutorService, for the sake of consistency
 */
public interface Scheduler {
    
    static Scheduler newScheduler(final String name) {
        return newScheduler(new Timer(name));
    }
    
    static Scheduler newScheduler(final Timer timer) {
        return new SchedulerTimerAdapter(timer);
    }
    
    static Scheduler commonScheduler() {
        return SchedulerTimerAdapter.COMMON;
    }
    
    /**
     * Schedules the specified task for execution after the specified delay.
     *
     * @param runnable the task to execute
     * @param delay the time to delay execution
     * @param unit the time unit of the initialDelay and period parameters
     */
    TimerTask schedule(Runnable runnable, long delay, TimeUnit unit);
    
    /**
     * Schedules the specified task for execution at the specified time.  If
     * the time is in the past, the task is scheduled for immediate execution.
     *
     * @param runnable the task to execute
     * @param time time at which task is to be executed.
     * @return the scheduled task which can be used to cancel the schedule
     */
    TimerTask schedule(final Runnable runnable, final Date time);
    
    /**
     * @param runnable the task to execute
     * @param initialDelay the time to delay first execution
     * @param period the period between successive executions
     * @param unit the time unit of the initialDelay and period parameters
     * @return the scheduled task which can be used to cancel the schedule
     */
    TimerTask scheduleAtFixedRate(final Runnable runnable, final long initialDelay, final long period, final TimeUnit unit);
    
    /**
     * @param runnable the task to execute
     * @param initialDelay the time to delay first execution
     * @param delay the delay between the termination of one execution and the commencement of the next
     * @param unit the time unit of the initialDelay and delay parameters
     * @return the scheduled task which can be used to cancel the schedule
     */
    TimerTask scheduleWithFixedDelay(final Runnable runnable, final long initialDelay, final long delay, final TimeUnit unit);
    
    void shutdown();
    
}
