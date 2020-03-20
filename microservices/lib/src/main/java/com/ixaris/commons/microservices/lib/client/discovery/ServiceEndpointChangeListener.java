package com.ixaris.commons.microservices.lib.client.discovery;

/**
 * Endpoint topology change listeners that gets notified every time an endpoint topology (set of nodes available for an
 * endpoint) changes.
 *
 * @author aldrin.seychell
 */
@FunctionalInterface
public interface ServiceEndpointChangeListener {
    
    /**
     * The listening party is expected to track the currently connected state and update internal state accordingly by
     * comparing with the current service topology (obtained through this listener). It is recommended that topology
     * changes are treated in a thread safe manner. This method SHOULD NOT BLOCK!
     *
     * <p>Recommended implementation stores last known topology update in an AtomicReference and asynchronously process
     * by invoking getAndSet(null) and comparing to the current state.
     *
     * @param endpoint the new endpoint topology, never null (will be empty if service is no longer available)
     */
    void onEndpointChanged(ServiceEndpoint endpoint);
    
}
