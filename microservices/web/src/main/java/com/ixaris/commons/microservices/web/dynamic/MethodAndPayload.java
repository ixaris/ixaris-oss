package com.ixaris.commons.microservices.web.dynamic;

import java.io.UnsupportedEncodingException;
import java.net.URLDecoder;

import com.ixaris.commons.microservices.web.HttpMethod;
import com.ixaris.commons.microservices.web.HttpRequest;

public final class MethodAndPayload {
    
    public final String method;
    public final String payload;
    
    public MethodAndPayload(final HttpRequest<String> request) {
        final String query = request.getQueryString();
        if (request.getMethod() == HttpMethod.GET) {
            method = "get";
            try {
                payload = query != null
                    ? URLDecoder.decode(query, request.getCharacterEncoding().toString()) : null;
            } catch (final UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
        } else {
            try {
                method = query == null
                    ? "post" : trimTrailingEquals(URLDecoder.decode(query, request.getCharacterEncoding().toString()));
            } catch (final UnsupportedEncodingException e) {
                throw new IllegalStateException(e);
            }
            payload = request.getBody(String.class).orElse(null);
        }
    }
    
    private static String trimTrailingEquals(final String str) {
        if (str.endsWith("=")) {
            return str.substring(0, str.length() - 1);
        }
        return str;
    }
}
