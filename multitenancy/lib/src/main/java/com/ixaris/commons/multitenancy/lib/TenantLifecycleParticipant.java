package com.ixaris.commons.multitenancy.lib;

import com.ixaris.commons.async.lib.Async;

/**
 * A participant that performs activation and deactivation logic per tenant
 */
public interface TenantLifecycleParticipant {
    
    String getName();
    
    /**
     * Pre activation stage. Typically used by datasources and infrastructure components to set up a tenant connection
     *
     * @param tenantId
     * @return
     */
    Async<Void> preActivate(String tenantId);
    
    /**
     * Activation stage. Activation logic to be performed here. One may use a tenant's data source connection here as this would have been set up
     * in preActivate()
     *
     * @param tenantId
     * @return
     */
    Async<Void> activate(String tenantId);
    
    /**
     * Deactivation stage, reverse of activate()
     *
     * @param tenantId
     * @return
     */
    Async<Void> deactivate(String tenantId);
    
    /**
     * Post deactivation stage. reverse of preActivate(). Typically used by datasources and infrastructure components to tear down tenant
     * connection
     *
     * @param tenantId
     * @return
     */
    Async<Void> postDeactivate(String tenantId);
    
}
