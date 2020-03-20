package com.ixaris.commons.microservices.defaults.live;

import static com.ixaris.commons.async.lib.Async.awaitExceptions;
import static com.ixaris.commons.async.lib.CompletionStageUtil.all;
import static com.ixaris.commons.async.lib.CompletionStageUtil.block;
import static com.ixaris.commons.async.lib.CompletionStageUtil.join;
import static com.ixaris.commons.misc.lib.exception.ExceptionUtil.sneakyThrow;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.concurrent.atomic.AtomicInteger;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.runner.RunWith;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.defaults.context.CommonsMicroservicesDefaultsContext.Context;
import com.ixaris.commons.microservices.defaults.live.example.Example.ExampleEvent;
import com.ixaris.commons.microservices.defaults.live.example.Example.ExampleRequest;
import com.ixaris.commons.microservices.defaults.live.example.Example.ExampleResponse;
import com.ixaris.commons.microservices.defaults.live.example.Example2SkeletonImpl;
import com.ixaris.commons.microservices.defaults.live.example.service.ExampleSkeleton;
import com.ixaris.commons.microservices.defaults.live.example2.client.Example2Stub;
import com.ixaris.commons.microservices.defaults.live.example2.service.Example2Skeleton;
import com.ixaris.commons.microservices.defaults.live.examplespi.client.ExampleSpiStub;
import com.ixaris.commons.microservices.defaults.live.examplespi.resource.ExampleSpiResource.ExampleSpiErrorException;
import com.ixaris.commons.microservices.defaults.live.support.StackTestTestEventListener;
import com.ixaris.commons.microservices.lib.client.ServiceEventSubscription;
import com.ixaris.commons.microservices.lib.common.EventAck;
import com.ixaris.commons.microservices.lib.common.ServiceEventHeader;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.test.TestTenants;

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
@SuppressWarnings({ "AbstractClassWithoutAbstractMethods", "squid:S1607", "squid:S2925" })
// This is abstract to run the same tests with different spring @ActiveProfile i.e. Local and TCP
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = LiveConfiguration.class)
public abstract class AbstractMicroservicesStackIT {
    
    private static final Logger LOG = LoggerFactory.getLogger(AbstractMicroservicesStackIT.class);
    
    private static final int EVENT_SUBSCRIBE_TIMEOUT_SEC = 20;
    private static final int EVENT_NOT_RECEIVED_TIMEOUT_SEC = 5;
    
    @BeforeClass
    public static void initProperties() {
        System.setProperty("environment.name", "test");
        System.setProperty("server.port", "0");
        System.setProperty("spring.application.name", "defaults_live_stack_test");
        System.setProperty("certificates.rootpath", "dummy");
        System.setProperty("kafka.replicationFactor", "1");
    }
    
    private ServiceEventSubscription subscription;
    
    @Autowired
    private Example2Stub example2Stub;
    
    @Autowired
    private ExampleSpiStub exampleSpiStub;
    
    @Autowired
    private Example2SkeletonImpl example2Resource;
    
    @Autowired
    private ExampleSkeleton.Watch examplePublisher;
    
    @Autowired
    private Example2Skeleton.Watch example2Publisher;
    
    @Autowired
    private MultiTenancy multiTenancy;
    
    private final AtomicInteger atomicInteger = new AtomicInteger();
    
    @Before
    public void setup() {
        multiTenancy.addTenant(TestTenants.DEFAULT);
        multiTenancy.addTenant(TestTenants.LEFT);
    }
    
    @After
    public void teardown() {
        multiTenancy.removeTenant(TestTenants.DEFAULT);
        multiTenancy.removeTenant(TestTenants.LEFT);
    }
    
    @Test
    public void testRequestResponse() throws Throwable {
        final int id = 100;
        final Async<ExampleResponse> future = TENANT.exec(TestTenants.DEFAULT, () -> example2Stub.op2(ServiceOperationHeader.newBuilder(Context.getDefaultInstance()).build(),
            ExampleRequest.newBuilder().setId(id).setSleepDuration(50).build()));
        
        assertEquals("Expecting ID to match by passing the parameter to the service and getting it back",
            id,
            block(future).getId());
    }
    
    @Test
    public void testRequestResponse_SPI() {
        TENANT.exec(TestTenants.DEFAULT, () -> {
            Awaitility.await().atMost(1, TimeUnit.SECONDS).until(() -> exampleSpiStub._keys().size() == 3);
            
            final int id = 1;
            
            final ExampleRequest.Builder requestBuilder = ExampleRequest.newBuilder().setId(id);
            
            final Set<String> keys = exampleSpiStub._keys();
            
            LOG.info("Available SPIs: {}", keys);
            
            final List<CompletionStage<ExampleResponse>> futures = new ArrayList<>(keys.size());
            final int timeout = 6000;
            for (final String key : keys) {
                LOG.info("Calling operation on SPI: {}", key);
                final ServiceOperationHeader<Context> context = ServiceOperationHeader.newBuilder(Context
                    .getDefaultInstance())
                    .withTimeout(timeout)
                    .withTargetServiceKey(key)
                    .build();
                futures.add(spiOp(context, requestBuilder.build()));
            }
            join(all(futures));
        });
    }
    
