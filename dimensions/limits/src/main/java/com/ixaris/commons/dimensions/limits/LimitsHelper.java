package com.ixaris.commons.dimensions.limits;

import static com.ixaris.commons.async.lib.Async.all;
import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.dimensions.limits.CommonsDimensionsLimits.LimitCriterion.COUNT;
import static com.ixaris.commons.dimensions.limits.CommonsDimensionsLimits.LimitCriterion.COUNT_MAX;
import static com.ixaris.commons.dimensions.limits.CommonsDimensionsLimits.LimitCriterion.COUNT_MIN;
import static com.ixaris.commons.dimensions.limits.CommonsDimensionsLimits.LimitCriterion.COUNT_MIN_MAX;
import static com.ixaris.commons.dimensions.limits.CommonsDimensionsLimits.LimitCriterion.MAX;
import static com.ixaris.commons.dimensions.limits.CommonsDimensionsLimits.LimitCriterion.MIN;
import static com.ixaris.commons.dimensions.limits.CommonsDimensionsLimits.LimitCriterion.MIN_MAX;

import java.util.ArrayList;
import java.util.Collections;
import java.util.EnumSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.SortedSet;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.jooq.UpdatableRecord;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.dimensions.counters.CounterDef;
import com.ixaris.commons.dimensions.counters.CounterValue;
import com.ixaris.commons.dimensions.counters.CountersHelper;
import com.ixaris.commons.dimensions.counters.WindowWidth;
import com.ixaris.commons.dimensions.lib.base.DimensionalHelper;
import com.ixaris.commons.dimensions.lib.context.Context;
import com.ixaris.commons.dimensions.lib.context.Dimension;
import com.ixaris.commons.dimensions.limits.CommonsDimensionsLimits.LimitCriterion;
import com.ixaris.commons.dimensions.limits.cache.LimitCacheEntry;
import com.ixaris.commons.dimensions.limits.cache.LimitsCache;
import com.ixaris.commons.dimensions.limits.cache.LimitsCacheProvider;
import com.ixaris.commons.dimensions.limits.data.AbstractLimitExtensionEntity;
import com.ixaris.commons.dimensions.limits.data.LimitEntity;
import com.ixaris.commons.jooq.persistence.JooqAsyncPersistenceProvider;

@Component
public final class LimitsHelper {
    
    private static final Set<LimitCriterion> IS_COUNT = EnumSet.of(COUNT, COUNT_MIN, COUNT_MAX, COUNT_MIN_MAX);
    private static final Set<LimitCriterion> IS_MIN = EnumSet.of(MIN, MIN_MAX, COUNT_MIN, COUNT_MIN_MAX);
    private static final Set<LimitCriterion> IS_MAX = EnumSet.of(MAX, MIN_MAX, COUNT_MAX, COUNT_MIN_MAX);
    
    private final JooqAsyncPersistenceProvider db;
    private final LimitsCacheProvider cache;
    private final CountersHelper counters;
    
    @Autowired
    public LimitsHelper(final JooqAsyncPersistenceProvider db, final LimitsCacheProvider cache, final CountersHelper counters) {
        this.db = db;
        this.cache = cache;
        this.counters = counters;
    }
    
    public <T, I extends AbstractLimitExtensionEntity<T, I>, L extends LimitDef<I>> Async<SortedSet<LimitEntity<I, L>>> getAllMatchingLimits(final Context<L> context) {
        DimensionalHelper.validateForQuery(context);
        final L def = context.getDef();
        return getCacheEntry(def).map(cacheEntry -> cacheEntry.getLimitsForContext(context));
    }
    
    public <T, I extends AbstractLimitExtensionEntity<T, I>, L extends ValueLimitDef<I>> Async<LimitBounds<I, L>> getMostRestrictive(final Context<L> context, final T extensionParam) {
        
        final SortedSet<LimitEntity<I, L>> limits = await(getAllMatchingLimits(context));
        if (limits.isEmpty()) {
            // no limit was found, and it is an optional limit
            return result(new LimitBounds<>(null, null, null, null, null, null));
        }
        
        final L def = context.getDef();
        
        Long count = null;
        LimitEntity<I, L> mostRestrictiveCountLimit = null;
        Long min = null;
        LimitEntity<I, L> mostRestrictiveMaxLimit = null;
        Long max = null;
        LimitEntity<I, L> mostRestrictiveMinLimit = null;
        
        for (final LimitEntity<I, L> limit : limits) {
            if (IS_COUNT.contains(def.getCriterion())) {
                final Long limitCount = await(limit.getInfo().getCount(extensionParam, limit.getLimit()));
                if ((limitCount != null) && ((count == null) || limitCount < count)) {
                    count = limitCount;
                    mostRestrictiveCountLimit = limit;
                }
            }
            if (IS_MIN.contains(def.getCriterion())) {
                final Long limitMin = await(limit.getInfo().getMin(extensionParam, limit.getLimit()));
                if ((limitMin != null) && ((min == null) || limitMin > min)) {
                    min = limitMin;
                    mostRestrictiveMinLimit = limit;
                }
            }
            if (IS_MAX.contains(def.getCriterion())) {
                final Long limitMax = await(limit.getInfo().getMax(extensionParam, limit.getLimit()));
                if ((limitMax != null) && ((max == null) || limitMax < max)) {
                    max = limitMax;
                    mostRestrictiveMaxLimit = limit;
                }
            }
        }
        
        return result(new LimitBounds<>(mostRestrictiveCountLimit, count, mostRestrictiveMinLimit, min, mostRestrictiveMaxLimit, max));
    }
    
