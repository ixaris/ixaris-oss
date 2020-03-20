package com.ixaris.commons.microservices.web.dynamic;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.web.HttpRequest;

@FunctionalInterface
public interface OperationResolver<C extends MessageLite, R extends ResolvedOperation<C>> {
    
    Async<R> resolve(HttpRequest<String> request, DecodedUrl decodedUrl, MethodAndPayload methodAndPayload);
    
}
