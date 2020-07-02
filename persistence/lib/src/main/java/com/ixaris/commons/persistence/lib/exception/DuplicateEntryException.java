package com.ixaris.commons.persistence.lib.exception;

/**
 * Exception thrown to indicate that an entity with the same ID already exists.
 */
public class DuplicateEntryException extends RuntimeException {
    
    public DuplicateEntryException(final String message, final Throwable cause) {
        super(message, cause);
    }
    
}
