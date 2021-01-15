/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.limits.admin;

import static com.ixaris.commons.async.lib.idempotency.Intent.INTENT;
import static com.ixaris.commons.jooq.persistence.TransactionalDSLContext.JOOQ_TX;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.dimensions.counters.WindowWidth;
import com.ixaris.commons.dimensions.lib.base.DimensionalHelper;
import com.ixaris.commons.dimensions.lib.context.Context;
import com.ixaris.commons.dimensions.limits.CounterLimitDef;
import com.ixaris.commons.dimensions.limits.LimitDef;
import com.ixaris.commons.dimensions.limits.ValueLimitDef;
import com.ixaris.commons.dimensions.limits.cache.LimitsCacheProvider;
import com.ixaris.commons.dimensions.limits.data.AbstractLimitExtensionEntity;
import com.ixaris.commons.dimensions.limits.data.LimitEntity;
import com.ixaris.commons.jooq.persistence.JooqAsyncPersistenceProvider;

@Component
public final class LimitsAdminHelper {
    
    public static <T, I extends AbstractLimitExtensionEntity<T, I>, L extends ValueLimitDef<I>> void setLimit(final LimitsCacheProvider cache,
                                                                                                              final Context<L> context,
                                                                                                              final Long effectiveFrom,
                                                                                                              final Long effectiveTo,
                                                                                                              final Long count,
                                                                                                              final Long min,
                                                                                                              final Long max,
                                                                                                              final Function<Long, I> infoCreate) {
        internalSetLimit(cache,
            context,
            effectiveFrom,
            effectiveTo,
            modifiedEffectiveFrom -> new LimitEntity<>(context, infoCreate)
                .apply(l -> {
                    l.setEffectiveFrom(modifiedEffectiveFrom);
                    l.setEffectiveTo(effectiveTo);
                    l.setMaxCount(count);
                    l.setMinAmount(min);
                    l.setMaxAmount(max);
                })
                .store());
    }
    
    public static <T, I extends AbstractLimitExtensionEntity<T, I>, L extends CounterLimitDef<I, ?>> void setLimit(final LimitsCacheProvider cache,
                                                                                                                   final Context<L> context,
                                                                                                                   final Long effectiveFrom,
                                                                                                                   final Long effectiveTo,
                                                                                                                   final Long count,
                                                                                                                   final Long min,
                                                                                                                   final Long max,
                                                                                                                   final WindowWidth narrowWindowWidth,
                                                                                                                   final int wideWindowMultiple,
                                                                                                                   final Function<Long, I> infoCreate) {
        if (narrowWindowWidth == null) {
            throw new IllegalArgumentException("narrowWindowWidth is null");
        }
        if (wideWindowMultiple < 1) {
            throw new IllegalArgumentException(String.format(
                "wideWindowMultiple %d is not positive", wideWindowMultiple));
        }
        internalSetLimit(cache,
            context,
            effectiveFrom,
            effectiveTo,
            modifiedEffectiveFrom -> new LimitEntity<>(context, infoCreate)
                .apply(l -> {
                    l.setEffectiveFrom(modifiedEffectiveFrom);
                    l.setEffectiveTo(effectiveTo);
                    l.setMaxCount(count);
                    l.setMinAmount(min);
                    l.setMaxAmount(max);
                    l.setWideWindowMultiple(wideWindowMultiple);
                })
                .setNarrowWindowWidth(narrowWindowWidth)
                .store());
    }
    
    public static <T, I extends AbstractLimitExtensionEntity<T, I>, L extends LimitDef<I>> void unsetLimit(final LimitsCacheProvider cache,
                                                                                                           final Context<L> context,
                                                                                                           final Long effectiveFrom,
                                                                                                           final Long effectiveTo) {
        internalSetLimit(cache, context, effectiveFrom, effectiveTo, nil -> {});
    }
    
