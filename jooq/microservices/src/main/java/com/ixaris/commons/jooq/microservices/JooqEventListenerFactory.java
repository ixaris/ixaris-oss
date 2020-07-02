package com.ixaris.commons.jooq.microservices;

import static com.ixaris.commons.async.lib.Async.result;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.jooq.persistence.JooqAsyncPersistenceProvider;
import com.ixaris.commons.microservices.lib.client.ServiceEventListener;
import com.ixaris.commons.persistence.microservices.PersistenceEventListenerFactory;

@Component
public final class JooqEventListenerFactory implements PersistenceEventListenerFactory {
    
    private final AtLeastOnceHandleEventType processor;
    
    private final Map<ServiceEventListener<?, ?>, ServiceEventListener<?, ?>> listeners = new HashMap<>();
    
    public JooqEventListenerFactory(final AtLeastOnceHandleEventType processor) {
        this.processor = processor;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public synchronized <C extends MessageLite, E extends MessageLite> ServiceEventListener<C, E> create(final ServiceEventListener<C, E> listener) {
        return (ServiceEventListener<C, E>) listeners.computeIfAbsent(listener, processor::register);
    }
    
}
