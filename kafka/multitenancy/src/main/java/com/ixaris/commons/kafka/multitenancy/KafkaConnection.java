package com.ixaris.commons.kafka.multitenancy;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixaris.commons.async.lib.Async;

/**
 * This class represents the connection to Kafka for publishing/subscribing to events
 *
 * @author <a href="mailto:Armand.Sciberras@ixaris.com">Armand.Sciberras</a>
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 * @author <a href="mailto:david.borg@ixaris.com">david.borg</a>
 */
public class KafkaConnection implements KafkaConnectionHandler {
    
    private static final Logger LOG = LoggerFactory.getLogger(KafkaConnection.class);
    private static final String TOPIC_SEPARATOR = ".";
    
    final ScheduledExecutorService executor;
    final KafkaSettings kafkaSettings;
    final AtomicBoolean active = new AtomicBoolean(false);
    
    private final KafkaProducerHelper producer;
    private final Map<String, KafkaConsumerThread> consumers = new HashMap<>();
    
    public KafkaConnection(final ScheduledExecutorService executor, final KafkaSettings kafkaSettings) {
        this.executor = executor;
        this.kafkaSettings = kafkaSettings;
        producer = new KafkaProducerHelper(this);
    }
    
    public synchronized void start() {
        if (active.compareAndSet(false, true)) {
            producer.start();
            consumers.values().forEach(KafkaConsumerThread::start);
        }
    }
    
    public synchronized void stop() {
        if (active.compareAndSet(true, false)) {
            LOG.info("Shutting down KafkaConnection [{}]", this);
            
            try {
                consumers.values().forEach(KafkaConsumerThread::shutdown);
                producer.shutdown();
                LOG.info("KafkaConnection shutdown complete [{}]", this);
            } catch (final RuntimeException t) {
                LOG.error("Failed to shutdown KafkaConnection", t);
            }
        }
    }
    
    @Override
    public void register(final Set<String> topics) {
        producer.register(topics.stream().map(this::getPrefixedTopic).collect(Collectors.toSet()));
    }
    
    @Override
    public Async<Void> publish(final String topic, final long partition, final byte[] message) {
        if (!active.get()) {
            throw new IllegalStateException("Connection is closed");
        }
        final String prefixedTopic = getPrefixedTopic(topic);
        return producer.publish(prefixedTopic, partition, message);
    }
    
    @Override
    public synchronized void subscribe(final String subscriberName, final String topic, final KafkaMessageHandler messageHandler) {
        final String groupId = resolveGroupId(subscriberName);
        final KafkaConsumerThread consumer = consumers.computeIfAbsent(groupId, k -> {
            final KafkaConsumerThread t = new KafkaConsumerThread(this, k);
            if (active.get()) {
                t.start();
            }
            return t;
        });
        final String prefixedTopic = getPrefixedTopic(topic);
        if (!consumer.subscribe(prefixedTopic, messageHandler)) {
            throw new IllegalStateException("Already registered [" + prefixedTopic + "] for [" + groupId + "]");
        } else {
            LOG.info("Added [{}] to the kafka maintained topics for group id [{}]", topic, groupId);
        }
    }
    
    @Override
    public synchronized void unsubscribe(final String subscriberName, final String topic) {
        if (!active.get()) {
            throw new IllegalStateException("Connection is closed");
        }
        final String groupId = resolveGroupId(subscriberName);
        final String prefixedTopic = getPrefixedTopic(topic);
        final KafkaConsumerThread consumer = consumers.get(groupId);
        if ((consumer == null) || !consumer.unsubscribe(prefixedTopic)) {
            throw new IllegalStateException("Not registered [" + prefixedTopic + "] for [" + groupId + "]");
        } else {
            LOG.info("Removed [{}] from the kafka maintained topics for group id [{}]", topic, groupId);
        }
        if (consumer.isEmpty()) {
            consumers.remove(groupId);
        }
    }
    
    private String resolveGroupId(final String subscriberName) {
        return subscriberName == null ? kafkaSettings.getGroupId() : kafkaSettings.getGroupId() + subscriberName;
    }
    
    private String getPrefixedTopic(final String qualifiedEventName) {
        return String.format("%s%s%s", kafkaSettings.getTopicPrefix(), TOPIC_SEPARATOR, qualifiedEventName);
    }
    
}
