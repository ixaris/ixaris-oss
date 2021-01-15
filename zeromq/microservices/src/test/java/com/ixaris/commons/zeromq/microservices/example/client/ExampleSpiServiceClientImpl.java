package com.ixaris.commons.zeromq.microservices.example.client;

import static com.ixaris.commons.async.lib.Async.awaitExceptions;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.multitenancy.test.TestTenants;
import com.ixaris.commons.zeromq.microservices.example.Example.ExampleContext;
import com.ixaris.commons.zeromq.microservices.example.Example.ExampleRequest;
import com.ixaris.commons.zeromq.microservices.example.Example.ExampleResponse;
import com.ixaris.commons.zeromq.microservices.examplespi.client.ExampleSpiStub;
import com.ixaris.commons.zeromq.microservices.examplespi.resource.ExampleSpiResource.ExampleSpiErrorException;

public final class ExampleSpiServiceClientImpl {
    
    private ExampleSpiStub exampleSpi;
    
    public ExampleSpiServiceClientImpl(final ExampleSpiStub exampleSpi) {
        this.exampleSpi = exampleSpi;
    }
    
    public Async<List<ExampleResponse>> doSomething(final int id, final int timeout, final Integer serverSleepDuration, final boolean serviceThrowException) {
        
        final ExampleRequest.Builder requestBuilder = ExampleRequest.newBuilder().setId(id);
        if (serverSleepDuration != null) {
            requestBuilder.setSleepDuration(serverSleepDuration);
        }
        requestBuilder.setThrowException(serviceThrowException);
        
        Set<String> keys = exampleSpi._keys();
        
        System.out.println("Available SPIs: " + keys);
        
        List<Async<ExampleResponse>> promises = new ArrayList<>(keys.size());
        for (final String key : keys) {
            System.out.println("Calling operation on SPI: " + key);
            final ServiceOperationHeader<ExampleContext> header = TENANT.exec(TestTenants.DEFAULT, () -> ServiceOperationHeader.newBuilder(ExampleContext.newBuilder().build())
                .withTimeout(timeout)
                .withTargetServiceKey(key)
                .build());
            promises.add(doSomething(header, requestBuilder.build()));
        }
        return Async.all(promises);
    }
    
    private Async<ExampleResponse> doSomething(final ServiceOperationHeader<ExampleContext> context, final ExampleRequest request) {
        try {
            return awaitExceptions(exampleSpi.op(context, request));
        } catch (final ExampleSpiErrorException conflict) {
            throw new IllegalStateException(conflict);
        }
    }
    
}
