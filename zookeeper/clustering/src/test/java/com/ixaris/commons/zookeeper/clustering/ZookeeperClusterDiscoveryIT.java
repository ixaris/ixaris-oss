package com.ixaris.commons.zookeeper.clustering;

import static com.ixaris.commons.zookeeper.clustering.ZookeeperClusterDiscoveryConnection.nodeIdToName;
import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.ForkJoinPool;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.RetryNTimes;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ixaris.common.zookeeper.test.TestZookeeperServer;
import com.ixaris.commons.async.lib.AsyncExecutor;
import com.ixaris.commons.clustering.lib.common.ClusterTopology;
import com.ixaris.commons.clustering.lib.service.DefaultShardAllocationStrategy;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;
import com.ixaris.commons.zookeeper.clustering.CommonsZookeeperClustering.NodeInfo;
import com.ixaris.commons.zookeeper.multitenancy.ZookeeperClient;
import com.ixaris.commons.zookeeper.multitenancy.ZookeeperClientConfiguration;

public class ZookeeperClusterDiscoveryIT {
    
    private static final long TOPOLOGY_CHANGES_TIMEOUT = 10L;
    private static TestZookeeperServer testZookeeperServer = null;
    
    private String clusterName = null;
    private ZookeeperClient registryClient;
    private ZookeeperClusterDiscoveryConnection registryConnection = null;
    private ZookeeperClusterRegistry clusterRegistry = null;
    private ZookeeperClient discoveryClient;
    private ZookeeperClusterDiscoveryConnection discoveryConnection = null;
    private ZookeeperClusterDiscovery clusterDiscovery = null;
    
    @BeforeAll
    public static void start() {
        testZookeeperServer = new TestZookeeperServer();
        testZookeeperServer.start();
    }
    
    @AfterAll
    public static void stop() {
        if (testZookeeperServer != null) {
            testZookeeperServer.stop();
            testZookeeperServer = null;
        }
    }
    
    @BeforeEach
    public void setup() {
        clusterName = "TEST_" + getNextUniqueIdentifier();
        
        final ZookeeperClientConfiguration rConf = new ZookeeperClientConfiguration(testZookeeperServer
            .getConnectString());
        rConf.setConnectionTimeout(1000);
        rConf.setSessionTimeout(5000);
        rConf.setMaximumRetries(10);
        rConf.setMaximumSleepTimeMs(1000);
        
        registryClient = new ZookeeperClient(rConf);
        registryConnection = new ZookeeperClusterDiscoveryConnection(clusterName, registryClient);
        clusterRegistry = new ZookeeperClusterRegistry(
            registryConnection,
            new DefaultShardAllocationStrategy(72),
            AsyncExecutor.DEFAULT,
            new LocalClusterRegistryHelperFactory(),
            Collections.emptySet(),
            Collections.emptySet());
        clusterRegistry.mergeAttributes(Collections.singletonMap("TEST", "VALUE"));
        clusterRegistry.start();
        
        final ZookeeperClientConfiguration dConf = new ZookeeperClientConfiguration(testZookeeperServer
            .getConnectString());
        dConf.setConnectionTimeout(1000);
        dConf.setSessionTimeout(5000);
        dConf.setMaximumRetries(10);
        dConf.setMaximumSleepTimeMs(1000);
        
        discoveryClient = new ZookeeperClient(dConf);
        discoveryConnection = new ZookeeperClusterDiscoveryConnection("CLIENT", discoveryClient);
        clusterDiscovery = new ZookeeperClusterDiscovery(discoveryConnection, ForkJoinPool.commonPool());
    }
    
    @AfterEach
    public void tearDown() {
        if (clusterRegistry != null) {
            clusterRegistry.shutdown();
            clusterRegistry = null;
        }
        if (registryConnection != null) {
            registryConnection.shutdown();
            registryConnection = null;
        }
        if (registryClient != null) {
            registryClient.close();
            registryClient = null;
        }
        if (clusterDiscovery != null) {
            clusterDiscovery = null;
        }
        if (discoveryConnection != null) {
            discoveryConnection.shutdown();
            discoveryConnection = null;
        }
        if (discoveryClient != null) {
            discoveryClient.close();
            discoveryClient = null;
        }
    }
    
    @Test
    public void addToplogyListenerForInexistentCluster_ShouldGetInitialEmptyTopology_Success() {
        final TestTopologyChangeListener listener = new TestTopologyChangeListener();
        clusterDiscovery.addTopologyListener("INEXISTENT", listener);
        
        await().atMost(TOPOLOGY_CHANGES_TIMEOUT, SECONDS).until(() -> listener.getLatestTopology() != null);
        final ClusterTopology initialTopology = listener.getLatestTopology();
        assertThat(initialTopology.getNodes()).isEmpty();
    }
    
    @Test
    public void addToplogyListener_ShouldGetInitialEmptyTopology_Success() {
        final TestTopologyChangeListener listener = new TestTopologyChangeListener();
        addAndCheckServiceTopologyListener(listener, true);
    }
    
    @Test
    public void nodeTopologyListener_ShouldReceiveUpdatesUponAdd() {
        final TestTopologyChangeListener listener = new TestTopologyChangeListener();
        addAndCheckServiceTopologyListener(listener, false);
        
        final ClusterTopology updatedTopology = listener.getAndClearLatestTopology();
        assertThat(updatedTopology.getNodes().keySet()).containsOnly(clusterRegistry.getNodeId());
        assertThat(updatedTopology.getNodes().get(clusterRegistry.getNodeId())).isEqualTo(
            clusterRegistry.getTopology().getNodes().get(clusterRegistry.getNodeId()));
    }
    
