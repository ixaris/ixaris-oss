/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.data;

import static com.ixaris.commons.dimensions.config.jooq.tables.LibDimConfigSet.LIB_DIM_CONFIG_SET;
import static com.ixaris.commons.dimensions.config.jooq.tables.LibDimConfigSetDimension.LIB_DIM_CONFIG_SET_DIMENSION;
import static com.ixaris.commons.dimensions.config.jooq.tables.LibDimConfigSetValue.LIB_DIM_CONFIG_SET_VALUE;
import static com.ixaris.commons.jooq.persistence.JooqHelper.eqNullable;
import static com.ixaris.commons.jooq.persistence.TransactionalDSLContext.JOOQ_TX;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;
import java.util.function.Supplier;
import java.util.stream.Collectors;

import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SelectJoinStep;
import org.jooq.UpdatableRecord;

import com.ixaris.commons.dimensions.config.CommonsDimensionsConfig.ConfigSet;
import com.ixaris.commons.dimensions.config.CommonsDimensionsConfig.ConfigSets;
import com.ixaris.commons.dimensions.config.SetDef;
import com.ixaris.commons.dimensions.config.SetUpdates;
import com.ixaris.commons.dimensions.config.jooq.tables.records.LibDimConfigSetDimensionRecord;
import com.ixaris.commons.dimensions.config.jooq.tables.records.LibDimConfigSetRecord;
import com.ixaris.commons.dimensions.config.jooq.tables.records.LibDimConfigSetValueRecord;
import com.ixaris.commons.dimensions.config.value.PersistedValue;
import com.ixaris.commons.dimensions.config.value.Value;
import com.ixaris.commons.dimensions.config.value.validation.CascadeSetValidation;
import com.ixaris.commons.dimensions.lib.base.DimensionalHelper;
import com.ixaris.commons.dimensions.lib.base.DimensionalHelper.RecordHolder;
import com.ixaris.commons.dimensions.lib.context.Context;
import com.ixaris.commons.dimensions.lib.context.PersistedDimensionValue;
import com.ixaris.commons.jooq.persistence.Entity;
import com.ixaris.commons.jooq.persistence.RecordMap;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;

