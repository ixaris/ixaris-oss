package com.ixaris.commons.zookeeper.clustering;

import static com.ixaris.commons.zookeeper.clustering.ZookeeperClusterDiscoveryConnection.getClusterNodeZnodePath;
import static com.ixaris.commons.zookeeper.clustering.ZookeeperClusterDiscoveryConnection.nodeIdToName;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collections;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryUntilElapsed;
import org.apache.curator.utils.ZKPaths;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Test;

import com.ixaris.common.zookeeper.test.TestZookeeperServer;
import com.ixaris.commons.async.lib.AsyncExecutor;
import com.ixaris.commons.clustering.lib.service.DefaultShardAllocationStrategy;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;
import com.ixaris.commons.zookeeper.multitenancy.ZookeeperClient;
import com.ixaris.commons.zookeeper.multitenancy.ZookeeperClientConfiguration;

public class ChrootedZookeeperClusterDiscoveryIT {
    
    private static final String CHROOT = "some_root" + System.currentTimeMillis();
    private static TestZookeeperServer testZookeeperServer = null;
    
    @BeforeAll
    public static void start() {
        testZookeeperServer = new TestZookeeperServer();
        testZookeeperServer.start();
    }
    
    @AfterAll
    public static void stop() {
        if (testZookeeperServer != null) {
            testZookeeperServer.stop();
        }
    }
    
    private static String getNextUniqueIdentifier() {
        return String.valueOf(UniqueIdGenerator.generate());
    }
    
    @Test
    public void itShouldWorkUnderChroot() throws Exception {
        try (final CuratorFramework client = connect()) {
            client.start();
            client.blockUntilConnected();
            
            final String clusterName = "TEST_" + getNextUniqueIdentifier();
            
            final String zkConnect = testZookeeperServer.getConnectString() + "/" + CHROOT;
            
            final ZookeeperClientConfiguration configuration = new ZookeeperClientConfiguration(zkConnect);
            configuration.setConnectionTimeout(1000);
            configuration.setSessionTimeout(5000);
            configuration.setMaximumRetries(10);
            configuration.setMaximumSleepTimeMs(1000);
            
            final ZookeeperClient zookeeperClient = new ZookeeperClient(configuration);
            final ZookeeperClusterDiscoveryConnection connection = new ZookeeperClusterDiscoveryConnection(
                clusterName, zookeeperClient);
            final ZookeeperClusterRegistry registry = new ZookeeperClusterRegistry(connection,
                new DefaultShardAllocationStrategy(72),
                AsyncExecutor.DEFAULT,
                new LocalClusterRegistryHelperFactory(),
                Collections.emptySet(),
                Collections.emptySet());
            registry.start();
            
            try {
                final int nodeId = registry.getNodeId();
                final String path = ZKPaths.makePath(CHROOT, getClusterNodeZnodePath(clusterName, nodeIdToName(nodeId)));
                assertNotNull(client.checkExists().forPath(path), "Path doesn't exist");
            } finally {
                connection.shutdown();
                zookeeperClient.close();
            }
        }
    }
    
    private CuratorFramework connect() {
        return CuratorFrameworkFactory.newClient(
            testZookeeperServer.getConnectString(), new RetryUntilElapsed(10000, 1000));
    }
}
