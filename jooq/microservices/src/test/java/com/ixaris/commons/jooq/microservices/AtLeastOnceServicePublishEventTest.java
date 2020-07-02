package com.ixaris.commons.jooq.microservices;

import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.CompletionStageUtil.join;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;
import static com.ixaris.commons.multitenancy.lib.data.DataUnit.DATA_UNIT;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.timeout;
import static org.mockito.Mockito.when;

import java.util.Collections;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;

import com.ixaris.commons.async.lib.AsyncLocal;
import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.clustering.lib.service.ClusterRegistry;
import com.ixaris.commons.jooq.microservices.example.Example.ExampleContext;
import com.ixaris.commons.jooq.microservices.example.Example.ExampleEvent;
import com.ixaris.commons.jooq.microservices.example.ExampleSkeletonImpl;
import com.ixaris.commons.jooq.microservices.example.service.ExampleSkeleton;
import com.ixaris.commons.jooq.microservices.example.service.ExampleSkeleton.Watch;
import com.ixaris.commons.jooq.microservices.support.JooqHikariTestHelper;
import com.ixaris.commons.jooq.microservices.support.TestClusterRegistry;
import com.ixaris.commons.jooq.microservices.support.TestServiceSecurityChecker;
import com.ixaris.commons.jooq.persistence.JooqAsyncPersistenceProvider;
import com.ixaris.commons.jooq.persistence.JooqMultiTenancyConfiguration;
import com.ixaris.commons.microservices.lib.common.ServiceEventHeader;
import com.ixaris.commons.microservices.lib.common.ServiceHandlerStrategy;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.local.LocalService;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventAckEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;
import com.ixaris.commons.microservices.lib.service.ServiceEventPublisher;
import com.ixaris.commons.microservices.lib.service.proxy.ServiceSkeletonProxy;
import com.ixaris.commons.microservices.lib.service.support.DefaultServiceKeys;
import com.ixaris.commons.microservices.lib.service.support.ServiceAsyncInterceptor;
import com.ixaris.commons.microservices.lib.service.support.ServiceEventDispatcher;
import com.ixaris.commons.microservices.lib.service.support.ServiceLoggingFilterFactory;
import com.ixaris.commons.microservices.lib.service.support.ServiceSupport;
import com.ixaris.commons.multitenancy.lib.async.ExecutorMultiTenantAtLeastOnceProcessorFactory;
import com.ixaris.commons.multitenancy.test.TestTenants;
import com.ixaris.commons.persistence.microservices.AtLeastOnceServiceEventPublisher;
import com.ixaris.commons.persistence.microservices.PersistenceEventPublisherFactory;

public final class AtLeastOnceServicePublishEventTest {
    
    private static final String SERVICE_NAME = "jooq_microservices";
    private static final String SECURITY = "security";
    private static final ServicePathHolder PATH = ServicePathHolder.empty();
    private static final ClusterRegistry CLUSTER_REGISTRY = new TestClusterRegistry();
    
    private AtLeastOncePublishEventType eventType;
    private PersistenceEventPublisherFactory publisherFactory;
    
    private JooqHikariTestHelper testHelper;
    private JooqAsyncPersistenceProvider db;
    private ServiceSupport serviceSupport;
    private ExampleSkeletonImpl exampleSkeletonImpl;
    private ServiceEventDispatcher dispatcher = mock(ServiceEventDispatcher.class);
    
