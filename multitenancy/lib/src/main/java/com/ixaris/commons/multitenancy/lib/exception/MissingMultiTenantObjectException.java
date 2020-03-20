package com.ixaris.commons.multitenancy.lib.exception;

import com.ixaris.commons.multitenancy.lib.object.AbstractEagerMultiTenantObject;

/**
 * Exception to indicate that an {@link AbstractEagerMultiTenantObject} was not precreated for some (presumably bad) reason.
 *
 * @author benjie.gatt
 */
public class MissingMultiTenantObjectException extends IllegalStateException {
    
    public MissingMultiTenantObjectException(final String s) {
        super(s);
    }
}