/**
 * A context property value. One to many relation with context property dimension
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
public final class ConfigSetEntity<T extends Value, S extends SetDef<T>> extends Entity<ConfigSetEntity<T, S>> {
    
    public static <T extends Value, S extends SetDef<T>> Optional<Set<T>> lookupBestMatch(final Context<S> context) {
        final List<ConfigSetEntity<T, S>> result = match(context, !context.getDef().isIncremental(), true);
        
        if (result.isEmpty()) {
            return Optional.empty();
        } else {
            if (context.getDef().isIncremental()) {
                final Set<T> incrementalSet = Comparable.class.isAssignableFrom(context.getDef().getType()) ? new TreeSet<>() : new HashSet<>();
                for (final ConfigSetEntity<T, S> cs : result) {
                    incrementalSet.addAll(cs.getSet());
                }
                return Optional.of(incrementalSet);
            } else {
                return Optional.of(result.get(0).getSet());
            }
        }
    }
    
    public static <T extends Value, S extends SetDef<T>> boolean isValueInBestMatchSet(final Context<S> context, final T value) {
        final PersistedValue persistedValue = value.getPersistedValue();
        final SelectJoinStep<Record> select = JOOQ_TX.get()
            .select()
            .from(LIB_DIM_CONFIG_SET)
            .leftJoin(LIB_DIM_CONFIG_SET_VALUE)
            .on(LIB_DIM_CONFIG_SET.ID.eq(LIB_DIM_CONFIG_SET_VALUE.ID),
                eqNullable(LIB_DIM_CONFIG_SET_VALUE.LONG_VALUE, persistedValue.getLongValue()),
                eqNullable(LIB_DIM_CONFIG_SET_VALUE.STRING_VALUE, persistedValue.getStringValue()));
        
        final List<RecordHolder<LibDimConfigSetRecord, LibDimConfigSetDimensionRecord>> result = DimensionalHelper.match(context,
            !context.getDef().isIncremental(),
            true,
            LIB_DIM_CONFIG_SET,
            LIB_DIM_CONFIG_SET_DIMENSION,
            select,
            LIB_DIM_CONFIG_SET.SET_KEY.eq(context.getDef().getKey()));
        
        if (result.isEmpty()) {
            return false;
        }
        
        final LibDimConfigSetValueRecord valueRecord = result.get(0).getRecord().into(LIB_DIM_CONFIG_SET_VALUE);
        return valueRecord.getId() != null;
    }
    
    public static <T extends Value, S extends SetDef<T>> Optional<ConfigSetEntity<T, S>> lookupExactMatch(final Context<S> context) {
        final List<ConfigSetEntity<T, S>> result = lookup(context, true, true);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }
    
    public static <T extends Value, S extends SetDef<T>> Optional<ConfigSetEntity<T, S>> lookupNextMatchingContext(final Context<S> context) {
        final List<ConfigSetEntity<T, S>> result = match(context, true, false);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }
    
    public static <T extends Value, S extends SetDef<T>> List<ConfigSetEntity<T, S>> lookupAllMatchingContext(final Context<S> context) {
        return match(context, false, true);
    }
    
    public static <T extends Value, S extends SetDef<T>> List<ConfigSetEntity<T, S>> lookupAllContainingContext(final Context<S> context) {
        return lookup(context, false, true);
    }
    
    /**
     * Apply cascade update by finding all values containing context and applying the validation to check whether the value changed
     *
     * @return the set of processed contexts, including the non-modified ones. This is used to determine which of the default values need to be
     *     overridden
     */
    public static <T extends Value, S extends SetDef<T>> void cascadeUpdate(final Context<S> context,
                                                                            final CascadeSetValidation<? super T> validation,
                                                                            final Set<T> rootSet,
                                                                            final SetUpdates<T> rootUpdates) {
        final List<ConfigSetEntity<T, S>> result = lookup(context, false, false);
        for (final ConfigSetEntity<T, S> cs : result) {
            final SetUpdates<T> updates = validation.cascadeUpdate(cs.getSet(), rootSet, rootUpdates);
            if (updates != null) {
                cs.updateSet(updates);
                cs.store();
            }
        }
    }
    
    public static <T extends Value, S extends SetDef<T>> ConfigSets collectionToProtobuf(final Collection<ConfigSetEntity<T, S>> results) {
        final ConfigSets.Builder builder = ConfigSets.newBuilder();
        for (final ConfigSetEntity<T, S> result : results) {
            builder.addSets(result.toProtobuf());
        }
        return builder.build();
    }
    
    /**
     * Will get the set that best matches the given gontext.
     *
     * @param context the context instance
     * @param mostSpecific
     * @return the sets that best matches the context instance, or an empty list if no match found
     */
    private static <T extends Value, S extends SetDef<T>> List<ConfigSetEntity<T, S>> match(final Context<S> context,
                                                                                            final boolean mostSpecific,
                                                                                            final boolean allowExactContextMatch) {
        final List<RecordHolder<LibDimConfigSetRecord, LibDimConfigSetDimensionRecord>> result = DimensionalHelper.match(context,
            mostSpecific,
            allowExactContextMatch,
            LIB_DIM_CONFIG_SET,
            LIB_DIM_CONFIG_SET_DIMENSION,
            JOOQ_TX.get().select().from(LIB_DIM_CONFIG_SET),
            LIB_DIM_CONFIG_SET.SET_KEY.eq(context.getDef().getKey()));
        
        return finishFetch(context.getDef(), result);
    }
    
    /**
     * Will get the value that matches the given context exactly.
     *
     * @param context the context instance
     * @return the sets (exactly) matching the context instance, or an empty list if no match found
     */
    private static <T extends Value, S extends SetDef<T>> List<ConfigSetEntity<T, S>> lookup(final Context<S> context,
                                                                                             final boolean exact,
                                                                                             final boolean allowExactContextMatch) {
        final List<RecordHolder<LibDimConfigSetRecord, LibDimConfigSetDimensionRecord>> result = DimensionalHelper.lookup(context,
            exact,
            allowExactContextMatch,
            LIB_DIM_CONFIG_SET,
            LIB_DIM_CONFIG_SET_DIMENSION,
            JOOQ_TX.get().select().from(LIB_DIM_CONFIG_SET),
            LIB_DIM_CONFIG_SET.SET_KEY.eq(context.getDef().getKey()));
        
        return finishFetch(context.getDef(), result);
    }
    
    private static <T extends Value, S extends SetDef<T>> List<ConfigSetEntity<T, S>> finishFetch(final S setDef,
                                                                                                  final List<RecordHolder<LibDimConfigSetRecord, LibDimConfigSetDimensionRecord>> result) {
        final List<Long> ids = result.stream().map(r -> r.getMainRecord().getId()).collect(Collectors.toList());
        final Map<Long, Result<LibDimConfigSetValueRecord>> groupedSets = JOOQ_TX.get()
            .selectFrom(LIB_DIM_CONFIG_SET_VALUE)
            .where(LIB_DIM_CONFIG_SET_VALUE.ID.in(ids))
            .fetch()
            .intoGroups(LIB_DIM_CONFIG_SET_VALUE.ID);
        
        return result.stream()
            .map(r -> new ConfigSetEntity<>(setDef, r.getMainRecord(), r.getDimensionRecords(), groupedSets.get(r.getMainRecord().getId())))
            .collect(Collectors.toList());
    }
    
    private static <T, I, R extends UpdatableRecord<R>> Supplier<? extends Map<I, R>> determineMapSupplier(final Class<T> type) {
        return Comparable.class.isAssignableFrom(type) ? TreeMap::new : HashMap::new;
    }
    
    private final Context<S> context;
    private final LibDimConfigSetRecord set;
    private final RecordMap<T, LibDimConfigSetValueRecord> values;
    private boolean contextStored;
    
    public ConfigSetEntity(final Context<S> context, final Set<T> set) {
        
        this(JOOQ_TX.get().newRecord(LIB_DIM_CONFIG_SET), context);
        this.set.setId(UniqueIdGenerator.generate());
        this.set.setSetKey(context.getDef().getKey());
        this.set.setContextDepth(context.getDepth());
        setSet(set);
    }
    
    private ConfigSetEntity(final S def,
                            final LibDimConfigSetRecord set,
                            final Collection<LibDimConfigSetDimensionRecord> dimensions,
                            final Collection<LibDimConfigSetValueRecord> values) {
        this(set,
            Context.newBuilder(def)
                .from(dimensions.stream()
                    .filter(d -> d.getId() != null)
                    .collect(Collectors.toMap(LibDimConfigSetDimensionRecord::getDimensionName, d -> new PersistedDimensionValue(d.getLongValue(), d.getStringValue()))))
                .build());
        
        final Value.Builder<T> builder = def.getValueBuilder();
        this.values.fromExisting(values, v -> builder.buildFromPersisted(new PersistedValue(v.getLongValue(), v.getStringValue())));
        contextStored = true;
    }
    
    private ConfigSetEntity(final LibDimConfigSetRecord set, final Context<S> context) {
        this.set = set;
        this.context = context;
        final Supplier<? extends Map<T, LibDimConfigSetValueRecord>> mapSupplier = determineMapSupplier(context.getDef().getType());
        this.values = RecordMap.withMapSupplierAndNewRecordFunction(mapSupplier, key -> {
            final PersistedValue persistedValue = key.getPersistedValue();
            final LibDimConfigSetValueRecord record = JOOQ_TX.get().newRecord(LIB_DIM_CONFIG_SET_VALUE);
            record.setId(this.set.getId());
            record.setLongValue(persistedValue.getLongValue());
            record.setStringValue(persistedValue.getStringValue());
            return record;
        });
        contextStored = false;
    }
    
    public Context<S> getContext() {
        return context;
    }
    
    public Set<T> getSet() {
        return Collections.unmodifiableSet(values.getMap().keySet());
    }
    
    public SetUpdates<T> setSet(final Set<T> set) {
        if (set == null) {
            throw new IllegalArgumentException("set is null");
        }
        values.syncWith(set);
        
        return new SetUpdates<>(new HashSet<>(values.getAdded().keySet()), new HashSet<>(values.getRemoved().keySet()));
    }
    
    public SetUpdates<T> updateSet(final SetUpdates<T> updates) {
        values.update(updates.getAdded());
        values.remove(updates.getRemoved());
        
        return new SetUpdates<>(Collections.unmodifiableSet(values.getAdded().keySet()),
            Collections.unmodifiableSet(values.getRemoved().keySet()));
    }
    
    public ConfigSet toProtobuf() {
        return ConfigSet.newBuilder().setContext(context.toProtobuf()).setSet(Value.setToProtobuf(values.getMap().keySet())).build();
    }
    
    @Override
    public ConfigSetEntity<T, S> store() {
        attachAndStore(set);
        if (!contextStored) {
            attachAndStore(context.values()
                .stream()
                .map(contextDimension -> {
                    final PersistedDimensionValue persistedValue = contextDimension.getPersistedValue();
                    return new LibDimConfigSetDimensionRecord(
                        set.getId(),
                        contextDimension.getDefinition().getKey(),
                        persistedValue.getLongValue(),
                        persistedValue.getStringValue());
                })
                .collect(Collectors.toList()));
            contextStored = true;
        }
        values.store();
        return this;
    }
    
    @Override
    public ConfigSetEntity<T, S> delete() {
        JOOQ_TX.get().delete(LIB_DIM_CONFIG_SET_VALUE).where(LIB_DIM_CONFIG_SET_VALUE.ID.eq(set.getId())).execute();
        JOOQ_TX.get().delete(LIB_DIM_CONFIG_SET_DIMENSION).where(LIB_DIM_CONFIG_SET_DIMENSION.ID.eq(set.getId())).execute();
        attachAndDelete(set);
        return this;
    }
    
}
