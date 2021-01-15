package com.ixaris.commons.microservices.defaults.app;

import static com.ixaris.common.zookeeper.test.TestZookeeperServer.TEST_ZK_PORT;
import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.CompletionStageUtil.all;
import static com.ixaris.commons.async.lib.CompletionStageUtil.block;
import static com.ixaris.commons.kafka.test.TestKafkaServer.TEST_KAFKA_HOST;
import static com.ixaris.commons.kafka.test.TestKafkaServer.TEST_KAFKA_PORT;
import static com.ixaris.commons.misc.lib.exception.ExceptionUtil.sneakyThrow;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.fail;
import static org.springframework.boot.test.context.SpringBootTest.WebEnvironment.DEFINED_PORT;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.assertj.core.api.Assertions;
import org.junit.After;
import org.junit.AfterClass;
import org.junit.Assert;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Ignore;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnit;
import org.mockito.junit.MockitoRule;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.context.ApplicationContextInitializer;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.annotation.Bean;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.TestPropertySourceUtils;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.defaults.app.example.Example.ExampleEvent;
import com.ixaris.commons.microservices.defaults.app.example.Example.ExampleRequest;
import com.ixaris.commons.microservices.defaults.app.example.Example.ExampleResponse;
import com.ixaris.commons.microservices.defaults.app.example.service.ExampleSkeleton;
import com.ixaris.commons.microservices.defaults.app.example2.client.Example2Stub;
import com.ixaris.commons.microservices.defaults.app.example2.service.Example2Skeleton;
import com.ixaris.commons.microservices.defaults.app.examplespi.client.ExampleSpiStub;
import com.ixaris.commons.microservices.defaults.app.examplespi.resource.ExampleSpiResource.ExampleSpiErrorException;
import com.ixaris.commons.microservices.defaults.app.support.StackTestTestEventListener;
import com.ixaris.commons.microservices.defaults.app.support.StackTestTestStack;
import com.ixaris.commons.microservices.defaults.app.test.spring.Example2ResourceSpringImpl;
import com.ixaris.commons.microservices.defaults.context.CommonsMicroservicesDefaultsContext.Context;
import com.ixaris.commons.microservices.lib.client.ServiceEventListener;
import com.ixaris.commons.microservices.lib.client.ServiceEventSubscription;
import com.ixaris.commons.microservices.lib.common.EventAck;
import com.ixaris.commons.microservices.lib.common.ServiceEventHeader;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.microservices.lib.common.exception.ServiceException;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;
import com.ixaris.commons.microservices.secrets.CertificateLoader;
import com.ixaris.commons.microservices.secrets.CertificateLoaderImpl;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;

