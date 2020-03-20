package com.ixaris.commons.microservices.lib.local;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixaris.commons.clustering.lib.extra.LocalCluster;
import com.ixaris.commons.clustering.lib.service.ClusterDispatchFilterFactory;
import com.ixaris.commons.clustering.lib.service.ClusterHandleFilterFactory;
import com.ixaris.commons.collections.lib.GuavaCollectors;
import com.ixaris.commons.collections.lib.ListenerSet;
import com.ixaris.commons.microservices.lib.client.discovery.ServiceDiscovery;
import com.ixaris.commons.microservices.lib.client.discovery.ServiceEndpoint;
import com.ixaris.commons.microservices.lib.client.discovery.ServiceEndpointChangeListener;
import com.ixaris.commons.microservices.lib.service.discovery.ServiceRegistry;

/**
 * Supports only one node per service name/key combo (this being a local service discovery). Tenant param is ignored
 * (same services for all tenants). This class is responsible both for service discovery and registry of the single
 * local node.
 *
 * <p>Since the {@link LocalService} is intended to be used locally, by it assumes a single service type and node id and
 * all operations are executed in the context of these service type and node id.
 *
 * @author brian.vella
 * @author aldrin.seychell
 */
public class LocalService extends LocalCluster implements ServiceDiscovery, ServiceRegistry {
    
    private static final Logger LOG = LoggerFactory.getLogger(LocalService.class);
    
    private final Object lock = new Object();
    
    private final Map<String, Set<String>> registeredEndpoints = new HashMap<>();
    private final Map<String, ListenerSet<ServiceEndpointChangeListener>> discoveryEndpointChangeListeners = new HashMap<>();
    
    public LocalService(final Set<? extends ClusterDispatchFilterFactory> dispatchFilterFactories,
                        final Set<? extends ClusterHandleFilterFactory> handleFilterFactories) {
        super(dispatchFilterFactories, handleFilterFactories);
    }
    
    public LocalService(final String clusterName,
                        final int nodeId,
                        final Set<? extends ClusterDispatchFilterFactory> dispatchFilterFactories,
                        final Set<? extends ClusterHandleFilterFactory> handleFilterFactories) {
        super(clusterName, nodeId, dispatchFilterFactories, handleFilterFactories);
    }
    
    @Override
    public Set<String> getServiceKeys(final String endpointName) {
        synchronized (lock) {
            return Optional.ofNullable(registeredEndpoints.get(endpointName)).orElse(Collections.emptySet());
        }
    }
    
    @Override
    public void addEndpointListener(final String name, final ServiceEndpointChangeListener listener) {
        synchronized (lock) {
            final ListenerSet<ServiceEndpointChangeListener> set = discoveryEndpointChangeListeners
                .computeIfAbsent(name, k -> new ListenerSet<>());
            if (set.add(listener)) {
                final ServiceEndpoint endpoint = Optional.ofNullable(registeredEndpoints.get(name))
                    .map(keys -> new ServiceEndpoint(
                        name, keys.stream().collect(GuavaCollectors.toImmutableMap(k -> k, k -> clusterName))))
                    .orElse(new ServiceEndpoint(name, null));
                try {
                    listener.onEndpointChanged(endpoint);
                } catch (final RuntimeException e) {
                    LOG.error("listener threw exception", e);
                }
            }
        }
    }
    
    @Override
    public void removeEndpointListener(final String name, final ServiceEndpointChangeListener listener) {
        synchronized (lock) {
            final ListenerSet<ServiceEndpointChangeListener> set = discoveryEndpointChangeListeners.get(name);
            if (set != null) {
                set.remove(listener);
                if (set.isEmpty()) {
                    discoveryEndpointChangeListeners.remove(name);
                }
            }
        }
    }
    
    @Override
    public void register(final String serviceName, final String serviceKey) {
        synchronized (lock) {
            final Set<String> keys = registeredEndpoints.computeIfAbsent(serviceName, k -> new HashSet<>());
            if (keys.add(serviceKey)) {
                final ServiceEndpoint endpoint = new ServiceEndpoint(
                    serviceName, keys.stream().collect(GuavaCollectors.toImmutableMap(k -> k, k -> clusterName)));
                final ListenerSet<ServiceEndpointChangeListener> s = discoveryEndpointChangeListeners.get(serviceName);
                if (s != null) {
                    s.publish(l -> l.onEndpointChanged(endpoint));
                }
            }
        }
    }
    
    @Override
    public void deregister(final String serviceName, final String serviceKey) {
        final Set<String> keys = registeredEndpoints.get(serviceName);
        if ((keys != null) && keys.remove(serviceKey)) {
            if (keys.isEmpty()) {
                registeredEndpoints.remove(serviceName);
            }
            final ServiceEndpoint endpoint = new ServiceEndpoint(
                serviceName, keys.stream().collect(GuavaCollectors.toImmutableMap(k -> k, k -> clusterName)));
            final ListenerSet<ServiceEndpointChangeListener> s = discoveryEndpointChangeListeners.get(serviceName);
            if (s != null) {
                s.publish(l -> l.onEndpointChanged(endpoint));
            }
        }
    }
}
