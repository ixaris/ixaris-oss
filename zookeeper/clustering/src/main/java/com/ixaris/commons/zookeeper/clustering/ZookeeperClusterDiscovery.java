package com.ixaris.commons.zookeeper.clustering;

import static com.ixaris.commons.clustering.lib.common.ClusterTopology.UNKNOWN_NODE_ID;
import static com.ixaris.commons.zookeeper.clustering.ZookeeperClusterDiscoveryConnection.decode;
import static com.ixaris.commons.zookeeper.clustering.ZookeeperClusterDiscoveryConnection.getClusterZnodePath;
import static com.ixaris.commons.zookeeper.clustering.ZookeeperClusterDiscoveryConnection.nodeNameToId;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Executor;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import com.ixaris.commons.async.lib.AsyncQueue;
import com.ixaris.commons.clustering.lib.client.ClusterDiscovery;
import com.ixaris.commons.clustering.lib.common.ClusterNodeInfo;
import com.ixaris.commons.clustering.lib.common.ClusterShardResolver;
import com.ixaris.commons.clustering.lib.common.ClusterTopology;
import com.ixaris.commons.clustering.lib.common.ClusterTopologyChangeListener;
import com.ixaris.commons.clustering.lib.common.ModClusterShardResolver;
import com.ixaris.commons.collections.lib.BitSet;
import com.ixaris.commons.collections.lib.ListenerSet;
import com.ixaris.commons.zookeeper.clustering.CommonsZookeeperClustering.ClusterInfo;
import com.ixaris.commons.zookeeper.clustering.CommonsZookeeperClustering.NodeInfo;

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
public class ZookeeperClusterDiscovery implements ClusterDiscovery {
    
    private static final Logger LOG = LoggerFactory.getLogger(ZookeeperClusterDiscovery.class);
    
    protected final ZookeeperClusterDiscoveryConnection connection;
    protected final Executor executor;
    private final Map<String, ClusterTopology> registeredClusters = new HashMap<>();
    private final Map<String, ListenerSet<ClusterTopologyChangeListener>> topologyChangeListeners = new HashMap<>();
    private final AsyncQueue topologyQueue = new AsyncQueue();
    
    private final PathChildrenCacheListener clusterNodesCacheListener = this::handleClusterNodesUpdated;
    private final PathNodeCacheListener clusterCacheListener = this::handleClusterUpdated;
    
    public ZookeeperClusterDiscovery(final ZookeeperClusterDiscoveryConnection connection, final Executor executor) {
        this.connection = connection;
        this.executor = executor;
    }
    
    @Override
    @SuppressWarnings({ "squid:S1166", "squid:S1188", "squid:S1141", "squid:S134" })
    public final void addTopologyListener(final String name, final ClusterTopologyChangeListener listener) {
        topologyQueue
            .exec(() -> {
                final ListenerSet<ClusterTopologyChangeListener> set = topologyChangeListeners
                    .computeIfAbsent(name, k -> new ListenerSet<>());
                if (set.add(listener)) {
                    final ClusterTopology topology = registeredClusters.computeIfAbsent(name, clusterName -> {
                        // Watch child nodes in zookeeper for new / changed cluster nodes
                        final String path = getClusterZnodePath(clusterName);
                        connection.watchNode(path, clusterCacheListener);
                        
                        try {
                            final CuratorFramework zookeeperClient = connection.getZookeeperClient();
                            final Stat stat = zookeeperClient.checkExists().forPath(path);
                            if (stat != null) {
                                final ClusterInfo clusterInfo = decode(
                                    ClusterInfo.class, zookeeperClient.getData().forPath(path));
                                if (clusterInfo != null) {
                                    return getClusterNodesAndWatch(zookeeperClient, path, clusterInfo);
                                } else {
                                    return new ClusterTopology(ClusterShardResolver.DEFAULT);
                                }
                            } else {
                                return new ClusterTopology(ClusterShardResolver.DEFAULT);
                            }
                        } catch (final NoNodeException e) {
                            return new ClusterTopology(ClusterShardResolver.DEFAULT);
                        } catch (final Exception e) {
                            throw new IllegalStateException(
                                String.format("Unable to read cluster data for [%s]", name), e);
                        }
                    });
                    try {
                        listener.onTopologyChanged(topology);
                    } catch (final RuntimeException e) {
                        LOG.error("listener threw exception", e);
                    }
                }
            })
            .onException(t -> LOG.error("Error in addTopology()", t));
    }
    
