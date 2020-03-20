package com.ixaris.commons.multitenancy.lib.clustering;

import com.ixaris.commons.async.lib.Async;

public interface TenantRegistry {
    
    Async<Void> registerTenant(String tenantId);
    
    Async<Void> deregisterTenant(String tenantId);
    
}
