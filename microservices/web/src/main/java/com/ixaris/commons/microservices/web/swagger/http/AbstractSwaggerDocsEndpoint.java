package com.ixaris.commons.microservices.web.swagger.http;

import static com.ixaris.commons.async.lib.Async.result;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;
import java.util.Collections;
import java.util.List;

import javax.servlet.AsyncContext;
import javax.servlet.ServletResponse;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServlet;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.fasterxml.jackson.core.JsonProcessingException;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.exception.ServiceException;

import io.swagger.v3.core.util.Json;
import io.swagger.v3.core.util.Yaml;
import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.servers.Server;
import io.swagger.v3.parser.OpenAPIV3Parser;
import io.swagger.v3.parser.core.models.AuthorizationValue;

public class AbstractSwaggerDocsEndpoint extends HttpServlet {
    
    private static final Logger LOG = LoggerFactory.getLogger(AbstractSwaggerDocsEndpoint.class);
    
    private final String apiYaml;
    private final String apiJson;
    private final String webhookYaml;
    private final String webhookJson;
    
    public AbstractSwaggerDocsEndpoint(final String filePrefix, final String serverUrl) {
        try {
            final String prefixToUse = ((filePrefix == null) || filePrefix.isEmpty()) ? "" : (filePrefix + '_');
            final OpenAPI api = new OpenAPIV3Parser()
                .readWithInfo(prefixToUse + "api.yaml", (List<AuthorizationValue>) null)
                .getOpenAPI();
            api.setServers(Collections.singletonList(new Server().url(serverUrl)));
            final OpenAPI webhook = new OpenAPIV3Parser()
                .readWithInfo(prefixToUse + "webhook.yaml", (List<AuthorizationValue>) null)
                .getOpenAPI();
            
            apiYaml = Yaml.pretty().writeValueAsString(api);
            apiJson = Json.pretty().writeValueAsString(api);
            webhookYaml = Yaml.pretty().writeValueAsString(webhook);
            webhookJson = Json.pretty().writeValueAsString(webhook);
        } catch (final JsonProcessingException e) {
            throw new IllegalStateException(e);
        }
    }
    
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
            invoke(asyncContext);
        } catch (final ServiceException e) {
            LOG.error("Service Exception in doGet", e);
            error(asyncContext, e.getStatusCode().getNumber());
        } catch (final RuntimeException e) {
            LOG.error("Error in doGet", e);
            error(asyncContext, 500);
        }
    }
    
    @SuppressWarnings({ "squid:S1181", "squid:S1141" })
    private Async<Void> invoke(final AsyncContext asyncContext) {
        final HttpServletRequest httpRequest = (HttpServletRequest) asyncContext.getRequest();
        try {
            final String type;
            final String doc;
            if (httpRequest.getRequestURI().endsWith("api.yaml")) {
                type = "application/x-yaml";
                doc = apiYaml;
            } else if (httpRequest.getRequestURI().endsWith("api.json")) {
                type = "application/json";
                doc = apiJson;
            } else if (httpRequest.getRequestURI().endsWith("webhook.yaml")) {
                type = "application/x-yaml";
                doc = webhookYaml;
            } else if (httpRequest.getRequestURI().endsWith("webhook.json")) {
                type = "application/json";
                doc = webhookJson;
            } else {
                type = null;
                doc = null;
            }
            try {
                final HttpServletResponse httpResponse = (HttpServletResponse) asyncContext.getResponse();
                if (type != null) {
                    httpResponse.getOutputStream().setWriteListener(new ResponseWriteListener(type, doc, httpResponse, asyncContext));
                } else {
                    error(asyncContext, 404);
                }
            } catch (final IOException e) {
                LOG.error("Error when handling docs", e);
                asyncContext.complete();
            }
        } catch (final Throwable tt) {
            LOG.error("Non-service exception while handling call on url: " + httpRequest.getPathInfo(), tt);
            error(asyncContext, 500);
        }
        return result();
    }
    
    private void error(final AsyncContext asyncContext, final int statusCode) {
        try {
            final ServletResponse response = asyncContext.getResponse();
            response.getOutputStream().setWriteListener(new ErrorWriteListener((HttpServletResponse) response, statusCode, asyncContext));
        } catch (final IOException e) {
            LOG.error("Error when invoking/handling microservice request/response", e);
            asyncContext.complete();
        }
    }
    
    private static final class ResponseWriteListener implements WriteListener {
        
        private static final Logger LOG = LoggerFactory.getLogger(ResponseWriteListener.class);
        
        private final String type;
        private final String doc;
        private final HttpServletResponse response;
        private final AsyncContext asyncContext;
        
        ResponseWriteListener(final String type,
                              final String doc,
                              final HttpServletResponse response,
                              final AsyncContext asyncContext) {
            this.type = type;
            this.doc = doc;
            this.response = response;
            this.asyncContext = asyncContext;
        }
        
        @Override
        public void onWritePossible() {
            try {
                response.setContentType(type);
                response.setCharacterEncoding("UTF-8");
                response.setStatus(200);
                response.getOutputStream().write(doc.getBytes(UTF_8));
            } catch (final IOException e) {
                LOG.error("Error in onWritePossible", e);
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
    
    private static final class ErrorWriteListener implements WriteListener {
        
        private static final Logger LOG = LoggerFactory.getLogger(ErrorWriteListener.class);
        
        private final HttpServletResponse response;
        private final int statusCode;
        private final AsyncContext asyncContext;
        
        public ErrorWriteListener(final HttpServletResponse response, final int statusCode, final AsyncContext asyncContext) {
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
    
}
