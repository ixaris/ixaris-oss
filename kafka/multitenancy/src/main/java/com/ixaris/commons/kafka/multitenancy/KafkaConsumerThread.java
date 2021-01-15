package com.ixaris.commons.kafka.multitenancy;

import static com.ixaris.commons.async.lib.Async.all;
import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;

import java.time.Duration;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Properties;
import java.util.stream.Collectors;

import org.apache.kafka.clients.CommonClientConfigs;
import org.apache.kafka.clients.consumer.ConsumerConfig;
import org.apache.kafka.clients.consumer.ConsumerRecord;
import org.apache.kafka.clients.consumer.ConsumerRecords;
import org.apache.kafka.clients.consumer.KafkaConsumer;
import org.apache.kafka.clients.consumer.OffsetAndMetadata;
import org.apache.kafka.common.TopicPartition;
import org.apache.kafka.common.errors.WakeupException;
import org.apache.kafka.common.serialization.ByteArrayDeserializer;
import org.apache.kafka.common.serialization.StringDeserializer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.CompletionStageUtil;
import com.ixaris.commons.collections.lib.GuavaCollections;

final class KafkaConsumerThread extends Thread {
    
    private static final Logger LOG = LoggerFactory.getLogger(KafkaConsumerThread.class);
    
    private static final class TopicPartitionPendingInfo {
        
        private int failureCount = 0;
        private long nextRetryTime = System.currentTimeMillis();
        private int curIndex = 0;
        private final List<ConsumerRecord<String, byte[]>> records;
        private final long lastOffset;
        
        private TopicPartitionPendingInfo(final List<ConsumerRecord<String, byte[]>> records) {
            this.records = new ArrayList<>(records);
            lastOffset = records.get(records.size() - 1).offset();
        }
        
    }
    
    private final KafkaConnection connection;
    private final String groupId;
    private volatile KafkaConsumer<String, byte[]> consumer;
    private volatile ImmutableMap<String, KafkaMessageHandler> topics = ImmutableMap.of();
    private final Map<TopicPartition, TopicPartitionPendingInfo> backoff = new HashMap<>();
    
    public KafkaConsumerThread(final KafkaConnection connection, final String groupId) {
        super("KafkaConsumer-" + groupId);
        this.connection = connection;
        this.groupId = groupId;
    }
    
    @SuppressWarnings({ "squid:S1698", "squid:S1166" })
    @Override
    public void run() {
        final Properties props = new Properties();
        props.put(CommonClientConfigs.BOOTSTRAP_SERVERS_CONFIG, connection.kafkaSettings.getUrl());
        props.put(ConsumerConfig.GROUP_ID_CONFIG, groupId);
        props.put(ConsumerConfig.KEY_DESERIALIZER_CLASS_CONFIG, StringDeserializer.class.getName());
        props.put(ConsumerConfig.VALUE_DESERIALIZER_CLASS_CONFIG, ByteArrayDeserializer.class.getName());
        props.put(ConsumerConfig.AUTO_OFFSET_RESET_CONFIG, "earliest");
        props.put(ConsumerConfig.REQUEST_TIMEOUT_MS_CONFIG, "20000");
        props.put(ConsumerConfig.RETRY_BACKOFF_MS_CONFIG, "500");
        
        this.consumer = new KafkaConsumer<>(props);
        ImmutableMap<String, KafkaMessageHandler> subscribedTopics = ImmutableMap.of();
        
        while (connection.active.get()) {
            if (subscribedTopics != topics) {
                consumer.subscribe(topics.keySet());
                subscribedTopics = topics;
            }
            
            try {
                final List<Async<Void>> partitions = new ArrayList<>();
                final Map<TopicPartition, OffsetAndMetadata> commit = new HashMap<>();
                if (subscribedTopics.isEmpty()) {
                    waitForTopicSubscriptions();
                } else {
                    processReceivedMessages(subscribedTopics, partitions, commit);
                }
                
                retryFailedMessages(subscribedTopics, partitions, commit);
                
                CompletionStageUtil.join(all(partitions));
                consumer.commitSync(commit);
            } catch (final WakeupException ignored) {
                // ignore for shutdown
            }
        }
        
        consumer.close();
    }
    
    @SuppressWarnings("squid:S2274")
    private void waitForTopicSubscriptions() {
        synchronized (backoff) {
            try {
                backoff.wait(connection.kafkaSettings.getMaxBlock());
            } catch (final InterruptedException e) {
                interrupt();
            }
        }
    }
    
