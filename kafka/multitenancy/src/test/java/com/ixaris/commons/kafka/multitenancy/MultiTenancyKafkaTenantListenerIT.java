package com.ixaris.commons.kafka.multitenancy;

import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.CompletionStageUtil.block;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;
import static java.nio.charset.StandardCharsets.UTF_8;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.HashSet;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

import org.assertj.core.util.Arrays;
import org.awaitility.Duration;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.AsyncExecutor;
import com.ixaris.commons.kafka.test.TestKafkaCluster;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.test.TestTenants;

/**
 * Tests that focus on the multi-tenancy aspect of the KafkaMultiTenantConnection. Ensures tenants can un/subscribe and receive messages
 * accordingly.
 *
 * <p>Does NOT test the service layer code.
 */
public class MultiTenancyKafkaTenantListenerIT {
    
    private static final Logger LOG = LoggerFactory.getLogger(MultiTenancyKafkaTenantListenerIT.class);
    private static final String TEST_EVENT_NAME = "test_event";
    
    private KafkaMultiTenantConnection connection;
    private final MultiTenancy multiTenancy = new MultiTenancy();
    
    @Test
    public void publishAndSubscribe_afterTenantSetup() throws Exception {
        final TestKafkaCluster testKafkaCluster = new TestKafkaCluster();
        testKafkaCluster.start();
        
        multiTenancy.start();
        final String kafkaUrl = testKafkaCluster.getKafkaHost() + ":" + testKafkaCluster.getKafkaPort();
        final KafkaSettings kafkaSettings = KafkaSettings.newBuilder()
            .setUrl(kafkaUrl)
            .setPartitions((short) 1)
            .setReplicationFactor((short) 1)
            .setGroupId(MultiTenancyKafkaTenantListenerIT.class.getSimpleName())
            .build();
        
        final ScheduledExecutorService executor = AsyncExecutor.DEFAULT;
        
        LOG.info("Starting Kafka Connection");
        connection = new KafkaMultiTenantConnection(new KafkaConnection(executor, kafkaSettings));
        multiTenancy.registerTenantLifecycleParticipant(connection);
        
        multiTenancy.addTenant(TestTenants.DEFAULT);
        multiTenancy.addTenant(TestTenants.LEFT);
        
        TENANT.exec(TestTenants.DEFAULT, () -> assertThat(connection.get()).isNotNull());
        
        // we could also use the value. key was easier!
        TENANT.exec(TestTenants.DEFAULT, () -> block(connection.publish(TEST_EVENT_NAME, 1L, "value".getBytes(UTF_8)), 10, TimeUnit.SECONDS));
        TENANT.exec(TestTenants.LEFT, () -> block(connection.publish(TEST_EVENT_NAME, 11L, "value".getBytes(UTF_8)), 10, TimeUnit.SECONDS));
        
        final TestMessageHandler messageHandler = new TestMessageHandler();
        connection.subscribe(null, TEST_EVENT_NAME, messageHandler);
        
        TENANT.exec(TestTenants.DEFAULT, () -> block(connection.publish(TEST_EVENT_NAME, 2L, "value".getBytes(UTF_8)), 10, TimeUnit.SECONDS));
        TENANT.exec(TestTenants.LEFT, () -> block(connection.publish(TEST_EVENT_NAME, 12L, "value".getBytes(UTF_8)), 10, TimeUnit.SECONDS));
        
        await()
            .atMost(Duration.TEN_SECONDS)
            .until(() -> messageHandler.getKeys().containsAll(Arrays.asList(new String[] { "1", "2", "11", "12" })));
        
        for (int i = 3; i < 10; i++) {
            final long pi = i;
            TENANT.exec(TestTenants.DEFAULT, () -> block(connection.publish(TEST_EVENT_NAME, pi, "value".getBytes(UTF_8)), 10, TimeUnit.SECONDS));
            TENANT.exec(TestTenants.LEFT, () -> block(connection.publish(TEST_EVENT_NAME, pi + 10, "value".getBytes(UTF_8)), 10, TimeUnit.SECONDS));
            
            await()
                .atMost(Duration.TEN_SECONDS)
                .until(() -> messageHandler.getKeys().contains(Long.toString(pi)) && messageHandler.getKeys().contains(Long.toString(pi + 10)));
        }
        
        testKafkaCluster.stop();
        
        multiTenancy.stop();
    }
    
    public static final class TestMessageHandler implements KafkaMessageHandler {
        
        private final Set<String> key = new HashSet<>();
        
        @Override
        public Async<Void> handle(final String partitionKey, final byte[] message) {
            LOG.info("Received message from Kafka! [{}]", message);
            if (key.add(partitionKey)) {
                return result();
            } else {
                throw new IllegalStateException("Already asked to handle " + partitionKey);
            }
        }
        
        Set<String> getKeys() {
            return key;
        }
        
    }
    
}
