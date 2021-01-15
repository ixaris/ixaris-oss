package com.ixaris.commons.kafka.microservices;

import static com.ixaris.commons.async.lib.Async.all;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.CompletionStageUtil.block;
import static org.assertj.core.api.Assertions.assertThat;
import static org.awaitility.Awaitility.await;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

import org.awaitility.Duration;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.AsyncExecutor;
import com.ixaris.commons.async.lib.filter.AsyncFilterChain;
import com.ixaris.commons.kafka.microservices.client.KafkaServiceClientSupport;
import com.ixaris.commons.kafka.microservices.service.KafkaServiceEventDispatcher;
import com.ixaris.commons.kafka.multitenancy.KafkaConnection;
import com.ixaris.commons.kafka.multitenancy.KafkaMultiTenantConnection;
import com.ixaris.commons.kafka.multitenancy.KafkaSettings;
import com.ixaris.commons.kafka.test.TestKafkaCluster;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventAckEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;
import com.ixaris.commons.misc.lib.object.Tuple3;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.test.TestTenants;

public class KafkaIT {
    
    private static final Logger LOG = LoggerFactory.getLogger(KafkaIT.class);
    
    private static final Duration EVENT_RECEIPT_TIMEOUT = Duration.TEN_SECONDS.plus(Duration.TEN_SECONDS);
    
    private static TestKafkaCluster TEST_KAFKA_CLUSTER;
    private static final MultiTenancy MULTI_TENANCY = new MultiTenancy();
    private static KafkaServiceClientSupport kafkaServiceClientSupport;
    private static KafkaServiceEventDispatcher kafkaServiceEventDispatcher;
    
    private ServicePathHolder eventPath;
    private boolean gatewaySubscribed = false;
    private final List<EventEnvelope> eventEnvelopeList = new ArrayList<>();
    private final List<EventEnvelope> gatewayEventEnvelopeList = new ArrayList<>();
    
    @BeforeClass
    public static void setUp() {
        TEST_KAFKA_CLUSTER = new TestKafkaCluster();
        TEST_KAFKA_CLUSTER.start();
        
        MULTI_TENANCY.start();
        
        final String kafkaUrl = TEST_KAFKA_CLUSTER.getKafkaHost() + ":" + TEST_KAFKA_CLUSTER.getKafkaPort();
        final KafkaSettings kfg = KafkaSettings.newBuilder()
            .setUrl(kafkaUrl)
            .setGroupId("test")
            .setTopicPrefix("testTopicPrefix")
            .setPartitions((short) 1)
            .setReplicationFactor((short) 1)
            .build();
        
        LOG.info("Starting Kafka Connection");
        final KafkaMultiTenantConnection kafkaMtConnection = new KafkaMultiTenantConnection(
            new KafkaConnection(AsyncExecutor.DEFAULT, kfg));
        MULTI_TENANCY.registerTenantLifecycleParticipant(kafkaMtConnection);
        
        MULTI_TENANCY.addTenant(TestTenants.DEFAULT);
        
        kafkaServiceClientSupport = new KafkaServiceClientSupport(AsyncExecutor.DEFAULT, kafkaMtConnection);
        kafkaServiceEventDispatcher = new KafkaServiceEventDispatcher(
            "someServiceName", Collections.emptySet(), kafkaMtConnection);
    }
    
    @Before
    public void subscribe() {
        // give every test a unique path
        eventPath = ServicePathHolder.empty().push(String.valueOf(UniqueIdGenerator.generate()));
        
        kafkaServiceClientSupport.createEventHandler("someServiceName",
            TestTenants.DEFAULT,
            eventPath,
            new AsyncFilterChain<EventEnvelope, EventAckEnvelope>(Collections.emptyList()).with(this::handleEvent, this::handleError));
    }
    
    @After
    public void unsubscribe() {
        kafkaServiceClientSupport.destroyEventHandler("someServiceName", TestTenants.DEFAULT, eventPath);
        if (gatewaySubscribed) {
            kafkaServiceClientSupport.destroyEventHandler("someServiceName", "gatewaySubscriber", eventPath);
        }
        gatewaySubscribed = false;
        
        eventPath = null;
    }
    
    @AfterClass
    public static void tearDown() throws Exception {
        MULTI_TENANCY.removeTenant(TestTenants.DEFAULT);
        
        TEST_KAFKA_CLUSTER.stop();
        
        MULTI_TENANCY.stop();
    }
    
    @Test
    public void publishMessage_acknowledgementReceivedByPublisher() throws InterruptedException {
        final long intentId = UniqueIdGenerator.generate();
        final EventAckEnvelope eventAckEnvelope = block(publishNewMessage(intentId));
        assertThat(eventAckEnvelope.getCorrelationId()).isEqualTo(intentId);
    }
    
