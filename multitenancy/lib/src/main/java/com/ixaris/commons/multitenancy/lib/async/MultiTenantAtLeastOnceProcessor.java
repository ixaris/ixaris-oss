package com.ixaris.commons.multitenancy.lib.async;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.clustering.lib.idempotency.AtLeastOnceProcessor;

/**
 * At least once processors that are tenant-aware, hence need to react when tenants are added/removed.
 *
 * @author brian.vella
 * @author aldrin.seychell
 */
public interface MultiTenantAtLeastOnceProcessor extends AtLeastOnceProcessor {
    
    Async<Void> registerTenant(String tenantId);
    
    Async<Void> deregisterTenant(String tenantId);
    
}
