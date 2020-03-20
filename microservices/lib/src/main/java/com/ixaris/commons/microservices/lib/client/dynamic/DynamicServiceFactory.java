package com.ixaris.commons.microservices.lib.client.dynamic;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.AsyncExecutor.sleep;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.StampedLock;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.client.proxy.UntypedOperationInvoker;
import com.ixaris.commons.microservices.lib.client.support.ServiceClientSupport;
import com.ixaris.commons.misc.lib.lock.LockUtil;

/**
 * Factory class to produce {@link UntypedOperationInvoker}s. Does its best to provide immediately useable proxies (ie
 * ones which have performed enough service discovery to work). Keeps a cache to prevent needless reinitialisation of
 * already created proxies.
 *
 * @author brian.vella
 */
public final class DynamicServiceFactory {
    
    private static final Logger LOG = LoggerFactory.getLogger(DynamicServiceFactory.class);
    
    private final ServiceClientSupport serviceClientSupport;
    private final boolean blockUntilDiscovered;
    
    private final Map<String, UntypedOperationInvoker> cache = new ConcurrentHashMap<>();
    private final StampedLock lock = new StampedLock();
    
    public DynamicServiceFactory(final ServiceClientSupport serviceClientSupport, final boolean blockUntilDiscovered) {
        if (serviceClientSupport == null) {
            throw new IllegalArgumentException("serviceClientSupport is null");
        }
        
        this.serviceClientSupport = serviceClientSupport;
        this.blockUntilDiscovered = blockUntilDiscovered;
    }
    
    public Async<UntypedOperationInvoker> getProxy(final String serviceName) {
        
        // not found so we need to create
        final AtomicBoolean createdNow = new AtomicBoolean(false);
        final UntypedOperationInvoker invoker = LockUtil.readMaybeWrite(lock,
            true,
            () -> cache.get(serviceName),
            Objects::nonNull,
            () -> cache.computeIfAbsent(serviceName, k -> {
                LOG.info("Preparing new proxy for service [{}]", serviceName);
                createdNow.set(true);
                
                return serviceClientSupport.getOperationInvoker(serviceName);
            }));
        
        if (blockUntilDiscovered && createdNow.get()) {
            // Only wait for discovery if we have just created the invoker, otherwise we have already connected
            final long start = System.currentTimeMillis();
            while (!invoker.isKeyAvailable("") && (System.currentTimeMillis() - start < 3000L)) {
                await(sleep(100L, TimeUnit.MILLISECONDS));
            }
        }
        return result(invoker);
    }
    
    public int getDefaultTimeout() {
        return serviceClientSupport.getDefaultTimeout();
    }
    
}
