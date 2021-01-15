package com.ixaris.commons.zeromq.microservices.example.service;

import static com.ixaris.commons.async.lib.Async.result;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.zeromq.microservices.example.Example.ExampleContext;
import com.ixaris.commons.zeromq.microservices.example.Example.ExampleRequest;
import com.ixaris.commons.zeromq.microservices.example.Example.ExampleResponse;
import com.ixaris.commons.zeromq.microservices.examplelistener.error.resource.ExampleListenerErrorResource;
import com.ixaris.commons.zeromq.microservices.examplelistener.error.service.ExampleListenerErrorSkeleton;

public final class ExampleListenerErrorResourceImpl implements ExampleListenerErrorResource, ExampleListenerErrorSkeleton {
    
    @Override
    public Async<ExampleResponse> op(final ServiceOperationHeader<ExampleContext> header, final ExampleRequest request) {
        return result(ExampleResponse.newBuilder().build());
    }
    
}
