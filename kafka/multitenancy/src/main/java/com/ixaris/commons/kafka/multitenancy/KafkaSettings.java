package com.ixaris.commons.kafka.multitenancy;

import java.util.Optional;
import java.util.concurrent.TimeUnit;

public final class KafkaSettings {
    
    public static KafkaConfigurationBuilder newBuilder() {
        return new KafkaConfigurationBuilder();
    }
    
    /**
     * This value should be less than DEFAULT_ACK_TIMEOUT, so we don't reply with a timeout for an event but in reality
     * the event is delivered to kafka.
     */
    private static final long DEFAULT_MAX_BLOCK = 20000L;
    
    private static final String DEFAULT_TOPIC_PREFIX = "";
    static final short DEFAULT_PARTITIONS = 18;
    static final short DEFAULT_REPLICATION_FACTOR = 3;
    private static final long DEFAULT_MAX_BACKOFF = TimeUnit.SECONDS.toMillis(600L);
    private static final long DEFAULT_MIN_BACKOFF = TimeUnit.SECONDS.toMillis(10L);
    
    /**
     * Time before failing an event publish acknowledgement with a timeout exception.
     */
    private static final long DEFAULT_ACK_TIMEOUT = TimeUnit.SECONDS.toMillis(20L);
    
    private final String url;
    private final String topicPrefix;
    private final short partitions;
    private final short replicationFactor;
    private final String groupId;
    private final long maxBlock;
    private final long ackTimeout;
    private final long minBackoff;
    private final long maxBackoff;
    
    private KafkaSettings(final String url,
                          final String topicPrefix,
                          final Short partitions,
                          final Short replicationFactor,
                          final String groupId,
                          final Long maxBlock,
                          final Long ackTimeoutMs,
                          final Long minBackoffMs,
                          final Long maxBackoffMs) {
        this.url = url;
        this.topicPrefix = Optional.ofNullable(topicPrefix).orElse(DEFAULT_TOPIC_PREFIX);
        this.partitions = Optional.ofNullable(partitions).orElse(DEFAULT_PARTITIONS);
        this.replicationFactor = Optional.ofNullable(replicationFactor).orElse(DEFAULT_REPLICATION_FACTOR);
        this.groupId = groupId;
        this.maxBlock = maxBlock != null ? maxBlock : DEFAULT_MAX_BLOCK;
        ackTimeout = orDefault(ackTimeoutMs, DEFAULT_ACK_TIMEOUT);
        minBackoff = orDefault(minBackoffMs, DEFAULT_MIN_BACKOFF);
        maxBackoff = orDefault(maxBackoffMs, DEFAULT_MAX_BACKOFF);
    }
    
    private static long orDefault(final Long durationMs, final long defaultDuration) {
        return Optional.ofNullable(durationMs).orElse(defaultDuration);
    }
    
    public String getUrl() {
        return url;
    }
    
    public String getTopicPrefix() {
        return topicPrefix;
    }
    
    public short getPartitions() {
        return partitions;
    }
    
    public short getReplicationFactor() {
        return replicationFactor;
    }
    
    public String getGroupId() {
        return groupId;
    }
    
    public long getMaxBlock() {
        return maxBlock;
    }
    
    public long getAckTimeout() {
        return ackTimeout;
    }
    
    public long getMinBackoff() {
        return minBackoff;
    }
    
    public long getMaxBackoff() {
        return maxBackoff;
    }
    
    public static final class KafkaConfigurationBuilder {
        
        private String url;
        private String topicPrefix;
        private Short partitions;
        private Short replicationFactor;
        private String groupId;
        private Long maxBlockMs;
        private Long ackTimeoutMs;
        private Long minBackoffMs;
        private Long maxBackoffMs;
        
        private KafkaConfigurationBuilder() {}
        
        public KafkaConfigurationBuilder setUrl(final String url) {
            this.url = url;
            return this;
        }
        
        public KafkaConfigurationBuilder setTopicPrefix(final String topicPrefix) {
            this.topicPrefix = topicPrefix;
            return this;
        }
        
        public KafkaConfigurationBuilder setPartitions(final Short partitions) {
            this.partitions = partitions;
            return this;
        }
        
        public KafkaConfigurationBuilder setReplicationFactor(final Short replicationFactor) {
            this.replicationFactor = replicationFactor;
            return this;
        }
        
        public KafkaConfigurationBuilder setGroupId(final String groupId) {
            this.groupId = groupId;
            return this;
        }
        
        public KafkaConfigurationBuilder setMaxBlockMs(final Long maxBlockMs) {
            this.maxBlockMs = maxBlockMs;
            return this;
        }
        
        public KafkaConfigurationBuilder setAckTimeoutMs(final Long ackTimeoutMs) {
            this.ackTimeoutMs = ackTimeoutMs;
            return this;
        }
        
        public KafkaConfigurationBuilder setMinBackoffMs(final Long minBackoffMs) {
            this.minBackoffMs = minBackoffMs;
            return this;
        }
        
        public KafkaConfigurationBuilder setMaxBackoffMs(final Long maxBackoffMs) {
            this.maxBackoffMs = maxBackoffMs;
            return this;
        }
        
        public KafkaSettings build() {
            return new KafkaSettings(url,
                topicPrefix,
                partitions,
                replicationFactor,
                groupId,
                maxBlockMs,
                ackTimeoutMs,
                minBackoffMs,
                maxBackoffMs);
        }
    }
}
