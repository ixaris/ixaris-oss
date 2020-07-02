package com.ixaris.commons.persistence.lib.exception;

/**
 * Exception thrown when an entity is persisted with a conflicting version due to optimistic locking
 */
public class OptimisticLockException extends RuntimeException {
    
    public OptimisticLockException(final Throwable cause) {
        super(cause);
    }
    
}
