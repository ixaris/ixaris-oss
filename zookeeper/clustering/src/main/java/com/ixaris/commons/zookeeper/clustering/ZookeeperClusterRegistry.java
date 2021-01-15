package com.ixaris.commons.zookeeper.clustering;

import static com.ixaris.commons.async.lib.CompletionStageUtil.join;
import static com.ixaris.commons.clustering.lib.common.ClusterTopology.UNKNOWN_NODE_ID;
import static com.ixaris.commons.zookeeper.clustering.ZookeeperClusterDiscoveryConnection.decode;
import static com.ixaris.commons.zookeeper.clustering.ZookeeperClusterDiscoveryConnection.encodeClusterInfo;
import static com.ixaris.commons.zookeeper.clustering.ZookeeperClusterDiscoveryConnection.encodeNodeInfo;
import static com.ixaris.commons.zookeeper.clustering.ZookeeperClusterDiscoveryConnection.nodeIdToName;
import static com.ixaris.commons.zookeeper.clustering.ZookeeperClusterDiscoveryConnection.nodeNameToId;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;
import java.util.concurrent.Executor;
import java.util.concurrent.atomic.AtomicBoolean;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.api.transaction.CuratorOp;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheEvent;
import org.apache.curator.framework.recipes.cache.PathChildrenCacheListener;
import org.apache.curator.framework.state.ConnectionState;
import org.apache.curator.framework.state.ConnectionStateListener;
import org.apache.curator.utils.ZKPaths;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.KeeperException.NoNodeException;
import org.apache.zookeeper.KeeperException.NodeExistsException;
import org.apache.zookeeper.data.Stat;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.AsyncQueue;
import com.ixaris.commons.clustering.lib.CommonsClusteringLib.ClusterBroadcastEnvelope;
import com.ixaris.commons.clustering.lib.common.ClusterNodeInfo;
import com.ixaris.commons.clustering.lib.common.ClusterShardResolver;
import com.ixaris.commons.clustering.lib.common.ClusterTopology;
import com.ixaris.commons.clustering.lib.common.ModClusterShardResolver;
import com.ixaris.commons.clustering.lib.service.AbstractClusterRegistry;
import com.ixaris.commons.clustering.lib.service.ClusterDispatchFilterFactory;
import com.ixaris.commons.clustering.lib.service.ClusterHandleFilterFactory;
import com.ixaris.commons.clustering.lib.service.ClusterLeadershipChangeListener;
import com.ixaris.commons.clustering.lib.service.ClusterRouteHandler;
import com.ixaris.commons.clustering.lib.service.ClusterRouteTimeoutException;
import com.ixaris.commons.clustering.lib.service.ClusterShardsChangeListener;
import com.ixaris.commons.clustering.lib.service.ShardAllocationStrategy;
import com.ixaris.commons.clustering.lib.service.ShardNotLocalException;
import com.ixaris.commons.clustering.lib.service.ShardRebalance;
import com.ixaris.commons.collections.lib.BitSet;
import com.ixaris.commons.collections.lib.BitSetIterator;
import com.ixaris.commons.collections.lib.GuavaCollections;
import com.ixaris.commons.collections.lib.ListenerSet;
import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.zookeeper.clustering.CommonsZookeeperClustering.NodeInfo;

/**
 * This class uses Zookeeper to maintain a Registry for each cluster. Each cluster node is responsible for
 * registering itself in the Registry so that it is discoverable.
 * <p>
 * Nodes are stored as ephemeral ZNodes i.e. they are removed when the node disconnects (cleanly or otherwise)
 * <p>
 * Since zookeeper is a remote service, the latest topology is cached locally (PathChildrenCache) such that nodes tolerate
 * temporary downtime in the connection with zookeeper and can still serve requests based on the latest cached information.
 */
public class ZookeeperClusterRegistry extends AbstractClusterRegistry {
    
    private static final Logger LOG = LoggerFactory.getLogger(ZookeeperClusterRegistry.class);
    
    protected final String clusterName;
    private final ZookeeperClusterDiscoveryConnection connection;
    private final ShardAllocationStrategy shardAllocationStrategy;
    private final Executor executor;
    private final ClusterShardResolver shardResolver;
    private final ZookeeperClusterRegistryHelper helper;
    
    private volatile boolean isLeader = false;
    private final ListenerSet<ClusterLeadershipChangeListener> leadershipChangeListeners = new ListenerSet<>();
    private final ListenerSet<ClusterShardsChangeListener> shardsChangeListeners = new ListenerSet<>();
    
