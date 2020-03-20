package com.ixaris.commons.microservices.web.swagger;

import java.util.HashSet;
import java.util.Set;

import com.ixaris.commons.microservices.lib.client.support.ServiceClientSupport;
import com.ixaris.commons.microservices.lib.service.support.ServiceSupport;
import com.ixaris.commons.microservices.web.swagger.events.BasicEventsLoggingFilter;
import com.ixaris.commons.microservices.web.swagger.events.SwaggerEventFilter;
import com.ixaris.commons.microservices.web.swagger.operations.BasicOperationsLoggingFilter;
import com.ixaris.commons.microservices.web.swagger.operations.SwaggerOperationFilter;

public final class TestScslSwaggerRouter extends ScslSwaggerRouter {
    
    public TestScslSwaggerRouter(
                                 final TestOperationResolver operationResolver,
                                 final ServiceSupport serviceSupport,
                                 final ServiceClientSupport serviceClientSupport,
                                 final Set<? extends TestSwaggerOperationFilter> operationFilters,
                                 final Set<? extends TestSwaggerEventFilter> eventFilters) {
        super(
            operationResolver,
            serviceSupport,
            serviceClientSupport,
            addOperationLogging(operationFilters),
            addEventLogging(eventFilters),
            "test");
    }
    
    private static Set<? extends SwaggerOperationFilter> addOperationLogging(final Set<? extends TestSwaggerOperationFilter> operationFilters) {
        final Set<SwaggerOperationFilter> operationFiltersWithLogging = new HashSet<>(operationFilters);
        operationFiltersWithLogging.add(new BasicOperationsLoggingFilter());
        return operationFiltersWithLogging;
    }
    
    private static Set<? extends SwaggerEventFilter> addEventLogging(final Set<? extends TestSwaggerEventFilter> eventFilters) {
        final Set<SwaggerEventFilter> eventFiltersWithLogging = new HashSet<>(eventFilters);
        eventFiltersWithLogging.add(new BasicEventsLoggingFilter());
        return eventFiltersWithLogging;
    }
    
}
