package com.ixaris.commons.clustering.lib.service;

import java.util.SortedMap;

import com.ixaris.commons.clustering.lib.common.ClusterNodeInfo;

public interface ShardAllocationStrategy {
    
    int getMaxShards();
    
    ShardRebalance rebalance(SortedMap<Integer, ClusterNodeInfo> nodes);
    
}
