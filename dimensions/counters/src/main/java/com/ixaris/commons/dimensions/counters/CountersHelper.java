/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.counters;

import static com.ixaris.commons.async.lib.idempotency.Intent.INTENT;
import static com.ixaris.commons.jooq.persistence.Entity.attachAndStore;
import static com.ixaris.commons.jooq.persistence.TransactionalDSLContext.JOOQ_TX;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;

import java.util.Iterator;
import java.util.function.Function;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import org.jooq.UpdatableRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.idempotency.Intent;
import com.ixaris.commons.clustering.lib.service.ClusterRegistry;
import com.ixaris.commons.clustering.lib.service.ClusterRouteHandler;
import com.ixaris.commons.clustering.lib.service.ClusterRouteTimeoutException;
import com.ixaris.commons.dimensions.counters.CommonsDimensionsCounters.CounterWindowWidth;
import com.ixaris.commons.dimensions.counters.CommonsDimensionsCounters.CounterWindowWidth.TimeUnit;
import com.ixaris.commons.dimensions.counters.CommonsDimensionsCounters.GetCounter;
import com.ixaris.commons.dimensions.counters.CommonsDimensionsCounters.GetCounterResult;
import com.ixaris.commons.dimensions.counters.cache.CounterCache;
import com.ixaris.commons.dimensions.counters.cache.CounterCacheProvider;
import com.ixaris.commons.dimensions.counters.data.CounterEntity;
import com.ixaris.commons.dimensions.counters.data.CounterEventEntity;
import com.ixaris.commons.dimensions.counters.jooq.tables.records.LibDimCounterEventQueueRecord;
import com.ixaris.commons.dimensions.lib.base.DimensionalHelper;
import com.ixaris.commons.dimensions.lib.context.Context;
import com.ixaris.commons.dimensions.lib.context.Dimension;
import com.ixaris.commons.dimensions.lib.context.DimensionDef;
import com.ixaris.commons.dimensions.lib.context.PersistedDimensionValue;
import com.ixaris.commons.jooq.persistence.JooqAsyncPersistenceProvider;

@Component
public final class CountersHelper {
    
    private static long getShardKeyFromString(final String key) {
        try {
            return Long.parseLong(key);
        } catch (final NumberFormatException e) {
            // hashcode for empty string is 0
            return key.hashCode();
        }
    }
    
    private static final long UPDATE_LAST_QUERIED_THRESHOLD_MILLIS = 24L * WindowTimeUnit.MILLISECONDS_IN_HOUR;
    
    public static long determineShardKey(final PersistedDimensionValue value) {
        if (value == null) {
            return getShardKeyFromString(TENANT.get());
        } else {
            final Long longValue = value.getLongValue();
            return longValue != null ? longValue : getShardKeyFromString(value.getStringValue());
        }
    }
    
    /**
     * Queue an event to be applied to counters. Requires a transactional context
     */
    public static <R extends UpdatableRecord<R>, C extends CounterDef<R, C>> void queueEvent(final CounterEventEntity<C> event, final ClusterRegistry clusterRegistry) {
        final String tenantId = TENANT.get();
        final C def = event.getContext().getDef();
        
        final LibDimCounterEventQueueRecord noDimensionRecord = getRecordFromEvent(event, def);
        noDimensionRecord.setShard(clusterRegistry.getShard(determineShardKey(null)));
        noDimensionRecord.setTenantId(tenantId);
        attachAndStore(noDimensionRecord);
        
        final Iterator<DimensionDef<?>> iteratorByImportance = def.getContextDef().iterator();
        for (int i = 0; i < def.getPartitionDimensionsCount() && iteratorByImportance.hasNext(); i++) {
            final DimensionDef<?> dimensionDef = iteratorByImportance.next();
            final Dimension<?> dimension = event.getContext().get(dimensionDef);
            if (dimension != null) {
                final LibDimCounterEventQueueRecord record = getRecordFromEvent(event, def);
                record.setDimensionName(dimensionDef.getKey());
                final PersistedDimensionValue value = dimension.getPersistedValue();
                record.setDimensionLongValue(value.getLongValue());
                record.setDimensionStringValue(value.getStringValue());
                record.setShard(clusterRegistry.getShard(determineShardKey(value)));
                record.setTenantId(tenantId);
                attachAndStore(record);
            }
        }
    }
    
    private static <C extends CounterDef<?, C>> LibDimCounterEventQueueRecord getRecordFromEvent(final CounterEventEntity<C> event, final C def) {
        final LibDimCounterEventQueueRecord record = new LibDimCounterEventQueueRecord();
        record.setCounterKey(def.getKey());
        record.setEventId(event.getId());
        record.setCounterAffected(event.isCounterAffected());
        final Intent intent = INTENT.get();
        if (intent != null) {
            record.setIntentId(intent.getId());
        }
        return record;
    }
    
    private static WindowWidth convert(final CounterWindowWidth windowWidth) {
        return new WindowWidth(windowWidth.getWidth(), WindowTimeUnit.valueOf(windowWidth.getUnit().name()));
    }
    
