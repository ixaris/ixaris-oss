package com.ixaris.commons.zookeeper.microservices;

import static com.ixaris.commons.zookeeper.clustering.ZookeeperClusterDiscoveryConnection.decode;
import static com.ixaris.commons.zookeeper.microservices.ZookeeperServiceDiscoveryConnection.getEndpointZnodePath;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.Executor;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.utils.ZKPaths;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

import com.ixaris.commons.async.lib.AsyncQueue;
import com.ixaris.commons.collections.lib.GuavaCollections;
import com.ixaris.commons.collections.lib.ListenerSet;
import com.ixaris.commons.microservices.lib.client.discovery.ServiceDiscovery;
import com.ixaris.commons.microservices.lib.client.discovery.ServiceEndpoint;
import com.ixaris.commons.microservices.lib.client.discovery.ServiceEndpointChangeListener;
import com.ixaris.commons.zookeeper.clustering.ZookeeperClusterDiscovery;

/**
 * Zookeeper Service discovery binding.
 *
 * <p>This class uses Zookeeper to maintain Service Discovery information. It reacts to events and updates happening in
 * the Zookeeper server.
 *
 * <p>Since zookeeper is a remote service, it is a good idea to cache locally the latest topology as observed on
 * zookeeper. We are using PathChildrenCache from Curator library to achieve this and react to zookeeper events to
 * update the local state. This enables the services to allow temporary downtime in the connection with zookeeper and
 * still be able to serve requests based on the latest cached copy. It is not required to monitor the state of the
 * connection to zookeeper directly since this is handled/abstracted by the PathChildrenCacheListener.
 *
 * @author aldrin.seychell
 */
@SuppressWarnings("squid:S3398")
public class ZookeeperServiceDiscovery extends ZookeeperClusterDiscovery implements ServiceDiscovery {
    
    private static final Logger LOG = LoggerFactory.getLogger(ZookeeperServiceDiscovery.class);
    
    private final ZookeeperServiceDiscoveryConnection connection;
    
    private volatile ImmutableMap<String, ServiceEndpoint> registeredEndpoints = ImmutableMap.of();
    private final Map<String, ListenerSet<ServiceEndpointChangeListener>> serviceEndpointChangeListeners = new HashMap<>();
    private final AsyncQueue endpointQueue = new AsyncQueue();
    
    private final PathChildrenCacheListener serviceKeysCacheListener = this::handleServiceKeysUpdated;
    
    public ZookeeperServiceDiscovery(final ZookeeperServiceDiscoveryConnection connection, final Executor executor) {
        super(connection, executor);
        this.connection = connection;
    }
    
    @Override
    public void addEndpointListener(final String name, final ServiceEndpointChangeListener listener) {
        endpointQueue
            .exec(() -> {
                final ListenerSet<ServiceEndpointChangeListener> set = serviceEndpointChangeListeners.computeIfAbsent(name, k -> new ListenerSet<>());
                if (set.add(listener)) {
                    ServiceEndpoint endpoint = registeredEndpoints.get(name);
                    if (endpoint == null) {
                        // Watch child nodes in zookeeper for new / changed service keys
                        final String path = getEndpointZnodePath(name);
                        connection.watchPath(path, serviceKeysCacheListener);
                        endpoint = new ServiceEndpoint(name, null);
                        registeredEndpoints = GuavaCollections.copyOfMapAdding(registeredEndpoints, name, endpoint);
                    }
                    try {
                        listener.onEndpointChanged(endpoint);
                    } catch (final RuntimeException e) {
                        LOG.error("listener threw exception", e);
                    }
                }
            })
            .onException(t -> LOG.error("Error in addEndpointListener()", t));
    }
    
    @Override
    public void removeEndpointListener(final String name, final ServiceEndpointChangeListener listener) {
        endpointQueue
            .exec(() -> {
                final ListenerSet<ServiceEndpointChangeListener> set = serviceEndpointChangeListeners.get(name);
                if (set != null) {
                    set.remove(listener);
                    if (set.isEmpty()) {
                        final String path = getEndpointZnodePath(name);
                        connection.unwatchPath(path, serviceKeysCacheListener);
                        serviceEndpointChangeListeners.remove(name);
                        final ServiceEndpoint endpoint = registeredEndpoints.get(name);
                        if (endpoint != null) {
                            registeredEndpoints = GuavaCollections.copyOfMapRemoving(registeredEndpoints, name);
                        }
                    }
                }
            })
            .onException(t -> LOG.error("Error in removeEndpointListener()", t));
    }
    
