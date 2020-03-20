package com.ixaris.commons.clustering.lib.common;

import com.ixaris.commons.misc.lib.id.Sequence;

public interface ClusterSequence {
    
    String DEFAULT = "default";
    
    /**
     * Obtain a cluster managed sequence (providing node id and width for this node in the cluster, guaranteed to be
     * unique cluster-wide for the given name
     *
     * @param name
     * @return
     */
    Sequence getSequence(String name);
    
}
