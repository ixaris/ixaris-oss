package com.ixaris.commons.jooq.microservices;

import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.CompletionStageUtil.join;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;
import static com.ixaris.commons.multitenancy.lib.data.DataUnit.DATA_UNIT;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.Collections;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Supplier;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.clustering.lib.service.ClusterRegistry;
import com.ixaris.commons.jooq.microservices.example.Example.ExampleContext;
import com.ixaris.commons.jooq.microservices.example.Example.ExampleEvent;
import com.ixaris.commons.jooq.microservices.example.client.ExampleStub;
import com.ixaris.commons.jooq.microservices.example.resource.ExampleResource;
import com.ixaris.commons.jooq.microservices.support.JooqHikariTestHelper;
import com.ixaris.commons.jooq.microservices.support.TestClusterRegistry;
import com.ixaris.commons.jooq.persistence.JooqAsyncPersistenceProvider;
import com.ixaris.commons.jooq.persistence.JooqMultiTenancyConfiguration;
import com.ixaris.commons.microservices.lib.client.ServiceEventListener;
import com.ixaris.commons.microservices.lib.client.proxy.ServiceStubProxy;
import com.ixaris.commons.microservices.lib.client.support.ServiceClientLoggingFilterFactory;
import com.ixaris.commons.microservices.lib.client.support.ServiceClientSupport;
import com.ixaris.commons.microservices.lib.client.support.ServiceOperationDispatcher;
import com.ixaris.commons.microservices.lib.common.ServiceEventHeader;
import com.ixaris.commons.microservices.lib.common.ServiceHandlerStrategy;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.local.LocalService;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventAckEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;
import com.ixaris.commons.microservices.lib.service.support.ServiceAsyncInterceptor;
import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;
import com.ixaris.commons.multitenancy.lib.async.ExecutorMultiTenantAtLeastOnceProcessorFactory;
import com.ixaris.commons.multitenancy.test.TestTenants;
import com.ixaris.commons.persistence.lib.AsyncPersistenceProvider;

public final class AtLeastOnceServiceHandleEventTest {
    
    private static final String SERVICE_NAME = "jooq_microservices";
    private static final String SECURITY = "security";
    private static final ServicePathHolder PATH = ServicePathHolder.empty();
    private static final ClusterRegistry CLUSTER_REGISTRY = new TestClusterRegistry();
    
    private ScheduledExecutorService executor;
    private AtLeastOnceHandleEventType eventType;
    private JooqEventListenerFactory listenerFactory;
    
    private JooqHikariTestHelper testHelper;
    private ServiceClientSupport serviceClientSupport;
    private ExampleStub stub;
    
    @BeforeEach
    public void setup() {
        testHelper = new JooqHikariTestHelper(Collections.singleton(SERVICE_NAME));
        final JooqAsyncPersistenceProvider db = new JooqAsyncPersistenceProvider(JooqMultiTenancyConfiguration.createDslContext(testHelper.getDataSource()));
        
        executor = Executors.newScheduledThreadPool(1);
        final ExecutorMultiTenantAtLeastOnceProcessorFactory factory = new ExecutorMultiTenantAtLeastOnceProcessorFactory(executor);
        
        final LocalService localService = new LocalService(Collections.emptySet(), Collections.emptySet());
        serviceClientSupport = new ServiceClientSupport(
            executor,
            testHelper.getMultiTenancy(),
            5000,
            name -> () -> localService.getServiceKeys(name),
            ServiceAsyncInterceptor.PASSTHROUGH,
            ServiceHandlerStrategy.PASSTHROUGH,
            Collections.singleton(new ServiceClientLoggingFilterFactory()),
            null) {
            
            @Override
            protected ServiceOperationDispatcher createOperationDispatcher(final String serviceName) {
                return new ServiceOperationDispatcher() {
                    
                    @Override
                    public Async<ResponseEnvelope> dispatch(final RequestEnvelope requestEnvelope) {
                        return null;
                    }
                    
                    @Override
                    public boolean isKeyAvailable(final String key) {
                        return false;
                    }
                    
                };
            }
            
            @Override
            protected void createEventHandler(final String serviceName,
                                              final String subscriberName,
                                              final ServicePathHolder path,
                                              final AsyncFilterNext<EventEnvelope, EventAckEnvelope> filterChain) {}
            
            @Override
            protected void destroyEventHandler(final String serviceName, final String subscriberName, final ServicePathHolder path) {}
            
        };
        
        // stub
        stub = serviceClientSupport.getOrCreate(ExampleStub.class).createProxy();
        
        eventType = new AtLeastOnceHandleEventType(
            db, CLUSTER_REGISTRY, serviceClientSupport, factory, 1000L, Collections.singleton(SERVICE_NAME));
        eventType.start();
        listenerFactory = new JooqEventListenerFactory(eventType);
        testHelper.getMultiTenancy().registerTenantLifecycleParticipant(eventType);
        testHelper.getMultiTenancy().addTenant(TestTenants.DEFAULT);
    }
    