    private void processReceivedMessages(final ImmutableMap<String, KafkaMessageHandler> topics,
                                         final List<Async<Void>> partitions,
                                         final Map<TopicPartition, OffsetAndMetadata> commit) {
        final Duration pollDuration = Duration.ofMillis(connection.kafkaSettings.getMaxBlock());
        final ConsumerRecords<String, byte[]> records = consumer.poll(pollDuration);
        for (final TopicPartition partition : records.partitions()) {
            synchronized (backoff) {
                if (backoff.containsKey(partition)) {
                    LOG.error("Partition [{}] received from kafka while pending commit", partition, new IllegalStateException());
                }
            }
            final TopicPartitionPendingInfo pendingInfo = new TopicPartitionPendingInfo(records.records(partition));
            final KafkaMessageHandler handler = topics.get(partition.topic());
            partitions.add(preparePartitionCommitOrBackoff(partition, handler, pendingInfo, commit));
        }
    }
    
    private void retryFailedMessages(final ImmutableMap<String, KafkaMessageHandler> topics,
                                     final List<Async<Void>> partitions,
                                     final Map<TopicPartition, OffsetAndMetadata> commit) {
        final Map<TopicPartition, TopicPartitionPendingInfo> retryPartitions;
        synchronized (backoff) {
            retryPartitions = backoff.entrySet()
                .stream()
                .filter(e -> e.getValue().nextRetryTime <= System.currentTimeMillis())
                .collect(Collectors.toMap(Entry::getKey, Entry::getValue));
        }
        for (final Entry<TopicPartition, TopicPartitionPendingInfo> partition : retryPartitions.entrySet()) {
            final KafkaMessageHandler handler = topics.get(partition.getKey().topic());
            partitions.add(preparePartitionCommitOrBackoff(partition.getKey(), handler, partition.getValue(), commit));
        }
    }
    
    @SuppressWarnings({ "squid:S2445", "squid:S1181" })
    private Async<Void> preparePartitionCommitOrBackoff(final TopicPartition partition,
                                                        final KafkaMessageHandler handler,
                                                        final TopicPartitionPendingInfo topicPartitionPendingInfo,
                                                        final Map<TopicPartition, OffsetAndMetadata> commit) {
        for (; topicPartitionPendingInfo.curIndex < topicPartitionPendingInfo.records.size(); topicPartitionPendingInfo.curIndex++) {
            final ConsumerRecord<String, byte[]> record = topicPartitionPendingInfo.records.get(topicPartitionPendingInfo.curIndex);
            try {
                await(handler.handle(record.key(), record.value()));
                topicPartitionPendingInfo.records.set(topicPartitionPendingInfo.curIndex, null); // done
            } catch (final Throwable t) {
                topicPartitionPendingInfo.failureCount++;
                topicPartitionPendingInfo.nextRetryTime = exponentialBackoff(topicPartitionPendingInfo.failureCount);
                if (topicPartitionPendingInfo.failureCount == 1) {
                    // add on first failure
                    synchronized (backoff) {
                        backoff.put(partition, topicPartitionPendingInfo);
                    }
                }
                LOG.error("Failed to process message (attempt {}) of event {}; not committed to Kafka.",
                    topicPartitionPendingInfo.failureCount,
                    partition.topic(),
                    t);
                return result();
            }
        }
        
        synchronized (commit) {
            commit.put(partition, new OffsetAndMetadata(topicPartitionPendingInfo.lastOffset + 1));
        }
        if (topicPartitionPendingInfo.failureCount > 0) {
            synchronized (backoff) {
                backoff.remove(partition);
            }
        }
        return result();
    }
    
    synchronized void shutdown() {
        consumer.wakeup();
        synchronized (backoff) {
            backoff.notifyAll();
        }
        try {
            join();
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
        }
    }
    
    synchronized boolean subscribe(final String topic, final KafkaMessageHandler handler) {
        if (!topics.containsKey(topic)) {
            topics = GuavaCollections.copyOfMapAdding(topics, topic, handler);
            if (consumer != null) {
                consumer.wakeup();
            }
            return true;
        } else {
            return false;
        }
    }
    
    synchronized boolean unsubscribe(final String topic) {
        if (topics.containsKey(topic)) {
            topics = GuavaCollections.copyOfMapRemoving(topics, topic);
            if (consumer != null) {
                consumer.wakeup();
            }
            return true;
        } else {
            return false;
        }
    }
    
    boolean isEmpty() {
        return topics.isEmpty();
    }
    
    private long exponentialBackoff(final int failureCount) {
        return System.currentTimeMillis() + Math.min(connection.kafkaSettings.getMinBackoff() * failureCount * failureCount, connection.kafkaSettings.getMaxBackoff());
    }
    
}