    @SuppressWarnings("squid:S1166")
    private ClusterTopology getClusterNodesAndWatch(final CuratorFramework zookeeperClient,
                                                    final String path,
                                                    final ClusterInfo clusterInfo) throws Exception {
        connection.watchPath(path, clusterNodesCacheListener);
        
        final List<String> registeredNodesForServiceType = zookeeperClient.getChildren().forPath(path);
        
        int leaderNodeId = UNKNOWN_NODE_ID;
        long oldestNodeTimestamp = Integer.MAX_VALUE;
        final SortedMap<Integer, ClusterNodeInfo> nodes = new TreeMap<>();
        for (final String nodeName : registeredNodesForServiceType) {
            final String childPath = ZKPaths.makePath(path, nodeName);
            int id = nodeNameToId(nodeName);
            try {
                final NodeInfo nodeInfo = decode(NodeInfo.class, zookeeperClient.getData().forPath(childPath));
                if ((leaderNodeId == UNKNOWN_NODE_ID)
                    || (nodeInfo.getTimestamp() < oldestNodeTimestamp)
                    || ((nodeInfo.getTimestamp() == oldestNodeTimestamp) && id < leaderNodeId)) {
                    oldestNodeTimestamp = nodeInfo.getTimestamp();
                    leaderNodeId = id;
                }
                nodes.put(
                    id,
                    new ClusterNodeInfo(
                        nodeInfo.getTimestamp(),
                        BitSet.of(clusterInfo.getMaxShards(), nodeInfo.getShardList()),
                        BitSet.empty(),
                        ImmutableMap.copyOf(nodeInfo.getAttributeMap())));
            } catch (final NoNodeException e) {
                // ignore
            }
        }
        return new ClusterTopology(nodes, leaderNodeId, new ModClusterShardResolver(clusterInfo.getMaxShards()));
    }
    
    @Override
    public final void removeTopologyListener(final String name, final ClusterTopologyChangeListener listener) {
        topologyQueue
            .exec(() -> {
                final ListenerSet<ClusterTopologyChangeListener> set = topologyChangeListeners.get(name);
                if (set != null) {
                    set.remove(listener);
                    if (set.isEmpty()) {
                        final String path = getClusterZnodePath(name);
                        connection.unwatchPath(path, clusterNodesCacheListener);
                        connection.unwatchNode(path, clusterCacheListener);
                        topologyChangeListeners.remove(name);
                        registeredClusters.remove(name);
                    }
                }
            })
            .onException(t -> LOG.error("Error in removeTopologyListener()", t));
    }
    
    @SuppressWarnings("squid:S1172")
    private void handleClusterNodesUpdated(final CuratorFramework client, final PathChildrenCacheEvent event) {
        if (event.getData() == null) {
            LOG.warn("Event with no data will be ignored: {}", event);
            return;
        }
        
        // path pattern is {base_path}/clusters/{CLUSTER_NAME}/{NODE_ID}
        final String[] pathParts = event.getData().getPath().split(ZKPaths.PATH_SEPARATOR, -1);
        final int nodeId = nodeNameToId(pathParts[pathParts.length - 1]);
        final String clusterName = pathParts[pathParts.length - 2];
        
        // Note that Connection events do not need to be handled explicitly since they are handled within
        // PathChildrenCache
        if ((event.getType() == PathChildrenCacheEvent.Type.CHILD_ADDED)
            || (event.getType() == PathChildrenCacheEvent.Type.CHILD_UPDATED)) {
            final NodeInfo nodeInfo = decode(NodeInfo.class, event.getData().getData());
            LOG.debug("Received zookeeper event of type [{}] for path [{}] with data {}",
                event.getType(),
                event.getData().getPath(),
                nodeId);
            handleClusterNodeChanged(clusterName, nodeId, nodeInfo);
            
        } else if (event.getType() == PathChildrenCacheEvent.Type.CHILD_REMOVED) {
            LOG.debug("Received zookeeper event of type [{}] for path [{}]", event.getType(), event.getData().getPath());
            handleClusterNodeRemoved(clusterName, nodeId);
            
        } else {
            LOG.debug("Received unsupported event type [{}] for path [{}]", event.getType(), event.getData().getPath());
        }
    }
    