    private Async<ExampleResponse> spiOp(final ServiceOperationHeader<Context> context, final ExampleRequest request) {
        try {
            return awaitExceptions(exampleSpiStub.op(context, request));
        } catch (final ExampleSpiErrorException e) {
            throw new IllegalStateException(e);
        }
    }
    
    @Test
    public void testEventSubscribe() {
        TENANT.exec(TestTenants.DEFAULT, () -> {
            final CompletableFuture<ExampleEvent> exampleEventCompletableFuture = new CompletableFuture<>();
            
            final ExampleEvent event = ExampleEvent.newBuilder().setId(atomicInteger.incrementAndGet()).build();
            final ServiceEventHeader<Context> header = ServiceEventHeader.newBuilder(Context.getDefaultInstance())
                .build();
            final StackTestTestEventListener<Context, ExampleEvent> stackTestTestEventListener = new StackTestTestEventListener<>();
            final ServiceEventSubscription subscription = example2Stub.watch(stackTestTestEventListener);
            
            stackTestTestEventListener.put(header.getIntentId(), null, exampleEventCompletableFuture);
            examplePublisher.publish(header, event);
            
            final ExampleEvent result;
            try {
                result = exampleEventCompletableFuture.get(EVENT_SUBSCRIBE_TIMEOUT_SEC, TimeUnit.SECONDS);
            } catch (Exception e) {
                fail("Unexpected fail: " + e);
                return;
            }
            
            subscription.cancel();
            stackTestTestEventListener.clear();
            
            assertNotNull("Received event should not be null", result);
            assertEquals("Event should be equal to the published event", event.getId(), result.getId());
        });
    }
    
    @Test
    public void testEventSubscribe_FailCallbackShouldRetry() {
        TENANT.exec(TestTenants.DEFAULT, () -> {
            final StackTestTestEventListener<Context, ExampleEvent> stackTestTestEventListener = new StackTestTestEventListener<>();
            final ServiceEventSubscription subscription = example2Stub.watch(stackTestTestEventListener);
            
            final CompletableFuture<ExampleEvent> exampleEventCompletableFuture = new CompletableFuture<>();
            final CompletableFuture<ExampleEvent> exampleEventCompletableFuture_afterAcknowledgement = new CompletableFuture<>();
            
            final ServiceOperationHeader<Context> header = ServiceOperationHeader.newBuilder(Context
                .getDefaultInstance())
                .build();
            stackTestTestEventListener.put(header.getIntentId(), 1, exampleEventCompletableFuture);
            
            final CompletionStage<ExampleResponse> op1Promise = example2Stub.op2(header,
                ExampleRequest.newBuilder().setId(1).build());
            
            op1Promise
                .thenCompose(r -> {
                    // Publish twice and second message should only be received after confirming the first event
                    final ServiceOperationHeader<Context> header2 = ServiceOperationHeader.newBuilder(Context
                        .getDefaultInstance())
                        .build();
                    
                    stackTestTestEventListener.put(header2.getIntentId(),
                        1,
                        exampleEventCompletableFuture_afterAcknowledgement);
                    return example2Stub.op2(header2, ExampleRequest.newBuilder().setId(2).build());
                })
                .exceptionally(t -> {
                    t.printStackTrace();
                    throw sneakyThrow(t);
                });
                
            // This array list should hold the results from the completable futures, in the order that they were received
            final List<Integer> results = Collections.synchronizedList(new ArrayList<>(2));
            final CompletableFuture<Boolean> resultFuture1 = exampleEventCompletableFuture.thenApply(example2Event -> results.add(example2Event.getId()));
            final CompletableFuture<Boolean> resultFuture2 = exampleEventCompletableFuture_afterAcknowledgement
                .thenApply(example2Event -> results.add(example2Event.getId()));
            
            final CompletableFuture<Void> voidCompletableFuture = CompletableFuture.allOf(resultFuture1, resultFuture2);
            
            try {
                voidCompletableFuture.get(EVENT_SUBSCRIBE_TIMEOUT_SEC, TimeUnit.SECONDS);
            } catch (final Exception e) {
                fail("Unexpected error: " + e);
                return;
            }
            
            subscription.cancel();
            stackTestTestEventListener.clear();
            
            assertEquals("Expected 2 results to be received", 2L, results.size());
            assertEquals("Expected first result to be with id 1", 1L, results.get(0).longValue());
            assertEquals("Expected first result to be with id 2", 2L, results.get(1).longValue());
        });
    }
    
