package com.ioxaris.commons.clustering.lib.service;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.SortedMap;
import java.util.TreeMap;

import org.junit.jupiter.api.Test;

import com.ixaris.commons.clustering.lib.common.ClusterNodeInfo;
import com.ixaris.commons.clustering.lib.common.ModClusterShardResolver;
import com.ixaris.commons.clustering.lib.service.DefaultShardAllocationStrategy;
import com.ixaris.commons.clustering.lib.service.ShardRebalance;
import com.ixaris.commons.collections.lib.BitSet;

public class DefaultShardAllocationStrategyTest {
    
    @Test
    public void testRebalanceNewCluster() {
        final DefaultShardAllocationStrategy shardAllocationStrategy = new DefaultShardAllocationStrategy(72);
        final ModClusterShardResolver shardResolver = new ModClusterShardResolver(72);
        
        final SortedMap<Integer, ClusterNodeInfo> nodes = new TreeMap<>();
        nodes.put(0, new ClusterNodeInfo(0L, null, null, null));
        final ShardRebalance rebalance = shardAllocationStrategy.rebalance(nodes);
        
        assertThat(rebalance.getToStop()).isEmpty();
        assertThat(rebalance.getToStart()).hasSize(1);
        assertThat(rebalance.getToStart().get(0).size()).isEqualTo(72);
    }
    
    @Test
    public void testRebalanceNewNode() {
        final DefaultShardAllocationStrategy shardAllocationStrategy = new DefaultShardAllocationStrategy(72);
        final ModClusterShardResolver shardResolver = new ModClusterShardResolver(72);
        
        final SortedMap<Integer, ClusterNodeInfo> nodes1 = new TreeMap<>();
        nodes1.put(0, new ClusterNodeInfo(0L, set(0, 71), null, null));
        nodes1.put(1, new ClusterNodeInfo(0L, null, null, null));
        final ShardRebalance rebalance1 = shardAllocationStrategy.rebalance(nodes1);
        
        assertThat(rebalance1.getToStop()).hasSize(1);
        assertThat(rebalance1.getToStop().get(0).size()).isEqualTo(36);
        assertThat(rebalance1.getToStart()).isEmpty();
        
        final SortedMap<Integer, ClusterNodeInfo> nodes2 = new TreeMap<>();
        nodes2.put(0, new ClusterNodeInfo(0L, set(36, 71), set(1, 35), null));
        nodes2.put(1, new ClusterNodeInfo(0L, null, null, null));
        final ShardRebalance rebalance2 = shardAllocationStrategy.rebalance(nodes2);
        
        assertThat(rebalance2.getToStop()).isEmpty();
        assertThat(rebalance2.getToStart()).hasSize(1);
        assertThat(rebalance2.getToStart().get(1).size()).isEqualTo(1);
    }
    
    @Test
    public void testRebalanceStoppingShards() {
        final DefaultShardAllocationStrategy shardAllocationStrategy = new DefaultShardAllocationStrategy(72);
        final ModClusterShardResolver shardResolver = new ModClusterShardResolver(72);
        
        final SortedMap<Integer, ClusterNodeInfo> nodes = new TreeMap<>();
        nodes.put(0, new ClusterNodeInfo(0L, set(0, 20), set(21, 23), null));
        nodes.put(1, new ClusterNodeInfo(0L, set(24, 47), null, null));
        nodes.put(2, new ClusterNodeInfo(0L, set(48, 68), null, null));
        final ShardRebalance rebalance = shardAllocationStrategy.rebalance(nodes);
        
        assertThat(rebalance.getToStop()).isEmpty();
        assertThat(rebalance.getToStart()).hasSize(2);
        assertThat(rebalance.getToStart().get(0).toArray()).containsExactlyInAnyOrder(21, 22, 23);
        assertThat(rebalance.getToStart().get(2).toArray()).containsExactlyInAnyOrder(69, 70, 71);
    }
    
    private BitSet set(final int from, final int to) {
        final BitSet set = BitSet.of(72);
        for (int i = from; i <= to; i++) {
            set.add(i);
        }
        return set;
    }
    
}
