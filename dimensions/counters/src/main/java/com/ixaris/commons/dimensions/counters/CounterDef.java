/*
 * Copyright 2002, 2011 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.counters;

import java.util.Iterator;

import org.jooq.Table;
import org.jooq.UpdatableRecord;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.CompletionStageCallableThrows;
import com.ixaris.commons.async.lib.LongIdAsyncQueue;
import com.ixaris.commons.async.lib.StringIdAsyncQueue;
import com.ixaris.commons.dimensions.counters.data.CounterEventEntity;
import com.ixaris.commons.dimensions.lib.base.DimensionalDef;
import com.ixaris.commons.dimensions.lib.context.Context;
import com.ixaris.commons.dimensions.lib.context.Dimension;
import com.ixaris.commons.dimensions.lib.context.DimensionDef;
import com.ixaris.commons.misc.lib.registry.Registerable;

/**
 * Interface to be defined by Counter Events (what used to be called deltas)
 *
 * <p>A table with the same name as the getKey() should exist in the database (extending AbstractCounterValue), with dimension field names
 * matching exactly the supportedContext dimensions.
 *
 * @author <a href="mailto:andrew.calafato@ixaris.com">Andrew Calafato</a>
 */
public interface CounterDef<R extends UpdatableRecord<R>, C extends CounterDef<R, C>> extends DimensionalDef, Registerable {
    
    @SuppressWarnings("squid:S1452")
    static <R extends UpdatableRecord<R>, C extends CounterDef<R, C>> Dimension<?> extractFirstPartitionDimension(final C def, final Context<C> context) {
        final Iterator<DimensionDef<?>> iteratorByImportance = def.getContextDef().iterator();
        for (int i = 0; i < def.getPartitionDimensionsCount() && iteratorByImportance.hasNext(); i++) {
            final DimensionDef<?> dimensionDef = iteratorByImportance.next();
            final Dimension<?> dimension = context.get(dimensionDef);
            if (dimension != null) {
                return dimension;
            }
        }
        return null;
    }
    
    static <T, E extends Exception> Async<T> queue(final String key,
                                                   final String dimensionName,
                                                   final String stringValue,
                                                   final Long longValue,
                                                   final CompletionStageCallableThrows<T, E> callable) throws E {
        if (stringValue != null) {
            return StringIdAsyncQueue.exec(key + '/' + dimensionName, stringValue, callable);
        } else if (longValue != null) {
            return LongIdAsyncQueue.exec(key + '/' + dimensionName, longValue, callable);
        } else {
            return LongIdAsyncQueue.exec(key, 0L, callable);
        }
    }
    
    /**
     * Calculate and return the Count and Sum values for a particular context, for a particular date range (window). Called to initialise the
     * data for a counter.
     *
     * @param context The context.
     * @param from The start dateTime for counter data (inclusive - query should use >= ).
     * @param to The end dateTime for counter data (exclusive - query should use < ). Can be null if requested to fetch values for all counters
     *     (typically until now).
     * @return WindowValue - including Count, Sum.
     */
    WindowValue fetchWindow(Context<C> context, long from, Long to);
    
    Table<R> getContextTable();
    
    Context<C> extractContextInstance(R contextRecord);
    
    /**
     * Create a new context record. Runs in an existing transaction.
     *
     * <p>The implementation would typically just call the AbstractCounterValue constructor (ConcreteCounterValue super Constructor) passing the
     * same parameters.
     *
     * @param id the id of the counter
     * @param context The full list of supported dimensions.
     * @return the created counter value
     */
    R newContextRecord(long id, Context<C> context);
    
    /**
     * Should retrieve an event with the given id. Runs in an existing transaction.
     *
     * @param eventId
     * @return
     */
    CounterEventEntity<C> lookupCounterEvent(long eventId);
    
    /**
     * For concurrency, counters may be processed on different nodes based on the partition dimensions. These should be the first dimensions
     * defined in the context. Partitioning on these dimensions means the same node should process counter event for the same value of a
     * partitioning dimension. E.g. for a partitioning dimension AccountId, node X always processes events for account Y. It is assumed that when
     * processing events for the nth partition dimensions, 0..n-1 dimensions are null, i.e. considering (A, B) as partitioning dimensions, for
     * event (vA, vB), the first partition will process counters for (vA, null) and (vA, vB) while the second partition will process events for
     * (null, vB)
     *
     * @return the number of partition dimensions
     */
    int getPartitionDimensionsCount();
    
    /**
     * For efficiency reasons, the match query is generated by using the cartesian product of the dimensions that narrow matches the most, which
     * would be the dimensions whose values vary significantly. The partition dimensions and these dimensions are used to generate a number of
     * queries that are combined using UNION ALL. This means that a composite index should be created over these dimensions.
     *
     * @return the number of cartesian product dimensions (beyond the partition dimensions)
     */
    int getCartesianProductDimensionsCount();
    
}
