package com.ixaris.commons.zeromq.microservices.example.client;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.ServiceEventHeader;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.multitenancy.test.TestTenants;
import com.ixaris.commons.zeromq.microservices.example.Example.ExampleContext;
import com.ixaris.commons.zeromq.microservices.example.Example.ExampleEvent;
import com.ixaris.commons.zeromq.microservices.example.Example.ExampleRequest;
import com.ixaris.commons.zeromq.microservices.example.Example.ExampleResponse;

public final class ExampleServiceClientImpl {
    
    public static boolean log = true;
    
    private final ExampleStub example;
    
    public ExampleServiceClientImpl(final ExampleStub example) {
        this.example = example;
        example.watch(this::onEvent);
    }
    
    public Async<ExampleResponse> doSomethingWithPromise(final int id, final Integer serverSleepDuration, final boolean serviceThrowException) {
        
        final ServiceOperationHeader<ExampleContext> header = TENANT.exec(TestTenants.DEFAULT, () -> ServiceOperationHeader.newBuilder(ExampleContext.newBuilder().build()).build());
        
        final ExampleRequest.Builder requestBuilder = ExampleRequest.newBuilder().setId(id);
        if (serverSleepDuration != null) {
            requestBuilder.setSleepDuration(serverSleepDuration);
        }
        requestBuilder.setThrowException(serviceThrowException);
        
        return example.op(header, requestBuilder.build());
    }
    
    public Async<Void> doSomething() {
        
        final ServiceOperationHeader<ExampleContext> header = TENANT.exec(TestTenants.DEFAULT, () -> ServiceOperationHeader.newBuilder(ExampleContext.newBuilder().build()).build());
        
        System.out.println(Thread.currentThread().getId() + " CLIENT: calling 1");
        Async<ExampleResponse> p = example.op(header, ExampleRequest.newBuilder().build());
        System.out.println(Thread.currentThread().getId() + " CLIENT: called 1");
        
        // simulate work
        try {
            Thread.sleep(1000L);
        } catch (InterruptedException e) {}
        
        try {
            final ExampleResponse r = await(p);
            System.out.println(Thread.currentThread().getId() + " CLIENT: success 1" + r);
        } catch (Throwable e) {
            System.out.println(Thread.currentThread().getId() + " CLIENT: fail 1");
            e.printStackTrace();
            throw e;
        }
        
        try {
            System.out.println(Thread.currentThread().getId() + " CLIENT: calling 2");
            final ExampleResponse r2 = await(example.op(header, ExampleRequest.newBuilder().build()));
            System.out.println(Thread.currentThread().getId() + " CLIENT: success 2 " + r2);
            System.out.println(Thread.currentThread().getId() + " CLIENT: calling 3");
            final ExampleResponse r3 = await(example.op(header, ExampleRequest.newBuilder().build()));
            System.out.println(Thread.currentThread().getId() + " CLIENT: success 3 " + r3);
        } catch (final RuntimeException t) {
            System.out.println(Thread.currentThread().getId() + " CLIENT: fail 3");
            t.printStackTrace();
            throw t;
        }
        return result();
    }
    
    public ExampleRequest buildExampleRequest(
                                              final int id, final Integer serverSleepDuration, final boolean serviceThrowException) {
        
        final ExampleRequest.Builder requestBuilder = ExampleRequest.newBuilder().setId(id);
        if (serverSleepDuration != null) {
            requestBuilder.setSleepDuration(serverSleepDuration);
        }
        requestBuilder.setThrowException(serviceThrowException);
        
        return requestBuilder.build();
    }
    
    private Async<Void> onEvent(final ServiceEventHeader<ExampleContext> context, final ExampleEvent event) {
        if (log) {
            System.out.println(Thread.currentThread().getId() + " CLIENT: event");
        }
        if (log) {
            System.out.println(Thread.currentThread().getId() + " CLIENT: event success");
        }
        return result();
    }
    
}
