package com.ixaris.commons.kafka.test;

import java.util.Collections;
import java.util.Properties;

import org.apache.kafka.common.utils.Time;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.io.Files;

import com.ixaris.commons.misc.lib.net.Localhost;

import kafka.server.KafkaConfig;
import kafka.server.KafkaServer;
import scala.collection.JavaConverters;

/**
 * Responsible for setting up and managing a Kafka server. This class is intended for use internally only for development environments as well as
 * testing.
 *
 * @author aldrin.seychell
 */
public class TestKafkaServer {
    
    private static final Logger LOG = LoggerFactory.getLogger(TestKafkaServer.class);
    
    public static final String TEST_KAFKA_HOST = "localhost";
    public static final int TEST_KAFKA_PORT = Localhost.findAvailableTcpPort(19092, 19192);
    
    private final String zookeeperConnectString;
    private final String kafkaHost;
    private final int kafkaPort;
    
    private KafkaServer kafkaServer;
    
    public TestKafkaServer(final String kafkaHost, final int kafkaPort, final String zookeeperConnectString) {
        this.zookeeperConnectString = zookeeperConnectString;
        this.kafkaHost = kafkaHost;
        this.kafkaPort = kafkaPort;
    }
    
    public void start() {
        final KafkaConfig config = getKafkaConfig(kafkaHost, String.valueOf(kafkaPort), zookeeperConnectString);
        // We are using KafkaServer directly an not KafkaServerStartable since KafkaServerStartable calls system.exit()
        // whenever it has problems starting up the server
        // instead of just throwing an exception
        kafkaServer = new KafkaServer(config,
            Time.SYSTEM,
            scala.Option.apply("test-kafka-server"),
            JavaConverters.asScalaBuffer(Collections.emptyList()));
        kafkaServer.startup();
    }
    
    public void stop() {
        if (kafkaServer != null) {
            try {
                kafkaServer.shutdown();
                kafkaServer.awaitShutdown();
            } catch (final Exception e) {
                LOG.error("Error when stopping test kafka server", e);
            }
        }
    }
    
    private static KafkaConfig getKafkaConfig(final String kafkaHost, final String kafkaPort, final String zkConnectString) {
        final Properties props = new Properties();
        
        props.setProperty("broker.id", "1");
        props.setProperty("listeners", "PLAINTEXT://0.0.0.0:" + kafkaPort);
        props.setProperty("advertised.listeners", "PLAINTEXT://" + kafkaHost + ":" + kafkaPort);
        
        props.setProperty("log.dir", Files.createTempDir().getAbsolutePath());
        props.setProperty("log.flush.interval.messages", "1");
        props.setProperty("zookeeper.connect", zkConnectString);
        props.setProperty("replica.socket.timeout.ms", "1500");
        
        props.setProperty("zookeeper.connect", zkConnectString);
        props.setProperty("num.partitions", "1");
        props.setProperty("default.replication.factor", "1");
        
        // The following is required to allow kafka offsets storage since default replication factor is 3 and for
        // testing purposes we only have 1 broker
        props.setProperty("offsets.topic.num.partitions", "1");
        props.setProperty("offsets.topic.replication.factor", "1");
        
        // Limit the number of threads used by kafka during tests
        props.setProperty("num.network.threads", "1");
        props.setProperty("num.io.threads", "1");
        props.setProperty("background.threads", "1");
        
        props.setProperty("auto.create.topics.enable", "false");
        
        return new KafkaConfig(props);
    }
    
}
