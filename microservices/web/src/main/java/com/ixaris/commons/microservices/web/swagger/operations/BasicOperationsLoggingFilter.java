package com.ixaris.commons.microservices.web.swagger.operations;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.logging.AsyncLogging.ASYNC_MDC;
import static com.ixaris.commons.microservices.lib.common.ServiceLoggingHelper.KEY_PATH;
import static com.ixaris.commons.microservices.lib.common.ServiceLoggingHelper.KEY_PATH_PARAMS;
import static com.ixaris.commons.microservices.lib.common.ServiceLoggingHelper.KEY_SERVICE_NAME;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.microservices.lib.common.ServiceConstants;
import com.ixaris.commons.microservices.lib.common.exception.ServiceException;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;
import com.ixaris.commons.microservices.web.logging.SanitisedLogFactory;

/**
 * Basic Implementation of API Logger for Swagger requests. This uses SLF4J to log request/responses and adds MDC fields
 * before logging.
 *
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public class BasicOperationsLoggingFilter implements SwaggerOperationFilter {
    
    private static final Logger LOG = LoggerFactory.getLogger(BasicOperationsLoggingFilter.class);
    private static final String KEY_CALL_REF = "CALL_REF";
    
    private final SanitisedLogFactory logFactory;
    
    public BasicOperationsLoggingFilter() {
        this.logFactory = new SanitisedLogFactory();
    }
    
    @Override
    public Async<SwaggerResponse> doFilter(final SwaggerRequest in, final AsyncFilterNext<SwaggerRequest, SwaggerResponse> next) {
        final ImmutableMap.Builder<String, String> mdc = new ImmutableMap.Builder<String, String>()
            .put(KEY_SERVICE_NAME, in.getOperation().serviceName)
            .put(KEY_PATH, in.getOperation().path + "/" + in.getMethodInfo().getName());
        if (!in.getOperation().params.isEmpty()) {
            mdc.put(KEY_PATH_PARAMS, in.getOperation().params.toString());
        }
        if (in.getOperation().callRef != null) {
            mdc.put(KEY_CALL_REF, in.getOperation().callRef);
        }
        
        return ASYNC_MDC.exec(mdc.build(), () -> {
            final String sanitisedRequest = logFactory.getSanitisedMessage(in.getRequest());
            LOG.info("API Request: [{}]", sanitisedRequest);
            
            final SwaggerResponse out = await(next.next(in));
            
            final ResponseStatusCode statusCode;
            final String sanitisedResponse;
            if (out.getThrowable() == null) {
                if (out.getConflictException() == null) {
                    statusCode = ResponseStatusCode.OK;
                    if (out.getResponse() != null) {
                        sanitisedResponse = logFactory.getSanitisedMessage(out.getResponse());
                    } else {
                        sanitisedResponse = null;
                    }
                } else {
                    statusCode = ResponseStatusCode.CLIENT_CONFLICT;
                    sanitisedResponse = logFactory.getSanitisedMessage(out.getConflictException().getConflict());
                }
            } else {
                statusCode = (out.getThrowable() instanceof ServiceException)
                    ? ((ServiceException) out.getThrowable()).getStatusCode() : ResponseStatusCode.SERVER_ERROR;
                sanitisedResponse = null;
            }
            
            switch (ServiceConstants.resolveStatusClass(statusCode)) {
                case OK:
                    LOG.info("API Response: [{}, {}]", statusCode, sanitisedResponse);
                    break;
                case CLIENT_ERROR:
                    LOG.warn("API Response: [{}, {}]", statusCode, sanitisedResponse);
                    break;
                case SERVER_ERROR:
                default:
                    LOG.error("API Response: [{}, {}]", statusCode, sanitisedResponse);
                    break;
            }
            
            return result(out);
        });
    }
    
    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }
    
}