    private final JooqAsyncPersistenceProvider db;
    private final CounterCacheProvider cache;
    private final ClusterRegistry clusterRegistry;
    private final ClusterRouteHandler<GetCounter, GetCounterResult> getCounterRouteHandler;
    
    @Autowired
    public CountersHelper(final JooqAsyncPersistenceProvider db,
                          final CounterCacheProvider cache,
                          ClusterRegistry clusterRegistry) {
        this.db = db;
        this.cache = cache;
        this.clusterRegistry = clusterRegistry;
        
        getCounterRouteHandler = new ClusterRouteHandler<GetCounter, GetCounterResult>() {
            
            @Override
            public String getKey() {
                return "counters_get";
            }
            
            @Override
            public Async<GetCounterResult> handle(final long id, final String key, final GetCounter request) {
                return internalHandle(request);
            }
            
        };
    }
    
    @PostConstruct
    public void startup() {
        clusterRegistry.register(getCounterRouteHandler);
    }
    
    @PreDestroy
    public void shutdown() {
        clusterRegistry.deregister(getCounterRouteHandler);
    }
    
    public <R extends UpdatableRecord<R>, C extends CounterDef<R, C>> Async<CounterValue> getCounter(final Context<C> context,
                                                                                                     final WindowWidth narrowWindowWidth,
                                                                                                     final int wideWindowMultiple) throws ClusterRouteTimeoutException {
        return getCounter(context, narrowWindowWidth, wideWindowMultiple, false);
    }
    
    /**
     * Returns Wide & Narrow windows count & sum values for a particular counter (event) with a context. The currently
     * active narrow window is used for narrow count/sum values.
     *
     * @param context The Context of the counter to retrieve. Cannot be null.
     * @param narrowWindowWidth The width of the narrow window
     * @param wideWindowMultiple Wide window width as a multiple of narrowWindowWidth (Wide window width is effectively
     *     narrowWindowWidth * wideWindowMultiple)
     * @return Returns Wide & Narrow windows count & sum values. Never returns null - returns zero values if no counter
     *     matches.
     */
    public <R extends UpdatableRecord<R>, C extends CounterDef<R, C>> Async<CounterValue> getCounter(final Context<C> context,
                                                                                                     final WindowWidth narrowWindowWidth,
                                                                                                     final int wideWindowMultiple,
                                                                                                     final boolean lastFull) throws ClusterRouteTimeoutException {
        
        if (lastFull && (wideWindowMultiple == 1)) {
            throw new IllegalArgumentException(
                "Only 1 window is maintained for counter with wideWindowMultiple = 1. Cannot retrieve LastFullNarrow window counter.");
        }
        
        final Dimension<?> dimension = CounterDef.extractFirstPartitionDimension(context.getDef(), context);
        final PersistedDimensionValue persistedValue = dimension != null
            ? dimension.getPersistedValue() : new PersistedDimensionValue(0L);
        
        final GetCounter request = GetCounter.newBuilder()
            .setKey(context.getDef().getKey())
            .setContext(context.toProtobuf())
            .setNarrowWindowWidth(
                CounterWindowWidth.newBuilder()
                    .setWidth(narrowWindowWidth.getWidth())
                    .setUnit(TimeUnit.valueOf(narrowWindowWidth.getUnit().name())))
            .setWideWindowMultiple(wideWindowMultiple)
            .setLastFull(lastFull)
            .build();
        
        return (persistedValue.getLongValue() != null
            ? clusterRegistry.route(getCounterRouteHandler, persistedValue.getLongValue(), request)
            : clusterRegistry.route(getCounterRouteHandler, persistedValue.getStringValue(), request))
                .map(res -> new CounterValue(new WindowValue(res.getWideCount(), res.getWideSum()), new WindowValue(res.getNarrowCount(), res.getNarrowSum())));
    }
    
    @SuppressWarnings("unchecked")
    private <R extends UpdatableRecord<R>, C extends CounterDef<R, C>> Async<GetCounterResult> internalHandle(final GetCounter request) {
        final C def = (C) CounterDefRegistry.getInstance().resolve(request.getKey());
        final Context<C> context = def.getContextDef().contextFromProtobuf(def, request.getContext());
        final Dimension<?> dimension = CounterDef.extractFirstPartitionDimension(def, context);
        final PersistedDimensionValue persistedValue = dimension != null
            ? dimension.getPersistedValue() : new PersistedDimensionValue(0L);
        
        return CounterDef
            .queue(
                def.getKey(),
                dimension != null ? dimension.getDefinition().getKey() : null,
                persistedValue.getStringValue(),
                persistedValue.getLongValue(),
                () -> localGetCounter(context, convert(request.getNarrowWindowWidth()), request.getWideWindowMultiple()))
            .map(c -> {
                final CounterValue r = request.getLastFull()
                    ? c.getWideAndNarrowValueLastFull()
                    : c.getWideAndNarrowValue();
                return GetCounterResult.newBuilder()
                    .setWideCount(r.getWide().getCount())
                    .setWideSum(r.getWide().getSum())
                    .setNarrowCount(r.getNarrow().getCount())
                    .setNarrowSum(r.getNarrow().getSum())
                    .build();
            });
    }
    
