package com.ixaris.commons.zookeeper.microservices;

import static com.ixaris.commons.zookeeper.microservices.ZookeeperServiceDiscoveryConnection.getEndpointKeyZnodePath;
import static com.ixaris.commons.zookeeper.microservices.ZookeeperServiceDiscoveryConnection.normaliseServiceKey;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.util.Set;
import java.util.concurrent.Executor;

import org.apache.curator.framework.CuratorFramework;
import org.apache.zookeeper.KeeperException.NodeExistsException;

import com.ixaris.commons.clustering.lib.service.ClusterDispatchFilterFactory;
import com.ixaris.commons.clustering.lib.service.ClusterHandleFilterFactory;
import com.ixaris.commons.clustering.lib.service.ShardAllocationStrategy;
import com.ixaris.commons.microservices.lib.service.discovery.ServiceRegistry;
import com.ixaris.commons.zookeeper.clustering.ZookeeperClusterRegistry;
import com.ixaris.commons.zookeeper.clustering.ZookeeperClusterRegistryHelperFactory;

/**
 * Zookeeper Service Registry
 *
 * <p>This class uses Zookeeper to maintain Service Registry for each service. Each service is responsible in
 * registering itself in the Service Registry so that other nodes of the same service type can discover it. It is also
 * responsible of publishing endpoints and maintain endpoint attributes for its published endpoints.
 *
 * <p>In zookeeper, data is stored within ZNodes and can be created in different modes (e.g. EPHEMERAL). Ephemeral nodes
 * are nodes that live only until the session that created them dies. This functionality is useful to automatically
 * un-register the service when it loses connection with zookeeper (e.g. for restarts).
 *
 * <p>Since zookeeper is a remote service, it is a good idea to cache locally the latest topology as observed on
 * zookeeper. We are using PathChildrenCache from Curator library to achieve this and react to zookeeper events to
 * update the local state. This enables the services to allow temporary downtime in the connection with zookeeper and
 * still be able to serve requests based on the latest cached copy.
 *
 * @author aldrin.seychell
 */
public class ZookeeperServiceRegistry extends ZookeeperClusterRegistry implements ServiceRegistry {
    
    private final ZookeeperServiceDiscoveryConnection connection;
    
    public ZookeeperServiceRegistry(final ZookeeperServiceDiscoveryConnection connection,
                                    final ShardAllocationStrategy shardAllocationStrategy,
                                    final Executor executor,
                                    final ZookeeperClusterRegistryHelperFactory helperFactory,
                                    final Set<? extends ClusterDispatchFilterFactory> dispatchFilterFactories,
                                    final Set<? extends ClusterHandleFilterFactory> handleFilterFactories) {
        super(connection, shardAllocationStrategy, executor, helperFactory, dispatchFilterFactories, handleFilterFactories);
        this.connection = connection;
    }
    
    @Override
    @SuppressWarnings({ "squid:S1166", "squid:S1141" })
    public void register(final String serviceName, final String serviceKey) {
        final CuratorFramework zookeeperClient = connection.getZookeeperClient();
        final String znodeBasePath = getEndpointKeyZnodePath(serviceName, normaliseServiceKey(serviceKey));
        
        try {
            if (zookeeperClient.checkExists().forPath(znodeBasePath) == null) {
                try {
                    zookeeperClient.create().creatingParentsIfNeeded().forPath(znodeBasePath, clusterName.getBytes(UTF_8));
                } catch (final NodeExistsException ignored) {
                    zookeeperClient.setData().forPath(znodeBasePath, clusterName.getBytes(UTF_8));
                }
            } else {
                zookeeperClient.setData().forPath(znodeBasePath, clusterName.getBytes(UTF_8));
            }
        } catch (final IllegalStateException e) {
            throw e;
        } catch (final Exception e) {
            throw new IllegalStateException(String.format("Unable to add/update node [%s] with key [%s] in service discovery", serviceName, serviceKey), e);
        }
    }
    
    @Override
    public void shutdown() {
        super.shutdown();
    }
    
    @Override
    public void deregister(final String serviceName, final String serviceKey) {
        // no-op, we leave the cluster name there even when the last node leaves, to avoid having to coordinate or lock,
        // in case the last node leaves while another is starting
    }
    
}
