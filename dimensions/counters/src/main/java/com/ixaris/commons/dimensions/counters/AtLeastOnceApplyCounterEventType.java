package com.ixaris.commons.dimensions.counters;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.awaitExceptions;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.logging.AsyncLogging.CORRELATION;
import static com.ixaris.commons.dimensions.counters.jooq.tables.LibDimCounterEventQueue.LIB_DIM_COUNTER_EVENT_QUEUE;
import static com.ixaris.commons.jooq.persistence.Entity.attachAndDelete;
import static com.ixaris.commons.jooq.persistence.Entity.attachAndStore;
import static com.ixaris.commons.jooq.persistence.TransactionalDSLContext.JOOQ_TX;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;

import java.util.List;
import java.util.Set;

import org.jooq.UpdatableRecord;
import org.jooq.exception.DataChangedException;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.AsyncLocal;
import com.ixaris.commons.async.lib.CommonsAsyncLib.Correlation;
import com.ixaris.commons.clustering.lib.idempotency.PendingMessages;
import com.ixaris.commons.clustering.lib.idempotency.StoredPendingMessage;
import com.ixaris.commons.clustering.lib.service.ClusterRegistry;
import com.ixaris.commons.clustering.lib.service.ShardNotLocalException;
import com.ixaris.commons.dimensions.counters.cache.CounterCache;
import com.ixaris.commons.dimensions.counters.cache.CounterCacheProvider;
import com.ixaris.commons.dimensions.counters.data.CounterEntity;
import com.ixaris.commons.dimensions.counters.data.CounterEventEntity;
import com.ixaris.commons.dimensions.counters.jooq.tables.records.LibDimCounterEventQueueRecord;
import com.ixaris.commons.dimensions.lib.context.Context;
import com.ixaris.commons.dimensions.lib.context.DimensionDef;
import com.ixaris.commons.jooq.persistence.JooqAsyncPersistenceProvider;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;
import com.ixaris.commons.multitenancy.lib.async.AbstractTenantAwareAtLeastOnceMessageType;
import com.ixaris.commons.multitenancy.lib.async.MultiTenantAtLeastOnceProcessorFactory;

/**
 * Counter event at least once message type. This ensures that counter events are processed at least once. Idempotency turns this at least once
 * guarantee into an exactly once guarantee.
 *
 * @author brian.vella
 */
public final class AtLeastOnceApplyCounterEventType extends AbstractTenantAwareAtLeastOnceMessageType<LibDimCounterEventQueueRecord> {
    
    public static final String KEY = "APPLY_COUNTER_EVENT";
    public static final String PROP_COUNTEREVENTAPPLY_REFRESH_INTERVAL = "countereventapply.refreshinterval";
    public static final long REFRESH_INTERVAL = 10000L;
    private final int BATCH_SIZE = 32;
    
    private final JooqAsyncPersistenceProvider db;
    private final CounterCacheProvider cache;
    private final ClusterRegistry clusterRegistry;
    
    public AtLeastOnceApplyCounterEventType(final JooqAsyncPersistenceProvider db,
                                            final CounterCacheProvider cache,
                                            final ClusterRegistry clusterRegistry,
                                            final MultiTenantAtLeastOnceProcessorFactory processorFactory,
                                            final Set<String> units) {
        this(db, cache, clusterRegistry, processorFactory, REFRESH_INTERVAL, units);
    }
    
    public AtLeastOnceApplyCounterEventType(final JooqAsyncPersistenceProvider db,
                                            final CounterCacheProvider cache,
                                            final ClusterRegistry clusterRegistry,
                                            final MultiTenantAtLeastOnceProcessorFactory processorFactory,
                                            final long refreshInterval,
                                            final Set<String> units) {
        super(processorFactory, refreshInterval, units);
        this.db = db;
        this.cache = cache;
        this.clusterRegistry = clusterRegistry;
    }
    
    @Override
    public String getKey() {
        return KEY;
    }
    
