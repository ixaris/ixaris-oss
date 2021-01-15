package com.ixaris.commons.zeromq.microservices.example.service;

import static com.ixaris.commons.async.lib.Async.result;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.zeromq.microservices.example.Example.ExampleContext;
import com.ixaris.commons.zeromq.microservices.example.Example.ExampleRequest;
import com.ixaris.commons.zeromq.microservices.example.Example.ExampleResponse;
import com.ixaris.commons.zeromq.microservices.examplesupport.resource.ExampleSupportResource;
import com.ixaris.commons.zeromq.microservices.examplesupport.service.ExampleSupportSkeleton;

public final class ExampleSupportSkeletonImpl implements ExampleSupportSkeleton, ExampleSupportResource {
    
    @Override
    public Async<ExampleResponse> op(final ServiceOperationHeader<ExampleContext> header, final ExampleRequest request) {
        final ExampleResponse.Builder builder = ExampleResponse.newBuilder();
        if (request.getId() != 0) {
            builder.setId(request.getId());
        }
        final ExampleResponse response = builder.build();
        
        if (request.getSleepDuration() != 0) {
            try {
                Thread.sleep(request.getSleepDuration());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        
        if (request.getThrowException()) {
            throw new RuntimeException("Service-side exception thrown");
        }
        
        return result(response);
    }
    
}
