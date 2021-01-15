package com.ixaris.commons.netty.clustering;

import static com.ixaris.commons.async.lib.Async.all;
import static com.ixaris.commons.async.lib.CompletionStageUtil.isDone;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.concurrent.TimeoutException;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ixaris.common.zookeeper.test.TestZookeeperServer;
import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.AsyncExecutor;
import com.ixaris.commons.clustering.lib.service.DefaultShardAllocationStrategy;
import com.ixaris.commons.misc.lib.net.Localhost;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.test.TestTenants;
import com.ixaris.commons.netty.clustering.test.ClusterTest.ClusterTestEvent;
import com.ixaris.commons.netty.clustering.test.ClusterTest.ClusterTestResponse;
import com.ixaris.commons.protobuf.lib.CommonsProtobufLib.Empty;
import com.ixaris.commons.zookeeper.clustering.ZookeeperClusterDiscoveryConnection;
import com.ixaris.commons.zookeeper.clustering.ZookeeperClusterRegistry;
import com.ixaris.commons.zookeeper.multitenancy.ZookeeperClient;
import com.ixaris.commons.zookeeper.multitenancy.ZookeeperClientConfiguration;

/**
 * This test creates a number of virtual nodes by creating separate zookeeper client, actor system and service instance
 * for each. This lets us test the default cluster sharding logic without requiring separate JVMs.
 */
public final class NettyClusterShardingTest {
    
    private static final int MAX_SHARDS = 72;
    private static final int NUM_NODES = 3;
    
    private MultiTenancy multiTenancy;
    private TestZookeeperServer zkServer;
    
    private ZookeeperClient[] zookeeperClient;
    private NettyBean[] nettyBean;
    private ZookeeperClusterRegistry[] clusterRegistry;
    private TestRouteHandler[] routeHandlers;
    private TestBroadcastHandler[] broadcastHandlers;
    
    @SuppressWarnings("squid:S2925")
    @BeforeEach
    public void setup() {
        multiTenancy = new MultiTenancy();
        multiTenancy.start();
        multiTenancy.addTenant(MultiTenancy.SYSTEM_TENANT);
        multiTenancy.addTenant(TestTenants.DEFAULT);
        
        zkServer = new TestZookeeperServer();
        zkServer.start();
        
        zookeeperClient = new ZookeeperClient[NUM_NODES];
        nettyBean = new NettyBean[NUM_NODES];
        clusterRegistry = new ZookeeperClusterRegistry[NUM_NODES];
        routeHandlers = new TestRouteHandler[NUM_NODES];
        broadcastHandlers = new TestBroadcastHandler[NUM_NODES];
        
        // set up virtual nodes
        final Thread[] startupThreads = new Thread[NUM_NODES];
        
        startupThreads[0] = new Thread(() -> startNode(0));
        startupThreads[0].start();
        try {
            startupThreads[0].join();
        } catch (final InterruptedException e) {
            throw new IllegalStateException(e);
        }
        
        for (int i = 1; i < NUM_NODES; i++) {
            final int it = i;
            startupThreads[i] = new Thread(() -> startNode(it));
            startupThreads[i].start();
        }
        for (int i = 1; i < NUM_NODES; i++) {
            try {
                startupThreads[i].join();
            } catch (final InterruptedException e) {
                throw new IllegalStateException(e);
            }
        }
        
        try {
            Thread.sleep(2000L);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    private void startNode(final int i) {
        final ZookeeperClientConfiguration configuration = new ZookeeperClientConfiguration(zkServer
            .getConnectString());
        configuration.setConnectionTimeout(1000);
        configuration.setSessionTimeout(5000);
        configuration.setMaximumRetries(10);
        configuration.setMaximumSleepTimeMs(1000);
        
        zookeeperClient[i] = new ZookeeperClient(configuration);
        nettyBean[i] = new NettyBean(1, Localhost.HOSTNAME, 9000);
        clusterRegistry[i] =
            new ZookeeperClusterRegistry(
                new ZookeeperClusterDiscoveryConnection("test", zookeeperClient[i]),
                new DefaultShardAllocationStrategy(MAX_SHARDS),
                AsyncExecutor.DEFAULT,
                new NettyClusterShardingFactory(nettyBean[i], AsyncExecutor.DEFAULT),
                Collections.emptySet(),
                Collections.emptySet());
        clusterRegistry[i].start();
        clusterRegistry[i].addLeadershipListener(isLeader -> System.out.println(i + " is leader: " + isLeader));
        clusterRegistry[i].addShardsListener((old, cur) -> System.out.println(i + " shards: (" + cur.size() + ") " + cur));
        
        routeHandlers[i] = new TestRouteHandler(i);
        broadcastHandlers[i] = new TestBroadcastHandler();
        clusterRegistry[i].register(routeHandlers[i]);
        clusterRegistry[i].register(broadcastHandlers[i]);
    }
    
    private void stopNode(final int i) {
        clusterRegistry[i].deregister(broadcastHandlers[i]);
        clusterRegistry[i].deregister(routeHandlers[i]);
        clusterRegistry[i].shutdown();
        nettyBean[i].shutdown();
        zookeeperClient[i].close();
    }
    
    @SuppressWarnings("squid:S2925")
    @AfterEach
    public void teardown() throws InterruptedException {
        for (int i = 0; i < NUM_NODES; i++) {
            stopNode(i);
        }
        Thread.sleep(1000L);
        
        if (zkServer != null) {
            zkServer.stop();
        }
        
        multiTenancy.stop();
    }
    
    @Test
    public void defaultClusterSharding_messageToAllShardsFromEveryNode_expectAllNodesToRespond() throws TimeoutException {
        final Map<Integer, Map<Long, Async<ClusterTestResponse>>> routeResponses = new HashMap<>();
        final Map<Integer, Async<Boolean>> broadcastResponses = new HashMap<>();
        
        // send a request from each node to each shard
        for (int i = 0; i < NUM_NODES; i++) {
            for (int j = 0; j < MAX_SHARDS; j++) {
                final Async<ClusterTestResponse> stage = clusterRegistry[i].route(routeHandlers[i], j, Empty.getDefaultInstance());
                routeResponses.computeIfAbsent(i, k -> new HashMap<>()).put((long) j, stage);
            }
            final Async<Boolean> stage = clusterRegistry[i].broadcast(
                broadcastHandlers[i], ClusterTestEvent.newBuilder().setId(i).build());
            broadcastResponses.put(i, stage);
        }
        
        // this time we expect that every node has at least one partition
        final Set<Integer> nodesThatRespondedForRoute = new HashSet<>();
        for (int i = 0; i < NUM_NODES; i++) {
            final Async<Void> routeFuture = all(routeResponses.get(i)).map(response -> {
                for (final Entry<Long, ClusterTestResponse> entry : response.entrySet()) {
                    final ClusterTestResponse r = entry.getValue();
                    assertThat(r.getId()).isEqualTo(entry.getKey());
                    nodesThatRespondedForRoute.add(r.getNode());
                }
                return null;
            });
            Awaitility.await().atMost(2, MINUTES).until(() -> isDone(routeFuture));
            
            final Async<Boolean> broadcastFuture = broadcastResponses.get(i);
            Awaitility.await().atMost(2, MINUTES).until(() -> isDone(broadcastFuture));
        }
        assertThat(nodesThatRespondedForRoute.size()).isEqualTo(NUM_NODES);
        
        for (int i = 0; i < NUM_NODES; i++) {
            assertThat(broadcastHandlers[i].received.size()).isEqualTo(NUM_NODES - 1);
            assertThat(broadcastHandlers[i].received).doesNotContain(i);
        }
    }
    
}