    @Test
    public void testEventSubscribe_AllAvailableTenantsAreSubscribed() {
        // subscribe on a topic... make sure all tenants available are subscribed to it
        // publish on one tenant and ascertain that it is only consumed by that tenant
        LOG.info("Starting test testEventSubscribe_AllAvailableTenantsAreSubscribed ------------------");
        final StackTestTestEventListener<Context, ExampleEvent> stackTestTestEventListener = new StackTestTestEventListener<>();
        final ServiceEventSubscription subscription = example2Stub.watch(stackTestTestEventListener);
        
        for (final String tenantId : multiTenancy.getActiveTenants()) {
            TENANT.exec(tenantId, () -> {
                for (int i = 0; i < 3; i++) {
                    final ExampleEvent event = ExampleEvent.newBuilder()
                        .setId((int) UniqueIdGenerator.generate())
                        .build();
                    
                    final ServiceEventHeader<Context> header = ServiceEventHeader.newBuilder(Context
                        .getDefaultInstance())
                        .build();
                    final CompletableFuture<ExampleEvent> future = new CompletableFuture<>();
                    stackTestTestEventListener.put(header.getIntentId(), null, future);
                    
                    examplePublisher.publish(header, event);
                    
                    final ExampleEvent result;
                    try {
                        result = future.get(EVENT_SUBSCRIBE_TIMEOUT_SEC, TimeUnit.SECONDS);
                    } catch (final Exception e) {
                        Assertions.fail(String
                            .format("Unexpected exception while waiting for message with intent id [%s] on tenant: %s",
                                header.getIntentId(),
                                tenantId),
                            e);
                        return;
                    }
                    
                    assertEquals("Event should be equal to the published event", event.getId(), result.getId());
                }
            });
        }
        subscription.cancel();
        stackTestTestEventListener.clear();
    }
    
    @Test
    public void testEventUnsubscribe_AllTenantsUnsubscribedAndPublishingOnAllTenantsFails() throws Exception {
        // unsubscribe the test setup.. all tenants should be unsubscribed by default publishing on any tenant should
        // not work and should result in a timeout
        final StackTestTestEventListener<Context, ExampleEvent> stackTestTestEventListener = new StackTestTestEventListener<>();
        
        for (final String tenantId : multiTenancy.getActiveTenants()) {
            TENANT.exec(tenantId, () -> {
                final CompletableFuture<ExampleEvent> exampleEventCompletableFuture = new CompletableFuture<>();
                
                final ExampleEvent event = ExampleEvent.newBuilder().build();
                
                final ServiceEventHeader<Context> header = ServiceEventHeader.newBuilder(Context.getDefaultInstance())
                    .build();
                stackTestTestEventListener.put(header.getIntentId(), null, exampleEventCompletableFuture);
                
                final CompletionStage<EventAck> p = examplePublisher.publish(header, event);
                
                try {
                    block(p, 1, TimeUnit.SECONDS);
                    // ok
                } catch (final TimeoutException e) {
                    fail("Should have received event acknowledgement");
                } catch (Throwable e) {
                    fail("Unexpected exception: " + e);
                }
                
                try {
                    exampleEventCompletableFuture.get(EVENT_NOT_RECEIVED_TIMEOUT_SEC, TimeUnit.SECONDS);
                    fail("Should not have received event");
                } catch (TimeoutException e) {
                    // ok to timeout.. thats what we are expecting
                }
            });
        }
        
        stackTestTestEventListener.clear();
    }
    
    @Test
    public void testEventSubscribe_TenantAddedAfterAndAutomaticallySubscribed() {
        final StackTestTestEventListener<Context, ExampleEvent> stackTestTestEventListener = new StackTestTestEventListener<>();
        final ServiceEventSubscription subscription = example2Stub.watch(stackTestTestEventListener);
        
        LOG.info("Starting test testEventSubscribe_TenantAddedAfterAndAutomaticallySubscribed ------------------");
        final String newTenant = "thenewtenant" + System.currentTimeMillis();
        
        // add new tenant, this should automatically be subscribed to the past topics
        multiTenancy.addTenant(newTenant);
        
        TENANT.exec(newTenant, () -> {
            final CompletableFuture<ExampleEvent> exampleEventCompletableFuture = new CompletableFuture<>();
            
            final ExampleEvent event = ExampleEvent.newBuilder().build();
            final ServiceEventHeader<Context> header = ServiceEventHeader.newBuilder(Context.getDefaultInstance())
                .build();
            
            stackTestTestEventListener.put(header.getIntentId(), null, exampleEventCompletableFuture);
            
            LOG.info("Publishing event {}", event);
            final CompletionStage<EventAck> publishPromise = examplePublisher.publish(header, event);
            
            try {
                block(publishPromise, 5, TimeUnit.SECONDS);
            } catch (final Exception e) {
                Assertions.fail(String.format("Unexpected exception while publishing message with correlation id [%s] on tenant: %s", header.getIntentId(), newTenant), e);
                return;
            }
            
            final ExampleEvent result;
            try {
                result = exampleEventCompletableFuture.get(EVENT_SUBSCRIBE_TIMEOUT_SEC, TimeUnit.SECONDS);
            } catch (final Exception e) {
                Assertions.fail(String.format("Unexpected exception while waiting for message with correlation id [%s] on tenant: %s", header.getIntentId(), newTenant), e);
                return;
            }
            
            subscription.cancel();
            stackTestTestEventListener.clear();
            
            assertEquals("Event should be equal to the published event", event.getId(), result.getId());
        });
    }
    
}
