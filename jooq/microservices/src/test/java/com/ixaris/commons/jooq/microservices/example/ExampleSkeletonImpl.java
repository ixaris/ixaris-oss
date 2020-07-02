package com.ixaris.commons.jooq.microservices.example;

import com.ixaris.commons.jooq.microservices.example.resource.ExampleResource;
import com.ixaris.commons.jooq.microservices.example.service.ExampleSkeleton;

public final class ExampleSkeletonImpl implements ExampleResource, ExampleSkeleton {
    
    private final Watch publisher;
    
    public ExampleSkeletonImpl(final Watch publisher) {
        this.publisher = publisher;
    }
    
}
