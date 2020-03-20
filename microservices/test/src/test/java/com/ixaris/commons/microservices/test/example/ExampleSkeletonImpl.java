package com.ixaris.commons.microservices.test.example;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.ServiceEventHeader;
import com.ixaris.commons.microservices.test.example.Example.ExampleContext;
import com.ixaris.commons.microservices.test.example.Example.ExampleEvent;
import com.ixaris.commons.microservices.test.example.resource.ExampleResource;
import com.ixaris.commons.microservices.test.example.service.ExampleSkeleton;

@Service
public final class ExampleSkeletonImpl implements ExampleResource, ExampleSkeleton {
    
    private final ExampleSkeleton.Watch events;
    
    @Autowired
    public ExampleSkeletonImpl(final ExampleSkeleton.Watch events) {
        this.events = events;
    }
    
    public Async<Void> publish(final ServiceEventHeader<ExampleContext> header) {
        return events.publish(header, ExampleEvent.newBuilder().build()).map(a -> null);
    }
    
}
