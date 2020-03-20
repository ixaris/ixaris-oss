package com.ixaris.commons.microservices.lib.examplespi.client;

import static com.ixaris.commons.async.lib.Async.all;
import static com.ixaris.commons.async.lib.Async.awaitExceptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.microservices.lib.example.Example.ExampleContext;
import com.ixaris.commons.microservices.lib.example.Example.ExampleRequest;
import com.ixaris.commons.microservices.lib.example.Example.ExampleRequest.FailureType;
import com.ixaris.commons.microservices.lib.example.Example.ExampleResponse;
import com.ixaris.commons.microservices.lib.examplespi.resource.ExampleSpiResource.ExampleErrorException;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;

public final class ExampleSpiServiceClientImpl {
    
    private ExampleSpiStub exampleSpi;
    
    public ExampleSpiServiceClientImpl(final ExampleSpiStub exampleSpi) {
        this.exampleSpi = exampleSpi;
    }
    
    public Async<List<ExampleResponse>> doSomethingOnAll(final int id,
                                                         final int timeout,
                                                         final Integer serverSleepDuration,
                                                         final boolean serviceThrowException) {
        
        final ExampleRequest.Builder requestBuilder = ExampleRequest.newBuilder().setId(id);
        if (serverSleepDuration != null) {
            requestBuilder.setSleepDuration(serverSleepDuration);
        }
        if (serviceThrowException) {
            requestBuilder.setFailureType(FailureType.RUNTIME);
        }
        Set<String> keys = exampleSpi._keys();
        
        System.out.println("Available SPIs: " + keys);
        
        List<Async<ExampleResponse>> promises = new ArrayList<>(keys.size());
        for (final String key : keys) {
            System.out.println("Calling operation on SPI: " + key);
            final ServiceOperationHeader<ExampleContext> context = ServiceOperationHeader.newBuilder(0L,
                MultiTenancy.SYSTEM_TENANT,
                ExampleContext.newBuilder().build())
                .withTimeout(timeout)
                .withTargetServiceKey(key)
                .build();
            promises.add(doSomething(context, requestBuilder.build()));
        }
        return all(promises);
    }
    
    private Async<ExampleResponse> doSomething(final ServiceOperationHeader<ExampleContext> context, final ExampleRequest request) {
        try {
            return awaitExceptions(exampleSpi.exampleSpiOperation(context, request));
        } catch (final ExampleErrorException conflict) {
            throw new IllegalStateException(conflict);
        }
    }
    
}
