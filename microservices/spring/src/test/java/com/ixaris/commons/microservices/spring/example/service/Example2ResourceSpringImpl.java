package com.ixaris.commons.microservices.spring.example.service;

import static com.ixaris.commons.async.lib.Async.result;

import org.springframework.stereotype.Service;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.microservices.spring.example.Example.ExampleContext;
import com.ixaris.commons.microservices.spring.example.Example.ExampleRequest;
import com.ixaris.commons.microservices.spring.example.Example.ExampleResponse;
import com.ixaris.commons.microservices.spring.example2.resource.Example2Resource;
import com.ixaris.commons.microservices.spring.example2.service.Example2Skeleton;

@Service
public final class Example2ResourceSpringImpl implements Example2Resource, Example2Skeleton {
    
    @Override
    public Async<ExampleResponse> op(final ServiceOperationHeader<ExampleContext> header, final ExampleRequest request) {
        return result(ExampleResponse.newBuilder().build());
    }
    
}
