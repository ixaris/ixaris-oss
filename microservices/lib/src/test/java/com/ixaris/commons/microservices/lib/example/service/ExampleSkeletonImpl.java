package com.ixaris.commons.microservices.lib.example.service;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.ServiceEventHeader;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.microservices.lib.common.exception.ServerNotImplementedException;
import com.ixaris.commons.microservices.lib.common.exception.ServerTimeoutException;
import com.ixaris.commons.microservices.lib.example.Example.ExampleContext;
import com.ixaris.commons.microservices.lib.example.Example.ExampleError;
import com.ixaris.commons.microservices.lib.example.Example.ExampleError.ExampleErrorCode;
import com.ixaris.commons.microservices.lib.example.Example.ExampleEvent;
import com.ixaris.commons.microservices.lib.example.Example.ExampleRequest;
import com.ixaris.commons.microservices.lib.example.Example.ExampleResponse;
import com.ixaris.commons.microservices.lib.example.resource.ExampleResource;

public final class ExampleSkeletonImpl implements ExampleResource, ExampleSkeleton {
    
    private final NestedSkeleton.Watch publisher;
    
    public ExampleSkeletonImpl(final NestedSkeleton.Watch publisher) {
        this.publisher = publisher;
    }
    
    @Override
    public Async<ExampleResponse> exampleOperation(final ServiceOperationHeader<ExampleContext> header, final ExampleRequest request) throws ExampleErrorException {
        System.out.println(Thread.currentThread().getId() + " SERVICE: exampleOperation");
        
        final ExampleResponse.Builder response = ExampleResponse.newBuilder();
        response.setId(request.getId());
        
        if (request.getSleepDuration() > 0) {
            try {
                Thread.sleep(request.getSleepDuration());
            } catch (InterruptedException e) {
                throw new RuntimeException(e);
            }
        }
        
        switch (request.getFailureType()) {
            case RUNTIME:
                throw new RuntimeException("Service-side exception thrown");
            case CONFLICT:
                throw new ExampleErrorException(ExampleError.newBuilder().setErrorCode(ExampleErrorCode.EXAMPLE_ERROR).build());
            case NOT_IMPLEMENTED:
                throw new ServerNotImplementedException();
            case UNSUPPORTED_OP:
                throw new UnsupportedOperationException();
            case TIMEOUT:
                throw new ServerTimeoutException();
        }
        
        System.out.println(Thread.currentThread().getId() + " SERVICE: publishing exampleEvent");
        
        try {
            await(publisher.publish(ServiceEventHeader.newBuilder(header).build(), ExampleEvent.newBuilder().build()));
            System.out.println(Thread.currentThread().getId() + " SERVICE: publish exampleEvent ack");
            return result(response.build());
        } catch (final RuntimeException e) {
            System.out.println(Thread.currentThread().getId() + " SERVICE: publish exampleEvent fail");
            e.printStackTrace();
            throw e;
        }
    }
    
    @Override
    public Async<ExampleResponse> exampleOperationNoLogs(final ServiceOperationHeader<ExampleContext> header, final ExampleRequest request) {
        return result(ExampleResponse.newBuilder().setId(request.getId()).build());
    }
    
    @Override
    public Async<ExampleResponse> exampleSecured(final ServiceOperationHeader<ExampleContext> header, final ExampleRequest request) {
        return result(ExampleResponse.newBuilder().setId(request.getId()).build());
    }
    
    private static class IdResourceImpl implements IdResource {
        
        private final int id;
        
        private IdResourceImpl(final int id) {
            this.id = id;
        }
        
        @Override
        public Async<ExampleResponse> exampleOperation(final ServiceOperationHeader<ExampleContext> header) {
            return result(ExampleResponse.newBuilder().setId(id).build());
        }
        
    }
    
    @Override
    public IdResource id(final int id) {
        return new IdResourceImpl(id);
    }
    
    private NestedResource nestedResource = new NestedResource() {};
    
    @Override
    public NestedResource nested() {
        return nestedResource;
    }
    
}
