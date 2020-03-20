package com.ixaris.commons.clustering.lib.extra;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.SortedMap;
import java.util.TreeMap;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.clustering.lib.CommonsClusteringLib.ClusterBroadcastEnvelope;
import com.ixaris.commons.clustering.lib.client.ClusterDiscovery;
import com.ixaris.commons.clustering.lib.common.ClusterNodeInfo;
import com.ixaris.commons.clustering.lib.common.ClusterSequence;
import com.ixaris.commons.clustering.lib.common.ClusterTopology;
import com.ixaris.commons.clustering.lib.common.ClusterTopologyChangeListener;
import com.ixaris.commons.clustering.lib.common.ModClusterShardResolver;
import com.ixaris.commons.clustering.lib.service.AbstractClusterRegistry;
import com.ixaris.commons.clustering.lib.service.ClusterDispatchFilterFactory;
import com.ixaris.commons.clustering.lib.service.ClusterHandleFilterFactory;
import com.ixaris.commons.clustering.lib.service.ClusterLeadershipChangeListener;
import com.ixaris.commons.clustering.lib.service.ClusterRouteHandler;
import com.ixaris.commons.clustering.lib.service.ClusterShardsChangeListener;
import com.ixaris.commons.collections.lib.BitSet;
import com.ixaris.commons.collections.lib.GuavaCollections;
import com.ixaris.commons.collections.lib.ListenerSet;
import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.misc.lib.id.Sequence;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;
import com.ixaris.commons.misc.lib.id.UpdateableSequence;

/**
 * Since the {@link LocalCluster} is intended to be used locally, by it assumes a single node and all operations are
 * executed in the context of this single node.
 *
 * @author brian.vella
 * @author aldrin.seychell
 */
public class LocalCluster extends AbstractClusterRegistry implements ClusterDiscovery, ClusterSequence {
    
    private static final int DEFAULT_NODE_ID = 1;
    private static final String DEFAULT_CLUSTER_NAME = "local";
    public static final int MAX_SHARDS = 72;
    
    private final Object lock = new Object();
    
    private final Map<String, ListenerSet<ClusterTopologyChangeListener>> discoveryTopologyListeners = new HashMap<>();
    private final ListenerSet<ClusterLeadershipChangeListener> registryLeadershipListeners = new ListenerSet<>();
    private final ListenerSet<ClusterShardsChangeListener> registryShardsListeners = new ListenerSet<>();
    private final Map<String, UpdateableSequence> sequences = new HashMap<>();
    
    protected final String clusterName;
    protected final int nodeId;
    private volatile ClusterNodeInfo nodeInfo;
    private volatile ClusterTopology topology;
    
    public LocalCluster(final Set<? extends ClusterDispatchFilterFactory> dispatchFilterFactories,
                        final Set<? extends ClusterHandleFilterFactory> handleFilterFactories) {
        this(DEFAULT_CLUSTER_NAME, DEFAULT_NODE_ID, dispatchFilterFactories, handleFilterFactories);
    }
    
    public LocalCluster(final String clusterName,
                        final int nodeId,
                        final Set<? extends ClusterDispatchFilterFactory> dispatchFilterFactories,
                        final Set<? extends ClusterHandleFilterFactory> handleFilterFactories) {
        super(dispatchFilterFactories, handleFilterFactories);
        this.clusterName = clusterName;
        this.nodeId = nodeId;
        topology = new ClusterTopology(this);
        final BitSet shards = BitSet.of(MAX_SHARDS);
        for (int i = 0; i < MAX_SHARDS; i++) {
            shards.add(i);
        }
        nodeInfo = new ClusterNodeInfo(System.currentTimeMillis(), shards, null, null);
        updateTopology();
    }
    
    private void updateTopology() {
        final SortedMap<Integer, ClusterNodeInfo> nodes = new TreeMap<>();
        nodes.put(nodeId, nodeInfo);
        topology = new ClusterTopology(nodes, nodeId, this);
    }
    
