package com.ixaris.commons.microservices.lib.example.client;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.Nil;
import com.ixaris.commons.microservices.lib.common.ServiceEventHeader;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.microservices.lib.example.Example.ExampleContext;
import com.ixaris.commons.microservices.lib.example.Example.ExampleEvent;
import com.ixaris.commons.microservices.lib.example.Example.ExampleRequest;
import com.ixaris.commons.microservices.lib.example.Example.ExampleRequest.FailureType;
import com.ixaris.commons.microservices.lib.example.Example.ExampleResponse;
import com.ixaris.commons.microservices.lib.example.resource.ExampleResource;
import com.ixaris.commons.microservices.lib.example.resource.ExampleResource.ExampleErrorException;
import com.ixaris.commons.multitenancy.test.TestTenants;

public final class ExampleServiceClientImpl {
    
    private final ExampleStub example;
    
    public ExampleServiceClientImpl(final ExampleStub example) {
        
        this.example = example;
        
        example.nested().watch(ExampleServiceClientImpl::onEvent);
    }
    
    public Async<ExampleResponse> sendRequest(final int id, final int timeout) throws ExampleErrorException {
        return sendRequest(id, timeout, TestTenants.DEFAULT);
    }
    
    public Async<ExampleResponse> sendRequest(final int id, final int timeout, final String tenantId) throws ExampleErrorException {
        final ServiceOperationHeader<ExampleContext> header =
            TENANT.exec(tenantId, () -> ServiceOperationHeader.newBuilder(ExampleContext.newBuilder().build()).withTimeout(timeout).build());
        
        return example.exampleOperation(header, ExampleRequest.newBuilder().setId(id).build());
    }
    
    public Async<ExampleResponse> sendRequestWithFailureType(final int id, final int timeout, final FailureType failureType) throws ExampleErrorException {
        return TENANT.exec(TestTenants.DEFAULT, () -> {
            final ServiceOperationHeader<ExampleContext> context = ServiceOperationHeader.newBuilder(
                ExampleContext.newBuilder().build())
                .withTimeout(timeout)
                .build();
            return example.exampleOperation(
                context, ExampleRequest.newBuilder().setId(id).setFailureType(failureType).build());
        });
    }
    
    public Async<ExampleResponse> sendRequestOnIdResource(final int id, final int timeout) {
        return TENANT.exec(TestTenants.DEFAULT, () -> {
            final ServiceOperationHeader<ExampleContext> context = ServiceOperationHeader.newBuilder(
                ExampleContext.newBuilder().build())
                .withTimeout(timeout)
                .build();
            final ExampleResource.IdResource idResource = example.id(id);
            return idResource.exampleOperation(context);
        });
    }
    
    public Async<ExampleResponse> sendSecuredRequest(final int id, final String security) throws ExampleErrorException {
        return TENANT.exec(TestTenants.DEFAULT, () -> {
            try {
                final ExampleContext.Builder builder = ExampleContext.newBuilder();
                if (security != null) {
                    builder.setSecurity(security);
                }
                final ServiceOperationHeader<ExampleContext> context = ServiceOperationHeader.newBuilder(builder
                    .build())
                    .withTimeout(1000)
                    .build();
                
                return example.exampleSecured(context, ExampleRequest.newBuilder().setId(id).build());
            } catch (final Throwable t) {
                throw t;
            }
        });
    }
    
    private static Async<Void> onEvent(final ServiceEventHeader<ExampleContext> context, final ExampleEvent event) {
        System.out.println(Thread.currentThread().getId() + " CLIENT: event success");
        return result();
    }
    
    public Async<Nil> sendChainedRequestsAndSimulateWork() throws ExampleErrorException {
        return TENANT.exec(TestTenants.DEFAULT, () -> {
            final ServiceOperationHeader<ExampleContext> context = ServiceOperationHeader.newBuilder(
                ExampleContext.newBuilder().build())
                .build();
            
            await(example.exampleOperation(context, ExampleRequest.newBuilder().build()));
            await(example.exampleOperation(context, ExampleRequest.newBuilder().build()));
            await(example.exampleOperation(context, ExampleRequest.newBuilder().build()));
            return Nil.getAsyncInstance();
        });
    }
    
}