    private final PathChildrenCacheListener clusterNodesCacheListener = this::handleChildrenUpdate;
    private final ConnectionStateListener connectionStateListener = this::handleConnectionStateChanged;
    private final AsyncQueue queue = new AsyncQueue();
    
    private final AtomicBoolean registered = new AtomicBoolean();
    private volatile ClusterNodeInfo localNodeInfo;
    private volatile ClusterTopology clusterTopology;
    private final SortedMap<Integer, ClusterNodeInfo> nodesWithUpdatedShards = new TreeMap<>();
    
    public ZookeeperClusterRegistry(final ZookeeperClusterDiscoveryConnection connection,
                                    final ShardAllocationStrategy shardAllocationStrategy,
                                    final Executor executor,
                                    final ZookeeperClusterRegistryHelperFactory helperFactory,
                                    final Set<? extends ClusterDispatchFilterFactory> dispatchFilterFactories,
                                    final Set<? extends ClusterHandleFilterFactory> handleFilterFactories) {
        super(dispatchFilterFactories, handleFilterFactories);
        
        if (connection == null) {
            throw new IllegalArgumentException("connection is null");
        }
        if (shardAllocationStrategy == null) {
            throw new IllegalArgumentException("shardAllocationStrategy is null");
        }
        if (executor == null) {
            throw new IllegalArgumentException("executor is null");
        }
        if (helperFactory == null) {
            throw new IllegalArgumentException("helperFactory is null");
        }
        
        this.clusterName = connection.getClusterName();
        this.connection = connection;
        this.shardAllocationStrategy = shardAllocationStrategy;
        this.executor = executor;
        
        shardResolver = new ModClusterShardResolver(shardAllocationStrategy.getMaxShards());
        localNodeInfo = new ClusterNodeInfo(System.currentTimeMillis(),
            BitSet.of(shardResolver.getMaxShards()),
            BitSet.of(shardResolver.getMaxShards()),
            null);
        final SortedMap<Integer, ClusterNodeInfo> nodes = new TreeMap<>();
        nodes.put(connection.getNodeId(), localNodeInfo);
        clusterTopology = new ClusterTopology(nodes, UNKNOWN_NODE_ID, shardResolver);
        helper = helperFactory.create(this, connection.getNodeId(), this::handleShardStopped, handleRouteChain, handleBroadcastChain);
        connection.getZookeeperClient().getConnectionStateListenable().addListener(connectionStateListener);
    }
    
    @Override
    public final ClusterNodeInfo getNodeInfo() {
        return localNodeInfo;
    }
    
    @Override
    public int getNodeId() {
        return connection.getNodeId();
    }
    
    @Override
    public final ClusterTopology getTopology() {
        return clusterTopology;
    }
    
    @Override
    public final int getMaxShards() {
        return shardResolver.getMaxShards();
    }
    
    @Override
    public final int getShard(final long id) {
        return shardResolver.getShard(id);
    }
    
    @Override
    public final boolean isLeader() {
        return isLeader;
    }
    
    @SuppressWarnings("squid:S134")
    private boolean registerNode() {
        if (registered.compareAndSet(false, true)) {
            try {
                createClusterIfNeeded(clusterName);
                
                final int nodeId = connection.getNodeId();
                final String nodeIdPath = ZookeeperClusterDiscoveryConnection.getClusterNodeZnodePath(clusterName, nodeIdToName(nodeId));
                
                final Stat stat = connection.getZookeeperClient().checkExists().forPath(nodeIdPath);
                if ((stat != null) && !connection.ownsEphemeralNode(stat)) {
                    if (connection.ownedEphemeralNode(stat)) {
                        connection.getZookeeperClient().delete().forPath(nodeIdPath);
                    } else {
                        return false;
                    }
                }
                
                // Create an ephemeral node in zookeeper with the nodeId sub-node of the service znodeBasePath
                connection
                    .getZookeeperClient()
                    .create()
                    .withMode(CreateMode.EPHEMERAL)
                    .forPath(nodeIdPath, encodeNodeInfo(localNodeInfo));
                
                nodesWithUpdatedShards.clear();
                
                final SortedMap<Integer, ClusterNodeInfo> nodes = getClusterNodes(clusterName);
                nodes.put(nodeId, localNodeInfo);
                
                updateTopology(new ClusterTopology(nodes, determineLeaderNodeId(nodes), shardResolver));
                
                // Watch child nodes in zookeeper for new / changed cluster nodes
                final String path = ZookeeperClusterDiscoveryConnection.getClusterZnodePath(clusterName);
                connection.watchPath(path, clusterNodesCacheListener);
                return true;
            } catch (final IllegalStateException e) {
                throw e;
            } catch (final Exception e) {
                throw new IllegalStateException(String.format("Unable to add/update node of type [%s] in service registry", clusterName), e);
            }
        } else {
            LOG.warn("Node is already registered for {}", clusterName);
            return true;
        }
    }
    
