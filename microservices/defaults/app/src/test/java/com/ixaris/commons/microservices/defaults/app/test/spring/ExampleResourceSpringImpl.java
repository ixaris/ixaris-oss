package com.ixaris.commons.microservices.defaults.app.test.spring;

import static com.ixaris.commons.async.lib.Async.await;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.defaults.app.example.Example.ExampleEvent;
import com.ixaris.commons.microservices.defaults.app.example.Example.ExampleRequest;
import com.ixaris.commons.microservices.defaults.app.example.Example.ExampleResponse;
import com.ixaris.commons.microservices.defaults.app.example.resource.ExampleResource;
import com.ixaris.commons.microservices.defaults.app.example.service.ExampleSkeleton;
import com.ixaris.commons.microservices.defaults.context.CommonsMicroservicesDefaultsContext.Context;
import com.ixaris.commons.microservices.lib.common.ServiceEventHeader;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;

/**
 * @author <a href="mailto:aldrin.seychell@ixaris.com">$user</a>
 */
@Service
public class ExampleResourceSpringImpl implements ExampleResource, ExampleSkeleton {
    
    private static final Logger LOG = LoggerFactory.getLogger(ExampleResourceSpringImpl.class);
    
    private final ExampleSpringComponent exampleSpringComponent;
    private final ExampleSkeleton.Watch publisher;
    
    @Autowired
    public ExampleResourceSpringImpl(final ExampleSpringComponent exampleSpringComponent, final ExampleSkeleton.Watch publisher) {
        this.exampleSpringComponent = exampleSpringComponent;
        this.publisher = publisher;
    }
    
    @Override
    public Async<ExampleResponse> op(final ServiceOperationHeader<Context> header, final ExampleRequest request) {
        LOG.info("Doing an example operation for id: " + request.getId());
        exampleSpringComponent.exampleOperation(request.getSleepDuration());
        LOG.info("Operation for id: " + request.getId() + " ready.");
        
        final ExampleEvent event = ExampleEvent.newBuilder().setId(request.getId()).build();
        
        await(publisher.publish(ServiceEventHeader.newBuilder(header).build(), event));
        return Async.result(ExampleResponse.newBuilder().setId(request.getId()).build());
    }
    
}