    @SuppressWarnings("checkstyle:com.puppycrawl.tools.checkstyle.checks.metrics.NPathComplexityCheck")
    private <R extends UpdatableRecord<R>, C extends CounterDef<R, C>> Async<CounterEntity<R, C>> localGetCounter(final Context<C> context,
                                                                                                                  final WindowWidth narrowWindowWidth,
                                                                                                                  final int wideWindowMultiple) {
        // First we try fetching counter from cache. (If found, check if we need to update lastQueried).
        // If not found, try fetching counter from db.<br>
        // If not found, ask EventDef to get the counter values for the necessary windows.
        
        DimensionalHelper.validateForQuery(context);
        
        final long currentTimeMillis = System.currentTimeMillis(); // we use this for consistency between cache/db
        
        // first try the cache, if context is cacheable
        final CounterCache<R, C> defCache;
        if (context.isCacheable()) {
            defCache = cache.of(context.getDef());
            final CounterEntity<R, C> cached = defCache.get(context, narrowWindowWidth, wideWindowMultiple);
            if (cached != null) {
                // cached counter with the right Narrow Window number has been found in cache.
                return updateExistingCounter(cached, currentTimeMillis, c -> db.transaction(c::store), Async::result);
            }
        } else {
            defCache = null;
        }
        
        // if counter not found in cache or not cacheable, try the database
        return db.transaction(() -> {
            final CounterEntity<R, C> persisted = CounterEntity.lookup(context, narrowWindowWidth, wideWindowMultiple);
            final CounterEntity<R, C> counter;
            if (persisted != null) {
                counter = updateExistingCounter(persisted, currentTimeMillis, CounterEntity::store, c -> c);
            } else {
                counter = createNewCounter(context, narrowWindowWidth, wideWindowMultiple, currentTimeMillis);
            }
            
            // if context is cacheable and was not in cache, we cache it.
            if (defCache != null) {
                JOOQ_TX.get().onCommit(() -> defCache.put(counter));
            }
            
            return counter;
        });
    }
    
    private <R extends UpdatableRecord<R>, C extends CounterDef<R, C>> CounterEntity<R, C> createNewCounter(final Context<C> context,
                                                                                                            final WindowWidth narrowWindowWidth,
                                                                                                            final int wideWindowMultiple,
                                                                                                            final long currentTimeMillis) {
        final long currentWindowNumber = narrowWindowWidth.getWindowNumber(currentTimeMillis);
        
        // fetch data for current narrow window, 1 narrow window before current narrow window (last full)
        // and wide window (start of currentNarrowWindowNumber - wideWindowMultiple + 1)
        final long narrowStartTime = narrowWindowWidth.getStartTimestampForWindowNumber(currentWindowNumber);
        final long narrowLastFullStartTime = narrowWindowWidth.getStartTimestampForWindowNumber(currentWindowNumber - 1);
        final long wideStartTime = narrowWindowWidth.getStartTimestampForWindowNumber(currentWindowNumber - wideWindowMultiple + 1);
        
        final C def = context.getDef();
        final Dimension<?> dimension = CounterDef.extractFirstPartitionDimension(def, context);
        return new CounterEntity<>(context,
            narrowWindowWidth,
            wideWindowMultiple,
            def.fetchWindow(context, wideStartTime, null),
            wideWindowMultiple == 1 ? null : def.fetchWindow(context, narrowStartTime, null),
            wideWindowMultiple == 1
                ? null : def.fetchWindow(context, narrowLastFullStartTime, narrowStartTime),
            currentTimeMillis,
            clusterRegistry.getShard(determineShardKey(dimension == null ? null : dimension.getPersistedValue()))).store();
    }
    
    private <R extends UpdatableRecord<R>, C extends CounterDef<R, C>, T> T updateExistingCounter(final CounterEntity<R, C> counter,
                                                                                                  final long currentTimeMillis,
                                                                                                  final Function<CounterEntity<R, C>, T> persistFunction,
                                                                                                  final Function<CounterEntity<R, C>, T> noPersistFunction) {
        final long timeWithoutUpdateBoundary = counter.getCounter().getLastQueried() + UPDATE_LAST_QUERIED_THRESHOLD_MILLIS;
        counter.getCounter().setLastQueried(currentTimeMillis);
        counter.getCounter().setQueriedUpdatedDiff(0L);
        final boolean windowsDropped = counter.dropExpiredWindows();
        
        if (windowsDropped || (currentTimeMillis > timeWithoutUpdateBoundary)) {
            return persistFunction.apply(counter);
        } else {
            return noPersistFunction.apply(counter);
        }
    }
    
}
