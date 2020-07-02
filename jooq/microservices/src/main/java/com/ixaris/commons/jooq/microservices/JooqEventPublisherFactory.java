package com.ixaris.commons.jooq.microservices;

import java.util.HashMap;
import java.util.Map;

import org.springframework.stereotype.Component;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.microservices.lib.service.ServiceEventPublisher;
import com.ixaris.commons.persistence.microservices.AtLeastOnceServiceEventPublisher;
import com.ixaris.commons.persistence.microservices.PersistenceEventPublisherFactory;

@Component
public final class JooqEventPublisherFactory implements PersistenceEventPublisherFactory {
    
    private final AtLeastOncePublishEventType processor;
    
    private final Map<ServiceEventPublisher<?, ?, ?>, AtLeastOnceServiceEventPublisher<?, ?, ?>> publishers = new HashMap<>();
    
    public JooqEventPublisherFactory(final AtLeastOncePublishEventType processor) {
        this.processor = processor;
    }
    
    @SuppressWarnings("unchecked")
    @Override
    public synchronized <C extends MessageLite, E extends MessageLite, R> AtLeastOnceServiceEventPublisher<C, E, R> create(final ServiceEventPublisher<C, E, ?> publisher) {
        return (AtLeastOnceServiceEventPublisher<C, E, R>) publishers.computeIfAbsent(publisher, processor::register);
    }
    
}
