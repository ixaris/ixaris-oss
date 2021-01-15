package com.ixaris.commons.zookeeper.clustering;

import org.apache.curator.framework.CuratorFramework;

public interface PathNodeCacheListener {
    
    @SuppressWarnings("squid:S00112")
    void nodeEvent(CuratorFramework client, PathNodeCacheEvent event) throws Exception;
    
}
