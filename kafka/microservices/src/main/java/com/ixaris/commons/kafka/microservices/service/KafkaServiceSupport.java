package com.ixaris.commons.kafka.microservices.service;

import java.util.Set;

import com.ixaris.commons.kafka.multitenancy.KafkaConnectionHandler;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.service.support.ServiceEventDispatcher;

public class KafkaServiceSupport {
    
    private final KafkaConnectionHandler connection;
    
    public KafkaServiceSupport(final KafkaConnectionHandler connection) {
        this.connection = connection;
    }
    
    public ServiceEventDispatcher createEventsDispatcher(final String name, final Set<ServicePathHolder> paths) {
        return new KafkaServiceEventDispatcher(name, paths, connection);
    }
    
}