    @SuppressWarnings("squid:S134")
    private void handleClusterNodeChanged(final String clusterName, final int nodeId, final NodeInfo updatedNodeInfo) {
        executor.execute(() -> topologyQueue
            .exec(() -> {
                // Try to retrieve service topology and subsequent required info, and initialise with default empty
                // values when not available
                final ClusterTopology lastTopology = registeredClusters.get(clusterName);
                if (lastTopology != null) {
                    // Update node attributes by Inserting/Updating map of nodes for the specified service
                    // clusterName-key
                    final SortedMap<Integer, ClusterNodeInfo> updatedNodes = new TreeMap<>(lastTopology.getNodes());
                    final ClusterNodeInfo prevNodeInfo = updatedNodes.put(nodeId, new ClusterNodeInfo(updatedNodeInfo.getTimestamp(),
                        BitSet.of(
                            lastTopology.getShardResolver().getMaxShards(), updatedNodeInfo.getShardList()),
                        BitSet.empty(),
                        ImmutableMap.copyOf(updatedNodeInfo.getAttributeMap())));
                    
                    int leaderNodeId = UNKNOWN_NODE_ID;
                    if ((prevNodeInfo != null) && (prevNodeInfo.getTimestamp() == updatedNodeInfo.getTimestamp())) {
                        leaderNodeId = lastTopology.getLeaderNodeId();
                    } else {
                        long oldestNodeTimestamp = Integer.MAX_VALUE;
                        for (final Entry<Integer, ClusterNodeInfo> entry : updatedNodes.entrySet()) {
                            final int id = entry.getKey();
                            final ClusterNodeInfo nodeInfo = entry.getValue();
                            if ((leaderNodeId == UNKNOWN_NODE_ID)
                                || (nodeInfo.getTimestamp() < oldestNodeTimestamp)
                                || ((nodeInfo.getTimestamp() == oldestNodeTimestamp) && id < leaderNodeId)) {
                                oldestNodeTimestamp = nodeInfo.getTimestamp();
                                leaderNodeId = id;
                            }
                        }
                    }
                    
                    final ClusterTopology updatedTopology = new ClusterTopology(updatedNodes, leaderNodeId, lastTopology.getShardResolver());
                    registeredClusters.put(clusterName, updatedTopology);
                    topologyChangeListeners.get(clusterName).publish(l -> l.onTopologyChanged(updatedTopology));
                }
            })
            .onException(t -> LOG.error("Error in handleClusterNodeChanged()", t)));
    }
    
    private void handleClusterNodeRemoved(final String clusterName, final int removedNodeId) {
        executor.execute(() -> topologyQueue
            .exec(() -> {
                // Try to retrieve service topology and subsequent required info, and initialise with default empty
                // values when not available
                final ClusterTopology lastTopology = registeredClusters.get(clusterName);
                if ((lastTopology != null) && lastTopology.getNodes().containsKey(removedNodeId)) {
                    final SortedMap<Integer, ClusterNodeInfo> updatedNodes = new TreeMap<>(lastTopology.getNodes());
                    updatedNodes.remove(removedNodeId);
                    
                    int leaderNodeId = UNKNOWN_NODE_ID;
                    long oldestNodeTimestamp = Integer.MAX_VALUE;
                    for (final Entry<Integer, ClusterNodeInfo> entry : updatedNodes.entrySet()) {
                        final int id = entry.getKey();
                        final ClusterNodeInfo nodeInfo = entry.getValue();
                        if ((leaderNodeId == UNKNOWN_NODE_ID)
                            || (nodeInfo.getTimestamp() < oldestNodeTimestamp)
                            || ((nodeInfo.getTimestamp() == oldestNodeTimestamp) && id < leaderNodeId)) {
                            oldestNodeTimestamp = nodeInfo.getTimestamp();
                            leaderNodeId = id;
                        }
                    }
                    
                    final ClusterTopology updatedTopology = new ClusterTopology(updatedNodes, leaderNodeId, lastTopology.getShardResolver());
                    registeredClusters.put(clusterName, updatedTopology);
                    topologyChangeListeners.get(clusterName).publish(l -> l.onTopologyChanged(updatedTopology));
                }
            })
            .onException(t -> LOG.error("Error in handleClusterNodeRemoved()", t)));
    }
    
