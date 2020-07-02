package com.ixaris.commons.jooq.microservices;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.awaitExceptions;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.logging.AsyncLogging.CORRELATION;
import static com.ixaris.commons.jooq.microservices.jooq.tables.LibPublishEventQueue.LIB_PUBLISH_EVENT_QUEUE;
import static com.ixaris.commons.jooq.persistence.Entity.attachAndDelete;
import static com.ixaris.commons.jooq.persistence.Entity.attachAndStore;
import static com.ixaris.commons.jooq.persistence.TransactionalDSLContext.JOOQ_TX;
import static com.ixaris.commons.microservices.lib.client.ServiceEvent.extractCorrelation;
import static com.ixaris.commons.misc.lib.object.Tuple.tuple;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;
import static org.jooq.impl.DSL.row;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import org.checkerframework.checker.units.qual.C;
import org.jooq.Record2;
import org.jooq.Result;

import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.AsyncLocal;
import com.ixaris.commons.clustering.lib.idempotency.PendingMessages;
import com.ixaris.commons.clustering.lib.idempotency.StoredPendingMessage;
import com.ixaris.commons.clustering.lib.service.ClusterRegistry;
import com.ixaris.commons.clustering.lib.service.ShardNotLocalException;
import com.ixaris.commons.jooq.microservices.jooq.tables.records.LibPublishEventQueueRecord;
import com.ixaris.commons.jooq.persistence.JooqAsyncPersistenceProvider;
import com.ixaris.commons.microservices.lib.common.EventAck;
import com.ixaris.commons.microservices.lib.common.ServiceEventHeader;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.common.exception.ClientInvalidRequestException;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventEnvelope;
import com.ixaris.commons.microservices.lib.service.ServiceEventPublisher;
import com.ixaris.commons.microservices.lib.service.ServiceSkeleton;
import com.ixaris.commons.microservices.lib.service.proxy.ServiceSkeletonProxy;
import com.ixaris.commons.microservices.lib.service.support.ServiceSupport;
import com.ixaris.commons.misc.lib.object.EqualsUtil;
import com.ixaris.commons.misc.lib.object.Tuple2;
import com.ixaris.commons.multitenancy.lib.async.AbstractTenantAwareAtLeastOnceMessageType;
import com.ixaris.commons.multitenancy.lib.async.MultiTenantAtLeastOnceProcessorFactory;
import com.ixaris.commons.persistence.microservices.AtLeastOnceServiceEventPublisher;

/**
 * An event publisher with at-least-once delivery semantics.
 *
 * <p>Delivery of events to a message broker can fail. This ServiceEventPublisher wrapper makes sure that events are
 * resent until the broker acknowledges the delivery. When used with a persistent store, events are redelivered even
 * after the microservice is restarted. To work properly, events must be stored if and only if the rest of the business
 * logic succeeds (specifically, within the same transaction when the microservice is backed by an SQL database).
 *
 * <p>This publisher NEVER delivers events directly from publish/publishEnvelope methods. New events are regularly
 * polled from the store and eventually delivered.
 *
 * @author marcel
 * @author brian.vella
 * @author aldrin.seychell
 */
public final class AtLeastOncePublishEventType extends AbstractTenantAwareAtLeastOnceMessageType<LibPublishEventQueueRecord> {
    
    public static final String KEY = "PUBLISH_EVENTS";
    
    public static final String PROP_EVENTPUBLISH_REFRESH_INTERVAL = "eventpublish.refreshinterval";
    public static final long REFRESH_INTERVAL = 10000L;
    private static final int BATCH_SIZE = 32;
    
    private static EventEnvelope toEventEnvelope(final LibPublishEventQueueRecord r) {
        try {
            return EventEnvelope.parseFrom(r.getEventEnvelope());
        } catch (final InvalidProtocolBufferException e) {
            throw new IllegalStateException("Unable to parse stored event envelope", e);
        }
    }
    
