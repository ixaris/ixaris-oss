/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.limits.data;

import static com.ixaris.commons.dimensions.counters.data.CounterEntity.WINDOW_TIME_UNIT_MAPPING;
import static com.ixaris.commons.dimensions.limits.jooq.tables.LibDimLimit.LIB_DIM_LIMIT;
import static com.ixaris.commons.dimensions.limits.jooq.tables.LibDimLimitDimension.LIB_DIM_LIMIT_DIMENSION;
import static com.ixaris.commons.jooq.persistence.TransactionalDSLContext.JOOQ_TX;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.stream.Collectors;

import org.jooq.Condition;
import org.jooq.Record;
import org.jooq.SelectJoinStep;
import org.jooq.SortField;

import com.ixaris.commons.dimensions.counters.WindowWidth;
import com.ixaris.commons.dimensions.lib.base.DimensionalHelper;
import com.ixaris.commons.dimensions.lib.base.DimensionalHelper.RecordHolder;
import com.ixaris.commons.dimensions.lib.context.Context;
import com.ixaris.commons.dimensions.lib.context.PersistedDimensionValue;
import com.ixaris.commons.dimensions.limits.CommonsDimensionsLimits.CounterLimit;
import com.ixaris.commons.dimensions.limits.CommonsDimensionsLimits.CounterLimitValue;
import com.ixaris.commons.dimensions.limits.CommonsDimensionsLimits.CounterLimitValue.WindowTimeUnit;
import com.ixaris.commons.dimensions.limits.CommonsDimensionsLimits.CounterLimits;
import com.ixaris.commons.dimensions.limits.CommonsDimensionsLimits.LimitValue;
import com.ixaris.commons.dimensions.limits.CommonsDimensionsLimits.ValueLimit;
import com.ixaris.commons.dimensions.limits.CommonsDimensionsLimits.ValueLimits;
import com.ixaris.commons.dimensions.limits.LimitDef;
import com.ixaris.commons.dimensions.limits.data.AbstractLimitExtensionEntity.Fetch;
import com.ixaris.commons.dimensions.limits.jooq.tables.records.LibDimLimitDimensionRecord;
import com.ixaris.commons.dimensions.limits.jooq.tables.records.LibDimLimitRecord;
import com.ixaris.commons.jooq.persistence.Entity;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;
import com.ixaris.commons.misc.lib.object.EqualsUtil;
import com.ixaris.commons.protobuf.lib.ProtobufConverters;

