package com.ixaris.commons.misc.lib.startup;

import java.util.concurrent.atomic.AtomicInteger;

public class TestStartupTask implements StartupTask {
    
    static final AtomicInteger count = new AtomicInteger();
    
    @Override
    public void run() {
        count.incrementAndGet();
    }
    
}
