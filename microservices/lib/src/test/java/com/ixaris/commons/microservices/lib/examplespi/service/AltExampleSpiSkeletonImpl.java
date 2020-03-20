package com.ixaris.commons.microservices.lib.examplespi.service;

import static com.ixaris.commons.async.lib.Async.result;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.microservices.lib.example.Example.ExampleContext;
import com.ixaris.commons.microservices.lib.example.Example.ExampleRequest;
import com.ixaris.commons.microservices.lib.example.Example.ExampleRequest.FailureType;
import com.ixaris.commons.microservices.lib.example.Example.ExampleResponse;
import com.ixaris.commons.microservices.lib.examplespi.resource.ExampleSpiResource;
import com.ixaris.commons.microservices.lib.service.annotations.ServiceKey;

@ServiceKey("ALT")
public class AltExampleSpiSkeletonImpl implements ExampleSpiResource, ExampleSpiSkeleton {
    
    @Override
    public Async<ExampleResponse> exampleSpiOperation(final ServiceOperationHeader<ExampleContext> context, final ExampleRequest request) {
        System.out.println(Thread.currentThread().getId() + " SERVICE: exampleSpiOperation");
        
        final ExampleResponse.Builder response = ExampleResponse.newBuilder();
        response.setId(request.getId());
        
        try {
            Thread.sleep(request.getSleepDuration());
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        }
        
        if (FailureType.RUNTIME.equals(request.getFailureType())) {
            throw new RuntimeException("Service-side exception thrown");
        }
        
        return result(response.build());
    }
    
}