    private int determineLeaderNodeId(final SortedMap<Integer, ClusterNodeInfo> nodes) {
        int leaderNodeId = UNKNOWN_NODE_ID;
        long oldestNodeTimestamp = Integer.MAX_VALUE;
        for (final Entry<Integer, ClusterNodeInfo> entry : nodes.entrySet()) {
            final int id = entry.getKey();
            final ClusterNodeInfo nodeInfo = entry.getValue();
            if ((leaderNodeId == UNKNOWN_NODE_ID) || (nodeInfo.getTimestamp() < oldestNodeTimestamp) || ((nodeInfo.getTimestamp() == oldestNodeTimestamp) && id < leaderNodeId)) {
                oldestNodeTimestamp = nodeInfo.getTimestamp();
                leaderNodeId = id;
            }
        }
        return leaderNodeId;
    }
    
    private void updateTopology(final ClusterTopology updatedTopology) {
        clusterTopology = updatedTopology;
        
        final ClusterNodeInfo oldNodeInfo = localNodeInfo;
        localNodeInfo = clusterTopology.getNodes().get(connection.getNodeId());
        // TODO check that timestamp and attributes were not changed
        if (!oldNodeInfo.getShards().equals(localNodeInfo.getShards())) {
            LOG.info("Node {} shards: ({}) {}", connection.getNodeId(), localNodeInfo.getShards().size(), localNodeInfo.getShards());
            shardsChangeListeners.publish(l -> l.onShardsChanged(oldNodeInfo.getShards(), localNodeInfo.getShards()));
        }
        
        final boolean nowIsLeader = clusterTopology.getLeaderNodeId() == connection.getNodeId();
        if (nowIsLeader != isLeader) {
            isLeader = nowIsLeader;
            LOG.info("Node {} is {} leader", connection.getNodeId(), isLeader ? "" : "not ");
            leadershipChangeListeners.publish(l -> l.onLeadershipChanged(isLeader));
            if (isLeader) {
                nodesWithUpdatedShards.putAll(clusterTopology.getNodes());
            }
        }
        helper.updateTopology(clusterTopology);
        if (isLeader && registered.get()) {
            rebalanceTopology();
        } else {
            nodesWithUpdatedShards.clear();
        }
    }
    
    private void rebalanceTopology() {
        final ShardRebalance rebalance = shardAllocationStrategy.rebalance(nodesWithUpdatedShards);
        if (!rebalance.isEmpty()) {
            final SortedMap<Integer, ClusterNodeInfo> updatedNodes = new TreeMap<>();
            
            for (final Entry<Integer, ClusterNodeInfo> entry : nodesWithUpdatedShards.entrySet()) {
                final BitSet shardsToStop = rebalance.getToStop().get(entry.getKey());
                final BitSet shardsToStart = rebalance.getToStart().get(entry.getKey());
                
                if ((shardsToStop != null) || (shardsToStart != null)) {
                    updatedNodes.put(entry.getKey(), rebalanceNode(entry.getKey(), entry.getValue(), shardsToStop, shardsToStart));
                }
            }
            
            try {
                final List<CuratorOp> operations = new ArrayList<>(updatedNodes.size());
                for (final Entry<Integer, ClusterNodeInfo> entry : updatedNodes.entrySet()) {
                    operations.add(connection
                        .getZookeeperClient()
                        .transactionOp()
                        .setData()
                        .forPath(ZookeeperClusterDiscoveryConnection.getClusterNodeZnodePath(clusterName, nodeIdToName(entry.getKey())), encodeNodeInfo(entry.getValue())));
                }
                connection.getZookeeperClient().transaction().forOperations(operations);
            } catch (final Exception e) {
                throw new IllegalStateException("Unable to update topology", e);
            }
            
            nodesWithUpdatedShards.putAll(updatedNodes);
        }
    }
    
