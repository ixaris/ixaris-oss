package com.ixaris.commons.zeromq.microservices.example.service;

import static com.ixaris.commons.async.lib.Async.result;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.zeromq.microservices.example.Example.ExampleContext;
import com.ixaris.commons.zeromq.microservices.example.Example.ExampleRequest;
import com.ixaris.commons.zeromq.microservices.example.Example.ExampleResponse;
import com.ixaris.commons.zeromq.microservices.examplelistener2.resource.ExampleListener2Resource;
import com.ixaris.commons.zeromq.microservices.examplelistener2.service.ExampleListener2Skeleton;

public final class ExampleListener2ResourceImpl implements ExampleListener2Resource, ExampleListener2Skeleton {
    
    @Override
    public Async<ExampleResponse> op(final ServiceOperationHeader<ExampleContext> header, final ExampleRequest request) {
        return result(ExampleResponse.newBuilder().build());
    }
    
}
