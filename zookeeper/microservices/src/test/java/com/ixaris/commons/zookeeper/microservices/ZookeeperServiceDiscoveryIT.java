package com.ixaris.commons.zookeeper.microservices;

import static java.util.concurrent.TimeUnit.SECONDS;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;
import static org.awaitility.Awaitility.await;

import java.util.Collections;
import java.util.List;
import java.util.Set;
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
import com.ixaris.commons.clustering.lib.service.DefaultShardAllocationStrategy;
import com.ixaris.commons.microservices.lib.client.discovery.ServiceEndpoint;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;
import com.ixaris.commons.zookeeper.clustering.LocalClusterRegistryHelperFactory;
import com.ixaris.commons.zookeeper.multitenancy.ZookeeperClient;
import com.ixaris.commons.zookeeper.multitenancy.ZookeeperClientConfiguration;

public class ZookeeperServiceDiscoveryIT {
    
    private static final long TOPOLOGY_CHANGES_TIMEOUT = 3L;
    private static TestZookeeperServer testZookeeperServer = null;
    
    private ZookeeperServiceDiscoveryConnection connection = null;
    private ZookeeperServiceDiscovery zookeeperServiceDiscovery = null;
    private ZookeeperServiceRegistry zookeeperServiceRegistry = null;
    
    @BeforeAll
    public static void start() {
        testZookeeperServer = new TestZookeeperServer(TestZookeeperServer.TEST_ZK_PORT);
        testZookeeperServer.start();
    }
    
    @AfterAll
    public static void stop() {
        if (testZookeeperServer != null) {
            testZookeeperServer.stop();
        }
    }
    
    @BeforeEach
    public void setup() {
        final String clusterName = "TEST_" + getNextUniqueIdentifier();
        
        final ZookeeperClientConfiguration configuration = new ZookeeperClientConfiguration(testZookeeperServer.getConnectString());
        configuration.setConnectionTimeout(1000);
        configuration.setSessionTimeout(5000);
        configuration.setMaximumRetries(10);
        configuration.setMaximumSleepTimeMs(1000);
        
        ZookeeperClient zookeeperClient = new ZookeeperClient(configuration);
        connection = new ZookeeperServiceDiscoveryConnection(clusterName, zookeeperClient);
        zookeeperServiceDiscovery = new ZookeeperServiceDiscovery(connection, ForkJoinPool.commonPool());
        zookeeperServiceRegistry = new ZookeeperServiceRegistry(connection,
            new DefaultShardAllocationStrategy(72),
            ForkJoinPool.commonPool(),
            new LocalClusterRegistryHelperFactory(),
            Collections.emptySet(),
            Collections.emptySet());
    }
    
    @AfterEach
    public void tearDown() {
        connection.shutdown();
    }
    
    @Test
    public void addEndpointListener_ShouldGetInitialEmptyTopology_Success() {
        
        final String serviceName = getNextUniqueIdentifier();
        
        final TestServiceChangeListener listener = new TestServiceChangeListener();
        addAndCheckEndpointListener(serviceName, listener);
    }
    
    @Test
    public void getServiceKeys_UndefinedService_ShouldReturnEmpty() {
        
        final String serviceName = getNextUniqueIdentifier();
        final Set<String> serviceKeys = zookeeperServiceDiscovery.getServiceKeys(serviceName);
        
        assertThat(serviceKeys).isEmpty();
    }
    
    @Test
    public void getServiceKeys_SingleServiceKey_ShouldReturnTheServiceKey() {
        
        final String serviceName = getNextUniqueIdentifier();
        final String serviceKey = getNextUniqueIdentifier();
        zookeeperServiceRegistry.register(serviceName, serviceKey);
        
        // adding listener after key added
        zookeeperServiceDiscovery.addEndpointListener(serviceName, endpointTopology -> {});
        
        await()
            .atMost(TOPOLOGY_CHANGES_TIMEOUT, SECONDS)
            .until(() -> !zookeeperServiceDiscovery.getServiceKeys(serviceName).isEmpty());
        final Set<String> serviceKeys = zookeeperServiceDiscovery.getServiceKeys(serviceName);
        assertThat(serviceKeys).hasSize(1).containsExactly(serviceKey);
    }
    
