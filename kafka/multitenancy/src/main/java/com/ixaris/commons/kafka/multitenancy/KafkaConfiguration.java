package com.ixaris.commons.kafka.multitenancy;

import static com.ixaris.commons.kafka.multitenancy.KafkaSettings.DEFAULT_PARTITIONS;
import static com.ixaris.commons.kafka.multitenancy.KafkaSettings.DEFAULT_REPLICATION_FACTOR;

import java.util.concurrent.ScheduledExecutorService;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.core.env.Environment;

import com.ixaris.commons.misc.spring.EnvDefaults;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;

@Configuration
public class KafkaConfiguration {
    
    private static final String KAFKA_TOPIC_PREFIX_PROP = "kafka.topicPrefix";
    
    @Bean
    @ConditionalOnProperty(value = "local", matchIfMissing = true, havingValue = "false")
    public KafkaConnectionHandler kafkaConnectionHandler(final MultiTenancy multiTenancy,
                                                         final ScheduledExecutorService executor,
                                                         final KafkaSettings kafkaSettings) {
        final KafkaMultiTenantConnection conn = new KafkaMultiTenantConnection(new KafkaConnection(executor, kafkaSettings));
        multiTenancy.registerTenantLifecycleParticipant(conn);
        return conn;
    }
    
    @Bean
    public KafkaSettings kafkaSettings(@Value("${spring.application.name}") final String serviceName,
                                       @Value("${environment.name}") final String envName,
                                       @Value("${kafka.url:kafka-service:9092}") final String kafkaUrl,
                                       @Value("${kafka.partitions:" + DEFAULT_PARTITIONS + "}") final short kafkaPartitions,
                                       @Value("${kafka.replicationFactor:" + DEFAULT_REPLICATION_FACTOR + "}") final short kafkaReplicationFactor,
                                       final Environment environment) {
        // services with the same group id "compete" with each other for Kafka events -
        // we want only one service per "type" to respond (regardless of replicas), so use the name as the group id
        return KafkaSettings.newBuilder()
            .setUrl(kafkaUrl)
            .setTopicPrefix(getKafkaTopicPrefix(environment, envName))
            .setPartitions(kafkaPartitions)
            .setReplicationFactor(kafkaReplicationFactor)
            .setGroupId(serviceName)
            .build();
    }
    
    static String getKafkaTopicPrefix(final Environment env, final String defaultValue) {
        return EnvDefaults.getOrDefault(env, KAFKA_TOPIC_PREFIX_PROP, defaultValue);
    }
    
}
