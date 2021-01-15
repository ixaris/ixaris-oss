package com.ixaris.commons.kafka.microservices.client;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.ScheduledExecutorService;

import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.kafka.multitenancy.KafkaConnectionHandler;
import com.ixaris.commons.microservices.lib.client.proxy.ListenerKey;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventAckEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventEnvelope;

public class KafkaServiceClientSupport {
    
    private final ScheduledExecutorService executor;
    private final KafkaConnectionHandler kafkaConnectionHandler;
    
    private final Map<String, Map<ListenerKey, KafkaServiceEventHandler>> eventHandlers = new HashMap<>();
    
    public KafkaServiceClientSupport(final ScheduledExecutorService executor, final KafkaConnectionHandler kafkaConnectionHandler) {
        this.executor = executor;
        this.kafkaConnectionHandler = kafkaConnectionHandler;
    }
    
    public synchronized void createEventHandler(final String name,
                                                final String subscriberName,
                                                final ServicePathHolder path,
                                                final AsyncFilterNext<EventEnvelope, EventAckEnvelope> filterChain) {
        eventHandlers
            .computeIfAbsent(name, n -> new HashMap<>())
            .compute(new ListenerKey(subscriberName, path), (k, v) -> {
                if (v != null) {
                    throw new IllegalStateException("Event handler for [" + name + "/" + subscriberName + "] already registered");
                }
                return new KafkaServiceEventHandler(name,
                    subscriberName,
                    path,
                    filterChain,
                    kafkaConnectionHandler);
            })
            .subscribe();
    }
    
    public synchronized void destroyEventHandler(final String name, final String subscriberName, final ServicePathHolder path) {
        final Map<ListenerKey, KafkaServiceEventHandler> handlers = eventHandlers.get(name);
        final KafkaServiceEventHandler handler = handlers == null ? null : handlers.remove(new ListenerKey(subscriberName, path));
        if (handler == null) {
            throw new IllegalStateException("Event handler for [" + name + "/" + subscriberName + "/" + path + "] not registered");
        }
        if (handlers.isEmpty()) {
            eventHandlers.remove(name);
        }
        handler.unsubscribe();
    }
    
}
