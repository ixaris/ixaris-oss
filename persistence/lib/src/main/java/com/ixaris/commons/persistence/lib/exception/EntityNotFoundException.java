package com.ixaris.commons.persistence.lib.exception;

/**
 * Exception thrown when an entity is not found
 */
public class EntityNotFoundException extends RuntimeException {
    
    public EntityNotFoundException() {}
    
    public EntityNotFoundException(final String message) {
        super(message);
    }
    
}
