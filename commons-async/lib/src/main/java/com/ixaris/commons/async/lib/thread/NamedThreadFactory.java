package com.ixaris.commons.async.lib.thread;

import java.util.concurrent.ThreadFactory;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Thread factory that names the created threads by a given prefix
 */
public final class NamedThreadFactory implements ThreadFactory {
    
    private final String prefix;
    private final AtomicInteger threadNum = new AtomicInteger();
    
    public NamedThreadFactory(final String prefix) {
        if (prefix == null) {
            throw new IllegalArgumentException("prefix is null");
        }
        
        this.prefix = prefix;
    }
    
    @Override
    public Thread newThread(final Runnable r) {
        return new Thread(r, prefix + threadNum.getAndIncrement());
    }
    
}