    private ClusterNodeInfo rebalanceNode(final int nodeId, final ClusterNodeInfo nodeInfo, final BitSet shardsToStop, final BitSet shardsToStart) {
        final BitSet newShards = BitSet.copy(nodeInfo.getShards());
        final BitSet newShardsStopping = BitSet.copy(nodeInfo.getShardsStopping());
        
        if (shardsToStop != null) {
            final BitSetIterator it = shardsToStop.iterator();
            while (it.hasNext()) {
                final int toStop = it.next();
                if (newShards.remove(toStop)) {
                    newShardsStopping.add(toStop);
                } else {
                    throw new IllegalStateException("Unable to remove " + toStop + " from " + nodeId);
                }
            }
        }
        
        if (shardsToStart != null) {
            final BitSetIterator it = shardsToStart.iterator();
            while (it.hasNext()) {
                final int toStart = it.next();
                // just in case starting a shard that is being stopped on the same node
                // (given priority over taking shards from other nodes)
                newShardsStopping.remove(toStart);
                newShards.add(toStart);
            }
        }
        
        return new ClusterNodeInfo(nodeInfo.getTimestamp(), newShards, newShardsStopping, nodeInfo.getAttributes());
    }
    
    private Async<Void> handleShardStopped(final int nodeId, final int shard) {
        return queue.exec(() -> {
            if (isLeader && registered.get()) {
                final ClusterNodeInfo stoppedShardNodeInfo = nodesWithUpdatedShards.get(nodeId);
                if (!stoppedShardNodeInfo.getShardsStopping().contains(shard)) {
                    // already processed or out of date, ignore
                    return;
                }
                final BitSet newShardsStopping = BitSet.copy(stoppedShardNodeInfo.getShardsStopping());
                newShardsStopping.remove(shard);
                final ClusterNodeInfo updatedStoppedShardNodeInfo = new ClusterNodeInfo(stoppedShardNodeInfo.getTimestamp(),
                    stoppedShardNodeInfo.getShards(),
                    newShardsStopping,
                    stoppedShardNodeInfo.getAttributes());
                try {
                    connection
                        .getZookeeperClient()
                        .setData()
                        .forPath(ZookeeperClusterDiscoveryConnection.getClusterNodeZnodePath(clusterName, nodeIdToName(nodeId)), encodeNodeInfo(updatedStoppedShardNodeInfo));
                } catch (final Exception e) {
                    LOG.error("Error in handleShardStopped()", e);
                    throw new IllegalStateException("Unable to update topology", e);
                }
                
                nodesWithUpdatedShards.put(nodeId, updatedStoppedShardNodeInfo);
            } else {
                throw new IllegalStateException("Not leader or unregistered");
            }
        });
    }
    
    public void start() {
        join(queue.exec(this::registerNode));
    }
    
    public void shutdown() {
        LOG.debug("Initialising shutdown of service registry for node [{}]", localNodeInfo);
        join(queue.exec(this::deregisterNode));
        connection.getZookeeperClient().getConnectionStateListenable().removeListener(connectionStateListener);
        helper.shutdown();
    }
    
    private void deregisterNode() {
        if (registered.compareAndSet(true, false)) {
            final String path = ZookeeperClusterDiscoveryConnection.getClusterZnodePath(clusterName);
            connection.unwatchPath(path, clusterNodesCacheListener);
            
            final int id = connection.getNodeId();
            if (connection.getZookeeperClient().getZookeeperClient().isConnected()) {
                try {
                    connection
                        .getZookeeperClient()
                        .delete()
                        .forPath(ZookeeperClusterDiscoveryConnection.getClusterNodeZnodePath(clusterName, nodeIdToName(id)));
                } catch (final Exception e) {
                    LOG.warn("Unable to delete node during deregistration. Continuing deregistration", e);
                }
            }
        }
    }
    
    @Override
    public void addLeadershipListener(final ClusterLeadershipChangeListener listener) {
        queue.exec(() -> {
            if (leadershipChangeListeners.add(listener)) {
                try {
                    listener.onLeadershipChanged(isLeader);
                } catch (final RuntimeException e) {
                    LOG.error("listener threw exception", e);
                }
            }
        });
    }
    
    @Override
    public final void removeLeadershipListener(final ClusterLeadershipChangeListener listener) {
        leadershipChangeListeners.remove(listener);
    }
    
