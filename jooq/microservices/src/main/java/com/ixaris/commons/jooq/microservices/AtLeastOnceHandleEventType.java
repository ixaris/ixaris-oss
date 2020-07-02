package com.ixaris.commons.jooq.microservices;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.awaitExceptions;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.logging.AsyncLogging.CORRELATION;
import static com.ixaris.commons.jooq.microservices.jooq.tables.LibHandleEventQueue.LIB_HANDLE_EVENT_QUEUE;
import static com.ixaris.commons.jooq.persistence.Entity.attachAndDelete;
import static com.ixaris.commons.jooq.persistence.Entity.attachAndStore;
import static com.ixaris.commons.jooq.persistence.TransactionalDSLContext.JOOQ_TX;
import static com.ixaris.commons.microservices.lib.client.ServiceEvent.extractCorrelation;
import static com.ixaris.commons.microservices.lib.client.support.ServiceClientSupport.resetCorrelation;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.AsyncLocal;
import com.ixaris.commons.clustering.lib.idempotency.PendingMessages;
import com.ixaris.commons.clustering.lib.idempotency.StoredPendingMessage;
import com.ixaris.commons.clustering.lib.service.ClusterRegistry;
import com.ixaris.commons.clustering.lib.service.ShardNotLocalException;
import com.ixaris.commons.jooq.microservices.jooq.tables.records.LibHandleEventQueueRecord;
import com.ixaris.commons.jooq.persistence.JooqAsyncPersistenceProvider;
import com.ixaris.commons.microservices.lib.client.ServiceEventListener;
import com.ixaris.commons.microservices.lib.client.ServiceStubEvent;
import com.ixaris.commons.microservices.lib.client.proxy.ServiceStubEventProcessor;
import com.ixaris.commons.microservices.lib.client.proxy.ServiceStubProxy;
import com.ixaris.commons.microservices.lib.client.support.ServiceClientSupport;
import com.ixaris.commons.microservices.lib.common.ServiceEventHeader;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventAckEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;
import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.misc.lib.object.Wrapper;
import com.ixaris.commons.multitenancy.lib.async.AbstractTenantAwareAtLeastOnceMessageType;
import com.ixaris.commons.multitenancy.lib.async.MultiTenantAtLeastOnceProcessorFactory;

/**
 * An at least once processor which handles the processing of events which failed to be handled by event listeners.
 *
 * @author <a href="mailto:keith.spiteri@ixaris.com">keith.spiteri</a>
 */
public final class AtLeastOnceHandleEventType extends AbstractTenantAwareAtLeastOnceMessageType<LibHandleEventQueueRecord> {
    
    public static final String KEY = "HANDLE_EVENTS";
    
    public static final String PROP_EVENTHANDLE_REFRESH_INTERVAL = "eventhandle.refreshinterval";
    public static final long REFRESH_INTERVAL = 10000L;
    
    private static final AsyncLocal<Boolean> RETRY = new AsyncLocal<>("alo_handle_event_retry");
    private static final int BATCH_SIZE = 32;
    
    private static EventEnvelope toEventEnvelope(final LibHandleEventQueueRecord r) {
        try {
            return EventEnvelope.parseFrom(r.getEventEnvelope());
        } catch (final InvalidProtocolBufferException e) {
            throw new IllegalStateException("Unable to parse stored event envelope", e);
        }
    }
    
    private static class AtLeastOnceListenerWrapper<C extends MessageLite, T extends MessageLite> implements ServiceEventListener<C, T>, Wrapper<ServiceEventListener<C, T>> {
        
        private final ServiceEventListener<C, T> wrapped;
        private final JooqAsyncPersistenceProvider db;
        private final ClusterRegistry clusterRegistry;
        
        private AtLeastOnceListenerWrapper(final ServiceEventListener<C, T> wrapped,
                                           final JooqAsyncPersistenceProvider db,
                                           final ClusterRegistry clusterRegistry) {
            this.wrapped = wrapped;
            this.db = db;
            this.clusterRegistry = clusterRegistry;
        }
        
        @Override
        public Async<Void> onEvent(final ServiceEventHeader<C> header, final T event) {
            return wrapped.onEvent(header, event);
        }
        
        @Override
        public String getName() {
            return wrapped.getName();
        }
        
        @Override
        public Async<EventAckEnvelope> handle(final ServiceStubEvent event) {
            final EventAckEnvelope eventAckEnvelope = await(wrapped.handle(event));
            if ((RETRY.get() == null) && (eventAckEnvelope.getStatusCode() != ResponseStatusCode.OK)) {
                final EventEnvelope eventEnvelope = event.getEventEnvelope();
                return wrapped
                    .aroundAsync(() -> markFailedEvent(eventEnvelope))
                    .map(r -> EventAckEnvelope.newBuilder()
                        .setCorrelationId(eventEnvelope.getCorrelationId())
                        .setCallRef(eventEnvelope.getCallRef())
                        .setStatusCode(ResponseStatusCode.OK)
                        .build());
            }
            return result(eventAckEnvelope);
        }
        
        @Override
        public <X, E extends Exception> X aroundAsync(CallableThrows<X, E> callable) throws E {
            return wrapped.aroundAsync(callable);
        }
        
        @Override
        public ServiceEventListener<C, T> unwrap() {
            return wrapped;
        }
        
