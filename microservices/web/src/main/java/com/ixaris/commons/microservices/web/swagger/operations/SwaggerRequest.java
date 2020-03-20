package com.ixaris.commons.microservices.web.swagger.operations;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.microservices.lib.client.proxy.StubResourceMethodInfo;

/**
 * POJO to store details on the resolved request, parsed request and original request info for correlation/logging
 * purposes
 *
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public final class SwaggerRequest {
    
    private final ResolvedOperation<?> resolvedOperation;
    private final StubResourceMethodInfo<?, ?, ?> methodInfo;
    private final MessageLite request;
    
    public SwaggerRequest(final ResolvedOperation<?> resolvedOperation,
                          final StubResourceMethodInfo<?, ?, ?> methodInfo,
                          final MessageLite request) {
        this.resolvedOperation = resolvedOperation;
        this.methodInfo = methodInfo;
        this.request = request;
    }
    
    @SuppressWarnings("squid:S1452")
    public ResolvedOperation<?> getOperation() {
        return resolvedOperation;
    }
    
    @SuppressWarnings("squid:S1452")
    public StubResourceMethodInfo<?, ?, ?> getMethodInfo() {
        return methodInfo;
    }
    
    public MessageLite getRequest() {
        return request;
    }
    
}