    @Override
    public final void addShardsListener(final ClusterShardsChangeListener listener) {
        queue.exec(() -> {
            if (shardsChangeListeners.add(listener)) {
                listener.onShardsChanged(BitSet.empty(), localNodeInfo.getShards());
            }
        });
    }
    
    @Override
    public final void removeShardsListener(final ClusterShardsChangeListener listener) {
        shardsChangeListeners.remove(listener);
    }
    
    @Override
    public final void mergeAttributes(final Map<String, String> toMerge) {
        if (registered.get()) {
            throw new IllegalStateException("Cannot modify attributes " + toMerge + " while registered");
        } else {
            join(queue.exec(() -> localNodeInfo = new ClusterNodeInfo(localNodeInfo.getTimestamp(),
                localNodeInfo.getShards(),
                localNodeInfo.getShardsStopping(),
                GuavaCollections.copyOfMapAdding(localNodeInfo.getAttributes(), toMerge))));
        }
    }
    
    @Override
    public final <T, E extends Exception> Async<T> forShard(final int shard, final CallableThrows<Async<T>, E> callable) throws E, ShardNotLocalException {
        return helper.forShard(shard, callable);
    }
    
    @Override
    protected final <REQ extends MessageLite, RES extends MessageLite> Async<RES> route(final ClusterRouteHandler<REQ, RES> handler,
                                                                                        final long id,
                                                                                        final String key,
                                                                                        final REQ request) throws ClusterRouteTimeoutException {
        return helper.route(handler, id, key, request, this::defaultRoute);
    }
    
    @Override
    public final Async<Boolean> broadcast(final ClusterBroadcastEnvelope message) {
        return helper.broadcast(message);
    }
    
    @SuppressWarnings({ "squid:S00108", "squid:S1166", "checkstyle:com.puppycrawl.tools.checkstyle.checks.blocks.EmptyCatchBlockCheck" })
    private void createClusterIfNeeded(final String clusterName) throws Exception {
        final String clusterPath = ZookeeperClusterDiscoveryConnection.getClusterZnodePath(clusterName);
        
        // Make sure that the required paths exists in ZK
        if (connection.getZookeeperClient().checkExists().forPath(clusterPath) == null) {
            try {
                connection
                    .getZookeeperClient()
                    .create()
                    .creatingParentsIfNeeded()
                    .forPath(clusterPath, encodeClusterInfo(shardResolver.getMaxShards()));
                // If there were no parent znodes in ZK, we can assume that there are no other services
            } catch (final NodeExistsException ignored) {}
            /*
             * TODO when needed, a way to change sharding strategy
             * Changing sharding strategy is dangerous as data could have been tagged already with the shard id.
             * for timestamp based ids, can use a cutoff date, where sharding strategy remains the same before cutoff
             */
        }
    }
    
    @SuppressWarnings({ "squid:S00108", "squid:S1166", "checkstyle:com.puppycrawl.tools.checkstyle.checks.blocks.EmptyCatchBlockCheck" })
    private SortedMap<Integer, ClusterNodeInfo> getClusterNodes(final String clusterName) throws Exception {
        final String clusterPath = ZookeeperClusterDiscoveryConnection.getClusterZnodePath(clusterName);
        final SortedMap<Integer, ClusterNodeInfo> nodes = new TreeMap<>();
        
        final List<String> registeredNodesForServiceType = connection
            .getZookeeperClient()
            .getChildren()
            .forPath(clusterPath);
        for (final String nodeName : registeredNodesForServiceType) {
            final String childPath = ZKPaths.makePath(clusterPath, nodeName);
            final int id = nodeNameToId(nodeName);
            try {
                final NodeInfo newNodeInfo = decode(NodeInfo.class, connection.getZookeeperClient().getData().forPath(childPath));
                nodes.put(id, new ClusterNodeInfo(newNodeInfo.getTimestamp(),
                    BitSet.of(shardResolver.getMaxShards(), newNodeInfo.getShardList()),
                    BitSet.of(shardResolver.getMaxShards(), newNodeInfo.getShardStoppingList()),
                    ImmutableMap.copyOf(newNodeInfo.getAttributeMap())));
            } catch (final NoNodeException e) {
                // ignore
            }
        }
        return nodes;
    }
    
