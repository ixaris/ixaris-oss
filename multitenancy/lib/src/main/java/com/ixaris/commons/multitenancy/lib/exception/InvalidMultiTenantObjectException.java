package com.ixaris.commons.multitenancy.lib.exception;

/**
 * Exception to indicate that a {@link com.ixaris.commons.multitenancy.lib.object.MultiTenantObject} has is invalid.
 *
 * @author benjie.gatt
 */
public class InvalidMultiTenantObjectException extends IllegalStateException {
    
    public InvalidMultiTenantObjectException(final String s) {
        super(s);
    }
}