    private static final class PublisherKey {
        
        private final String serviceName;
        private final ServicePathHolder path;
        
        private PublisherKey(final String serviceName, final ServicePathHolder path) {
            this.serviceName = serviceName;
            this.path = path;
        }
        
        @Override
        public boolean equals(final Object o) {
            return EqualsUtil.equals(this, o, other -> serviceName.equals(other.serviceName) && path.equals(other.path));
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(serviceName, path);
        }
        
    }
    
    private static class AtLeastOnceEventPublisherImpl<C extends MessageLite, E extends MessageLite, R> implements AtLeastOnceServiceEventPublisher<C, E, R> {
        
        private final ServicePathHolder path;
        private final ServiceSkeletonProxy<?> proxy;
        private final ClusterRegistry clusterRegistry;
        private final Runnable pollNow;
        
        private AtLeastOnceEventPublisherImpl(final ServicePathHolder path,
                                              final ServiceSkeletonProxy<?> proxy,
                                              final ClusterRegistry clusterRegistry,
                                              final Runnable pollNow) {
            this.path = path;
            this.proxy = proxy;
            this.clusterRegistry = clusterRegistry;
            this.pollNow = pollNow;
        }
        
        @Override
        public void publish(final ServiceEventHeader<C> header, final E event) {
            try {
                final LibPublishEventQueueRecord record = new LibPublishEventQueueRecord();
                record.setPath(path.toString());
                record.setEventEnvelope(buildEnvelope(path, proxy, header, event).toByteArray());
                record.setShard(clusterRegistry.getShard(header.getPartitionId()));
                attachAndStore(record);
                JOOQ_TX.get().onCommit(pollNow);
            } catch (final ClientInvalidRequestException e) {
                throw new IllegalArgumentException("Invalid event", e);
            }
        }
        
    }
    
    private final JooqAsyncPersistenceProvider db;
    private final ClusterRegistry clusterRegistry;
    private final ServiceSupport serviceSupport;
    private final Map<PublisherKey, Tuple2<ServicePathHolder, ServiceSkeletonProxy<?>>> publishers = new HashMap<>();
    
    public AtLeastOncePublishEventType(final JooqAsyncPersistenceProvider db,
                                       final ClusterRegistry clusterRegistry,
                                       final ServiceSupport serviceSupport,
                                       final MultiTenantAtLeastOnceProcessorFactory processorFactory,
                                       final Set<String> units) {
        this(db, clusterRegistry, serviceSupport, processorFactory, REFRESH_INTERVAL, units);
    }
    
    public AtLeastOncePublishEventType(final JooqAsyncPersistenceProvider db,
                                       final ClusterRegistry clusterRegistry,
                                       final ServiceSupport serviceSupport,
                                       final MultiTenantAtLeastOnceProcessorFactory processorFactory,
                                       final long refreshInterval,
                                       final Set<String> units) {
        super(processorFactory, refreshInterval, units);
        this.db = db;
        this.clusterRegistry = clusterRegistry;
        this.serviceSupport = serviceSupport;
    }
    
    @Override
    public String getKey() {
        return KEY;
    }
    
    @Override
    public Async<PendingMessages<LibPublishEventQueueRecord>> pending(final long timestamp) {
        return db.transaction(() -> {
            final Result<Record2<Integer, String>> blockedDueToFailure = JOOQ_TX.get()
                .select(LIB_PUBLISH_EVENT_QUEUE.SHARD, LIB_PUBLISH_EVENT_QUEUE.PATH)
                .from(LIB_PUBLISH_EVENT_QUEUE)
                .where(LIB_PUBLISH_EVENT_QUEUE.NEXT_RETRY_TIME.gt(timestamp))
                .fetch();
            
            return JOOQ_TX.get()
                .selectFrom(LIB_PUBLISH_EVENT_QUEUE)
                .where(row(LIB_PUBLISH_EVENT_QUEUE.SHARD, LIB_PUBLISH_EVENT_QUEUE.PATH).notIn(blockedDueToFailure))
                .and(LIB_PUBLISH_EVENT_QUEUE.SHARD.in(clusterRegistry.getNodeInfo().getShards().toBoxedArray()))
                .orderBy(LIB_PUBLISH_EVENT_QUEUE.SEQUENCE_NUMBER)
                .limit(BATCH_SIZE)
                .fetch()
                .map(r -> new StoredPendingMessage<>(r.getSequenceNumber().longValue(), r.getShard(), r.getPath(), r));
        })
            .map(l -> new PendingMessages<>(l, l.size() == BATCH_SIZE));
    }
    
