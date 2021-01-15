package com.ixaris.commons.dimensions.counters.admin;

import static com.ixaris.commons.dimensions.counters.jooq.tables.LibDimCounter.LIB_DIM_COUNTER;
import static com.ixaris.commons.jooq.persistence.TransactionalDSLContext.JOOQ_TX;

import java.util.Collections;
import java.util.List;

import org.jooq.Table;
import org.jooq.UpdatableRecord;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.dimensions.counters.CounterDef;
import com.ixaris.commons.dimensions.counters.CounterDefRegistry;
import com.ixaris.commons.dimensions.counters.cache.CounterCache;
import com.ixaris.commons.dimensions.counters.cache.CounterCacheProvider;
import com.ixaris.commons.dimensions.counters.data.CounterEntity;
import com.ixaris.commons.dimensions.counters.jooq.tables.records.LibDimCounterRecord;
import com.ixaris.commons.dimensions.lib.context.Context;
import com.ixaris.commons.jooq.persistence.JooqAsyncPersistenceProvider;

@Component
public final class CountersAdminHelper {
    
    private static final Logger LOG = LoggerFactory.getLogger(CountersAdminHelper.class);
    
    private final JooqAsyncPersistenceProvider db;
    private final CounterCacheProvider cache;
    
    @Autowired
    public CountersAdminHelper(final JooqAsyncPersistenceProvider db,
                               final CounterCacheProvider cache) {
        if (db == null) {
            throw new IllegalArgumentException("provider is null");
        }
        if (cache == null) {
            throw new IllegalArgumentException("cache is null");
        }
        this.cache = cache;
        this.db = db;
    }
    
    public Async<Void> cleanUp(final long unqueriedCounterThresholdMillis) {
        return db.transaction(() -> {
            for (final CounterDef<?, ?> def : CounterDefRegistry.getInstance().getRegisteredValues()) {
                cleanUp(def, unqueriedCounterThresholdMillis);
            }
            return null;
        });
    }
    
    @SuppressWarnings("unchecked")
    private <R extends UpdatableRecord<R>, C extends CounterDef<R, C>> void cleanUp(final CounterDef<R, C> counterDef,
                                                                                    final long unqueriedCounterThresholdMillis) {
        final C def = (C) counterDef;
        // final long unqueriedCounterThresholdMillis = 7 * WindowTimeUnit.MILLISECONDS_IN_DAY;
        final Table<R> contextTable = def.getContextTable();
        final List<CounterEntity<R, C>> entities = JOOQ_TX.get()
            .select()
            .from(LIB_DIM_COUNTER)
            .join(contextTable)
            .on(contextTable.field("id", Long.class).eq(LIB_DIM_COUNTER.ID))
            .where(LIB_DIM_COUNTER.QUERIED_UPDATED_DIFF.ge(unqueriedCounterThresholdMillis))
            //                LIB_DIM_COUNTER.SHARD.in(clusterRegistry.getNodeInfo().getShards())
            
            .fetch()
            .map(r -> {
                final LibDimCounterRecord counter = r.into(LIB_DIM_COUNTER);
                final R context = r.into(contextTable);
                return new CounterEntity<>(def, counter, context, Collections.emptyList());
            });
        
        for (final CounterEntity<R, C> entity : entities) {
            final Context<C> context = entity.getContext();
            if (context.isCacheable()) {
                final CounterCache<R, C> defCache = cache.of(def);
                defCache.invalidate(context, entity.getNarrowWindowWidth(), entity.getWideWindowMultiple());
            }
            entity.delete();
            LOG.debug("Cleaning up counter with id [" + entity.getCounter().getId() + "]");
        }
    }
    
}
