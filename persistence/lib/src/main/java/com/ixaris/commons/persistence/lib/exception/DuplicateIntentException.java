package com.ixaris.commons.persistence.lib.exception;

/**
 * Exception thrown when the intent already exists.
 */
public class DuplicateIntentException extends Exception {
    
    public DuplicateIntentException() {
        super();
    }
    
    public DuplicateIntentException(final String message) {
        super(message);
    }
    
}
