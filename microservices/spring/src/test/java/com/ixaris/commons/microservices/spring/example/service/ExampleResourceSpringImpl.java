package com.ixaris.commons.microservices.spring.example.service;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.ServiceEventHeader;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.microservices.spring.example.Example.ExampleContext;
import com.ixaris.commons.microservices.spring.example.Example.ExampleEvent;
import com.ixaris.commons.microservices.spring.example.Example.ExampleRequest;
import com.ixaris.commons.microservices.spring.example.Example.ExampleResponse;
import com.ixaris.commons.microservices.spring.example.resource.ExampleResource;

@Service
public final class ExampleResourceSpringImpl implements ExampleResource, ExampleSkeleton {
    
    private final ExampleSkeleton.Watch events;
    
    @Autowired
    public ExampleResourceSpringImpl(final ExampleSkeleton.Watch events) {
        this.events = events;
    }
    
    @Override
    public Async<ExampleResponse> op(final ServiceOperationHeader<ExampleContext> header, final ExampleRequest request) {
        
        System.out.println(Thread.currentThread().getId() + " SERVICE: exampleOperation");
        
        System.out.println(Thread.currentThread().getId() + " SERVICE: publishing exampleEvent");
        
        try {
            final ServiceEventHeader<ExampleContext> eventHeader = ServiceEventHeader.from(header);
            await(events.publish(eventHeader, ExampleEvent.newBuilder().build()));
            System.out.println(Thread.currentThread().getId() + " SERVICE: publish exampleEvent ack");
        } catch (final Throwable t) {
            System.out.println(Thread.currentThread().getId() + " SERVICE: publish exampleEvent fail");
        }
        
        return result(ExampleResponse.newBuilder().build());
    }
    
    @Override
    public Async<ExampleResponse> opNoLogs(final ServiceOperationHeader<ExampleContext> header, final ExampleRequest request) {
        throw new UnsupportedOperationException();
    }
    
}