    public <T, I extends AbstractLimitExtensionEntity<T, I>, R extends UpdatableRecord<R>, C extends CounterDef<R, C>, L extends CounterLimitDef<I, C>> Async<LimitBounds<I, L>> getDeltasToReactLimit(final Context<L> context,
                                                                                                                                                                                                       final T extensionParam) throws TimeoutException {
        final SortedSet<LimitEntity<I, L>> limits = await(getAllMatchingLimits(context));
        if (limits.isEmpty()) {
            // no limit was found, and it is an optional limit
            return result(new LimitBounds<>(null, null, null, null, null, null));
        }
        
        final List<Async<LimitBounds<I, L>>> promises = new ArrayList<>(limits.size());
        for (final LimitEntity<I, L> limit : limits) {
            promises.add(getDeltasToReachSingleLimit(context, limit, extensionParam));
        }
        return all(promises).map(this::collapseLimitBounds);
    }
    
    private <T, I extends AbstractLimitExtensionEntity<T, I>, R extends UpdatableRecord<R>, C extends CounterDef<R, C>, L extends CounterLimitDef<I, C>> Async<LimitBounds<I, L>> getDeltasToReachSingleLimit(final Context<L> context,
                                                                                                                                                                                                              final LimitEntity<I, L> limit,
                                                                                                                                                                                                              final T extensionParam) throws TimeoutException {
        final CounterValue cv = await(getCounterValue(
            context, limit.getContext(), limit.getNarrowWindowWidth(), limit.getWideWindowMultiple()));
        
        final L def = context.getDef();
        
        Long count = null;
        LimitEntity<I, L> mostRestrictiveCountLimit = null;
        Long min = null;
        LimitEntity<I, L> mostRestrictiveMaxLimit = null;
        Long max = null;
        LimitEntity<I, L> mostRestrictiveMinLimit = null;
        
        if (IS_COUNT.contains(def.getCriterion())) {
            final Long limitCount = await(limit.getInfo().getCount(extensionParam, limit.getLimit()));
            if (limitCount != null) {
                count = limitCount - cv.getWide().getCount();
                mostRestrictiveCountLimit = limit;
            }
        }
        if (IS_MIN.contains(def.getCriterion())) {
            final Long limitMin = await(limit.getInfo().getMin(extensionParam, limit.getLimit()));
            if (limitMin != null) {
                min = limitMin - cv.getWide().getSum();
                mostRestrictiveMinLimit = limit;
            }
        }
        if (IS_MAX.contains(def.getCriterion())) {
            final Long limitMax = await(limit.getInfo().getMax(extensionParam, limit.getLimit()));
            if (limitMax != null) {
                max = limitMax - cv.getWide().getSum();
                mostRestrictiveMaxLimit = limit;
            }
        }
        
        return result(new LimitBounds<>(mostRestrictiveCountLimit, count, mostRestrictiveMinLimit, min, mostRestrictiveMaxLimit, max));
    }
    
    public <T, I extends AbstractLimitExtensionEntity<T, I>, R extends UpdatableRecord<R>, C extends CounterDef<R, C>, L extends CounterLimitDef<I, C>> Async<List<LimitExceeded>> validateDeltaAgainstLimit(final Context<L> context,
                                                                                                                                                                                                             final T extensionParam,
                                                                                                                                                                                                             final long countDelta,
                                                                                                                                                                                                             final long sumDelta) throws TimeoutException {
        
        DimensionalHelper.validateForQuery(context);
        final L def = context.getDef();
        final LimitCacheEntry<I, L> cacheEntry = await(getCacheEntry(def));
        final SortedSet<LimitEntity<I, L>> limits = cacheEntry.getLimitsForContext(context);
        if (limits.isEmpty()) {
            // no limit was found, and it is an optional limit
            return result(Collections.emptyList());
        }
        
        final List<Async<LimitExceeded>> promises = new ArrayList<>(limits.size());
        for (final LimitEntity<I, L> limit : limits) {
            promises.add(validateDeltaAgainstSingleLimit(context, limit, extensionParam, countDelta, sumDelta));
        }
        
        return all(promises).map(l -> l.stream().filter(Objects::nonNull).collect(Collectors.toList()));
    }
    
