package com.ixaris.commons.async.lib.scheduler;

import java.util.Date;
import java.util.Timer;
import java.util.TimerTask;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Adapter that exposes a {@link Timer} as a {@link Scheduler}
 */
final class SchedulerTimerAdapter implements Scheduler {
    
    static final Scheduler COMMON = new SchedulerTimerAdapter(new Timer("CommonScheduler", true));
    
    private static final Logger LOG = LoggerFactory.getLogger(SchedulerTimerAdapter.class);
    
    private static TimerTask convert(final Runnable runnable) {
        if (runnable == null) {
            throw new IllegalArgumentException("runnable is null");
        }
        
        return new TimerTask() {
            
            @Override
            public void run() {
                // catch exceptions to prevent timer task from stopping timer thread
                try {
                    runnable.run();
                } catch (final Throwable t) { // NOSONAR framework code
                    LOG.error("Timer task failed", t);
                }
            }
            
        };
    }
    
    private final Timer timer;
    
    SchedulerTimerAdapter(final Timer timer) {
        this.timer = timer;
    }
    
    @Override
    public TimerTask schedule(final Runnable runnable, final long delay, final TimeUnit unit) {
        final TimerTask task = convert(runnable);
        timer.schedule(task, unit.toMillis(delay));
        return task;
    }
    
    @Override
    public TimerTask schedule(final Runnable runnable, final Date time) {
        final TimerTask task = convert(runnable);
        timer.schedule(task, time);
        return task;
    }
    
    @Override
    public TimerTask scheduleAtFixedRate(final Runnable runnable, final long initialDelay, final long period, final TimeUnit unit) {
        final TimerTask task = convert(runnable);
        timer.scheduleAtFixedRate(task, unit.toMillis(initialDelay), unit.toMillis(period));
        return task;
    }
    
    @Override
    public TimerTask scheduleWithFixedDelay(final Runnable runnable, final long initialDelay, final long delay, final TimeUnit unit) {
        final TimerTask task = convert(runnable);
        timer.schedule(task, unit.toMillis(initialDelay), unit.toMillis(delay));
        return task;
    }
    
    @Override
    public void shutdown() {
        if (this != COMMON) {
            timer.cancel();
        }
    }
    
}
