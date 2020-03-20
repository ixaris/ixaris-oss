package com.ixaris.commons.microservices.lib.client.discovery;

import java.util.Set;

import com.ixaris.commons.clustering.lib.client.ClusterDiscovery;

/**
 * Service discovery used by clients to discover endpoints of other services.
 *
 * @author brian.vella
 * @author aldrin.seychell
 */
public interface ServiceDiscovery extends ClusterDiscovery {
    
    /**
     * @param endpointName
     * @return the currently available keys for the given tenant
     */
    Set<String> getServiceKeys(String endpointName);
    
    /**
     * Add a listener to be notified of topology changes. The listener is immediately notified of the current service
     * topology, and for any subsequent change.
     *
     * @param name the service name
     * @param listener the listener to set for the given service name
     */
    void addEndpointListener(String name, ServiceEndpointChangeListener listener);
    
    /**
     * Remove a listener. Given listener instance must match an added listener, otherwise request is ignored.
     *
     * @param name
     * @param listener the listener to unset for the given service name
     */
    void removeEndpointListener(String name, ServiceEndpointChangeListener listener);
    
}
