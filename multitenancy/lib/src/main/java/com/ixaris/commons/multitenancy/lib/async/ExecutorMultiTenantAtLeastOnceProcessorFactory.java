package com.ixaris.commons.multitenancy.lib.async;

import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;

import com.ixaris.commons.async.lib.executor.AsyncScheduledExecutorServiceWrapper;
import com.ixaris.commons.async.lib.thread.NamedThreadFactory;
import com.ixaris.commons.clustering.lib.idempotency.AtLeastOnceMessageType;
import com.ixaris.commons.misc.lib.object.Wrapper;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;

public class ExecutorMultiTenantAtLeastOnceProcessorFactory implements MultiTenantAtLeastOnceProcessorFactory {
    
    private static ScheduledExecutorService createScheduledExecutorService(int size) {
        return Executors.newScheduledThreadPool(size,
            new NamedThreadFactory("ExecutorMultiTenantAtLeastOnceProcessorFactory-"));
    }
    
    private final ScheduledExecutorService executor;
    
    public ExecutorMultiTenantAtLeastOnceProcessorFactory(final int size) {
        this(createScheduledExecutorService(size));
    }
    
    public ExecutorMultiTenantAtLeastOnceProcessorFactory(final ScheduledExecutorService executor) {
        if (executor == null) {
            throw new IllegalArgumentException("executor is null");
        }
        
        this.executor = Wrapper.isWrappedBy(executor, AsyncScheduledExecutorServiceWrapper.class)
            ? executor : new AsyncScheduledExecutorServiceWrapper<>(false, executor);
    }
    
    @Override
    public ExecutorMultiTenantAtLeastOnceProcessor<?> create(final AtLeastOnceMessageType<?> messageType, final long refreshInterval) {
        return new ExecutorMultiTenantAtLeastOnceProcessor<>(messageType, refreshInterval, executor);
    }
    
}
