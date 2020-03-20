package com.ixaris.commons.microservices.lib.example2.client;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.Nil;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.microservices.lib.example.Example.ExampleContext;
import com.ixaris.commons.microservices.lib.example.Example.ExampleRequest;
import com.ixaris.commons.microservices.lib.example.Example.ExampleRequest.FailureType;
import com.ixaris.commons.microservices.lib.example.Example.ExampleResponse;
import com.ixaris.commons.microservices.lib.example.resource.ExampleResource.ExampleErrorException;
import com.ixaris.commons.multitenancy.test.TestTenants;

public final class Example2ServiceClientImpl {
    
    private final Example2Stub example2;
    
    public Example2ServiceClientImpl(final Example2Stub example2) {
        this.example2 = example2;
    }
    
    private static ExampleRequest getExampleRequest(final int id, final Integer serverSleepDuration, final boolean serviceThrowException) {
        final ExampleRequest.Builder requestBuilder = ExampleRequest.newBuilder().setId(id);
        if (serverSleepDuration != null) {
            requestBuilder.setSleepDuration(serverSleepDuration);
        }
        if (serviceThrowException) {
            requestBuilder.setFailureType(FailureType.RUNTIME);
        }
        return requestBuilder.build();
    }
    
    public Async<ExampleResponse> sendRequest(final int id, final int timeout, final String tenantId) throws ExampleErrorException {
        final ServiceOperationHeader<ExampleContext> context = ServiceOperationHeader.newBuilder(0L,
            TestTenants.DEFAULT,
            ExampleContext.newBuilder().build())
            .withTimeout(timeout)
            .build();
        return example2.exampleOperation2(context, ExampleRequest.newBuilder().setId(id).build());
    }
    
    public Async<ExampleResponse> sendRequestOnIdResource(final int id, final int timeout) throws ExampleErrorException {
        final ServiceOperationHeader<ExampleContext> context = ServiceOperationHeader.newBuilder(0L,
            TestTenants.DEFAULT,
            ExampleContext.newBuilder().build())
            .withTimeout(timeout)
            .build();
        return example2.id(id).exampleOperation(context);
    }
    
    public Async<Nil> sendChainedRequestsAndSimulateWork() throws ExampleErrorException {
        
        final ServiceOperationHeader<ExampleContext> context = ServiceOperationHeader.newBuilder(0L,
            TestTenants.DEFAULT,
            ExampleContext.newBuilder().build())
            .withTimeout(5000)
            .build();
        
        await(example2.exampleOperation2(context, ExampleRequest.newBuilder().build()));
        await(example2.exampleOperation2(context, ExampleRequest.newBuilder().build()));
        await(example2.exampleOperation2(context, ExampleRequest.newBuilder().build()));
        return result(Nil.getInstance());
    }
    
}
