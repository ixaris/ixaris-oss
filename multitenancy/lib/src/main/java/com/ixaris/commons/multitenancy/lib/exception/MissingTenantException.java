package com.ixaris.commons.multitenancy.lib.exception;

/**
 * Exception to indicate that an action on a tenant was called, but the tenant was not loaded.
 *
 * @author benjie.gatt
 */
public class MissingTenantException extends IllegalStateException {
    
    private final String tenantId;
    
    public MissingTenantException(final String tenantId) {
        super("TenantId [" + tenantId + "] is not present in the tenant list.");
        this.tenantId = tenantId;
    }
    
    public String getTenantId() {
        return tenantId;
    }
}
