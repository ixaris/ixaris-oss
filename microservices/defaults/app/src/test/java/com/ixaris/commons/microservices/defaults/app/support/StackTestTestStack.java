package com.ixaris.commons.microservices.defaults.app.support;

import static com.ixaris.common.zookeeper.test.TestZookeeperServer.TEST_ZK_PORT;
import static com.ixaris.commons.kafka.test.TestKafkaServer.TEST_KAFKA_PORT;

import java.util.concurrent.TimeUnit;

import org.apache.curator.framework.CuratorFramework;
import org.apache.curator.framework.CuratorFrameworkFactory;
import org.apache.curator.retry.ExponentialBackoffRetry;

import com.ixaris.commons.kafka.test.TestKafkaCluster;

/**
 * Keep in sync between commons-microservices-stack and commons-microservices-stack-test
 *
 * @author brian.vella
 */
public class StackTestTestStack {
    
    private final TestKafkaCluster testKafkaCluster;
    
    public StackTestTestStack() {
        this.testKafkaCluster = new TestKafkaCluster(TEST_ZK_PORT, TEST_KAFKA_PORT);
    }
    
    public void start() {
        testKafkaCluster.blockingStart();
    }
    
    public void stop() {
        testKafkaCluster.stop();
    }
    
}