    @SuppressWarnings("squid:S1166")
    @Override
    public Async<Void> processMessage(final StoredPendingMessage<LibPublishEventQueueRecord> pendingMessage,
                                      final NextRetryTimeFunction nextRetryTimeFunction) {
        final LibPublishEventQueueRecord queueRecord = pendingMessage.getMessage();
        final EventEnvelope eventEnvelope = toEventEnvelope(queueRecord);
        return AsyncLocal
            .with(TENANT, eventEnvelope.getTenantId())
            .with(CORRELATION, extractCorrelation(eventEnvelope))
            .<Void, RuntimeException>exec(() -> {
                try {
                    return awaitExceptions(clusterRegistry.<Void, RuntimeException>forShard(pendingMessage.getShard(), () -> {
                        final PublisherKey key = new PublisherKey(
                            eventEnvelope.getServiceName(), ServicePathHolder.of(eventEnvelope.getPathList()));
                        final Tuple2<ServicePathHolder, ServiceSkeletonProxy<?>> publisher;
                        synchronized (publishers) {
                            publisher = publishers.get(key);
                        }
                        await(publishEnvelope(publisher.get1(), publisher.get2(), eventEnvelope));
                        return db.transaction(() -> {
                            attachAndDelete(queueRecord);
                            return (Void) null;
                        });
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
    
    public <C extends MessageLite, E extends MessageLite, R> AtLeastOnceServiceEventPublisher<C, E, R> register(final ServiceEventPublisher<C, E, ?> publisher) {
        final ServiceSkeletonProxy<?> proxy = serviceSupport.getOrCreate(publisher.getSkeletonType());
        final ServicePathHolder path = publisher.getPath();
        final PublisherKey key = new PublisherKey(ServiceSkeleton.extractServiceName(publisher.getSkeletonType()), path);
        synchronized (publishers) {
            if (publishers.putIfAbsent(key, tuple(path, proxy)) != null) {
                throw new IllegalStateException("Publisher with path " + path + " already registered");
            }
        }
        return new AtLeastOnceEventPublisherImpl<>(path, proxy, clusterRegistry, this::pollNow);
    }
    
    private static EventEnvelope buildEnvelope(final ServicePathHolder path,
                                               final ServiceSkeletonProxy<?> proxy,
                                               final ServiceEventHeader<?> header,
                                               final MessageLite event) {
        return proxy.getPublisherForServiceKey(header.getTargetServiceKey()).buildEnvelope(header, event, path);
    }
    
    private static Async<EventAck> publishEnvelope(final ServicePathHolder path,
                                                   final ServiceSkeletonProxy<?> proxy,
                                                   final EventEnvelope eventEnvelope) {
        // Make sure that the event being published is applicable to the path of this publisher
        final ServicePathHolder eventPath = ServicePathHolder.of(eventEnvelope.getPathList());
        if (!path.equals(eventPath)) {
            throw new IllegalStateException(String
                .format("Event envelope path [%s] does not correspond to the path [%s]", eventPath, path));
        }
        
        return proxy.getPublisherForServiceKey(eventEnvelope.getServiceKey()).publishEnvelope(eventEnvelope);
    }
}