/**
 * A context property value. One to many relation with context property dimension
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
public final class LimitEntity<I extends AbstractLimitExtensionEntity<?, I>, L extends LimitDef<I>> extends Entity<LimitEntity<I, L>> {
    
    public static <I extends AbstractLimitExtensionEntity<?, I>, L extends LimitDef<I>> LimitEntity<I, L> fetch(final L def, final long id) {
        final Fetch<I, ?> infoFetch = def.getInfoFetch();
        SelectJoinStep<Record> from = JOOQ_TX.get().select().from(LIB_DIM_LIMIT);
        SelectJoinStep<Record> records = def.getInfoFetch().joinWithInfo(from);
        final RecordHolder<LibDimLimitRecord, LibDimLimitDimensionRecord> result = DimensionalHelper.fetch(def,
            LIB_DIM_LIMIT,
            LIB_DIM_LIMIT_DIMENSION,
            records,
            id);
        
        return finishFetch(def, infoFetch, id, result);
    }
    
    public static <I extends AbstractLimitExtensionEntity<?, I>, L extends LimitDef<I>> List<LimitEntity<I, L>> lookupExactMatches(final Context<L> context) {
        return lookup(context, true, true, true, System.currentTimeMillis(), true);
    }
    
    public static <I extends AbstractLimitExtensionEntity<?, I>, L extends LimitDef<I>> List<LimitEntity<I, L>> lookupForCache(final L def) {
        return lookup(Context.empty(def), false, true, true, System.currentTimeMillis(), true);
    }
    
    public static <I extends AbstractLimitExtensionEntity<?, I>, L extends LimitDef<I>> List<LimitEntity<I, L>> lookupAllMatchingContext(final Context<L> context) {
        return match(context, false, true, true, System.currentTimeMillis(), false);
    }
    
    public static <I extends AbstractLimitExtensionEntity<?, I>, L extends LimitDef<I>> List<LimitEntity<I, L>> lookupAllContainingContext(final Context<L> context) {
        return lookup(context, false, true, true, System.currentTimeMillis(), false);
    }
    
    public static <I extends AbstractLimitExtensionEntity<?, I>, L extends LimitDef<I>> ValueLimits collectionToValueProtobuf(final Collection<LimitEntity<I, L>> results) {
        final ValueLimits.Builder builder = ValueLimits.newBuilder();
        for (final LimitEntity<I, L> result : results) {
            builder.addLimits(result.toValueProtobuf());
        }
        return builder.build();
    }
    
    public static <I extends AbstractLimitExtensionEntity<?, I>, L extends LimitDef<I>> CounterLimits collectionToCounterProtobuf(final Collection<LimitEntity<I, L>> results) {
        final CounterLimits.Builder builder = CounterLimits.newBuilder();
        for (final LimitEntity<I, L> result : results) {
            builder.addLimits(result.toCounterProtobuf());
        }
        return builder.build();
    }
    
    /**
     * Will get the set that best matches the given gontext.
     *
     * @param context the context instance
     * @param mostSpecific
     * @param effective set true for currently effective or effective in the future, false to get ineffective i.e. limits in the past
     * @return the sets that best matches the context instance, or an empty list if no match found
     */
    private static <I extends AbstractLimitExtensionEntity<?, I>, L extends LimitDef<I>> List<LimitEntity<I, L>> match(final Context<L> context,
                                                                                                                       final boolean mostSpecific,
                                                                                                                       final boolean allowExactContextMatch,
                                                                                                                       final boolean effective,
                                                                                                                       final long now,
                                                                                                                       final boolean orderByEffective) {
        final L def = context.getDef();
        final Fetch<I, ?> infoFetch = def.getInfoFetch();
        Condition condition = LIB_DIM_LIMIT.LIMIT_KEY.eq(def.getKey());
        if (effective) {
            condition = condition.and(LIB_DIM_LIMIT.EFFECTIVE_TO.ge(now).or(LIB_DIM_LIMIT.EFFECTIVE_TO.isNull()));
        } else {
            condition = condition.and(LIB_DIM_LIMIT.EFFECTIVE_TO.lt(now));
        }
        final SortField<?>[] orderBy;
        if (orderByEffective) {
            orderBy = new SortField<?>[2];
            orderBy[0] = LIB_DIM_LIMIT.EFFECTIVE_FROM.asc();
            orderBy[1] = LIB_DIM_LIMIT.EFFECTIVE_TO.asc();
        } else {
            orderBy = new SortField<?>[0];
        }
        final List<RecordHolder<LibDimLimitRecord, LibDimLimitDimensionRecord>> result = DimensionalHelper.match(context,
            mostSpecific,
            allowExactContextMatch,
            LIB_DIM_LIMIT,
            LIB_DIM_LIMIT_DIMENSION,
            def.getInfoFetch().joinWithInfo(JOOQ_TX.get().select().from(LIB_DIM_LIMIT)),
            condition,
            orderBy);
        
        return finishFetch(def, infoFetch, result);
    }
    
    /**
     * Will get the value that matches the given context exactly.
     *
     * @param context the context instance
     * @return the sets (exactly) matching the context instance, or an empty list if no match found
     */
    private static <I extends AbstractLimitExtensionEntity<?, I>, L extends LimitDef<I>> List<LimitEntity<I, L>> lookup(final Context<L> context,
                                                                                                                        final boolean exact,
                                                                                                                        final boolean allowExactContextMatch,
                                                                                                                        final boolean effective,
                                                                                                                        final long now,
                                                                                                                        final boolean orderByEffective) {
        final L def = context.getDef();
        final Fetch<I, ?> infoFetch = def.getInfoFetch();
        Condition condition = LIB_DIM_LIMIT.LIMIT_KEY.eq(def.getKey());
        if (effective) {
            condition = condition.and(LIB_DIM_LIMIT.EFFECTIVE_TO.ge(now).or(LIB_DIM_LIMIT.EFFECTIVE_TO.isNull()));
        } else {
            condition = condition.and(LIB_DIM_LIMIT.EFFECTIVE_TO.lt(now));
        }
        final SortField<?>[] orderBy;
        if (orderByEffective) {
            orderBy = new SortField<?>[2];
            orderBy[0] = LIB_DIM_LIMIT.EFFECTIVE_FROM.asc();
            orderBy[1] = LIB_DIM_LIMIT.EFFECTIVE_TO.asc();
        } else {
            orderBy = new SortField<?>[0];
        }
        final List<RecordHolder<LibDimLimitRecord, LibDimLimitDimensionRecord>> result = DimensionalHelper.lookup(context,
            exact,
            allowExactContextMatch,
            LIB_DIM_LIMIT,
            LIB_DIM_LIMIT_DIMENSION,
            infoFetch.joinWithInfo(JOOQ_TX.get().select().from(LIB_DIM_LIMIT)),
            condition,
            orderBy);
        
        return finishFetch(def, infoFetch, result);
    }
    
    private static <I extends AbstractLimitExtensionEntity<?, I>, G, L extends LimitDef<I>> LimitEntity<I, L> finishFetch(final L limitDef,
                                                                                                                          final Fetch<I, G> infoFetch,
                                                                                                                          final long id,
                                                                                                                          final RecordHolder<LibDimLimitRecord, LibDimLimitDimensionRecord> tuple) {
        final G related = infoFetch.fetchRelated(id);
        return new LimitEntity<>(limitDef, tuple.getMainRecord(), tuple.getDimensionRecords(), infoFetch.from(tuple.getRecord(), related));
    }
    
    private static <I extends AbstractLimitExtensionEntity<?, I>, G, L extends LimitDef<I>> List<LimitEntity<I, L>> finishFetch(final L limitDef,
                                                                                                                                final Fetch<I, G> infoFetch,
                                                                                                                                final List<RecordHolder<LibDimLimitRecord, LibDimLimitDimensionRecord>> result) {
        final List<Long> ids = result.stream().map(r -> r.getMainRecord().getId()).collect(Collectors.toList());
        final Map<Long, G> groupedRelated = infoFetch.fetchRelated(ids);
        
        return result.stream()
            .map(r -> new LimitEntity<>(limitDef,
                r.getMainRecord(),
                r.getDimensionRecords(),
                infoFetch.from(r.getRecord(), groupedRelated.get(r.getMainRecord().getId()))))
            .collect(Collectors.toList());
    }
    
    private final LibDimLimitRecord limit;
    private final Context<L> context; // no need for record maps as context is immutable (only stored once)
    private boolean contextStored;
    private final I info;
    
    private LimitEntity(final LibDimLimitRecord limit, final Context<L> context, final I info) {
        this.limit = limit;
        this.context = context;
        contextStored = false;
        this.info = info;
    }
    
    public LimitEntity(final Context<L> context, final Function<Long, I> infoCreate) {
        limit = new LibDimLimitRecord();
        limit.setId(UniqueIdGenerator.generate());
        limit.setLimitKey(context.getDef().getKey());
        limit.setContextDepth(context.getDepth());
        this.context = context;
        contextStored = false;
        info = infoCreate.apply(limit.getId());
    }
    
    private LimitEntity(final L def,
                        final LibDimLimitRecord limit,
                        final Collection<LibDimLimitDimensionRecord> dimensions,
                        final I info) {
        this(limit, Context.newBuilder(def).from(dimensions.stream()
            .filter(d -> d.getId() != null)
            .collect(Collectors.toMap(LibDimLimitDimensionRecord::getDimensionName, d -> new PersistedDimensionValue(d.getLongValue(), d.getStringValue())))).build(), info);
        
        contextStored = true;
    }
    
    public Long getId() {
        return limit.getId();
    }
    
    public LibDimLimitRecord getLimit() {
        return limit;
    }
    
    public Context<L> getContext() {
        return context;
    }
    
    public I getInfo() {
        return info;
    }
    
    public WindowWidth getNarrowWindowWidth() {
        return new WindowWidth(limit.getNarrowWindowWidth(), WINDOW_TIME_UNIT_MAPPING.resolve(limit.getNarrowWindowUnit()));
    }
    
    public LimitEntity<I, L> setNarrowWindowWidth(final WindowWidth windowWidth) {
        limit.setNarrowWindowWidth(windowWidth.getWidth());
        limit.setNarrowWindowUnit(WINDOW_TIME_UNIT_MAPPING.codify(windowWidth.getUnit()));
        return this;
    }
    
    public int getWideWindowMultiple() {
        return limit.getWideWindowMultiple();
    }
    
    public LimitEntity<I, L> apply(final Consumer<LibDimLimitRecord> consumer) {
        consumer.accept(limit);
        return this;
    }
    
    public ValueLimit toValueProtobuf() {
        return ValueLimit.newBuilder().setContext(context.toProtobuf()).setValue(valueToProtobuf()).build();
    }
    
    public CounterLimit toCounterProtobuf() {
        return CounterLimit.newBuilder()
            .setContext(context.toProtobuf())
            .setValue(CounterLimitValue.newBuilder()
                .setValue(valueToProtobuf())
                .setNarrowWindowWidth(limit.getNarrowWindowWidth())
                .setNarrowWindowUnit(WindowTimeUnit.valueOf(WINDOW_TIME_UNIT_MAPPING.resolve(limit.getNarrowWindowUnit()).name()))
                .setWideWindowMultiple(limit.getWideWindowMultiple())
                .build())
            .build();
    }
    
    private LimitValue valueToProtobuf() {
        return LimitValue.newBuilder()
            .setEffectiveFrom(limit.getEffectiveFrom())
            .setEffectiveTo(limit.getEffectiveTo())
            .setMaxCount(ProtobufConverters.convert(limit.getMaxCount()))
            .setMinAmount(ProtobufConverters.convert(limit.getMinAmount()))
            .setMaxAmount(ProtobufConverters.convert(limit.getMaxAmount()))
            .build();
    }
    
    @Override
    public LimitEntity<I, L> store() {
        attachAndStore(limit);
        if (!contextStored) {
            attachAndStore(context.values()
                .stream()
                .map(contextDimension -> {
                    final PersistedDimensionValue persistedValue = contextDimension.getPersistedValue();
                    return new LibDimLimitDimensionRecord(
                        limit.getId(),
                        contextDimension.getDefinition().getKey(),
                        persistedValue.getLongValue(),
                        persistedValue.getStringValue());
                })
                .collect(Collectors.toList()));
            contextStored = true;
        }
        info.store();
        return this;
    }
    
    @Override
    public LimitEntity<I, L> delete() {
        info.delete();
        JOOQ_TX.get().delete(LIB_DIM_LIMIT_DIMENSION).where(LIB_DIM_LIMIT_DIMENSION.ID.eq(limit.getId())).execute();
        attachAndDelete(limit);
        return this;
    }
    
    @Override
    public boolean equals(final Object o) {
        return EqualsUtil.equals(this, o, other -> getId().equals(other.getId()));
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(getId());
    }
}
