package com.ixaris.commons.microservices.lib.service.discovery;

import java.util.Map;
import java.util.Set;

import com.ixaris.commons.clustering.lib.service.ClusterRegistry;

/**
 * The service registry holds all the details on the available nodes, grouped by service type. Each node is responsible of publishing the details
 * of how other nodes can connect to it.
 *
 * <p>There are 2 main reasons why other nodes would want to connect to a node:
 *
 * <ol>
 *   <li>Connecting with other nodes of the same service type to form a cluster and coordinating together. These are referred to as Service
 *       Nodes.
 *   <li>Connecting with other nodes as a client of those services. These are value to as endpoints and each service node can have its own
 *       published endpoints and may register itself under multiple endpoints (especially in the case of SPIs).
 * </ol>
 *
 * <p>Service registration should consume unclean shutdown (crash) and update internal state accordingly. Clean shutdown should be internally
 * handled by hooking to a lifecycle event and un-registering cleanly, but service registry should not depend on this clean shutdown.
 *
 * @author brian.vella
 * @author aldrin.seychell
 */
public interface ServiceRegistry extends ClusterRegistry {
    
    /**
     * Registers a new endpoint for the current node
     *
     * @param serviceName The service endpoint name
     * @param serviceKey The service endpoint key (in case of an SPI)
     * @throws IllegalStateException if the caller has not yet registered itself as a node and attempts to deregister
     *     other nodes
     */
    void register(String serviceName, String serviceKey);
    
    /**
     * De-add this node from a specified endpoint name.
     *
     * @param serviceName The service endpoint name
     * @param serviceKey The service endpoint key (in case of an SPI)
     */
    void deregister(String serviceName, String serviceKey);
    
    /**
     * @param attributes the attributes to merge
     */
    void mergeAttributes(Map<String, String> attributes);
    
}