    private static <T, I extends AbstractLimitExtensionEntity<T, I>, L extends LimitDef<I>> void internalSetLimit(final LimitsCacheProvider cache,
                                                                                                                  final Context<L> context,
                                                                                                                  final Long effectiveFrom,
                                                                                                                  final Long effectiveTo,
                                                                                                                  final Consumer<Long> limitAction) {
        final long now = System.currentTimeMillis();
        final long validEffectiveFrom;
        if (effectiveFrom == null) {
            validEffectiveFrom = now;
        } else if (effectiveFrom < now) {
            throw new IllegalArgumentException("effectiveFrom is in the past");
        } else {
            validEffectiveFrom = effectiveFrom;
        }
        
        if (compareEffective(effectiveTo, validEffectiveFrom) <= 0) {
            throw new IllegalArgumentException("effectiveTo is before effectiveFrom");
        }
        
        DimensionalHelper.validateForConfig(context);
        final L def = context.getDef();
        
        final List<LimitEntity<I, L>> matches = LimitEntity.lookupExactMatches(context);
        
        for (final LimitEntity<I, L> limit : matches) {
            if (compareEffective(limit.getLimit().getEffectiveFrom(), validEffectiveFrom) < 0) {
                if (compareEffective(limit.getLimit().getEffectiveTo(), validEffectiveFrom) >= 0) {
                    
                    /*
                     * overlap at beginning. Options are:
                     * 1. move effectiveTo to earlier if this limit extends beyond the existing limit's effective to
                     * 2. split in 2 limits surrounding new limit
                     */
                    if (compareEffective(limit.getLimit().getEffectiveTo(), effectiveTo) > 0) {
                        new LimitEntity<>(context, id -> limit.getInfo().copyForSplitLimit(id))
                            .apply(l -> {
                                l.setEffectiveFrom(effectiveTo + 1L);
                                l.setEffectiveTo(limit.getLimit().getEffectiveTo());
                                l.setMaxCount(limit.getLimit().getMaxCount());
                                l.setMinAmount(limit.getLimit().getMinAmount());
                                l.setMaxAmount(limit.getLimit().getMaxAmount());
                                l.setNarrowWindowUnit(limit.getLimit().getNarrowWindowUnit());
                                l.setNarrowWindowWidth(limit.getLimit().getNarrowWindowWidth());
                                l.setWideWindowMultiple(limit.getLimit().getWideWindowMultiple());
                            })
                            .store();
                    }
                    limit.getLimit().setEffectiveTo(validEffectiveFrom - 1L);
                    limit.store();
                }
            } else if (compareEffective(limit.getLimit().getEffectiveFrom(), effectiveTo) <= 0) {
                
                /*
                 * overlap at end. Options are:
                 * 1. move effectiveFrom to later if the existing limit extends beyond this limit's effective to
                 * 2. remove the limit if new limit completely overlaps the existing limit
                 */
                if (compareEffective(limit.getLimit().getEffectiveTo(), effectiveTo) > 0) {
                    limit.getLimit().setEffectiveFrom(effectiveTo + 1L);
                    limit.store();
                } else {
                    limit.delete();
                }
            }
        }
        
        limitAction.accept(validEffectiveFrom);
        JOOQ_TX.get().onCommit(() -> cache.of(def).clear());
    }
    
    public static int compareEffective(final Long l1, final Long l2) {
        if (l1 == null) {
            if (l2 == null) {
                return 0;
            } else {
                return 1;
            }
        } else {
            if (l2 == null) {
                return -1;
            } else {
                return Long.compare(l1, l2);
            }
        }
    }
    
    private final JooqAsyncPersistenceProvider db;
    private final LimitsCacheProvider cache;
    
    @Autowired
    public LimitsAdminHelper(final JooqAsyncPersistenceProvider db, final LimitsCacheProvider cache) {
        if (db == null) {
            throw new IllegalArgumentException("provider is null");
        }
        if (cache == null) {
            throw new IllegalArgumentException("cache is null");
        }
        this.cache = cache;
        this.db = db;
    }
    
