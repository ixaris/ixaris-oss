package com.ixaris.commons.microservices.web;

import javax.servlet.AsyncContext;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * A {@link WriteListener} that will asynchronously write error-related data when required.
 *
 * @author benjie.gatt
 */
final class ServiceErrorWriteListener implements WriteListener {
    
    private static final Logger LOG = LoggerFactory.getLogger(ServiceErrorWriteListener.class);
    
    private final HttpServletResponse response;
    private final int statusCode;
    private final AsyncContext asyncContext;
    
    protected ServiceErrorWriteListener(final HttpServletResponse response, final int statusCode, final AsyncContext asyncContext) {
        this.response = response;
        this.statusCode = statusCode;
        this.asyncContext = asyncContext;
    }
    
    @Override
    public void onWritePossible() {
        try {
            response.setStatus(statusCode);
        } finally {
            asyncContext.complete();
        }
    }
    
    @Override
    public void onError(final Throwable t) {
        LOG.error("Error in writeListener", t);
        asyncContext.complete();
    }
    
}