    public <T, I extends AbstractLimitExtensionEntity<T, I>, R extends UpdatableRecord<R>, C extends CounterDef<R, C>, L extends CounterLimitDef<I, C>> Async<LimitExceeded> validateDeltaAgainstSingleLimit(final Context<L> context,
                                                                                                                                                                                                             final LimitEntity<I, L> limit,
                                                                                                                                                                                                             final T extensionParam,
                                                                                                                                                                                                             final long countDelta,
                                                                                                                                                                                                             final long sumDelta) throws TimeoutException {
        final CounterValue cv = await(getCounterValue(
            context, limit.getContext(), limit.getNarrowWindowWidth(), limit.getWideWindowMultiple()));
        
        final L def = context.getDef();
        
        boolean countExceeded = false;
        boolean minExceeded = false;
        boolean maxExceeded = false;
        
        if (IS_COUNT.contains(def.getCriterion())) {
            final Long limitCount = await(limit.getInfo().getCount(extensionParam, limit.getLimit()));
            if ((limitCount != null) && ((limitCount - cv.getWide().getCount()) < countDelta)) {
                countExceeded = true;
            }
        }
        if (IS_MIN.contains(def.getCriterion())) {
            final Long limitMin = await(limit.getInfo().getMin(extensionParam, limit.getLimit()));
            if ((limitMin != null) && ((limitMin - cv.getWide().getSum()) > sumDelta)) {
                minExceeded = true;
            }
        }
        if (IS_MAX.contains(def.getCriterion())) {
            final Long limitMax = await(limit.getInfo().getMax(extensionParam, limit.getLimit()));
            if ((limitMax != null) && ((limitMax - cv.getWide().getSum()) < sumDelta)) {
                maxExceeded = true;
            }
        }
        
        if (countExceeded || minExceeded || maxExceeded) {
            return result(new LimitExceeded(limit, countExceeded, minExceeded, maxExceeded));
        } else {
            return result(null);
        }
    }
    
    private <I extends AbstractLimitExtensionEntity<?, I>, L extends LimitDef<I>> Async<LimitCacheEntry<I, L>> getCacheEntry(final L def) {
        final LimitsCache<I, L> defCache = cache.of(def);
        final LimitCacheEntry<I, L> entry = defCache.get();
        if (entry != null) {
            return result(entry);
        } else {
            return db
                .transaction(() -> LimitEntity.lookupForCache(def))
                .map(matching -> {
                    final LimitCacheEntry<I, L> cacheEntry = new LimitCacheEntry<>(matching);
                    defCache.set(cacheEntry);
                    return cacheEntry;
                });
        }
    }
    
    private <R extends UpdatableRecord<R>, C extends CounterDef<R, C>, L extends CounterLimitDef<?, C>> Async<CounterValue> getCounterValue(final Context<L> queryContext,
                                                                                                                                            final Context<L> limitContext,
                                                                                                                                            final WindowWidth narrowWindowWidth,
                                                                                                                                            final int wideWindowMultiple) throws TimeoutException {
        
        // Iterate over the dimensions defined for the given limit to fill up the counter context. If a dimension is set
        // as a MATCH_ANY value, then
        // replace this
        // value with the value specified in the fullLimitContext.
        final Context.Builder<C> counterContext = Context.newBuilder(limitContext.getDef().getCounterDef());
        
        for (final Dimension<?> dimension : limitContext.getDimensions()) {
            if (dimension.getValue() == null) {
                counterContext.add(queryContext.getDimension(dimension
                    .getDefinition())); // replace this dimension with that specified in the limitContext.
            } else {
                counterContext.add(dimension); // If the dimension is not supported by the counter, this will not be added.
            }
        }
        // add any dimensions which are not configurable but still need to be used with this counter
        counterContext.addAll(limitContext.getDef().getConstantDimensions());
        final Context<C> context = counterContext.build();
        
        return counters.getCounter(context, narrowWindowWidth, wideWindowMultiple);
    }
    
    private <I extends AbstractLimitExtensionEntity<?, I>, L extends CounterLimitDef<I, ?>> LimitBounds<I, L> collapseLimitBounds(final List<LimitBounds<I, L>> l) {
        Long count = null;
        LimitEntity<I, L> mostRestrictiveCountLimit = null;
        Long min = null;
        LimitEntity<I, L> mostRestrictiveMaxLimit = null;
        Long max = null;
        LimitEntity<I, L> mostRestrictiveMinLimit = null;
        
        for (final LimitBounds<I, L> limit : l) {
            final Long limitCount = limit.getCount();
            if ((limitCount != null) && ((count == null) || limitCount < count)) {
                count = limitCount;
                mostRestrictiveCountLimit = limit.getCountLimit();
            }
            final Long limitMin = limit.getMin();
            if ((limitMin != null) && ((min == null) || limitMin > min)) {
                min = limitMin;
                mostRestrictiveMinLimit = limit.getMinLimit();
            }
            final Long limitMax = limit.getMax();
            if ((limitMax != null) && ((max == null) || limitMax < max)) {
                max = limitMax;
                mostRestrictiveMaxLimit = limit.getMaxLimit();
            }
        }
        
        return new LimitBounds<>(mostRestrictiveCountLimit, count, mostRestrictiveMinLimit, min, mostRestrictiveMaxLimit, max);
    }
    
}