    @BeforeEach
    public void setup() {
        testHelper = new JooqHikariTestHelper(Collections.singleton(SERVICE_NAME));
        db = new JooqAsyncPersistenceProvider(JooqMultiTenancyConfiguration.createDslContext(testHelper
            .getDataSource()));
        
        final ExecutorMultiTenantAtLeastOnceProcessorFactory factory = new ExecutorMultiTenantAtLeastOnceProcessorFactory(1);
        
        final LocalService localService = new LocalService(Collections.emptySet(), Collections.emptySet());
        final TestServiceSecurityChecker serviceSecurityChecker = new TestServiceSecurityChecker();
        serviceSupport = new ServiceSupport(
            testHelper.getMultiTenancy(),
            localService,
            serviceSecurityChecker,
            ServiceAsyncInterceptor.PASSTHROUGH,
            ServiceHandlerStrategy.PASSTHROUGH,
            Collections.emptySet(),
            Collections.singleton(new ServiceLoggingFilterFactory()),
            new DefaultServiceKeys(),
            null) {
            
            @Override
            protected void createOperationHandler(
                                                  final String serviceName,
                                                  final String serviceKey,
                                                  final AsyncFilterNext<RequestEnvelope, ResponseEnvelope> filterChain) {}
            
            @Override
            protected void destroyOperationHandler(final String serviceName, final String serviceKey) {}
            
            @Override
            protected ServiceEventDispatcher createEventDispatcher(final String serviceName, final String serviceKey, final Set<ServicePathHolder> paths) {
                return dispatcher;
            }
            
            @Override
            protected void destroyEventDispatcher(final String serviceName, final String serviceKey) {}
            
        };
        
        // service
        final ServiceSkeletonProxy<ExampleSkeleton> proxy = serviceSupport.getOrCreate(ExampleSkeleton.class);
        exampleSkeletonImpl = new ExampleSkeletonImpl(proxy.createPublisherProxy(Watch.class));
        serviceSupport.init(exampleSkeletonImpl);
        
        eventType = new AtLeastOncePublishEventType(
            db, CLUSTER_REGISTRY, serviceSupport, factory, 1000L, Collections.singleton(SERVICE_NAME));
        eventType.start();
        publisherFactory = new JooqEventPublisherFactory(eventType);
        testHelper.getMultiTenancy().registerTenantLifecycleParticipant(eventType);
        testHelper.getMultiTenancy().addTenant(TestTenants.DEFAULT);
    }
    
    @AfterEach
    public void tearDown() {
        eventType.stop();
        serviceSupport.destroy(exampleSkeletonImpl);
        testHelper.destroy();
    }
    
    @Test
    public void itShouldFulfillPublishRequestAfterStoreSucceeded() {
        final ServiceEventPublisher<ExampleContext, ExampleEvent, ?> publisherMock = mockPublisher(PATH);
        Mockito.doReturn(ExampleSkeleton.class).when(publisherMock).getSkeletonType();
        final AtLeastOnceServiceEventPublisher<ExampleContext, ExampleEvent, ?> publisher = publisherFactory.create(
            publisherMock);
        
        when(dispatcher.dispatch(any())).thenAnswer(i -> {
            final EventEnvelope eventEnvelope = i.getArgument(0);
            return result(
                EventAckEnvelope.newBuilder()
                    .setCallRef(eventEnvelope.getCallRef())
                    .setCorrelationId(eventEnvelope.getCorrelationId())
                    .setStatusCode(ResponseStatusCode.OK)
                    .build());
        });
        
        AsyncLocal
            .with(TENANT, TestTenants.DEFAULT)
            .with(DATA_UNIT, SERVICE_NAME)
            .<Void, RuntimeException>exec(() -> join(db.transaction(() -> {
                publisher.publish(header(), event(1));
                return null;
            })));
        
        Mockito.verify(dispatcher, timeout(1000L).times(1)).dispatch(any());
    }
    
    @Test
    public void itShouldRedeliverFailedRequests() {
        final ServiceEventPublisher<ExampleContext, ExampleEvent, ?> publisherMock = mockPublisher(PATH);
        Mockito.doReturn(ExampleSkeleton.class).when(publisherMock).getSkeletonType();
        final AtLeastOnceServiceEventPublisher<ExampleContext, ExampleEvent, ?> publisher = publisherFactory.create(
            publisherMock);
        
        final AtomicBoolean failed = new AtomicBoolean();
        when(dispatcher.dispatch(any())).thenAnswer(i -> {
            final EventEnvelope eventEnvelope = i.getArgument(0);
            return result(
                EventAckEnvelope.newBuilder()
                    .setCallRef(eventEnvelope.getCallRef())
                    .setCorrelationId(eventEnvelope.getCorrelationId())
                    // simulate 1 failure
                    .setStatusCode(failed.getAndSet(true) ? ResponseStatusCode.OK : ResponseStatusCode.SERVER_ERROR)
                    .build());
        });
        
        AsyncLocal
            .with(TENANT, TestTenants.DEFAULT)
            .with(DATA_UNIT, SERVICE_NAME)
            .<Void, RuntimeException>exec(() -> join(db.transaction(() -> {
                publisher.publish(header(), event(1));
                return null;
            })));
        
        Mockito.verify(dispatcher, timeout(8000L).times(2)).dispatch(any());
    }
    
    @SuppressWarnings("unchecked")
    private static ServiceEventPublisher<ExampleContext, ExampleEvent, ?> mockPublisher(final ServicePathHolder path) {
        final ServiceEventPublisher<ExampleContext, ExampleEvent, ?> mock = Mockito.mock(ServiceEventPublisher.class);
        when(mock.getPath()).thenReturn(path);
        return mock;
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
    
}
