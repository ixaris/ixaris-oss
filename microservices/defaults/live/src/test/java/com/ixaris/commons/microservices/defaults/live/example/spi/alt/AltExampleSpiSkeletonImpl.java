package com.ixaris.commons.microservices.defaults.live.example.spi.alt;

import org.springframework.stereotype.Service;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.defaults.context.CommonsMicroservicesDefaultsContext.Context;
import com.ixaris.commons.microservices.defaults.live.example.Example.ExampleRequest;
import com.ixaris.commons.microservices.defaults.live.example.Example.ExampleResponse;
import com.ixaris.commons.microservices.defaults.live.examplespi.resource.ExampleSpiResource;
import com.ixaris.commons.microservices.defaults.live.examplespi.service.ExampleSpiSkeleton;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;

@Service
public class AltExampleSpiSkeletonImpl implements ExampleSpiResource, ExampleSpiSkeleton {
    
    @Override
    public Async<ExampleResponse> op(final ServiceOperationHeader<Context> header, final ExampleRequest request) {
        System.out.println(Thread.currentThread().getId() + " SERVICE " + getClass().getSimpleName() + ": exampleSpiOperation");
        
        final ExampleResponse.Builder response = ExampleResponse.newBuilder();
        if (request.getId() != 0) {
            response.setId(request.getId());
        }
        
        if (request.getSleepDuration() > 0) {
            try {
                Thread.sleep(request.getSleepDuration());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        
        if (request.getThrowException()) {
            throw new RuntimeException("Service-side exception thrown");
        }
        
        return Async.result(response.build());
    }
    
}