        private Async<Void> markFailedEvent(final EventEnvelope eventEnvelope) {
            return db.transaction(() -> {
                final LibHandleEventQueueRecord record = new LibHandleEventQueueRecord();
                record.setListenerName(wrapped.getName());
                record.setEventEnvelope(eventEnvelope.toByteArray());
                record.setShard(clusterRegistry.getShard(eventEnvelope.getPartitionId()));
                attachAndStore(record);
                return null;
            });
        }
        
    }
    
    private final JooqAsyncPersistenceProvider db;
    private final ClusterRegistry clusterRegistry;
    private final ServiceClientSupport serviceClientSupport;
    private final Map<String, ServiceEventListener<?, ?>> listeners = new HashMap<>();
    
    public AtLeastOnceHandleEventType(final JooqAsyncPersistenceProvider db,
                                      final ClusterRegistry clusterRegistry,
                                      final ServiceClientSupport serviceClientSupport,
                                      final MultiTenantAtLeastOnceProcessorFactory processorFactory,
                                      final Set<String> units) {
        this(db, clusterRegistry, serviceClientSupport, processorFactory, REFRESH_INTERVAL, units);
    }
    
    public AtLeastOnceHandleEventType(final JooqAsyncPersistenceProvider db,
                                      final ClusterRegistry clusterRegistry,
                                      final ServiceClientSupport serviceClientSupport,
                                      final MultiTenantAtLeastOnceProcessorFactory processorFactory,
                                      final long refreshInterval,
                                      final Set<String> dataUnits) {
        super(processorFactory, refreshInterval, dataUnits);
        this.db = db;
        this.clusterRegistry = clusterRegistry;
        this.serviceClientSupport = serviceClientSupport;
    }
    
    @Override
    public String getKey() {
        return KEY;
    }
    
    @Override
    public Async<PendingMessages<LibHandleEventQueueRecord>> pending(final long timestamp) {
        return db.transaction(() -> JOOQ_TX.get()
            .selectFrom(LIB_HANDLE_EVENT_QUEUE)
            .where(LIB_HANDLE_EVENT_QUEUE.NEXT_RETRY_TIME.le(timestamp))
            .and(LIB_HANDLE_EVENT_QUEUE.SHARD.in(clusterRegistry.getNodeInfo().getShards().toBoxedArray()))
            .orderBy(LIB_HANDLE_EVENT_QUEUE.SEQUENCE_NUMBER)
            .limit(BATCH_SIZE)
            .fetch()
            .map(r -> new StoredPendingMessage<>(r.getSequenceNumber().longValue(), r.getShard(), r.getListenerName(), r)))
            .map(l -> new PendingMessages<>(l, l.size() == BATCH_SIZE));
    }
    
    @SuppressWarnings("squid:S1166")
    @Override
    public Async<Void> processMessage(final StoredPendingMessage<LibHandleEventQueueRecord> pendingMessage, final NextRetryTimeFunction nextRetryTimeFunction) {
        final LibHandleEventQueueRecord queueRecord = pendingMessage.getMessage();
        final EventEnvelope eventEnvelope = resetCorrelation(toEventEnvelope(queueRecord));
        return AsyncLocal
            .with(TENANT, eventEnvelope.getTenantId())
            .with(CORRELATION, extractCorrelation(eventEnvelope))
            .<Void, RuntimeException>exec(() -> {
                try {
                    return awaitExceptions(clusterRegistry.<Void, RuntimeException>forShard(pendingMessage.getShard(), () -> {
                        final ServiceStubProxy<?> proxy = serviceClientSupport.get(eventEnvelope.getServiceName());
                        final ServiceStubEventProcessor<?> eventProcessor = proxy.getEventProcessor(
                            ServicePathHolder.of(eventEnvelope.getPathList()), pendingMessage.getMessageSubType());
                        if (eventProcessor != null) {
                            final EventAckEnvelope eventAckEnvelope = await(RETRY.exec(true, () -> eventProcessor.process(eventEnvelope)));
                            if (eventAckEnvelope.getStatusCode() == ResponseStatusCode.OK) {
                                return db.transaction(() -> {
                                    attachAndDelete(queueRecord);
                                    return null;
                                });
                            } else {
                                throw new IllegalStateException("Event handling failed");
                            }
                        } else {
                            return result();
                        }
                    }));
                } catch (final ShardNotLocalException e) {
                    // abandon as shard no longer local
                    return result();
                } catch (final RuntimeException e) {
                    await(db.transaction(() -> {
                        queueRecord.setFailureCount(queueRecord.getFailureCount() + 1);
                        queueRecord.setNextRetryTime(nextRetryTimeFunction.calculate(queueRecord.getFailureCount()));
                        attachAndStore(queueRecord);
                        return null;
                    }));
                    throw e;
                }
            });
    }
    
    public <C extends MessageLite, E extends MessageLite> ServiceEventListener<C, E> register(final ServiceEventListener<C, E> listener) {
        synchronized (listeners) {
            if (listeners.putIfAbsent(listener.getName(), listener) != null) {
                throw new IllegalStateException("Listener with name " + listener.getName() + " already registered");
            }
        }
        return new AtLeastOnceListenerWrapper<>(listener, db, clusterRegistry);
    }
    
}
