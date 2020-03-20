package com.ixaris.commons.microservices.defaults.live.example;

import static com.ixaris.commons.async.lib.Async.await;

import javax.annotation.PreDestroy;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.defaults.context.CommonsMicroservicesDefaultsContext.Context;
import com.ixaris.commons.microservices.defaults.live.example.Example.ExampleEvent;
import com.ixaris.commons.microservices.defaults.live.example.Example.ExampleRequest;
import com.ixaris.commons.microservices.defaults.live.example.Example.ExampleResponse;
import com.ixaris.commons.microservices.defaults.live.example.client.ExampleStub;
import com.ixaris.commons.microservices.defaults.live.example2.resource.Example2Resource;
import com.ixaris.commons.microservices.defaults.live.example2.service.Example2Skeleton;
import com.ixaris.commons.microservices.lib.client.ServiceEventSubscription;
import com.ixaris.commons.microservices.lib.common.EventAck;
import com.ixaris.commons.microservices.lib.common.ServiceEventHeader;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;

/**
 * @author <a href="mailto:aldrin.seychell@ixaris.com">$user</a>
 */
@Service
public class Example2SkeletonImpl implements Example2Resource, Example2Skeleton {
    
    private final Example2Skeleton.Watch publisher;
    private final ExampleStub example;
    
    private final ServiceEventSubscription eventSubscription;
    
    @Autowired
    public Example2SkeletonImpl(final Example2Skeleton.Watch publisher, final ExampleStub example) {
        this.publisher = publisher;
        this.example = example;
        eventSubscription = example.watch((header, event) -> publish(header, ExampleEvent.newBuilder().setId(event.getId()).build()).map(ack -> null));
    }
    
    @PreDestroy
    public void unsubscribe() {
        eventSubscription.cancel();
    }
    
    @Override
    public Async<ExampleResponse> op2(final ServiceOperationHeader<Context> header, final ExampleRequest request) {
        // example.op(context,
        final ExampleResponse response = await(example.op(header, ExampleRequest.newBuilder().setId(request.getId()).setSleepDuration(50).build()));
        return Async.result(ExampleResponse.newBuilder().setId(response.getId()).build());
    }
    
    public Async<EventAck> publish(final ServiceEventHeader<Context> header, final ExampleEvent event) {
        return publisher.publish(header, event);
    }
    
}
