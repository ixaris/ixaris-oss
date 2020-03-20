package com.ixaris.commons.microservices.web.swagger.events;

import com.ixaris.commons.async.lib.Async;

@FunctionalInterface
public interface SwaggerEventPublisherParamResolver<T> {
    
    Async<T> resolve(SwaggerEvent event);
    
}
