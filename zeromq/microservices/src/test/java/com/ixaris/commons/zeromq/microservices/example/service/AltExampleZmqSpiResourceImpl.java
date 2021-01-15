package com.ixaris.commons.zeromq.microservices.example.service;

import static com.ixaris.commons.async.lib.Async.result;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.microservices.lib.service.annotations.ServiceKey;
import com.ixaris.commons.zeromq.microservices.example.Example.ExampleContext;
import com.ixaris.commons.zeromq.microservices.example.Example.ExampleRequest;
import com.ixaris.commons.zeromq.microservices.example.Example.ExampleResponse;
import com.ixaris.commons.zeromq.microservices.examplespi.resource.ExampleSpiResource;
import com.ixaris.commons.zeromq.microservices.examplespi.service.ExampleSpiSkeleton;

@ServiceKey(AltExampleZmqSpiResourceImpl.KEY)
public class AltExampleZmqSpiResourceImpl implements ExampleSpiSkeleton, ExampleSpiResource {
    
    public static final String KEY = "ALT";
    
    @Override
    public Async<ExampleResponse> op(final ServiceOperationHeader<ExampleContext> header, final ExampleRequest request) {
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
        
        return result(response.build());
    }
}
