package com.ixaris.commons.clustering.lib.client;

import com.ixaris.commons.clustering.lib.common.ClusterTopologyChangeListener;

/**
 * Service discovery used by clients to discover endpoints of other services.
 *
 * @author brian.vella
 * @author aldrin.seychell
 */
public interface ClusterDiscovery {
    
    /**
     * Add a listener to be notified of topology changes. The listener is immediately notified of the current service
     * topology, and for any subsequent change.
     *
     * @param name the cluster name
     * @param listener the listener to set for the given service name
     */
    void addTopologyListener(String name, ClusterTopologyChangeListener listener);
    
    /**
     * Remove a listener. Given listener instance must match an added listener, otherwise request is ignored.
     *
     * @param name
     * @param listener the listener to unset for the given service name
     */
    void removeTopologyListener(String name, ClusterTopologyChangeListener listener);
    
}
