package com.ixaris.commons.clustering.lib.service;

import com.ixaris.commons.collections.lib.BitSet;

@FunctionalInterface
public interface ClusterShardsChangeListener {
    
    void onShardsChanged(BitSet oldShards, BitSet newShards);
    
}
