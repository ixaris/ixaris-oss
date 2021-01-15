package com.ixaris.commons.microservices.defaults.app.test.spring.spi.alt;

import org.springframework.stereotype.Service;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.defaults.app.example.Example.ExampleRequest;
import com.ixaris.commons.microservices.defaults.app.example.Example.ExampleResponse;
import com.ixaris.commons.microservices.defaults.app.examplespi.resource.ExampleSpiResource;
import com.ixaris.commons.microservices.defaults.app.examplespi.service.ExampleSpiSkeleton;
import com.ixaris.commons.microservices.defaults.context.CommonsMicroservicesDefaultsContext.Context;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;

@Service
public class AltExampleSpiResourceImpl implements ExampleSpiResource, ExampleSpiSkeleton {
    
    @Override
    public Async<ExampleResponse> op(final ServiceOperationHeader<Context> header, final ExampleRequest request) throws ExampleSpiErrorException {
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
