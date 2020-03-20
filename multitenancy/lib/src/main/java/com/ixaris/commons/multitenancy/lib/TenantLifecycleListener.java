package com.ixaris.commons.multitenancy.lib;

public interface TenantLifecycleListener {
    
    void onTenantActive(String tenantId);
    
    void onTenantInactive(String tenantId);
    
}
