package com.ixaris.commons.microservices.defaults.app.support;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Semaphore;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.client.ServiceEventListener;
import com.ixaris.commons.microservices.lib.common.ServiceEventHeader;

/**
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public class StackTestTestEventListener<C extends MessageLite, E extends MessageLite> implements ServiceEventListener<C, E> {
    
    private static final Logger LOG = LoggerFactory.getLogger(StackTestTestEventListener.class);
    
    private static final Map<Long, Integer> EVENT_LISTENER_COUNT_TO_FAIL = new HashMap<>();
    private static final Map<Long, CompletableFuture<?>> EVENT_LISTENER_SUCCESS_FUTURE = new HashMap<>();
    public static final Semaphore eventListenerSemaphore = new Semaphore(1);
    
    public static void put(final long intentId, final Integer failCount, final CompletableFuture<?> future) {
        if (failCount != null && failCount > 0) {
            synchronized (EVENT_LISTENER_COUNT_TO_FAIL) {
                EVENT_LISTENER_COUNT_TO_FAIL.put(intentId, failCount);
            }
        }
        if (future != null) {
            synchronized (EVENT_LISTENER_SUCCESS_FUTURE) {
                EVENT_LISTENER_SUCCESS_FUTURE.put(intentId, future);
            }
        }
    }
    
    public static void clear() {
        synchronized (EVENT_LISTENER_COUNT_TO_FAIL) {
            EVENT_LISTENER_COUNT_TO_FAIL.clear();
        }
        synchronized (EVENT_LISTENER_SUCCESS_FUTURE) {
            EVENT_LISTENER_SUCCESS_FUTURE.clear();
        }
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public Async<Void> onEvent(final ServiceEventHeader<C> header, final E event) {
        
        try {
            eventListenerSemaphore.acquire();
        } catch (InterruptedException e) {
            throw new IllegalStateException(e);
        }
        
        try {
            LOG.info("Listener Received event [{}] for context [{}]", event, header);
            final long intentId = header.getIntentId();
            
            synchronized (EVENT_LISTENER_COUNT_TO_FAIL) {
                final Integer fc = EVENT_LISTENER_COUNT_TO_FAIL.computeIfPresent(intentId, ((key, oldValue) -> oldValue > 0 ? --oldValue : null));
                
                if (fc != null) {
                    LOG.info("Listener Instructed to fail for {} {}:{}, remaining fail count is {}",
                        header.getIntentId(),
                        header.getCorrelationId(),
                        header.getCallRef(),
                        fc);
                    throw new IllegalStateException("Instructed to fail for intent: " + header.getIntentId());
                } else {
                    LOG.info("Successful handling of {} {}:{}", header.getIntentId(), header.getCorrelationId(), header.getCallRef());
                    synchronized (EVENT_LISTENER_SUCCESS_FUTURE) {
                        Optional.ofNullable(EVENT_LISTENER_SUCCESS_FUTURE.remove(intentId)).ifPresent(f -> {
                            LOG.info("Completing future for {} {}:{}", header.getIntentId(), header.getCorrelationId(), header.getCallRef());
                            ((CompletableFuture<E>) f).complete(event);
                        });
                    }
                    return Async.result(null);
                }
            }
            
        } finally {
            eventListenerSemaphore.release();
        }
    }
    
}
