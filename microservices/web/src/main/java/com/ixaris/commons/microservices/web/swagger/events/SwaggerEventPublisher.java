package com.ixaris.commons.microservices.web.swagger.events;

import com.ixaris.commons.async.lib.Async;

public interface SwaggerEventPublisher<T> {
    
    String getName();
    
    Async<SwaggerEventAck.Status> publishEvent(SwaggerEvent swaggerEvent, SwaggerEventPublisherParamResolver<T> paramResolver);
    
}