    @Override
    public Async<PendingMessages<LibDimCounterEventQueueRecord>> pending(final long timestamp) {
        return db.transaction(() -> JOOQ_TX.get()
            .selectFrom(LIB_DIM_COUNTER_EVENT_QUEUE)
            .where(LIB_DIM_COUNTER_EVENT_QUEUE.NEXT_RETRY_TIME.le(timestamp))
            .and(LIB_DIM_COUNTER_EVENT_QUEUE.SHARD.in(clusterRegistry.getNodeInfo().getShards().toBoxedArray()))
            .orderBy(LIB_DIM_COUNTER_EVENT_QUEUE.SEQUENCE_NUMBER)
            .limit(BATCH_SIZE)
            .fetch()
            .into(LIB_DIM_COUNTER_EVENT_QUEUE)
            .map(record -> {
                final String messageSubType;
                if (record.getDimensionName() == null) {
                    messageSubType = record.getCounterKey();
                } else {
                    messageSubType = record.getCounterKey() + "/" + record.getDimensionName();
                }
                return new StoredPendingMessage<>(
                    record.getSequenceNumber().longValue(), record.getShard(), messageSubType, record);
            }))
            .map(l -> new PendingMessages<>(l, l.size() == BATCH_SIZE));
    }
    
    @SuppressWarnings("squid:S1166")
    @Override
    public Async<Void> processMessage(final StoredPendingMessage<LibDimCounterEventQueueRecord> pendingMessage,
                                      final NextRetryTimeFunction nextRetryTimeFunction) {
        final LibDimCounterEventQueueRecord eventRecord = pendingMessage.getMessage();
        try {
            return awaitExceptions(clusterRegistry.forShard(pendingMessage.getShard(), () -> AsyncLocal
                .with(TENANT, eventRecord.getTenantId())
                .with(CORRELATION,
                    Correlation.newBuilder().setCorrelationId(UniqueIdGenerator.generate()).setIntentId((eventRecord.getIntentId() != null) ? eventRecord.getIntentId() : 0).build())
                .<Void, RuntimeException>exec(() -> awaitExceptions(CounterDef.queue(eventRecord.getCounterKey(),
                    eventRecord.getDimensionName(),
                    eventRecord.getDimensionStringValue(),
                    eventRecord.getDimensionLongValue(),
                    () -> applyCounterEvent(eventRecord))))));
        } catch (final ShardNotLocalException e) {
            // abandon as shard no longer local
            return result();
        } catch (final RuntimeException e) {
            await(db.transaction(() -> {
                eventRecord.setFailureCount(eventRecord.getFailureCount() + 1);
                eventRecord.setNextRetryTime(nextRetryTimeFunction.calculate(eventRecord.getFailureCount()));
                attachAndStore(eventRecord);
                return null;
            }));
            throw e;
        }
    }
    
    @SuppressWarnings({ "unchecked", "squid:S1166" })
    private <R extends UpdatableRecord<R>, C extends CounterDef<R, C>> Async<Void> applyCounterEvent(final LibDimCounterEventQueueRecord eventRecord) {
        final C def = (C) CounterDefRegistry.getInstance().resolve(eventRecord.getCounterKey());
        return db.transaction(() -> {
            // idempotent marking that this event is processed (will be rolled back if processing fails)
            // failures should be removed automatically due to cascade delete
            try {
                if (attachAndDelete(eventRecord) == 0) {
                    return null; // absence of record means already processed
                }
            } catch (final DataChangedException e) {
                return null; // absence of record means already processed
            }
            
            final DimensionDef<?> partitionDimensionDef = eventRecord.getDimensionName() != null
                ? def.getContextDef().resolve(eventRecord.getDimensionName()) : null;
            final CounterEventEntity<C> event = def.lookupCounterEvent(eventRecord.getEventId());
            final List<CounterEntity<R, C>> counters = CounterEntity.lookupMatching(
                event.getContext(), partitionDimensionDef);
            
            for (final CounterEntity<R, C> counter : counters) {
                if (eventRecord.getCounterAffected()) {
                    counter.increment(event.getDelta(), event.getTimestamp());
                } else {
                    counter.decrement(event.getDelta(), event.getTimestamp());
                }
                // update cache if cacheable
                final Context<C> context = counter.getContext();
                final CounterCache<R, C> defCache;
                if (context.isCacheable()) {
                    defCache = cache.of(def);
                    final CounterEntity<R, C> cached = defCache.get(
                        context, counter.getNarrowWindowWidth(), counter.getWideWindowMultiple());
                    
                    // if cached counter with the right Narrow Window number has been found in cache.
                    if (cached != null) {
                        counter.getCounter().setLastQueried(cached.getCounter().getLastQueried());
                    }
                } else {
                    defCache = null;
                }
                counter
                    .getCounter()
                    .setQueriedUpdatedDiff(System.currentTimeMillis() - counter.getCounter().getLastQueried());
                counter.store();
                if (defCache != null) {
                    JOOQ_TX.get().onCommit(() -> defCache.put(counter));
                }
            }
            return null;
        });
    }
    
}
