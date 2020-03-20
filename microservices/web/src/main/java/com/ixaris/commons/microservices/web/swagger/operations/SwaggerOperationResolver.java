package com.ixaris.commons.microservices.web.swagger.operations;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.web.HttpRequest;

public interface SwaggerOperationResolver<C extends MessageLite, R extends ResolvedOperation<C>> {
    
    Async<R> resolve(HttpRequest<?> httpRequest,
                     String serviceName,
                     String serviceKey,
                     ServicePathHolder path,
                     ServicePathHolder params,
                     String method,
                     boolean create,
                     MessageLite request);
    
}
