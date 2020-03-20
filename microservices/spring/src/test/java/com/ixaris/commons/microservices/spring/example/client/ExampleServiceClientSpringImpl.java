package com.ixaris.commons.microservices.spring.example.client;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.Nil;
import com.ixaris.commons.microservices.lib.common.ServiceEventHeader;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.microservices.lib.common.exception.ServiceException;
import com.ixaris.commons.microservices.spring.example.Example.ExampleContext;
import com.ixaris.commons.microservices.spring.example.Example.ExampleEvent;
import com.ixaris.commons.microservices.spring.example.Example.ExampleRequest;
import com.ixaris.commons.microservices.spring.example.Example.ExampleResponse;
import com.ixaris.commons.multitenancy.test.TestTenants;

@Component
public final class ExampleServiceClientSpringImpl {
    
    private final ExampleStub example;
    
    @Autowired
    private ExampleServiceClientSpringImpl(final ExampleStub example) {
        
        this.example = example;
        
        example.watch(ExampleServiceClientSpringImpl::onEvent);
    }
    
    public Async<Nil> doSomething() {
        
        final ServiceOperationHeader<ExampleContext> context = ServiceOperationHeader.newBuilder(0L,
            TestTenants.DEFAULT,
            ExampleContext.newBuilder().build())
            .withTimeout(5000)
            .build();
        
        System.out.println(Thread.currentThread().getId() + " CLIENT: calling 1");
        
        try {
            final ExampleResponse r = await(example.op(context, ExampleRequest.newBuilder().build()));
            System.out.println(Thread.currentThread().getId() + " CLIENT: success 1" + r);
        } catch (final ServiceException e) {
            System.out.println(Thread.currentThread().getId() + " CLIENT: fail 1");
        }
        
        System.out.println(Thread.currentThread().getId() + " CLIENT: calling 2");
        final ExampleResponse r = await(example.op(context, ExampleRequest.newBuilder().build()));
        System.out.println(Thread.currentThread().getId() + " CLIENT: success 2 " + r);
        System.out.println(Thread.currentThread().getId() + " CLIENT: calling 3");
        
        final ExampleResponse rr = await(example.op(context, ExampleRequest.newBuilder().build()));
        System.out.println(Thread.currentThread().getId() + " CLIENT: success 3 " + rr);
        return result(Nil.getInstance());
    }
    
    private static Async<Void> onEvent(final ServiceEventHeader<ExampleContext> context, final ExampleEvent event) {
        System.out.println(Thread.currentThread().getId() + " CLIENT: event success");
        return result();
    }
    
}