/**
 * This tests makes use of 2 Example Services: ExampleService and Example2Service, each having their own events ExampleEvent and Example2Event
 * respectively
 *
 * <p>ExampleService publishes ExampleEvent when exampleOperation is called with the same details passed in the operation. It replies with a
 * response only when the publishing was of ExampleEvent was successful
 *
 * <p>Example2Service is subscribed to ExampleEvent and whenever an ExampleEvent is received, it publishes an Example2Event with the same
 * details. example2Operation calls exampleOperation from ExampleService (which internally publishes ExampleEvent) and since Example2Service is
 * subscribed to this event, Example2Event is also published.
 *
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
@SuppressWarnings({ "squid:S1607", "squid:S2925" })
// This is abstract to run the same tests with different spring @ActiveProfile i.e. Local and TCP
@RunWith(SpringRunner.class)
@SpringBootTest(classes = { Application.class, MicroservicesStackIT.Config.class },
                webEnvironment = DEFINED_PORT)
@DirtiesContext
@ContextConfiguration(initializers = MicroservicesStackIT.Initializer.class)
// When using TCP, we require an IP address or hostname to connect. Since tests are run on the same JVM and machine, we
// default to localhost to avoid network adapter issues
@TestPropertySource(properties = { "advertisedUrl=127.0.0.1" })
public class MicroservicesStackIT {
    
    public static class Initializer implements ApplicationContextInitializer<ConfigurableApplicationContext> {
        
        @Override
        public void initialize(final ConfigurableApplicationContext configurableApplicationContext) {
            TestPropertySourceUtils.addInlinedPropertiesToEnvironment(configurableApplicationContext,
                "kafka.url=" + TEST_KAFKA_HOST + ":" + TEST_KAFKA_PORT,
                "zookeeper.url=127.0.0.1:" + TEST_ZK_PORT);
        }
        
    }
    
    public static class Config {
        
        @Bean
        public static CertificateLoader certificateLoader(@Value("${environment.name}") final String environment,
                                                          @Value("${spring.application.name}") final String serviceName) {
            return new CertificateLoaderImpl(environment, serviceName, "../../../secrets");
        }
        
    }
    
    private static final Logger LOG = LoggerFactory.getLogger(MicroservicesStackIT.class);
    private static final String DEFAULT1_TENANT = "default1";
    private static final String DEFAULT2_TENANT = "default2";
    private static final int EVENT_SUBSCRIBE_TIMEOUT_SEC = 20;
    private static final int EVENT_NOT_RECEIVED_TIMEOUT_SEC = 5;
    
    private static ServiceEventSubscription subscription;
    
    private static StackTestTestStack TEST_STACK;
    
    @BeforeClass
    public static void startStack() {
        System.setProperty("org.springframework.boot.logging.LoggingSystem", "com.ixaris.commons.microservices.defaults.app.Log4J2System");
        System.setProperty("environment.name", "test");
        System.setProperty("spring.application.name", "test");
        System.setProperty("server.port", "0"); // avoid port conflicts, allow things to run in parallel
        TEST_STACK = new StackTestTestStack();
        TEST_STACK.start();
    }
    
    @AfterClass
    public static void stopStack() {
        TEST_STACK.stop();
        TEST_STACK = null;
    }
    
    // see javadocs; but basically gives us better errors
    @Rule
    public MockitoRule mockitoRule = MockitoJUnit.rule();
    
    private final ServiceEventListener<Context, ExampleEvent> listener = new StackTestTestEventListener<>();
    
    @Autowired
    private Example2Stub example2Stub;
    
    @Autowired
    private ExampleSpiStub exampleSpiStub;
    
    @Autowired
    private Example2ResourceSpringImpl example2Resource;
    
    @Autowired
    private ExampleSkeleton.Watch examplePublisher;
    
    @Autowired
    private Example2Skeleton.Watch example2Publisher;
    
    @Autowired
    private MultiTenancy multiTenancy;
    
    private final AtomicLong gid = new AtomicLong();
    private final AtomicInteger atomicInteger = new AtomicInteger();
    
    @Before
    public void setup() {
        multiTenancy.addTenant(DEFAULT1_TENANT);
        multiTenancy.addTenant(DEFAULT2_TENANT);
        
        // intentionally only subscribe once - there appear to be timing issues on Kafka's side (client or server,
        // unclear where)
        // when we subscribe/unsubscribe rapidly. not an issue on production - the current use case is to have subscribe
        // (once) on startup and not modify subscriptions
        if (subscription == null) {
            subscription = example2Stub.watch(listener);
        }
    }
    
    @After
    public void tearDown() {
        StackTestTestEventListener.clear();
    }
    
    @Test
    public void testRequestResponseWithSpringAndZMQInproc() throws Throwable {
        
        final int id = 100;
        final CompletionStage<ExampleResponse> future = example2Stub.op2(ServiceOperationHeader.newBuilder(0L,
            DEFAULT1_TENANT,
            Context.getDefaultInstance())
            .build(),
            ExampleRequest.newBuilder().setId(id).setSleepDuration(50).build());
        
        assertEquals("Expecting ID to match by passing the parameter to the service and getting it back",
            id,
            block(future).getId());
    }
    
    @Test
    public void testRequestResponse_SPI() throws Throwable {
        
        final int availableSpis = exampleSpiStub._keys().size();
        assertEquals("Expected 3 SPI implementations to be available but was: " + availableSpis, 3, availableSpis);
        
        final int id = 1;
        
        final ExampleRequest.Builder requestBuilder = ExampleRequest.newBuilder().setId(id);
        
        final Set<String> keys = exampleSpiStub._keys();
        
        LOG.info("Available SPIs: {}", keys);
        
        final List<CompletionStage<ExampleResponse>> futures = new ArrayList<>(keys.size());
        final int timeout = 1000;
        for (final String key : keys) {
            LOG.info("Calling operation on SPI: {}", key);
            final ServiceOperationHeader<Context> context = ServiceOperationHeader.newBuilder(gid.incrementAndGet(),
                DEFAULT1_TENANT,
                Context.getDefaultInstance())
                .withTimeout(timeout)
                .withTargetServiceKey(key)
                .build();
            futures.add(spiOp(context, requestBuilder.build()));
        }
        block(all(futures));
    }
    
    private Async<ExampleResponse> spiOp(final ServiceOperationHeader<Context> context, final ExampleRequest request) {
        try {
            return Async.result(await(exampleSpiStub.op(context, request)));
        } catch (ExampleSpiErrorException e) {
            throw new IllegalStateException(e);
        }
    }
    
    @Test
    public void testEventSubscribe() {
        
        final CompletableFuture<ExampleEvent> exampleEventCompletableFuture = new CompletableFuture<>();
        
        final ExampleEvent event = ExampleEvent.newBuilder().setId(atomicInteger.incrementAndGet()).build();
        final long correlationId = System.currentTimeMillis();
        final ServiceEventHeader<Context> context = ServiceEventHeader.newBuilder(correlationId, DEFAULT1_TENANT, Context.getDefaultInstance())
            .build();
        
        StackTestTestEventListener.put(correlationId, null, exampleEventCompletableFuture);
        examplePublisher.publish(context, event);
        
        final ExampleEvent result;
        try {
            result = exampleEventCompletableFuture.get(EVENT_SUBSCRIBE_TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (Exception e) {
            Assert.fail("Unexpected fail: " + e);
            return;
        }
        
        Assert.assertNotNull("Received event should not be null", result);
        Assert.assertEquals("Event should be equal to the published event", event.getId(), result.getId());
    }
    
    @Test
    public void testEventSubscribe_FailCallbackShouldRetry() {
        final CompletableFuture<ExampleEvent> exampleEventCompletableFuture = new CompletableFuture<>();
        final CompletableFuture<ExampleEvent> exampleEventCompletableFuture_afterAcknowledgement = new CompletableFuture<>();
        
        final long correlationId = 1L;
        
        StackTestTestEventListener.put(correlationId, 1, exampleEventCompletableFuture);
        
        final String tenant = DEFAULT1_TENANT;
        final CompletionStage<ExampleResponse> op1Promise = example2Stub.op2(ServiceOperationHeader.newBuilder(correlationId,
            tenant,
            Context.getDefaultInstance())
            .build(),
            ExampleRequest.newBuilder().setId(1).build());
        
        op1Promise
            .thenCompose(r -> {
                // Publish twice and second message should only be received after confirming the first event
                final long correlationId2 = correlationId + 1L;
                StackTestTestEventListener.put(correlationId2, 1, exampleEventCompletableFuture_afterAcknowledgement);
                return example2Stub.op2(ServiceOperationHeader.newBuilder(correlationId2, tenant, Context.getDefaultInstance()).build(),
                    ExampleRequest.newBuilder().setId(2).build());
            })
            .exceptionally(t -> {
                t.printStackTrace();
                throw sneakyThrow(t);
            });
        
        // This array list should hold the results from the completable futures, in the order that they were received
        final List<Integer> results = Collections.synchronizedList(new ArrayList<>(2));
        final CompletableFuture<Boolean> resultFuture1 = exampleEventCompletableFuture.thenApply(example2Event -> results.add(example2Event.getId()));
        final CompletableFuture<Boolean> resultFuture2 = exampleEventCompletableFuture_afterAcknowledgement.thenApply(example2Event -> results.add(example2Event.getId()));
        
        final CompletableFuture<Void> voidCompletableFuture = CompletableFuture.allOf(resultFuture1, resultFuture2);
        
        try {
            voidCompletableFuture.get(EVENT_SUBSCRIBE_TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (final Exception e) {
            fail("Unexpected error: " + e);
            return;
        }
        
        assertEquals("Expected 2 results to be received", 2L, results.size());
        assertEquals("Expected first result to be with id 1", 1L, results.get(0).longValue());
        assertEquals("Expected first result to be with id 2", 2L, results.get(1).longValue());
    }
    
    @Test
    public void testEventSubscribe_MultiplePublishShouldReceiveThemInOrder() throws InterruptedException {
        
        // Acquire the semaphore from the event listener
        StackTestTestEventListener.eventListenerSemaphore.acquire();
        
        final CompletableFuture<ExampleEvent> exampleEventCompletableFuture = new CompletableFuture<>();
        final CompletableFuture<ExampleEvent> exampleEventCompletableFuture_afterAcknowledgement = new CompletableFuture<>();
        
        final long intentId = System.currentTimeMillis();
        
        final int firstId = 100;
        final int secondId = firstId + 1;
        
        StackTestTestEventListener.put(intentId, 1, exampleEventCompletableFuture);
        
        final String tenant = DEFAULT1_TENANT;
        final ServiceEventHeader<Context> context = ServiceEventHeader.newBuilder(intentId, tenant, Context.getDefaultInstance()).build();
        final ExampleEvent event = ExampleEvent.newBuilder().setId(firstId).build();
        
        example2Publisher
            .publish(context, event)
            .thenCompose(r -> {
                final long correlationId2 = intentId + 1L;
                StackTestTestEventListener.put(correlationId2, 0, exampleEventCompletableFuture_afterAcknowledgement);
                
                final ServiceEventHeader<Context> context2 = ServiceEventHeader.newBuilder(correlationId2, tenant, Context.getDefaultInstance())
                    .build();
                final ExampleEvent event2 = ExampleEvent.newBuilder().setId(secondId).build();
                return example2Publisher.publish(context2, event2);
            })
            .thenAccept(r -> {
                // Start processing events .. should receive them in order as they were published
                StackTestTestEventListener.eventListenerSemaphore.release();
            });
        
        // This array list should hold the results from the completable futures, in the order that they were received
        final List<Integer> results = Collections.synchronizedList(new ArrayList<>(2));
        final CompletableFuture<Boolean> resultFuture1 = exampleEventCompletableFuture.thenApply(exampleEvent -> results.add(exampleEvent.getId()));
        final CompletableFuture<Boolean> resultFuture2 = exampleEventCompletableFuture_afterAcknowledgement.thenApply(exampleEvent -> results.add(exampleEvent.getId()));
        
        final CompletableFuture<Void> voidCompletableFuture = CompletableFuture.allOf(resultFuture1, resultFuture2);
        
        try {
            voidCompletableFuture.get(EVENT_SUBSCRIBE_TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (final Exception e) {
            Assertions.fail("Unexpected exception while waiting for events to be received in order for tenant: " + tenant, e);
            return;
        }
        
        assertEquals("Expected 2 results to be received", 2L, results.size());
        assertEquals("Expected first result to be with id " + firstId, firstId, results.get(0).longValue());
        assertEquals("Expected first result to be with id " + secondId, secondId, results.get(1).longValue());
    }
    
    @Test
    public void testEventSubscribe_AllAvailableTenantsAreSubscribed() {
        // subscribe on a topic... make sure all tenants available are subscribed to it
        // publish on one tenant and ascertain that it is only consumed by that tenant
        LOG.info("Starting test testEventSubscribe_AllAvailableTenantsAreSubscribed ------------------");
        for (final String tenant : multiTenancy.getActiveTenants()) {
            for (int i = 0; i < 3; i++) {
                final CompletableFuture<ExampleEvent> future = new CompletableFuture<>();
                final ExampleEvent event = ExampleEvent.newBuilder().setId((int) UniqueIdGenerator.generate()).build();
                
                final long intentId = System.currentTimeMillis();
                final ServiceEventHeader<Context> context = ServiceEventHeader.newBuilder(intentId, tenant, Context.getDefaultInstance())
                    .build();
                StackTestTestEventListener.put(intentId, null, future);
                
                TENANT.exec(tenant, () -> examplePublisher.publish(context, event));
                
                final ExampleEvent result;
                try {
                    result = future.get(EVENT_SUBSCRIBE_TIMEOUT_SEC, TimeUnit.SECONDS);
                } catch (final Exception e) {
                    Assertions.fail(String.format("Unexpected exception while waiting for message with intent id [%s] on tenant: %s",
                        intentId,
                        tenant),
                        e);
                    return;
                }
                
                Assert.assertEquals("Event should be equal to the published event", event.getId(), result.getId());
            }
        }
    }
    
    @Test
    public void testEventUnsubscribe_AllTenantsUnsubscribedAndPublishingOnAllTenantsFails() throws Exception {
        
        // unsubscribe the test setup.. all tenants should be unsubscribed by default publishing on any tenant should
        // not work and should result in a timeout
        subscription.cancel();
        subscription = null;
        // setupTenants = false;
        
        for (final String tenant : multiTenancy.getActiveTenants()) {
            final CompletableFuture<ExampleEvent> exampleEventCompletableFuture = new CompletableFuture<>();
            
            final ExampleEvent event = ExampleEvent.newBuilder().build();
            
            final long correlationId = System.currentTimeMillis();
            final ServiceEventHeader<Context> context = ServiceEventHeader.newBuilder(correlationId, tenant, Context.getDefaultInstance())
                .build();
            StackTestTestEventListener.put(correlationId, null, exampleEventCompletableFuture);
            
            final CompletionStage<EventAck> p = examplePublisher.publish(context, event);
            
            try {
                block(p, 1, TimeUnit.SECONDS);
                // ok
            } catch (final TimeoutException e) {
                Assert.fail("Should have received event acknowledgement");
            } catch (Throwable e) {
                Assert.fail("Unexpected exception: " + e);
            }
            
            try {
                exampleEventCompletableFuture.get(EVENT_NOT_RECEIVED_TIMEOUT_SEC, TimeUnit.SECONDS);
                Assert.fail("Should not have received event");
            } catch (TimeoutException e) {
                // ok to timeout.. thats what we are expecting
            }
        }
    }
    
    @Test
    public void testEventSubscribe_TenantAddedAfterAndAutomaticallySubscribed() {
        
        LOG.info("Starting test testEventSubscribe_TenantAddedAfterAndAutomaticallySubscribed ------------------");
        final String newTenant = "theNewTenant" + System.currentTimeMillis();
        
        // add new tenant, this should automatically be subscribed to the past topics
        multiTenancy.addTenant(newTenant);
        
        final CompletableFuture<ExampleEvent> exampleEventCompletableFuture = new CompletableFuture<>();
        
        final ExampleEvent event = ExampleEvent.newBuilder().build();
        final long correlationId = System.currentTimeMillis();
        final ServiceEventHeader<Context> context = ServiceEventHeader.newBuilder(correlationId, newTenant, Context.getDefaultInstance())
            .build();
        
        StackTestTestEventListener.put(correlationId, null, exampleEventCompletableFuture);
        
        LOG.info("Publishing event {}", event);
        final CompletionStage<EventAck> publishPromise = examplePublisher.publish(context, event);
        
        try {
            block(publishPromise, 5, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOG.error("Unexpected exception while publishing message with correlation id [{}] on tenant: {}", correlationId, newTenant);
            Assertions.fail(String.format("Unexpected exception while publishing message with correlation id [%s] on tenant: %s",
                correlationId,
                newTenant),
                e);
            return;
        }
        
        final ExampleEvent result;
        try {
            result = exampleEventCompletableFuture.get(EVENT_SUBSCRIBE_TIMEOUT_SEC, TimeUnit.SECONDS);
        } catch (final Exception e) {
            LOG.error("Unexpected exception while waiting for message with correlation id [{}] on tenant: {}", correlationId, newTenant);
            Assertions.fail(String.format("Unexpected exception while waiting for message with correlation id [%s] on tenant: %s",
                correlationId,
                newTenant),
                e);
            return;
        }
        
        Assert.assertEquals("Event should be equal to the published event", event.getId(), result.getId());
    }
    
    @Test
    @Ignore("no longer guaranteed to stop publishing when tenant removed")
    public void testEventUnsubscribe_TenantRemovedAndAutomaticallyUnsubscribed() throws Throwable {
        
        multiTenancy.removeTenant(DEFAULT1_TENANT);
        
        final CompletableFuture<ExampleEvent> exampleEventCompletableFuture = new CompletableFuture<>();
        
        final ExampleEvent event = ExampleEvent.newBuilder().build();
        
        final long correlationId = System.currentTimeMillis();
        final ServiceEventHeader<Context> context = ServiceEventHeader.newBuilder(correlationId, DEFAULT1_TENANT, Context.getDefaultInstance())
            .build();
        StackTestTestEventListener.put(correlationId, null, exampleEventCompletableFuture);
        
        final CompletionStage<EventAck> p = examplePublisher.publish(context, event);
        
        try {
            block(p, EVENT_NOT_RECEIVED_TIMEOUT_SEC, TimeUnit.SECONDS);
            Assert.fail("Shouldn't have received event ack for inactive tenant");
        } catch (final ServiceException e) {
            assertEquals("Invalid Status Code", ResponseStatusCode.SERVER_ERROR, e.getStatusCode());
        }
    }
    
    /**
     * the scope of this test is to test the exponential backoff for retrying sending the messages the delay time between one attempt and the
     * next should exponentially increase verification is done manually hence this test is ignored as it makes no sense leaving it running
     * (besides taking quite some time to run)
     */
    @Ignore("read comment above")
    @Test
    public void testExponentialBackoff_messageRetryDelayedExponentially() {
        
        final CompletableFuture<ExampleEvent> exampleEventCompletableFuture = new CompletableFuture<>();
        
        final ExampleEvent event = ExampleEvent.newBuilder().build();
        
        final long correlationId = 22;
        final ServiceEventHeader<Context> context = ServiceEventHeader.newBuilder(correlationId, DEFAULT1_TENANT, Context.getDefaultInstance())
            .build();
        StackTestTestEventListener.put(correlationId, 5, exampleEventCompletableFuture);
        
        final CompletionStage<EventAck> p = examplePublisher.publish(context, event);
        
        try {
            exampleEventCompletableFuture.get(100, TimeUnit.SECONDS);
            Assert.fail("Should not have received the message during this time");
        } catch (TimeoutException e) {
            // ok
        } catch (InterruptedException | ExecutionException e) {
            Assert.fail("Unexpected exception:" + e);
        }
        
        try {
            block(p, 150, TimeUnit.SECONDS);
            Assert.fail("Shouldn't have received event");
        } catch (Exception e) {
            Assert.fail("Unexpected exception: " + e);
        }
    }
    
}
