package com.ixaris.commons.microservices.web;

import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.Charset;
import java.nio.charset.UnsupportedCharsetException;
import java.util.Optional;

import javax.servlet.http.HttpServletRequest;

public class HttpRequest<T> {
    
    public static HttpRequest<String> from(final String body, final HttpServletRequest httpRequest) {
        return new HttpRequest<>(body, httpRequest);
    }
    
    private final T body;
    private final HttpServletRequest httpRequest;
    
    public HttpRequest(final T body, final HttpServletRequest httpRequest) {
        this.body = body;
        this.httpRequest = httpRequest;
    }
    
    public URI getUri() {
        try {
            return new URI(httpRequest.getRequestURI());
        } catch (final URISyntaxException e) {
            throw new IllegalStateException(e);
        }
    }
    
    public HttpMethod getMethod() {
        return HttpMethod.valueOf(httpRequest.getMethod());
    }
    
    public Charset getCharacterEncoding() {
        try {
            return Charset.forName(httpRequest.getCharacterEncoding());
        } catch (UnsupportedCharsetException x) {
            throw new IllegalStateException(x);
        }
    }
    
    @SuppressWarnings({ "unchecked", "squid:S1172" })
    public <B> Optional<B> getBody(final Class<B> bodyClass) {
        return (Optional<B>) Optional.ofNullable(body);
    }
    
    public String getPath() {
        return httpRequest.getRequestURI();
    }
    
    public String getQueryString() {
        return httpRequest.getQueryString();
    }
    
    public HttpHeaders getHeaders() {
        return new HttpHeaders() {
            
            @Override
            public String get(final String key) {
                return httpRequest.getHeader(key);
            }
            
            @Override
            public void add(final String key, final String value) {
                throw new UnsupportedOperationException();
            }
            
        };
    }
    
}
