package com.ixaris.commons.kafka.multitenancy;

import static com.ixaris.commons.async.lib.Async.result;
import static java.nio.charset.StandardCharsets.UTF_8;
import static java.util.concurrent.TimeUnit.MINUTES;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.Test;

import com.ixaris.commons.async.lib.AsyncExecutor;
import com.ixaris.commons.kafka.test.TestKafkaCluster;

public final class BasicTest {
    
    @Test
    public void testSuccess() throws InterruptedException, ExecutionException, TimeoutException {
        final TestKafkaCluster kafka = new TestKafkaCluster();
        kafka.start();
        try {
            final ScheduledExecutorService executor = AsyncExecutor.DEFAULT;
            final KafkaSettings kafkaSettings = KafkaSettings.newBuilder()
                .setUrl(kafka.getKafkaHost() + ":" + kafka.getKafkaPort())
                .setTopicPrefix("test")
                .setPartitions((short) 1)
                .setReplicationFactor((short) 1)
                .setGroupId("testing")
                .setMaxBlockMs(5000L)
                .setAckTimeoutMs(5000L)
                .setMinBackoffMs(5000L)
                .setMaxBackoffMs(5000L)
                .build();
            final KafkaConnection kafkaConnection = new KafkaConnection(executor, kafkaSettings);
            kafkaConnection.start();
            
            try {
                final CompletableFuture<Long> future = new CompletableFuture<>();
                kafkaConnection.subscribe("sub", "topic", (p, m) -> {
                    future.complete(Long.parseLong(p));
                    return result();
                });
                
                kafkaConnection.publish("topic", 1L, "message".getBytes(UTF_8)).toCompletableFuture().join();
                assertThat(future.get(1L, MINUTES)).isEqualTo(1L);
            } finally {
                kafkaConnection.stop();
            }
        } finally {
            kafka.stop();
        }
    }
    
    @Test
    public void testAtLeastOnce() throws InterruptedException, ExecutionException, TimeoutException {
        final TestKafkaCluster kafka = new TestKafkaCluster();
        kafka.start();
        try {
            final ScheduledExecutorService executor = AsyncExecutor.DEFAULT;
            final KafkaSettings kafkaSettings = KafkaSettings.newBuilder()
                .setUrl(kafka.getKafkaHost() + ":" + kafka.getKafkaPort())
                .setTopicPrefix("test")
                .setPartitions((short) 1)
                .setReplicationFactor((short) 1)
                .setGroupId("testing")
                .setMaxBlockMs(5000L)
                .setAckTimeoutMs(5000L)
                .setMinBackoffMs(5000L)
                .setMaxBackoffMs(5000L)
                .build();
            final KafkaConnection kafkaConnection = new KafkaConnection(executor, kafkaSettings);
            kafkaConnection.start();
            
            try {
                final CompletableFuture<Long> future = new CompletableFuture<>();
                final AtomicBoolean failed = new AtomicBoolean();
                kafkaConnection.subscribe("sub", "topic", (p, m) -> {
                    if (failed.compareAndSet(false, true)) {
                        throw new IllegalStateException("Test instructed to fail");
                    } else {
                        future.complete(Long.parseLong(p));
                        return result();
                    }
                });
                
                kafkaConnection.publish("topic", 1L, "message".getBytes(UTF_8)).toCompletableFuture().join();
                assertThat(future.get(1L, MINUTES)).isEqualTo(1L);
            } finally {
                kafkaConnection.stop();
            }
        } finally {
            kafka.stop();
        }
    }
    
}
