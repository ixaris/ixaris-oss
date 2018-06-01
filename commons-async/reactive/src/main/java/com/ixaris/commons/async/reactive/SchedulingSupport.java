package com.ixaris.commons.async.reactive;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;

public interface SchedulingSupport {
    
    ScheduledFuture<?> schedule(Runnable runnable, long delay, TimeUnit unit);
    
}
