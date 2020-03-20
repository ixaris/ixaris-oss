package com.ixaris.commons.microservices.lib.local;

import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.AsyncExecutor.relay;
import static com.ixaris.commons.async.lib.CompletableFutureUtil.complete;
import static com.ixaris.commons.microservices.lib.client.support.ServiceClientSupport.extractTimeout;

import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.AsyncExecutor;
import com.ixaris.commons.async.lib.FutureAsyncWithTimeout;
import com.ixaris.commons.collections.lib.GuavaCollections;
import com.ixaris.commons.microservices.lib.client.support.ServiceOperationDispatcher;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;

/**
 * Operation dispatcher that uses local(in-memory) message-queue to send messages.
 *
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public final class LocalServiceOperationDispatcher implements ServiceOperationDispatcher {
    
    private static final Logger LOG = LoggerFactory.getLogger(LocalServiceOperationDispatcher.class);
    
    private final String name;
    private final ScheduledExecutorService executor;
    private final LocalOperations localOperations;
    
    private volatile ImmutableMap<String, LocalServiceOperationHandler> handlers = ImmutableMap.of();
    
    LocalServiceOperationDispatcher(final String name,
                                    final ScheduledExecutorService executor,
                                    final LocalOperations localOperations) {
        this.name = name;
        this.executor = executor;
        this.localOperations = localOperations;
    }
    
    @Override
    public Async<ResponseEnvelope> dispatch(final RequestEnvelope requestEnvelope) {
        final LocalServiceOperationHandler handler = handlers.get(requestEnvelope.getServiceKey());
        if (handler == null) {
            return result(ResponseEnvelope.newBuilder()
                .setCorrelationId(requestEnvelope.getCorrelationId())
                .setCallRef(requestEnvelope.getCallRef())
                .setStatusCode(ResponseStatusCode.SERVER_UNAVAILABLE)
                .build());
        } else {
            final FutureAsyncWithTimeout<ResponseEnvelope> future = AsyncExecutor.schedule(
                extractTimeout(requestEnvelope),
                TimeUnit.MILLISECONDS,
                () -> {
                    // It is possible that the future was completed while servicing a response, in which case, ignore
                    LOG.debug("Publishing timeout response [{}:{}] for [{}]",
                        requestEnvelope.getCorrelationId(),
                        requestEnvelope.getCallRef(),
                        name);
                    return ResponseEnvelope.newBuilder()
                        .setCorrelationId(requestEnvelope.getCorrelationId())
                        .setCallRef(requestEnvelope.getCallRef())
                        .setStatusCode(ResponseStatusCode.SERVER_TIMEOUT)
                        .setStatusMessage(String.format("Timed out calling [%s:%s]",
                            name,
                            requestEnvelope.getServiceKey()))
                        .build();
                });
            
            // It is possible that the future is completed by the schedule
            // intentionally do not propagate async local and trace
            executor.execute(() -> complete(future, () -> handler.handle(requestEnvelope)));
            
            return relay(future);
        }
    }
    
    @Override
    public boolean isKeyAvailable(final String key) {
        return localOperations.isHandlerRegistered(name, key);
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + " for " + name;
    }
    
    synchronized void setHandler(final String key, final LocalServiceOperationHandler handler) {
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }
        if (handler == null) {
            throw new IllegalArgumentException("handler is null");
        }
        if (handlers.containsKey(key)) {
            throw new IllegalStateException("handler already set");
        }
        handlers = GuavaCollections.copyOfMapAdding(handlers, key, handler);
    }
    
    synchronized void unsetHandler(final String key) {
        if (key == null) {
            throw new IllegalArgumentException("key is null");
        }
        if (!handlers.containsKey(key)) {
            throw new IllegalStateException("handler not set");
        }
        handlers = GuavaCollections.copyOfMapRemoving(handlers, key);
    }
    
}
