package com.ixaris.commons.kafka.multitenancy;

import java.util.Collections;
import java.util.Properties;
import java.util.Set;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.BiConsumer;
import java.util.stream.Collectors;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.admin.AdminClient;
import org.apache.kafka.clients.admin.CreateTopicsResult;
import org.apache.kafka.clients.admin.NewTopic;
import org.apache.kafka.clients.producer.KafkaProducer;
import org.apache.kafka.clients.producer.ProducerConfig;
import org.apache.kafka.clients.producer.ProducerRecord;
import org.apache.kafka.common.errors.TopicExistsException;
import org.apache.kafka.common.serialization.ByteArraySerializer;
import org.apache.kafka.common.serialization.StringSerializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableSet;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.AsyncExecutor;
import com.ixaris.commons.async.lib.FutureAsync;
import com.ixaris.commons.collections.lib.GuavaCollections;

final class KafkaProducerHelper {
    
    private static final Logger LOG = LoggerFactory.getLogger(KafkaProducerHelper.class);
    
    private final KafkaConnection connection;
    private volatile KafkaProducer<String, byte[]> producer;
    private volatile AdminClient adminClient;
    
    @SuppressWarnings("squid:S3077")
    private volatile ImmutableSet<String> toCreateTopics = ImmutableSet.of();
    
    private volatile ImmutableSet<String> createdTopics = ImmutableSet.of();
    
    KafkaProducerHelper(final KafkaConnection connection) {
        this.connection = connection;
    }
    
    synchronized void start() {
        final Properties props = new Properties();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, connection.kafkaSettings.getUrl());
        adminClient = AdminClient.create(props);
        
        register(toCreateTopics);
        toCreateTopics = ImmutableSet.of();
        
        props.put(ProducerConfig.KEY_SERIALIZER_CLASS_CONFIG, StringSerializer.class.getName());
        props.put(ProducerConfig.VALUE_SERIALIZER_CLASS_CONFIG, ByteArraySerializer.class.getName());
        props.put(ProducerConfig.MAX_BLOCK_MS_CONFIG, Long.toString(connection.kafkaSettings.getMaxBlock()));
        props.put(ProducerConfig.REQUEST_TIMEOUT_MS_CONFIG, "20000");
        props.put(ProducerConfig.RETRY_BACKOFF_MS_CONFIG, "500");
        producer = new KafkaProducer<>(props);
    }
    
    synchronized void shutdown() {
        producer.close();
        producer = null;
        adminClient.close();
        adminClient = null;
    }
    
    void register(final Set<String> topics) {
        if (topics.stream().anyMatch(t -> !createdTopics.contains(t))) {
            createTopics(topics);
        }
    }
    
    @SuppressWarnings("squid:S1166")
    private synchronized void createTopics(final Set<String> topics) {
        // recheck just in case created by other thread
        final Set<String> toCreate = topics.stream()
            .filter(t -> !createdTopics.contains(t))
            .collect(Collectors.toSet());
        if (!toCreate.isEmpty()) {
            if (adminClient != null) {
                LOG.debug("Creating topics {}.", toCreate);
                try {
                    final CreateTopicsResult result = adminClient.createTopics(toCreate.stream()
                        .map(t -> new NewTopic(t, connection.kafkaSettings.getPartitions(), connection.kafkaSettings.getReplicationFactor()))
                        .collect(Collectors.toSet()));
                    result.all().get();
                    createdTopics = GuavaCollections.copyOfSetAdding(createdTopics, toCreate);
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                } catch (final ExecutionException e) {
                    if (!(e.getCause() instanceof TopicExistsException)) {
                        LOG.error("Error creating topics {}.", toCreate, e.getCause());
                    }
                    // ignore topic already exists
                }
            } else {
                toCreateTopics = GuavaCollections.copyOfSetAdding(toCreateTopics, toCreate);
            }
        }
    }
    
    Async<Void> publish(final String topic, final long partition, final byte[] message) {
        final FutureAsync<Void> future = new FutureAsync<>();
        final BiConsumer<? super Void, ? super Throwable> relayConsumer = AsyncExecutor.relayConsumer(future);
        
        final ScheduledFuture<?> schedule = connection.executor.schedule(
            () -> {
                if (!future.isDone()) {
                    relayConsumer.accept(
                        null,
                        new TimeoutException(String.format(
                            "Failed to ack event published to %s in %d ms",
                            topic,
                            connection.kafkaSettings.getAckTimeout())));
                }
            },
            connection.kafkaSettings.getAckTimeout(),
            TimeUnit.MILLISECONDS);
        
        // make sure topic is created
        register(Collections.singleton(topic));
        
        final ProducerRecord<String, byte[]> record = new ProducerRecord<>(topic, Long.toString(partition), message);
        producer.send(record, (r, e) -> {
            schedule.cancel(false);
            relayConsumer.accept(null, e);
        });
        
        return future;
    }
    
}