    @Override
    public void addTopologyListener(final String name, final ClusterTopologyChangeListener listener) {
        synchronized (lock) {
            final ListenerSet<ClusterTopologyChangeListener> set = discoveryTopologyListeners.computeIfAbsent(name, k -> new ListenerSet<>());
            if (set.add(listener)) {
                listener.onTopologyChanged(name.equals(clusterName) ? topology : new ClusterTopology(this));
            }
        }
    }
    
    @Override
    public void removeTopologyListener(final String name, final ClusterTopologyChangeListener listener) {
        synchronized (lock) {
            final ListenerSet<ClusterTopologyChangeListener> set = discoveryTopologyListeners.get(name);
            if (set != null) {
                set.remove(listener);
                if (set.isEmpty()) {
                    discoveryTopologyListeners.remove(name);
                }
            }
        }
    }
    
    @Override
    public int getNodeId() {
        return nodeId;
    }
    
    @Override
    public ClusterNodeInfo getNodeInfo() {
        return nodeInfo;
    }
    
    @Override
    public boolean isLeader() {
        return true;
    }
    
    @Override
    public ClusterTopology getTopology() {
        return topology;
    }
    
    @Override
    public void mergeAttributes(final Map<String, String> toMerge) {
        synchronized (lock) {
            nodeInfo = new ClusterNodeInfo(
                nodeInfo.getTimestamp(),
                nodeInfo.getShards(),
                nodeInfo.getShardsStopping(),
                GuavaCollections.copyOfMapAdding(nodeInfo.getAttributes(), toMerge));
            updateTopology();
        }
    }
    
    @Override
    public void addLeadershipListener(final ClusterLeadershipChangeListener listener) {
        synchronized (lock) {
            if (registryLeadershipListeners.add(listener)) {
                listener.onLeadershipChanged(true);
            }
        }
    }
    
    @Override
    public void removeLeadershipListener(final ClusterLeadershipChangeListener listener) {
        synchronized (lock) {
            registryLeadershipListeners.remove(listener);
        }
    }
    
    @Override
    public int getMaxShards() {
        return MAX_SHARDS;
    }
    
    @Override
    public int getShard(final long id) {
        return ModClusterShardResolver.getShard(id, MAX_SHARDS);
    }
    
    @Override
    public <T, E extends Exception> Async<T> forShard(final int shard, final CallableThrows<Async<T>, E> callable) throws E {
        return SHARD.exec(shard, callable);
    }
    
    @Override
    protected <REQ extends MessageLite, RES extends MessageLite> Async<RES> route(final ClusterRouteHandler<REQ, RES> handler,
                                                                                  final long id,
                                                                                  final String key,
                                                                                  final REQ request) {
        return forShard(0, () -> handler.handle(id, key, request));
    }
    
    @Override
    public Async<Boolean> broadcast(final ClusterBroadcastEnvelope message) {
        return handleBroadcastChain.next(message);
    }
    
    @Override
    public void addShardsListener(final ClusterShardsChangeListener listener) {
        synchronized (lock) {
            if (registryShardsListeners.add(listener)) {
                listener.onShardsChanged(BitSet.empty(), nodeInfo.getShards());
            }
        }
    }
    
    @Override
    public void removeShardsListener(final ClusterShardsChangeListener listener) {
        synchronized (lock) {
            registryShardsListeners.remove(listener);
        }
    }
    
    @Override
    public Sequence getSequence(final String name) {
        synchronized (sequences) {
            return sequences.computeIfAbsent(name, k -> {
                final UpdateableSequence sequence = ClusterSequence.DEFAULT.equals(name)
                    ? UniqueIdGenerator.DEFAULT_SEQUENCE : new UpdateableSequence();
                sequence.setNodeId(1, sequence.getNodeWidth());
                return sequence;
            });
        }
    }
    
}
