package com.ixaris.commons.zeromq.microservices.example.service;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.zeromq.microservices.example.Example.ExampleContext;
import com.ixaris.commons.zeromq.microservices.example.Example.ExampleRequest;
import com.ixaris.commons.zeromq.microservices.example.Example.ExampleResponse;
import com.ixaris.commons.zeromq.microservices.example.resource.ExampleResource;
import com.ixaris.commons.zeromq.microservices.examplesupport.client.ExampleSupportStub;

public final class ExampleResourceImpl implements ExampleSkeleton, ExampleResource {
    
    private static final Logger LOG = LoggerFactory.getLogger(ExampleResourceImpl.class);
    public static boolean log = true;
    
    private final ExampleSkeleton.Watch events;
    private final ExampleSupportStub exampleSupportResource;
    
    public ExampleResourceImpl(final ExampleSkeleton.Watch events, final ExampleSupportStub exampleSupportResource) {
        this.events = events;
        this.exampleSupportResource = exampleSupportResource;
    }
    
    @Override
    public Async<ExampleResponse> op(final ServiceOperationHeader<ExampleContext> header, final ExampleRequest request) {
        if (log) {
            LOG.info("{} SERVICE: example", Thread.currentThread().getId());
        }
        
        if (request.getThrowException()) {
            throw new RuntimeException("Service-side exception thrown");
        }
        
        final ExampleResponse response = await(exampleSupportResource.op(header, request));
        if (log) {
            LOG.info("{} SERVICE: publishing exampleEvent", Thread.currentThread().getId());
        }
        
        return result(response);
    }
    
}