    @Test
    public void getServiceKeys_MultipleServiceKey_ShouldReturnAllServiceKeys() {
        
        final String serviceName = getNextUniqueIdentifier();
        zookeeperServiceDiscovery.addEndpointListener(serviceName, endpointTopology -> {});
        final String serviceKey = "K1";
        final String serviceKey2 = "K2";
        final String serviceKey3 = "K3";
        zookeeperServiceRegistry.register(serviceName, serviceKey);
        zookeeperServiceRegistry.register(serviceName, serviceKey2);
        zookeeperServiceRegistry.register(serviceName, serviceKey3);
        
        await()
            .atMost(TOPOLOGY_CHANGES_TIMEOUT, SECONDS)
            .until(() -> zookeeperServiceDiscovery.getServiceKeys(serviceName).size() >= 3);
        final Set<String> serviceKeys = zookeeperServiceDiscovery.getServiceKeys(serviceName);
        assertThat(serviceKeys).hasSize(3).containsExactlyInAnyOrder(serviceKey, serviceKey2, serviceKey3);
    }
    
    @Test
    public void addServiceEmptyKey_ShouldGetTopologyUpdates_Success() {
        
        final String serviceName = getNextUniqueIdentifier();
        
        final TestServiceChangeListener listener = new TestServiceChangeListener();
        addAndCheckEndpointListener(serviceName, listener);
        
        final String serviceKey = "";
        zookeeperServiceRegistry.register(serviceName, serviceKey);
        
        await().atMost(TOPOLOGY_CHANGES_TIMEOUT, SECONDS).until(() -> listener.getLatestTopology() != null);
        final ServiceEndpoint endpoint = listener.getLatestTopology();
        assertThat(endpoint.getName()).isEqualTo(serviceName);
        assertThat(endpoint.getServiceKeys()).containsOnlyKeys(serviceKey);
        final String clusterName = endpoint.getServiceKeys().get(serviceKey);
        assertThat(clusterName).isEqualTo(clusterName);
    }
    
    @Test
    public void addServiceAndDestroyServiceDiscovery_ShouldRemoveNodesInZK() {
        
        final String endpointName = getNextUniqueIdentifier();
        
        final TestServiceChangeListener listener = new TestServiceChangeListener();
        addAndCheckEndpointListener(endpointName, listener);
        
        final String endpointKey = getNextUniqueIdentifier();
        zookeeperServiceRegistry.register(endpointName, endpointKey);
        
        await().atMost(TOPOLOGY_CHANGES_TIMEOUT, SECONDS).until(() -> listener.getLatestTopology() != null);
        
        zookeeperServiceRegistry.shutdown();
        connection.shutdown();
        
        try (
            CuratorFramework zookeeperClient = CuratorFrameworkFactory.newClient(
                testZookeeperServer.getConnectString(), new RetryNTimes(5, 500))) {
            zookeeperClient.start();
            final List<String> serviceKeyNodes = zookeeperClient
                .getChildren()
                .forPath(String.format("/discovery/endpoints/%s/%s", endpointName, endpointKey));
            assertThat(serviceKeyNodes).isEmpty();
        } catch (final Exception e) {
            fail("Unable to retrieve children for service name/key in ZK", e);
        }
    }
    
    private void addAndCheckEndpointListener(final String serviceName, final TestServiceChangeListener listener) {
        zookeeperServiceDiscovery.addEndpointListener(serviceName, listener);
        
        await().atMost(TOPOLOGY_CHANGES_TIMEOUT, SECONDS).until(() -> listener.getLatestTopology() != null);
        final ServiceEndpoint initialTopology = listener.getLatestTopology();
        assertThat(initialTopology.getName()).isEqualTo(serviceName);
        assertThat(initialTopology.getServiceKeys()).isEmpty();
    }
    
    private static String getNextUniqueIdentifier() {
        return String.valueOf(UniqueIdGenerator.generate());
    }
    
}