    @AfterEach
    public void tearDown() {
        eventType.stop();
        testHelper.destroy();
    }
    
    @Test
    public void itShouldPassThroughSuccessHandleSucceeded() {
        final ServiceEventListener<ExampleContext, ExampleEvent> listener = listener(Async::result);
        
        final ServiceEventListener<ExampleContext, ExampleEvent> wrapped = listenerFactory.create(listener);
        
        final ServiceStubProxy<?> proxy = serviceClientSupport.get(ExampleResource.NAME);
        stub.watch(wrapped);
        final EventAckEnvelope ack = join(TENANT.exec(TestTenants.DEFAULT, () -> proxy.getEventProcessor(PATH, SERVICE_NAME).process(envelope(header(), event(1)))));
        assertThat(ack.getStatusCode()).isEqualTo(ResponseStatusCode.OK);
    }
    
    @Test
    public void itShouldRetryFailedRequests() {
        final AtomicInteger count = new AtomicInteger();
        final ServiceEventListener<ExampleContext, ExampleEvent> listener = listener(() -> {
            if (count.getAndIncrement() == 0) {
                throw new RuntimeException("failure");
            }
            return result();
        });
        
        final ServiceEventListener<ExampleContext, ExampleEvent> wrapped = listenerFactory.create(listener);
        
        final ServiceStubProxy<?> proxy = serviceClientSupport.get(ExampleResource.NAME);
        stub.watch(wrapped);
        final EventAckEnvelope ack = join(TENANT.exec(TestTenants.DEFAULT, () -> proxy.getEventProcessor(PATH, SERVICE_NAME).process(envelope(header(), event(1)))));
        assertThat(ack.getStatusCode()).isEqualTo(ResponseStatusCode.OK);
        assertThat(count.get()).isEqualTo(1);
        Awaitility.await().atMost(2, TimeUnit.SECONDS).until(() -> count.get() == 2);
    }
    
    private ServiceEventListener<ExampleContext, ExampleEvent> listener(final Supplier<Async<Void>> supplier) {
        return new ServiceEventListener<ExampleContext, ExampleEvent>() {
            
            @Override
            public String getName() {
                return SERVICE_NAME;
            }
            
            @Override
            public Async<Void> onEvent(final ServiceEventHeader<ExampleContext> header, final ExampleEvent event) {
                return supplier.get();
            }
            
            @Override
            public <X, E extends Exception> X aroundAsync(final CallableThrows<X, E> callable) throws E {
                return DATA_UNIT.exec(SERVICE_NAME, callable);
            }
            
        };
    }
    
    private static ExampleContext context() {
        return ExampleContext.newBuilder().setSecurity(SECURITY).build();
    }
    
    private static ExampleEvent event(final int id) {
        return ExampleEvent.newBuilder().setId(id).build();
    }
    
    private static ServiceEventHeader<ExampleContext> header() {
        return TENANT.exec(TestTenants.DEFAULT, () -> ServiceEventHeader.newBuilder(context()).withPartitionId(0L).build());
    }
    
    private static EventEnvelope envelope(final ServiceEventHeader<ExampleContext> header, final ExampleEvent event) {
        final EventEnvelope.Builder builder = EventEnvelope.newBuilder()
            .setCorrelationId(header.getCorrelationId())
            .setCallRef(UniqueIdGenerator.generate())
            .setParentRef(header.getCallRef())
            .setServiceName(ExampleResource.NAME)
            .setServiceKey("") // ignore the service key in the header and assign own key
            .addAllPath(PATH)
            .setPartitionId(header.getPartitionId())
            .setIntentId(header.getIntentId())
            .setContext(header.getContext().toByteString())
            .setPayload(event.toByteString());
        
        if (header.getTenantId() != null) {
            builder.setTenantId(header.getTenantId());
        }
        
        return builder.build();
    }
    
}
