package com.ixaris.commons.microservices.defaults.live.example;

import static com.ixaris.commons.async.lib.Async.await;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.defaults.context.CommonsMicroservicesDefaultsContext.Context;
import com.ixaris.commons.microservices.defaults.live.example.Example.ExampleEvent;
import com.ixaris.commons.microservices.defaults.live.example.Example.ExampleRequest;
import com.ixaris.commons.microservices.defaults.live.example.Example.ExampleResponse;
import com.ixaris.commons.microservices.defaults.live.example.resource.ExampleResource;
import com.ixaris.commons.microservices.defaults.live.example.service.ExampleSkeleton;
import com.ixaris.commons.microservices.lib.common.ServiceEventHeader;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;

/**
 * @author <a href="mailto:aldrin.seychell@ixaris.com">$user</a>
 */
@Service
public class ExampleSkeletonImpl implements ExampleResource, ExampleSkeleton {
    
    private static final Logger LOG = LoggerFactory.getLogger(ExampleSkeletonImpl.class);
    
    private final ExampleComponent exampleComponent;
    private final ExampleSkeleton.Watch publisher;
    
    @Autowired
    public ExampleSkeletonImpl(final ExampleComponent exampleComponent, final ExampleSkeleton.Watch publisher) {
        this.exampleComponent = exampleComponent;
        this.publisher = publisher;
    }
    
    @Override
    public Async<ExampleResponse> op(final ServiceOperationHeader<Context> header, final ExampleRequest request) {
        LOG.info("Doing an example operation for id: " + request.getId());
        exampleComponent.exampleOperation(request.getSleepDuration());
        LOG.info("Operation for id: " + request.getId() + " ready.");
        
        final ExampleEvent event = ExampleEvent.newBuilder().setId(request.getId()).build();
        
        await(publisher.publish(ServiceEventHeader.newBuilder(header).build(), event));
        return Async.result(ExampleResponse.newBuilder().setId(request.getId()).build());
    }
    
}
