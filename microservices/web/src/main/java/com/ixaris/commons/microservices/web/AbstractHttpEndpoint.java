package com.ixaris.commons.microservices.web;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

import javax.servlet.AsyncContext;
import javax.servlet.ReadListener;
import javax.servlet.ServletInputStream;
import javax.servlet.ServletResponse;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.exception.ServiceException;

/**
 * HTTP Servlet that acts as an HTTP proxy to other microservices. In this way, microservices which otherwise do not
 * offer an HTTP endpoint can still be queried via HTTP.
 */
public abstract class AbstractHttpEndpoint extends HttpServlet {
    
    private static final Logger LOG = LoggerFactory.getLogger(AbstractHttpEndpoint.class);
    
    public static final String AUTHORIZATION_HEADER = "authorization";
    public static final String CALL_REF_HEADER = "call-ref";
    
    @Override
    public final void doGet(final HttpServletRequest req, final HttpServletResponse res) {
        final AsyncContext asyncContext;
        try {
            asyncContext = req.startAsync();
        } catch (final IllegalStateException e) {
            LOG.error("Error preparing async call", e);
            res.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
            return;
        }
        
        try {
            invoke(null, asyncContext);
        } catch (final ServiceException e) {
            LOG.error("Service Exception in doGet", e);
            error(asyncContext, e.getStatusCode().getNumber());
        } catch (final RuntimeException e) {
            LOG.error("Error in doGet", e);
            error(asyncContext, 500);
        }
    }
    
    @Override
    public final void doPost(final HttpServletRequest req, final HttpServletResponse res) throws IOException {
        final AsyncContext asyncContext;
        try {
            asyncContext = req.startAsync();
        } catch (final IllegalStateException e) {
            LOG.error("Error preparing async call", e);
            res.setStatus(HttpServletResponse.SC_NOT_IMPLEMENTED);
            return;
        }
        
        final ServletInputStream input = req.getInputStream();
        
        input.setReadListener(new ReadListener() {
            
            private final byte[] buffer = new byte[4096];
            private final StringBuilder sb = new StringBuilder();
            
            @Override
            public void onDataAvailable() throws IOException {
                do {
                    final int length = input.read(buffer);
                    sb.append(new String(buffer, 0, length, StandardCharsets.UTF_8));
                } while (input.isReady());
            }
            
            @Override
            public void onAllDataRead() {
                try {
                    invoke(sb.toString(), asyncContext);
                } catch (final ServiceException e) {
                    LOG.error("Service Exception in doPost", e);
                    error(asyncContext, e.getStatusCode().getNumber());
                } catch (final RuntimeException e) {
                    LOG.error("Error in doPost", e);
                    error(asyncContext, 500);
                }
            }
            
            @Override
            public void onError(final Throwable t) {
                LOG.error("Error in doPost > readListener", t);
                error(asyncContext, 500);
            }
        });
    }
    
    private Async<Void> invoke(final String body, final AsyncContext asyncContext) {
        final HttpServletRequest httpRequest = (HttpServletRequest) asyncContext.getRequest();
        try {
            final HttpResponse<String> operationResponse = await(invoke(HttpRequest.from(body, httpRequest)));
            try {
                final HttpServletResponse httpResponse = (HttpServletResponse) asyncContext.getResponse();
                httpResponse.getOutputStream().setWriteListener(new ServiceResponseWriteListener(operationResponse, httpResponse, httpRequest, asyncContext));
            } catch (final IOException e) {
                LOG.error("Error when invoking/handling microservice request/response", e);
                asyncContext.complete();
            }
        } catch (final Throwable tt) { // NOSONAR edge needs to catch Throwable
            LOG.error("Non-service exception while handling call on url: " + httpRequest.getPathInfo(), tt);
            error(asyncContext, 500);
        }
        return result();
    }
    
    private void error(final AsyncContext asyncContext, final int statusCode) {
        try {
            final ServletResponse response = asyncContext.getResponse();
            response.getOutputStream().setWriteListener(new ServiceErrorWriteListener((HttpServletResponse) response, statusCode, asyncContext));
        } catch (final IOException e) {
            LOG.error("Error when invoking/handling microservice request/response", e);
            asyncContext.complete();
        }
    }
    
    protected abstract Async<HttpResponse<String>> invoke(final HttpRequest<String> request);
    
}
