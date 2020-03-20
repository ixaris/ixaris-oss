package com.ixaris.commons.microservices.lib.local;

import static com.ixaris.commons.async.lib.Async.all;
import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;

import java.util.Collection;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableSet;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.collections.lib.GuavaCollections;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventAckEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;
import com.ixaris.commons.microservices.lib.service.support.ServiceEventDispatcher;

/**
 * Operation dispatcher that uses local(in-memory) message-queue to publish events.
 *
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
final class LocalServiceEventDispatcher implements ServiceEventDispatcher {
    
    private final String name;
    private final String key;
    
    private volatile ImmutableSet<LocalServiceEventHandler> handlers = ImmutableSet.of();
    
    LocalServiceEventDispatcher(final String name, final String key) {
        this.name = name;
        this.key = key;
    }
    
    @Override
    public Async<EventAckEnvelope> dispatch(final EventEnvelope eventEnvelope) {
        if (handlers.isEmpty()) {
            return result(EventAckEnvelope.newBuilder()
                .setCorrelationId(eventEnvelope.getCorrelationId())
                .setCallRef(eventEnvelope.getCallRef())
                .setStatusCode(ResponseStatusCode.OK)
                .build());
        } else {
            // attempt 2 times to handle messages
            final EventAckEnvelope ack = await(handle(eventEnvelope));
            if (ack.getStatusCode() == ResponseStatusCode.OK) {
                return result(ack);
            } else {
                return handle(eventEnvelope);
            }
        }
    }
    
    private Async<EventAckEnvelope> handle(EventEnvelope eventEnvelope) {
        return all(handlers.stream().map(h -> h.onEvent(eventEnvelope)).collect(
            Collectors.toList())).map(all -> {
                if (all.isEmpty()) {
                    throw new IllegalStateException("Expecting at least one ack envelope");
                }
                for (final EventAckEnvelope e : all) {
                    if (e.getStatusCode() != ResponseStatusCode.OK) {
                        return e;
                    }
                }
                return all.get(0);
            });
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + " for " + name + "-" + key;
    }
    
    synchronized void addHandlers(final Collection<LocalServiceEventHandler> toAdd) {
        handlers = GuavaCollections.copyOfSetAdding(handlers, toAdd);
    }
    
    synchronized void removeHandlers(final Collection<LocalServiceEventHandler> toRemove) {
        handlers = GuavaCollections.copyOfSetRemoving(handlers, toRemove);
    }
    
}
