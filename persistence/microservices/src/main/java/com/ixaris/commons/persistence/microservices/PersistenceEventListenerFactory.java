package com.ixaris.commons.persistence.microservices;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.microservices.lib.client.ServiceEventListener;

@FunctionalInterface
public interface PersistenceEventListenerFactory {
    
    <C extends MessageLite, E extends MessageLite> ServiceEventListener<C, E> create(ServiceEventListener<C, E> listener);
    
}
