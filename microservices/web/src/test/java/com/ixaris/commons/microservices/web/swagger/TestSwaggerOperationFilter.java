package com.ixaris.commons.microservices.web.swagger;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.microservices.web.swagger.operations.SwaggerOperationFilter;
import com.ixaris.commons.microservices.web.swagger.operations.SwaggerRequest;
import com.ixaris.commons.microservices.web.swagger.operations.SwaggerResponse;

public abstract class TestSwaggerOperationFilter implements SwaggerOperationFilter {
    
    public Async<SwaggerRequest> onRequest(final SwaggerRequest request) {
        return result(request);
    }
    
    public Async<SwaggerResponse> onResponse(final SwaggerResponse response) {
        return result(response);
    }
    
    @Override
    public final Async<SwaggerResponse> doFilter(final SwaggerRequest origRequest, final AsyncFilterNext<SwaggerRequest, SwaggerResponse> next) {
        final SwaggerRequest request = await(onRequest(origRequest));
        final SwaggerResponse origResponse = await(next.next(request));
        return onResponse(origResponse);
    }
    
}
