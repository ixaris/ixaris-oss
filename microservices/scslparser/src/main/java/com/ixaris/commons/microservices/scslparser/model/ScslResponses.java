package com.ixaris.commons.microservices.scslparser.model;

import java.util.Arrays;

import com.ixaris.commons.microservices.scslparser.model.exception.ScslParseException;

/**
 * Supported status codes
 */
public enum ScslResponses {
    
    SUCCESS("success"),
    CONFLICT("conflict");
    
    private final String code;
    
    ScslResponses(final String code) {
        this.code = code;
    }
    
    public String getCode() {
        return code;
    }
    
    public static ScslResponses parse(final String status) {
        return Arrays.stream(values())
            .filter(c -> c.getCode().equals(status))
            .findFirst()
            .orElseThrow(() -> new ScslParseException("Unhandled Status: " + status));
    }
    
}