    @SuppressWarnings("squid:S1172")
    private void handleClusterUpdated(final CuratorFramework client, final PathNodeCacheEvent event) {
        if (event.getData() == null) {
            LOG.warn("Event with no data will be ignored: {}", event);
            return;
        }
        
        // path is something like {base_path}/clusters/{CLUSTER_NAME}
        final String[] pathParts = event.getData().getPath().split(ZKPaths.PATH_SEPARATOR, -1);
        final String clusterName = pathParts[pathParts.length - 1];
        
        // Note that Connection events do not need to be handled explicitly since they are handled within
        // PathChildrenCache
        if (event.getType() == PathNodeCacheEvent.Type.NODE_UPDATED) {
            final ClusterInfo clusterInfo = decode(ClusterInfo.class, event.getData().getData());
            LOG.debug("Received zookeeper event of type [{}] for path [{}] with data {}",
                event.getType(),
                event.getData().getPath(),
                clusterInfo);
            handleClusterChanged(clusterName, clusterInfo);
            
        } else if (event.getType() == PathNodeCacheEvent.Type.NODE_REMOVED) {
            LOG.debug("Received zookeeper event of type [{}] for path [{}]", event.getType(), event.getData().getPath());
            handleClusterRemoved(clusterName);
            
        } else {
            LOG.debug("Received unsupported event type [{}] for path [{}]", event.getType(), event.getData().getPath());
        }
    }
    
    private void handleClusterChanged(final String clusterName, final ClusterInfo clusterInfo) {
        executor.execute(() -> topologyQueue
            .exec(() -> {
                final ClusterTopology topology = registeredClusters.get(clusterName);
                final String path = getClusterZnodePath(clusterName);
                if (topology.getShardResolver() == ClusterShardResolver.DEFAULT) {
                    if (clusterInfo != null) {
                        final ClusterTopology updatedTopology = getClusterNodesAndWatch(connection.getZookeeperClient(), path, clusterInfo);
                        registeredClusters.put(clusterName, updatedTopology);
                        topologyChangeListeners.get(clusterName).publish(l -> l.onTopologyChanged(updatedTopology));
                    }
                } else if (clusterInfo == null) {
                    connection.unwatchPath(path, clusterNodesCacheListener);
                    final ClusterTopology updatedTopology = new ClusterTopology(ClusterShardResolver.DEFAULT);
                    registeredClusters.put(clusterName, updatedTopology);
                    topologyChangeListeners.get(clusterName).publish(l -> l.onTopologyChanged(updatedTopology));
                } else {
                    final ClusterTopology updatedTopology = new ClusterTopology(topology.getNodes(),
                        topology.getLeaderNodeId(),
                        new ModClusterShardResolver(clusterInfo.getMaxShards()));
                    registeredClusters.put(clusterName, updatedTopology);
                    topologyChangeListeners.get(clusterName).publish(l -> l.onTopologyChanged(updatedTopology));
                }
            })
            .onException(t -> LOG.error("Error in handleClusterChanged()", t)));
    }
    
    private void handleClusterRemoved(final String clusterName) {
        executor.execute(() -> topologyQueue
            .exec(() -> {
                final ClusterTopology topology = registeredClusters.get(clusterName);
                final String path = getClusterZnodePath(clusterName);
                if (topology.getShardResolver() != ClusterShardResolver.DEFAULT) {
                    connection.unwatchPath(path, clusterNodesCacheListener);
                    final ClusterTopology updatedTopology = new ClusterTopology(ClusterShardResolver.DEFAULT);
                    registeredClusters.put(clusterName, updatedTopology);
                    topologyChangeListeners.get(clusterName).publish(l -> l.onTopologyChanged(updatedTopology));
                }
            })
            .onException(t -> LOG.error("Error in handleClusterRemoved()", t)));
    }
    
}
