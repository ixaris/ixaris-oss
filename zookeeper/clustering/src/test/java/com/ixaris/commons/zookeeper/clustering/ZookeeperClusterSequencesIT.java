package com.ixaris.commons.zookeeper.clustering;

import static com.ixaris.commons.zookeeper.clustering.ZookeeperClusterDiscoveryConnection.getSequenceIdZnodePath;
import static com.ixaris.commons.zookeeper.clustering.ZookeeperClusterSequences.RESERVED_IN_FIRST_GROUP;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ixaris.common.zookeeper.test.TestZookeeperServer;
import com.ixaris.commons.zookeeper.multitenancy.ZookeeperClient;
import com.ixaris.commons.zookeeper.multitenancy.ZookeeperClientConfiguration;

public class ZookeeperClusterSequencesIT {
    
    private static final String CLUSTER_NAME = "TEST";
    private static TestZookeeperServer testZookeeperServer = null;
    
    private ZookeeperClient zookeeperClient;
    
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
    
    @BeforeEach
    public void before() {
        final ZookeeperClientConfiguration configuration = new ZookeeperClientConfiguration(testZookeeperServer
            .getConnectString());
        configuration.setConnectionTimeout(1000);
        configuration.setSessionTimeout(5000);
        configuration.setMaximumRetries(10);
        configuration.setMaximumSleepTimeMs(1000);
        
        zookeeperClient = new ZookeeperClient(configuration);
    }
    
    @AfterEach
    public void after() {
        zookeeperClient.close();
    }
    
    @Test
    public void testResolveUniqueId_shouldAcquireSequentialIds() {
        final ZookeeperClusterDiscoveryConnection[] connections = new ZookeeperClusterDiscoveryConnection[300];
        
        for (int i = 0; i < connections.length; i++) {
            connections[i] = new ZookeeperClusterDiscoveryConnection(CLUSTER_NAME, zookeeperClient);
            assertThat(connections[i].getNodeId()).isEqualTo(i + RESERVED_IN_FIRST_GROUP);
        }
        
        for (final ZookeeperClusterDiscoveryConnection connection : connections) {
            connection.shutdown();
        }
    }
    
    @Test
    public void testResolveUniqueId_withGap_shouldAcquireGap() {
        final ZookeeperClusterDiscoveryConnection[] connections = new ZookeeperClusterDiscoveryConnection[5];
        
        for (int i = 0; i < connections.length - 1; i++) {
            connections[i] = new ZookeeperClusterDiscoveryConnection(CLUSTER_NAME, zookeeperClient);
            assertThat(connections[i].getNodeId()).isEqualTo(i + RESERVED_IN_FIRST_GROUP);
        }
        
        // create gap
        final int connectionIdToShutdown = 2;
        connections[connectionIdToShutdown].shutdown();
        
        // should acquire gap
        connections[4] = new ZookeeperClusterDiscoveryConnection(CLUSTER_NAME, zookeeperClient);
        assertThat(connections[connections.length - 1].getNodeId()).isEqualTo(
            connectionIdToShutdown + RESERVED_IN_FIRST_GROUP);
        
        for (int i = 0; i < connections.length; i++) {
            if (i != connectionIdToShutdown) {
                connections[i].shutdown();
            }
        }
    }
    
    @Test
    public void testRemovedId_shouldReaquireSameId() throws Exception {
        final ZookeeperClusterDiscoveryConnection[] connections = new ZookeeperClusterDiscoveryConnection[5];
        
        for (int i = 0; i < connections.length; i++) {
            connections[i] = new ZookeeperClusterDiscoveryConnection(CLUSTER_NAME, zookeeperClient);
            assertThat(connections[i].getNodeId()).isEqualTo(i + RESERVED_IN_FIRST_GROUP);
        }
        
        Thread.sleep(100L);
        
        connections[0].getZookeeperClient().delete().forPath(getSequenceIdZnodePath(CLUSTER_NAME, "00", "0A"));
        connections[0].getZookeeperClient().delete().forPath(getSequenceIdZnodePath(CLUSTER_NAME, "00", "0B"));
        connections[0].getZookeeperClient().delete().forPath(getSequenceIdZnodePath(CLUSTER_NAME, "00", "0C"));
        
        Thread.sleep(100L);
        
        for (int i = 0; i < connections.length; i++) {
            assertThat(connections[i].getNodeId()).isEqualTo(i + RESERVED_IN_FIRST_GROUP);
        }
        
        for (final ZookeeperClusterDiscoveryConnection connection : connections) {
            connection.shutdown();
        }
    }
    
}