    public <T, I extends AbstractLimitExtensionEntity<T, I>, L extends LimitDef<I>> Async<LimitEntity<I, L>> lookup(
                                                                                                                    final L def, final long id) {
        return db.transactionRequired(() -> LimitEntity.fetch(def, id));
    }
    
    /**
     * Gets all limits and corresponding context defined that match the given context.
     *
     * @param context the context
     * @return all matching defined values. Never null
     */
    public <I extends AbstractLimitExtensionEntity<?, I>, L extends LimitDef<I>> Async<List<LimitEntity<I, L>>> getAllLimitsMatchingContext(final Context<L> context) {
        DimensionalHelper.validateForConfigLookup(context);
        return db.transactionRequired(() -> LimitEntity.lookupAllMatchingContext(context));
    }
    
    /**
     * Gets all limits and corresponding context defined that contain the given context. Will return all limits if an
     * empty context is passed
     *
     * @param context the context
     * @return all matching defined set. Never null
     */
    public <I extends AbstractLimitExtensionEntity<?, I>, L extends LimitDef<I>> Async<List<LimitEntity<I, L>>> getAllLimitsContainingContext(final Context<L> context) {
        DimensionalHelper.validateForConfigLookup(context);
        return db.transactionRequired(() -> LimitEntity.lookupAllContainingContext(context));
    }
    
    /**
     * @param context The context used to filter.
     * @return All the {@link LimitDef}s having a context exactly as specified by the passed {@code context}.
     */
    public <I extends AbstractLimitExtensionEntity<?, I>, L extends LimitDef<I>> Async<List<LimitEntity<I, L>>> getAllLimitsExactlyMatchingContext(final Context<L> context) {
        DimensionalHelper.validateForConfigLookup(context);
        return db.transactionRequired(() -> LimitEntity.lookupExactMatches(context));
    }
    
    public <T, I extends AbstractLimitExtensionEntity<T, I>, L extends ValueLimitDef<I>> Async<Void> setLimit(final Context<L> context,
                                                                                                              final Long effectiveFrom,
                                                                                                              final Long effectiveTo,
                                                                                                              final Long count,
                                                                                                              final Long min,
                                                                                                              final Long max,
                                                                                                              final Function<Long, I> infoCreate) {
        return db
            .transaction(INTENT.get(), () -> {
                setLimit(cache, context, effectiveFrom, effectiveTo, count, min, max, infoCreate);
                return (Void) null;
            })
            .onDuplicateIntent(e -> null);
    }
    
    public <T, I extends AbstractLimitExtensionEntity<T, I>, L extends CounterLimitDef<I, ?>> Async<Void> setLimit(final Context<L> context,
                                                                                                                   final Long effectiveFrom,
                                                                                                                   final Long effectiveTo,
                                                                                                                   final Long count,
                                                                                                                   final Long min,
                                                                                                                   final Long max,
                                                                                                                   final WindowWidth narrowWindowWidth,
                                                                                                                   final int wideWindowMultiple,
                                                                                                                   final Function<Long, I> infoCreate) {
        return db
            .transaction(INTENT.get(), () -> {
                setLimit(cache,
                    context,
                    effectiveFrom,
                    effectiveTo,
                    count,
                    min,
                    max,
                    narrowWindowWidth,
                    wideWindowMultiple,
                    infoCreate);
                return (Void) null;
            })
            .onDuplicateIntent(e -> null);
    }
    
    public <T, I extends AbstractLimitExtensionEntity<T, I>, L extends LimitDef<I>> Async<Void> unsetLimit(final Context<L> context, final Long effectiveFrom, final Long effectiveTo) {
        return db
            .transaction(INTENT.get(), () -> {
                unsetLimit(cache, context, effectiveFrom, effectiveTo);
                return (Void) null;
            })
            .onDuplicateIntent(e -> null);
    }
}
