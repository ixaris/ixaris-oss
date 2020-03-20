package com.ixaris.commons.microservices.web.swagger;

/**
 * Exception used to indicate that a URL was invalid
 *
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public final class InvalidUrlException extends Exception {
    
    public InvalidUrlException(final String url) {
        super("Invalid URL: [" + url + "]");
    }
    
    public InvalidUrlException(final String url, final String message) {
        super("Invalid URL: [" + url + "] with message: " + message);
    }
}
