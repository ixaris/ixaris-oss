package com.ixaris.commons.async.reactive;

import java.util.concurrent.TimeUnit;

public interface SchedulingSupport {
    
    ScheduledTask schedule(Runnable runnable, long delay, TimeUnit unit);
    
    @FunctionalInterface
    interface ScheduledTask {
        
        boolean cancel();
        
    }
    
}
