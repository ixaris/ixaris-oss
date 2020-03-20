package com.ixaris.commons.microservices.web;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.nio.charset.Charset;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

public class HttpResponse<T> {
    
    public static <T> HttpResponse<T> status(final HttpStatus httpStatus) {
        return new HttpResponse<>(httpStatus);
    }
    
    public static HttpResponse<String> noContent() {
        return status(HttpStatus.NO_CONTENT);
    }
    
    public static <T> HttpResponse<T> ok() {
        return status(HttpStatus.OK);
    }
    
    public static HttpResponse<String> serverError() {
        return status(HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    private final HttpStatus status;
    private final Map<String, String> headers = new HashMap<>();
    private T body;
    private Charset charset = UTF_8;
    private String contentType = MediaType.APPLICATION_JSON;
    
    public HttpResponse(final HttpStatus status) {
        this.status = status;
    }
    
    public HttpResponse<T> characterEncoding(final Charset charset) {
        this.charset = charset;
        return this;
    }
    
    public HttpResponse<T> contentType(final String contentType) {
        this.contentType = contentType;
        return this;
    }
    
    public HttpResponse<T> body(final T body) {
        this.body = body;
        return this;
    }
    
    public HttpHeaders getHeaders() {
        return new HttpHeaders() {
            
            @Override
            public String get(final String key) {
                return headers.get(key);
            }
            
            @Override
            public void add(final String key, final String value) {
                headers.put(key, value);
            }
            
        };
    }
    
    public HttpStatus getStatus() {
        return status;
    }
    
    public String getBody() {
        return Optional.ofNullable(body).map(s -> (String) s).orElse("");
    }
    
}
