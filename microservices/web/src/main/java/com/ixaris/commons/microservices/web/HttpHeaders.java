package com.ixaris.commons.microservices.web;

public interface HttpHeaders {
    
    String get(String key);
    
    void add(final String key, final String value);
    
}
