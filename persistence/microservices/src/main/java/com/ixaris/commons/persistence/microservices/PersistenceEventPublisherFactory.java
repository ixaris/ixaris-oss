package com.ixaris.commons.persistence.microservices;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.microservices.lib.service.ServiceEventPublisher;

@FunctionalInterface
public interface PersistenceEventPublisherFactory {
    
    <C extends MessageLite, E extends MessageLite, R> AtLeastOnceServiceEventPublisher<C, E, R> create(ServiceEventPublisher<C, E, ?> publisher);
    
}
