package com.ixaris.commons.microservices.lib.local;

import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.CompletableFutureUtil.complete;
import static com.ixaris.commons.microservices.lib.client.support.ServiceClientLoggingFilterFactory.EVENT_CHANNEL;

import java.util.concurrent.ScheduledExecutorService;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.AsyncQueue;
import com.ixaris.commons.async.lib.FutureAsync;
import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventAckEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;

/**
 * Event handler that uses local(in-memory) message-queue to receive messages
 *
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
class LocalServiceEventHandler {
    
    private final String name;
    private final ServicePathHolder path;
    private final ScheduledExecutorService executor;
    private final AsyncFilterNext<EventEnvelope, EventAckEnvelope> filterChain;
    private final AsyncQueue eventQueue = new AsyncQueue();
    
    LocalServiceEventHandler(final String name,
                             final ServicePathHolder path,
                             final ScheduledExecutorService executor,
                             final AsyncFilterNext<EventEnvelope, EventAckEnvelope> filterChain) {
        this.name = name;
        this.path = path;
        this.executor = executor;
        this.filterChain = filterChain;
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + " for " + name;
    }
    
    Async<EventAckEnvelope> onEvent(final EventEnvelope eventEnvelope) {
        // ignore events for which we do not have subscribers
        if (path.equals(ServicePathHolder.of(eventEnvelope.getPathList()))) {
            return eventQueue.exec(() -> {
                final FutureAsync<EventAckEnvelope> future = new FutureAsync<>();
                // intentionally do not propagate async local and trace
                executor.execute(() -> complete(
                    future,
                    () -> EVENT_CHANNEL.exec("LOCAL", () -> filterChain.next(eventEnvelope))));
                return future;
            });
        } else {
            return result(EventAckEnvelope.newBuilder()
                .setCorrelationId(eventEnvelope.getCorrelationId())
                .setCallRef(eventEnvelope.getCallRef())
                .setStatusCode(ResponseStatusCode.OK)
                .build());
        }
    }
    
}
