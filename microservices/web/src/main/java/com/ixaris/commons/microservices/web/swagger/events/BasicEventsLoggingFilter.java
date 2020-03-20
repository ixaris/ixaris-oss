package com.ixaris.commons.microservices.web.swagger.events;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

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
public class BasicEventsLoggingFilter implements SwaggerEventFilter {
    
    private static final Logger LOG = LoggerFactory.getLogger(BasicEventsLoggingFilter.class);
    
    private final SanitisedLogFactory logFactory;
    
    public BasicEventsLoggingFilter() {
        this.logFactory = new SanitisedLogFactory();
    }
    
    @Override
    public Async<SwaggerEventAck> doFilter(final SwaggerEvent in, final AsyncFilterNext<SwaggerEvent, SwaggerEventAck> next) {
        // no need to set mds as this is executed within listener scope, which already has mdc set
        final String sanitisedEvent = logFactory.getSanitisedMessage(in.getEvent());
        LOG.info("API Event: [{}]", sanitisedEvent);
        
        final SwaggerEventAck out = await(next.next(in));
        
        if (out.isApplicable()) {
            final ResponseStatusCode statusCode;
            if (out.getThrowable() == null) {
                statusCode = ResponseStatusCode.OK;
            } else {
                statusCode = (out.getThrowable() instanceof ServiceException)
                    ? ((ServiceException) out.getThrowable()).getStatusCode() : ResponseStatusCode.SERVER_ERROR;
            }
            
            switch (ServiceConstants.resolveStatusClass(statusCode)) {
                case OK:
                    LOG.info("API EventAck: [{}]", statusCode);
                    break;
                case CLIENT_ERROR:
                    LOG.warn("API EventAck: [{}]", statusCode);
                    break;
                case SERVER_ERROR:
                default:
                    LOG.error("API EventAck: [{}]", statusCode);
                    break;
            }
        } else {
            LOG.info("API EventAck: [N/A]");
        }
        
        return result(out);
    }
    
    @Override
    public int getOrder() {
        return HIGHEST_PRECEDENCE;
    }
    
}
