package com.ixaris.commons.kafka.test;

import static com.ixaris.common.zookeeper.test.TestZookeeperServer.TEST_ZK_PORT;
import static com.ixaris.commons.kafka.test.TestKafkaServer.TEST_KAFKA_HOST;
import static com.ixaris.commons.kafka.test.TestKafkaServer.TEST_KAFKA_PORT;

import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixaris.common.zookeeper.test.TestZookeeperServer;

/**
 * Responsible for setting up and managing a Kafka server and its associated Zookeeper instance. This class is intended for use internally on
 * development environments as well as testing.
 *
 * @author aldrin.seychell
 */
public class TestKafkaCluster {
    
    private static final Logger LOG = LoggerFactory.getLogger(TestKafkaCluster.class);
    
    private final int zookeeperPort;
    private final String kafkaHost;
    private final int kafkaPort;
    
    private TestKafkaServer kafkaServer;
    private TestZookeeperServer zkServer;
    
    public TestKafkaCluster() {
        this(TEST_ZK_PORT, TEST_KAFKA_HOST, TEST_KAFKA_PORT);
    }
    
    public TestKafkaCluster(final int zookeeperPort, final int kafkaPort) {
        this(zookeeperPort, TEST_KAFKA_HOST, kafkaPort);
    }
    
    public TestKafkaCluster(final int zookeeperPort, final String kafkaHost, final int kafkaPort) {
        this.zookeeperPort = zookeeperPort;
        this.kafkaHost = kafkaHost;
        this.kafkaPort = kafkaPort;
    }
    
    public void start() {
        LOG.info("Starting TestKafkaCluster. Zookeeper port = " + zookeeperPort);
        zkServer = new TestZookeeperServer(zookeeperPort);
        zkServer.start();
        
        kafkaServer = new TestKafkaServer(kafkaHost, kafkaPort, zkServer.getConnectString());
        kafkaServer.start();
        LOG.info("Started TestKafkaCluster");
    }
    
    public void blockingStart() {
        start();
        
        final CuratorFramework curatorFramework = CuratorFrameworkFactory.newClient(zkServer.getConnectString(), new ExponentialBackoffRetry(100, 6));
        // start connection
        curatorFramework.start();
        for (int i = 0; i < 10; i++) {
            // wait 3 second to establish connect
            try {
                curatorFramework.blockUntilConnected(3, TimeUnit.SECONDS);
                if (curatorFramework.getZookeeperClient().isConnected()) {
                    break;
                }
            } catch (final InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        }
        curatorFramework.close();
    }
    
    public void stop() {
        LOG.info("Shutting down TestKafkaCluster");
        if (kafkaServer != null) {
            kafkaServer.stop();
        }
        
        if (zkServer != null) {
            zkServer.stop();
        }
        
        try {
            // Sleep for some time to allow some graceful cleanup
            // without this sleep the tests go in an unresolvable state where the test is waiting
            // for zookeeper to come back for deregistration but the zookeeper server has already been stopped
            Thread.sleep(1000L);
        } catch (final InterruptedException ignored) {
            Thread.currentThread().interrupt();
        }
        
        LOG.info("Shutting down of TestKafkaCluster complete");
    }
    
    public int getZookeeperPort() {
        return zookeeperPort;
    }
    
    public String getKafkaHost() {
        return kafkaHost;
    }
    
    public int getKafkaPort() {
        return kafkaPort;
    }
    
    public TestZookeeperServer getZkServer() {
        return zkServer;
    }
    
    public TestKafkaServer getKafkaServer() {
        return kafkaServer;
    }
    
}