    @Test
    public void nodeTopologyListener_ShouldReceiveUpdatesAfterDestroy() {
        final TestTopologyChangeListener listener = new TestTopologyChangeListener();
        addAndCheckServiceTopologyListener(listener, true);
        
        clusterRegistry.shutdown();
        
        await().atMost(TOPOLOGY_CHANGES_TIMEOUT, SECONDS).until(() -> listener.getLatestTopology() != null);
        final ClusterTopology topology = listener.getAndClearLatestTopology();
        assertThat(topology.getNodes()).isEmpty();
    }
    
    @Test
    public void nodeTopologyListener_multipleNodes_ShouldReceiveUpdatesUponAdd() {
        final TestTopologyChangeListener listener = new TestTopologyChangeListener();
        addAndCheckServiceTopologyListener(listener, true);
        
        final ZookeeperClusterDiscoveryConnection connection2 = new ZookeeperClusterDiscoveryConnection(
            clusterName, discoveryClient);
        final ZookeeperClusterRegistry serviceRegistry2 = new ZookeeperClusterRegistry(
            connection2,
            new DefaultShardAllocationStrategy(72),
            AsyncExecutor.DEFAULT,
            new LocalClusterRegistryHelperFactory(),
            Collections.emptySet(),
            Collections.emptySet());
        serviceRegistry2.start();
        
        await().atMost(TOPOLOGY_CHANGES_TIMEOUT, SECONDS).until(() -> listener.getLatestTopology() != null);
        final ClusterTopology afterSecondNodeTopology = listener.getAndClearLatestTopology();
        assertThat(afterSecondNodeTopology.getNodes().keySet())
            .hasSize(2)
            .containsExactlyInAnyOrder(clusterRegistry.getNodeId(), serviceRegistry2.getNodeId());
        
        connection2.shutdown();
    }
    
    @Test
    public void addServiceAndDestroyServiceDiscovery_ShouldRemoveNodesInZK() {
        final TestTopologyChangeListener listener = new TestTopologyChangeListener();
        addAndCheckServiceTopologyListener(listener, true);
        
        clusterRegistry.shutdown();
        registryConnection.shutdown();
        
        try (
            CuratorFramework zookeeperClient = CuratorFrameworkFactory.newClient(
                testZookeeperServer.getConnectString(), new RetryNTimes(5, 500))) {
            zookeeperClient.start();
            final List<String> clusterNodes = zookeeperClient
                .getChildren()
                .forPath(String.format("/discovery/clusters/%s", clusterName));
            assertThat(clusterNodes).isEmpty();
        } catch (final Exception e) {
            fail("Unable to retrieve children for service name/key in ZK", e);
        }
    }
    
    @Test
    @SuppressWarnings("squid:S2925")
    public void addService_KillZookeeper_RestartZookeeper_ShouldReRegisterService() throws Exception {
        final TestTopologyChangeListener listener = new TestTopologyChangeListener();
        addAndCheckServiceTopologyListener(listener, true);
        
        stop();
        
        Thread.sleep(7000L);
        
        assertThat(listener.getLatestTopology()).isNull();
        
        start();
        
        await()
            .atMost(120L, SECONDS)
            .until(
                () -> (listener.getLatestTopology() != null) && (listener.getLatestTopology().getNodes().size() == 1));
        
        final ClusterTopology updatedTopology = listener.getLatestTopology();
        assertThat(updatedTopology.getNodes()).hasSize(1);
        assertThat(updatedTopology.getNodes().keySet()).containsOnly(clusterRegistry.getNodeId());
        assertThat(updatedTopology.getNodes().get(clusterRegistry.getNodeId()).getAttributes()).containsExactly(
            Collections.singletonMap("TEST", "VALUE").entrySet().iterator().next());
        
        try (
            final CuratorFramework zookeeperClient = CuratorFrameworkFactory.newClient(
                testZookeeperServer.getConnectString(), new RetryNTimes(5, 500))) {
            zookeeperClient.start();
            final List<String> clusterNodes = zookeeperClient
                .getChildren()
                .forPath(String.format("/discovery/clusters/%s", clusterName));
            
            assertThat(clusterNodes).isNotEmpty().containsExactly(nodeIdToName(clusterRegistry.getNodeId()));
            byte[] bytes = zookeeperClient
                .getData()
                .forPath(String.format(
                    "/discovery/clusters/%s/%s", clusterName, nodeIdToName(clusterRegistry.getNodeId())));
            assertThat(ZookeeperClusterDiscoveryConnection.decode(NodeInfo.class, bytes).getAttributeMap())
                .isNotEmpty()
                .containsExactly(Collections.singletonMap("TEST", "VALUE").entrySet().iterator().next());
        }
    }
    
    private void addAndCheckServiceTopologyListener(final TestTopologyChangeListener listener, final boolean clear) {
        clusterDiscovery.addTopologyListener(clusterName, listener);
        
        await().atMost(TOPOLOGY_CHANGES_TIMEOUT, SECONDS).until(() -> listener.getLatestTopology() != null);
        final ClusterTopology initialTopology = clear
            ? listener.getAndClearLatestTopology() : listener.getLatestTopology();
        assertThat(initialTopology.getNodes()).hasSize(1);
        assertThat(initialTopology.getNodes().keySet()).containsOnly(clusterRegistry.getNodeId());
    }
    
    private static String getNextUniqueIdentifier() {
        return String.valueOf(UniqueIdGenerator.generate());
    }
    
}
