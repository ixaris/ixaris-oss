package com.ixaris.commons.clustering.lib.service;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.SortedMap;

import com.ixaris.commons.clustering.lib.common.ClusterNodeInfo;
import com.ixaris.commons.collections.lib.BitSet;
import com.ixaris.commons.collections.lib.BitSetIterator;

/**
 * Rebalance shards across the cluster. attempts to distribute shards evenly across nodes assuming each shard is
 * equivalent. Shards to stop are moved from shards to stopping shards. Nodes should advise the leader that a shards has
 * been stopped locally such that the leader can reallocate. Node should also keep trying until message is acknowledged
 * just in case leader changes while advising about stopped shard. Priority is given to shards already on the node, e.g.
 * if a node is stopping a shard and another node goes down, the shard being stopped is restarted on the same node
 */
public final class DefaultShardAllocationStrategy implements ShardAllocationStrategy {
    
    private final int maxShards;
    
    public DefaultShardAllocationStrategy(final int maxShards) {
        this.maxShards = maxShards;
    }
    
    @Override
    public int getMaxShards() {
        return maxShards;
    }
    
    @Override
    public ShardRebalance rebalance(final SortedMap<Integer, ClusterNodeInfo> nodes) {
        if (nodes.isEmpty()) {
            return new ShardRebalance(Collections.emptyMap(), Collections.emptyMap());
        }
        
        final Map<Integer, BitSet> toStop = new HashMap<>();
        final Map<Integer, BitSet> toStart = new HashMap<>();
        final BitSet availablePool = BitSet.of(maxShards);
        for (int i = 0; i < maxShards; i++) {
            availablePool.add(i);
        }
        
        final Map<Integer, Integer> nodeShardTargetDiff = determineShardTargetDiff(nodes, toStop, toStart, availablePool);
        distributeAvailableShards(toStart, availablePool, nodeShardTargetDiff);
        return new ShardRebalance(toStop, toStart);
    }
    
    private Map<Integer, Integer> determineShardTargetDiff(final SortedMap<Integer, ClusterNodeInfo> nodes,
                                                           final Map<Integer, BitSet> toStop,
                                                           final Map<Integer, BitSet> toStart,
                                                           final BitSet availablePool) {
        final int shardTarget = maxShards / nodes.size();
        final int extraShardIndex = nodes.size() - (maxShards % nodes.size());
        final Map<Integer, Integer> nodeShardTargetDiff = new HashMap<>();
        
        int i = 0;
        for (final Entry<Integer, ClusterNodeInfo> entry : nodes.entrySet()) {
            final int actualShardTarget = (i < extraShardIndex) ? shardTarget : (shardTarget + 1);
            final ClusterNodeInfo nodeInfo = entry.getValue();
            availablePool.removeAll(nodeInfo.getShards());
            availablePool.removeAll(nodeInfo.getShardsStopping());
            int shardsDiff = nodeInfo.getShards().size() - actualShardTarget;
            if (shardsDiff > 0) {
                for (final BitSetIterator shardsIt = nodeInfo.getShards().iterator(); shardsDiff > 0; shardsDiff--) {
                    toStop.computeIfAbsent(entry.getKey(), k -> BitSet.of(maxShards)).add(shardsIt.next());
                }
            } else if (shardsDiff < 0) {
                final BitSetIterator shardsStoppingIt = nodeInfo.getShardsStopping().iterator();
                while (shardsStoppingIt.hasNext() && (shardsDiff < 0)) {
                    final int shardStopping = shardsStoppingIt.next();
                    toStart.computeIfAbsent(entry.getKey(), k -> BitSet.of(maxShards)).add(shardStopping);
                    shardsDiff++;
                }
                if (shardsDiff < 0) {
                    nodeShardTargetDiff.putIfAbsent(entry.getKey(), shardsDiff);
                }
            }
            i++;
        }
        return nodeShardTargetDiff;
    }
    
    private void distributeAvailableShards(final Map<Integer, BitSet> toStart,
                                           final BitSet availablePool,
                                           final Map<Integer, Integer> nodeShardTargetDiff) {
        final BitSetIterator availableIt = availablePool.iterator();
        while (availableIt.hasNext()) {
            int largestDiff = 0;
            int largestDiffNode = 0;
            for (final Entry<Integer, Integer> entry : nodeShardTargetDiff.entrySet()) {
                if (entry.getValue() < largestDiff) {
                    largestDiff = entry.getValue();
                    largestDiffNode = entry.getKey();
                }
            }
            toStart.computeIfAbsent(largestDiffNode, k -> BitSet.of(maxShards)).add(availableIt.next());
            availableIt.remove();
            if (largestDiff == -1) {
                nodeShardTargetDiff.remove(largestDiffNode);
            } else {
                nodeShardTargetDiff.put(largestDiffNode, largestDiff + 1);
            }
        }
    }
    
}
