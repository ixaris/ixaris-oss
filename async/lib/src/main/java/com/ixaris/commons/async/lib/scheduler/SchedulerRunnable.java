package com.ixaris.commons.async.lib.scheduler;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class SchedulerRunnable {
    
    private static final Logger LOG = LoggerFactory.getLogger(SchedulerRunnable.class);
    
    @SuppressWarnings("squid:S1181")
    public static Runnable wrapLoggingAndIgnoringErrors(final Runnable runnable) {
        return () -> {
            try {
                runnable.run();
            } catch (final Throwable t) {
                LOG.error("Unexpected error in scheduled runnable", t);
            }
        };
    }
    
    private SchedulerRunnable() {}
    
}
