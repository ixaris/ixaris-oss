package com.ixaris.commons.zookeeper.microservices;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.utils.ZKPaths;

import com.ixaris.commons.microservices.lib.service.support.ServiceSupport;
import com.ixaris.commons.zookeeper.clustering.ZookeeperClusterDiscoveryConnection;
import com.ixaris.commons.zookeeper.multitenancy.ZookeeperClient;

/**
 * Zookeeper Service discovery Connection
 *
 * <p>This class is responsible to connect with Zookeeper and ensures we have only a single connection to zookeeper for
 * both Service Discovery and Registry. It also supports storing a set of listeners connected to zookeeper to ensure a
 * smooth/clean shutdown.
 */
public class ZookeeperServiceDiscoveryConnection extends ZookeeperClusterDiscoveryConnection {
    
    private static final String DEFAULT_KEY = "_";
    
    private static final String PATH_ENDPOINTS = "/discovery/endpoints";
    
    static String getEndpointZnodePath(final String endpointName) {
        return ZKPaths.makePath(PATH_ENDPOINTS, endpointName);
    }
    
    static String getEndpointKeyZnodePath(final String endpointName, final String key) {
        return ZKPaths.makePath(getEndpointZnodePath(endpointName), key);
    }
    
    static String normaliseServiceKey(final String key) {
        return ServiceSupport.NO_KEY.equals(key) ? DEFAULT_KEY : key;
    }
    
    static String denormaliseServiceKey(final String key) {
        return DEFAULT_KEY.equals(key) ? "" : key;
    }
    
    public ZookeeperServiceDiscoveryConnection(final String clusterName, final ZookeeperClient zookeeperClient) {
        super(clusterName, zookeeperClient);
    }
    
    @Override
    protected CuratorFramework getZookeeperClient() {
        return super.getZookeeperClient();
    }
    
}