    @Test
    public void publishMultipleMessage_allAcknowledgementsReceivedByPublisherPublished() throws InterruptedException {
        final long intentId1 = UniqueIdGenerator.generate();
        final long intentId2 = UniqueIdGenerator.generate();
        final long intentId3 = UniqueIdGenerator.generate();
        
        final Tuple3<EventAckEnvelope, EventAckEnvelope, EventAckEnvelope> acks = block(all(
            publishNewMessage(intentId1), publishNewMessage(intentId2), publishNewMessage(intentId3)));
        
        waitForEventReceived(eventEnvelopeList, intentId1, intentId2, intentId3);
        
        // Make sure event is published
        assertThat(new long[] { acks.get1().getCorrelationId(), acks.get2().getCorrelationId(), acks.get3().getCorrelationId() })
            .containsExactlyInAnyOrder(intentId1, intentId2, intentId3);
    }
    
    @Test
    public void subscriberAndPublisher_subscriberReceivesMessage_acknowledgementReceivedByPublisher() throws InterruptedException {
        final long intentId = UniqueIdGenerator.generate();
        final EventAckEnvelope eventAckEnvelope = block(publishNewMessage(intentId));
        assertThat(eventAckEnvelope.getCorrelationId()).isEqualTo(intentId);
        
        waitForEventReceived(eventEnvelopeList, intentId);
        
        final Set<EventEnvelope> eventEnvelopes = eventEnvelopeList.stream()
            .filter(e -> e.getIntentId() == intentId)
            .collect(Collectors.toSet());
        assertThat(eventEnvelopes).hasSize(1);
        final EventEnvelope eventEnvelope = eventEnvelopes.iterator().next();
        assertThat(eventEnvelope.getIntentId()).isEqualTo(intentId);
    }
    
    @Test
    public void singlePublisherAndMultipleSubscribers_subscribersReceivesMessage_acknowledgementReceivedByPublisher() throws InterruptedException {
        kafkaServiceClientSupport.createEventHandler("someServiceName",
            "gatewaySubscriber",
            eventPath,
            new AsyncFilterChain<EventEnvelope, EventAckEnvelope>(Collections.emptyList()).with(this::handleGatewayEvent, this::handleError));
        
        gatewaySubscribed = true;
        
        final long intentId = UniqueIdGenerator.generate();
        final EventAckEnvelope eventAckEnvelope = block(publishNewMessage(intentId));
        assertThat(eventAckEnvelope.getCorrelationId()).isEqualTo(intentId);
        
        waitForEventReceived(eventEnvelopeList, intentId);
        waitForEventReceived(gatewayEventEnvelopeList, intentId);
        
        Set<EventEnvelope> eventEnvelopes = eventEnvelopeList.stream()
            .filter(eventEnvelope -> eventEnvelope.getIntentId() == intentId)
            .collect(Collectors.toSet());
        assertThat(eventEnvelopes).hasSize(1);
        final EventEnvelope receivedEventEnvelope = eventEnvelopes.iterator().next();
        assertThat(receivedEventEnvelope.getIntentId()).isEqualTo(intentId);
        
        eventEnvelopes = gatewayEventEnvelopeList.stream()
            .filter(e -> e.getIntentId() == intentId)
            .collect(Collectors.toSet());
        assertThat(eventEnvelopes).hasSize(1);
        final EventEnvelope eventEnvelope = eventEnvelopes.iterator().next();
        assertThat(eventEnvelope.getIntentId()).isEqualTo(intentId);
    }
    
    private Async<EventAckEnvelope> publishNewMessage(final long id) {
        return kafkaServiceEventDispatcher.dispatch(
            EventEnvelope.newBuilder()
                .setIntentId(id)
                .setCorrelationId(id)
                .setTenantId(TestTenants.DEFAULT)
                .addAllPath(eventPath)
                .build());
    }
    
    private void waitForEventReceived(final List<EventEnvelope> list, final Long... intentIds) {
        final Set<Long> ids = new HashSet<>(Arrays.asList(intentIds));
        await()
            .atMost(EVENT_RECEIPT_TIMEOUT)
            .until(() -> list.stream().filter(eventEnvelope -> ids.contains(eventEnvelope.getIntentId())).count() == ids.size());
    }
    
    private Async<EventAckEnvelope> handleEvent(final EventEnvelope eventEnvelope) {
        eventEnvelopeList.add(eventEnvelope);
        return result(EventAckEnvelope.newBuilder()
            .setStatusCode(ResponseStatusCode.OK)
            .setCorrelationId(eventEnvelope.getCorrelationId())
            .setCallRef(eventEnvelope.getCallRef())
            .build());
    }
    
    private Async<EventAckEnvelope> handleGatewayEvent(final EventEnvelope eventEnvelope) {
        gatewayEventEnvelopeList.add(eventEnvelope);
        return result(EventAckEnvelope.newBuilder()
            .setStatusCode(ResponseStatusCode.OK)
            .setCorrelationId(eventEnvelope.getCorrelationId())
            .setCallRef(eventEnvelope.getCallRef())
            .build());
    }
    
    private Async<EventAckEnvelope> handleError(final EventEnvelope eventEnvelope, final Throwable t) {
        return result(EventAckEnvelope.newBuilder()
            .setStatusCode(ResponseStatusCode.SERVER_ERROR)
            .setCorrelationId(eventEnvelope.getCorrelationId())
            .setCallRef(eventEnvelope.getCallRef())
            .build());
    }
    
}