    private void handleChildrenUpdate(final CuratorFramework client, final PathChildrenCacheEvent event) {
        if (event.getData() == null) {
            LOG.warn("Event with no data will be ignored: {}", event);
            return;
        }
        
        // path is something like {base_path}/clusters/{CLUSTER_NAME}/{NODE_ID}
        final String[] pathParts = event.getData().getPath().split(ZKPaths.PATH_SEPARATOR, -1);
        // Last part is the node id in ZK (should be a number)
        final int nodeId = nodeNameToId(pathParts[pathParts.length - 1]);
        
        if ((event.getType() == PathChildrenCacheEvent.Type.CHILD_ADDED)
            || (event.getType() == PathChildrenCacheEvent.Type.CHILD_UPDATED)) {
            final NodeInfo updatedNodeInfo = decode(NodeInfo.class, event.getData().getData());
            LOG.debug("Received zookeeper event of type [{}] for path [{}] with data {}", event.getType(), event.getData().getPath(), updatedNodeInfo);
            handleNodeAddedOrUpdated(nodeId, updatedNodeInfo);
            
        } else if (event.getType() == PathChildrenCacheEvent.Type.CHILD_REMOVED) {
            LOG.debug("Received zookeeper event of type [{}] for path [{}]", event.getType(), event.getData().getPath());
            handleNodeRemoved(nodeId);
        }
    }
    
    private void handleNodeAddedOrUpdated(final int nodeId, final NodeInfo updatedNodeInfo) {
        executor.execute(() -> queue
            .exec(() -> {
                final ClusterNodeInfo updatedNode = new ClusterNodeInfo(updatedNodeInfo.getTimestamp(),
                    BitSet.of(shardResolver.getMaxShards(), updatedNodeInfo.getShardList()),
                    BitSet.of(shardResolver.getMaxShards(), updatedNodeInfo.getShardStoppingList()),
                    ImmutableMap.copyOf(updatedNodeInfo.getAttributeMap()));
                
                if (nodeId == connection.getNodeId()) {
                    localNodeInfo = updatedNode;
                }
                
                if (isLeader) {
                    // leader holds the source of truth on shard allocation; it is important to ignore the intermediary
                    // events received from zookeeper and instead use the view maintained by the leader
                    nodesWithUpdatedShards.compute(nodeId, (k, v) -> {
                        if ((v == null) || (v.getTimestamp() != updatedNode.getTimestamp())) {
                            return updatedNode;
                        } else {
                            return new ClusterNodeInfo(updatedNodeInfo.getTimestamp(), v.getShards(), v.getShardsStopping(), ImmutableMap.copyOf(updatedNodeInfo.getAttributeMap()));
                        }
                    });
                }
                
                final SortedMap<Integer, ClusterNodeInfo> updatedTopologyNodes = new TreeMap<>(clusterTopology.getNodes());
                updatedTopologyNodes.put(nodeId, updatedNode);
                
                updateTopology(new ClusterTopology(updatedTopologyNodes, determineLeaderNodeId(updatedTopologyNodes), shardResolver));
            })
            .onException(t -> LOG.error("Error in handleNodeAddedOrUpdated()", t)));
    }
    
    private void handleNodeRemoved(final int nodeId) {
        executor.execute(() -> queue
            .exec(() -> {
                if (nodeId == connection.getNodeId()) {
                    connection.handleNodeRemoved();
                    return;
                }
                
                if (isLeader) {
                    nodesWithUpdatedShards.remove(nodeId);
                }
                
                final SortedMap<Integer, ClusterNodeInfo> updatedTopologyNodes = new TreeMap<>(clusterTopology.getNodes());
                updatedTopologyNodes.remove(nodeId);
                
                updateTopology(new ClusterTopology(updatedTopologyNodes, determineLeaderNodeId(updatedTopologyNodes), shardResolver));
            })
            .onException(t -> LOG.error("Error in handleNodeRemoved()", t)));
    }
    
    @SuppressWarnings("squid:S1172")
    private void handleConnectionStateChanged(final CuratorFramework client, final ConnectionState state) {
        executor.execute(() -> queue
            .exec(() -> {
                if (state.isConnected()) {
                    LOG.info("Connection to Zookeeper changed to [{}]! (Re-)Registering node", state);
                    if (!registerNode()) {
                        connection.handleNodeRemoved();
                    }
                } else {
                    LOG.info("Connection to Zookeeper Lost! De-registering node.");
                    deregisterNode();
                }
            })
            .onException(t -> LOG.error("Error in handleConnectionStateChanged()", t)));
    }
    
}