    @Override
    public Set<String> getServiceKeys(final String endpointName) {
        // This method gets the cached set of service keys. The registeredEndpoints map should get refreshed in the
        // background when changes occurs in zookeeper
        final ServiceEndpoint endpoint = registeredEndpoints.get(endpointName);
        if (endpoint == null) {
            return ImmutableSet.of();
        } else {
            return ImmutableSet.copyOf(endpoint.getServiceKeys().keySet());
        }
    }
    
    private void handleServiceKeysUpdated(final CuratorFramework client, final PathChildrenCacheEvent event) {
        if (event.getData() == null) {
            LOG.warn("Event with no data will be ignored: {}", event);
            return;
        }
        
        final String path = event.getData().getPath();
        final String[] pathParts = path.split(ZKPaths.PATH_SEPARATOR, -1);
        final String serviceKey = ZookeeperServiceDiscoveryConnection.denormaliseServiceKey(pathParts[pathParts.length - 1]);
        final String serviceName = pathParts[pathParts.length - 2];
        
        if ((event.getType() == PathChildrenCacheEvent.Type.CHILD_ADDED) || (event.getType() == PathChildrenCacheEvent.Type.CHILD_UPDATED)) {
            final String clusterName = decode(event.getData().getData());
            LOG.debug("Received zookeeper event of type [{}] for path [{}] with data {}", event.getType(), event.getData().getPath(), clusterName);
            handleEndpointKeyUpdated(serviceName, serviceKey, clusterName);
            
        } else if (event.getType() == PathChildrenCacheEvent.Type.CHILD_REMOVED) {
            LOG.debug("Received zookeeper event of type [{}] for path [{}]", event.getType(), event.getData().getPath());
            handleEndpointKeyRemoved(serviceName, serviceKey);
            
        } else {
            LOG.debug("Received unsupported event type [{}] for service keys from zookeeper", event.getType());
        }
    }
    
    private void handleEndpointKeyUpdated(final String name, final String key, final String clusterName) {
        executor.execute(() -> endpointQueue
            .exec(() -> {
                final ImmutableMap<String, String> keys = registeredEndpoints.get(name).getServiceKeys();
                final String oldClusterName = keys.get(key);
                if ((oldClusterName == null) || !oldClusterName.equals(clusterName)) {
                    final ServiceEndpoint updatedEndpoint = new ServiceEndpoint(name, GuavaCollections.copyOfMapAdding(keys, key, clusterName));
                    registeredEndpoints = GuavaCollections.copyOfMapAdding(registeredEndpoints, name, updatedEndpoint);
                    final ListenerSet<ServiceEndpointChangeListener> s = serviceEndpointChangeListeners.get(name);
                    if (s != null) {
                        s.publish(l -> l.onEndpointChanged(updatedEndpoint));
                    }
                }
            })
            .onException(t -> LOG.error("Error in handleEndpointKeyUpdated()", t)));
    }
    
    private void handleEndpointKeyRemoved(final String name, final String key) {
        executor.execute(() -> endpointQueue
            .exec(() -> {
                final ServiceEndpoint endpoint = registeredEndpoints.get(name);
                if (endpoint != null) {
                    final String clusterName = endpoint.getServiceKeys().get(key);
                    if (clusterName != null) {
                        final ImmutableMap<String, String> updatedKeys = GuavaCollections.copyOfMapRemoving(endpoint.getServiceKeys(), key);
                        final ServiceEndpoint updatedEndpoint = new ServiceEndpoint(name, updatedKeys);
                        registeredEndpoints = GuavaCollections.copyOfMapAdding(registeredEndpoints, name, updatedEndpoint);
                        final ListenerSet<ServiceEndpointChangeListener> s = serviceEndpointChangeListeners.get(name);
                        if (s != null) {
                            s.publish(l -> l.onEndpointChanged(updatedEndpoint));
                        }
                    }
                }
            })
            .onException(t -> LOG.error("Error in handleEndpointKeyRemoved()", t)));
    }
    
}
