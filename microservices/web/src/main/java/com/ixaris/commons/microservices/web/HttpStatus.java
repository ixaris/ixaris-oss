package com.ixaris.commons.microservices.web;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

public enum HttpStatus {
    
    OK(200),
    CREATED(201),
    NO_CONTENT(204),
    BAD_REQUEST(400),
    UNAUTHORIZED(401),
    FORBIDDEN(403),
    NOT_FOUND(404),
    METHOD_NOT_ALLOWED(405),
    CONFLICT(409),
    INTERNAL_SERVER_ERROR(500),
    NOT_IMPLEMENTED(501),
    BAD_GATEWAY(502),
    SERVICE_UNAVAILABLE(503),
    GATEWAY_TIMEOUT(504);
    
    private static final Map<Integer, HttpStatus> BY_CODE;
    
    static {
        Map<Integer, HttpStatus> byCode = new LinkedHashMap<>();
        for (final HttpStatus status : values()) {
            byCode.put(status.code, status);
        }
        
        BY_CODE = Collections.unmodifiableMap(byCode);
    }
    
    public static HttpStatus valueOf(int code) {
        HttpStatus status = BY_CODE.get(code);
        if (status == null) {
            throw new IllegalArgumentException("Invalid HTTP status code: " + code);
        } else {
            return status;
        }
    }
    
    private final int code;
    
    HttpStatus(final int code) {
        this.code = code;
    }
    
    public int getCode() {
        return this.code;
    }
    
}
