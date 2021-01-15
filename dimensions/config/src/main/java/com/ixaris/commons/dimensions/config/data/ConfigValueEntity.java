/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config.data;

import static com.ixaris.commons.dimensions.config.jooq.tables.LibDimConfigValue.LIB_DIM_CONFIG_VALUE;
import static com.ixaris.commons.dimensions.config.jooq.tables.LibDimConfigValueDimension.LIB_DIM_CONFIG_VALUE_DIMENSION;
import static com.ixaris.commons.jooq.persistence.JooqHelper.eqNullable;
import static com.ixaris.commons.jooq.persistence.TransactionalDSLContext.JOOQ_TX;

import java.util.Collection;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import com.ixaris.commons.dimensions.config.CommonsDimensionsConfig.ConfigValue;
import com.ixaris.commons.dimensions.config.CommonsDimensionsConfig.ConfigValues;
import com.ixaris.commons.dimensions.config.ValueDef;
import com.ixaris.commons.dimensions.config.admin.UndefinedPropertyException;
import com.ixaris.commons.dimensions.config.jooq.tables.records.LibDimConfigValueDimensionRecord;
import com.ixaris.commons.dimensions.config.jooq.tables.records.LibDimConfigValueRecord;
import com.ixaris.commons.dimensions.config.value.PersistedValue;
import com.ixaris.commons.dimensions.config.value.Value;
import com.ixaris.commons.dimensions.config.value.validation.CascadeValueValidation;
import com.ixaris.commons.dimensions.lib.base.DimensionalHelper;
import com.ixaris.commons.dimensions.lib.base.DimensionalHelper.RecordHolder;
import com.ixaris.commons.dimensions.lib.context.Context;
import com.ixaris.commons.dimensions.lib.context.PersistedDimensionValue;
import com.ixaris.commons.jooq.persistence.Entity;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;

/**
 * A context property value. One to many relation with context property dimension
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
public final class ConfigValueEntity<T extends Value, V extends ValueDef<T>> extends Entity<ConfigValueEntity<T, V>> {
    
    public static <T extends Value, V extends ValueDef<T>> Optional<T> lookupBestMatch(final Context<V> context) {
        final List<ConfigValueEntity<T, V>> result = match(context, true, true);
        if (result.isEmpty()) {
            if (!context.getDef().isNullExpected()) {
                throw new UndefinedPropertyException("Missing configuration for [" + context.getDef() + "] and context [" + context + "]");
            }
            return Optional.empty();
        } else {
            return Optional.of(result.get(0).getValue());
        }
    }
    
    public static <T extends Value, V extends ValueDef<T>> Optional<ConfigValueEntity<T, V>> lookupExactMatch(final Context<V> context) {
        final List<ConfigValueEntity<T, V>> result = lookup(context, true, true);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }
    
    public static <T extends Value, V extends ValueDef<T>> List<ConfigValueEntity<T, V>> lookupByValue(final V def, final T value) {
        final PersistedValue persistedValue = value.getPersistedValue();
        final List<RecordHolder<LibDimConfigValueRecord, LibDimConfigValueDimensionRecord>> result = DimensionalHelper.lookup(Context.empty(def),
            false,
            true,
            LIB_DIM_CONFIG_VALUE,
            LIB_DIM_CONFIG_VALUE_DIMENSION,
            JOOQ_TX.get().select().from(LIB_DIM_CONFIG_VALUE),
            LIB_DIM_CONFIG_VALUE.VALUE_KEY
                .eq(def.getKey())
                .and(eqNullable(LIB_DIM_CONFIG_VALUE.LONG_VALUE, persistedValue.getLongValue()))
                .and(eqNullable(LIB_DIM_CONFIG_VALUE.STRING_VALUE, persistedValue.getStringValue())));
        
        return finishFetch(def, result);
    }
    
    public static <T extends Value, V extends ValueDef<T>> Optional<ConfigValueEntity<T, V>> lookupNextMatchingContext(final Context<V> context) {
        final List<ConfigValueEntity<T, V>> result = match(context, true, false);
        return result.isEmpty() ? Optional.empty() : Optional.of(result.get(0));
    }
    
    public static <T extends Value, V extends ValueDef<T>> List<ConfigValueEntity<T, V>> lookupAllMatchingContext(final Context<V> context) {
        return match(context, false, true);
    }
    
    public static <T extends Value, V extends ValueDef<T>> List<ConfigValueEntity<T, V>> lookupAllContainingContext(final Context<V> context) {
        return lookup(context, false, true);
    }
    
    /**
     * Apply cascade update by finding all values containing context and applying the validation to check whether the value changed
     */
    public static <T extends Value, V extends ValueDef<T>> void cascadeUpdate(final Context<V> context,
                                                                              final CascadeValueValidation<? super T> validation,
                                                                              final T rootValue) {
        final List<ConfigValueEntity<T, V>> result = lookup(context, false, false);
        for (final ConfigValueEntity<T, V> cv : result) {
            final T updatedValue = validation.cascadeUpdate(cv.value, rootValue);
            if (updatedValue != null) {
                cv.setValue(updatedValue);
                cv.store();
            }
        }
    }
    
    public static <T extends Value, V extends ValueDef<T>> ConfigValues collectionToProtobuf(final Collection<ConfigValueEntity<T, V>> results) {
        final ConfigValues.Builder builder = ConfigValues.newBuilder();
        for (final ConfigValueEntity<T, V> result : results) {
            builder.addValues(result.toProtobuf());
        }
        return builder.build();
    }
    
    /**
     * Will get the value that best matches the given gontext.
     *
     * @param context the context instance
     * @return the values that best matches the context instance, or an empty list if no match found
     */
    private static <T extends Value, V extends ValueDef<T>> List<ConfigValueEntity<T, V>> match(final Context<V> context,
                                                                                                final boolean mostSpecific,
                                                                                                final boolean allowExactContextMatch) {
        
        final List<RecordHolder<LibDimConfigValueRecord, LibDimConfigValueDimensionRecord>> result = DimensionalHelper.match(context,
            mostSpecific,
            allowExactContextMatch,
            LIB_DIM_CONFIG_VALUE,
            LIB_DIM_CONFIG_VALUE_DIMENSION,
            JOOQ_TX.get().select().from(LIB_DIM_CONFIG_VALUE),
            LIB_DIM_CONFIG_VALUE.VALUE_KEY.eq(context.getDef().getKey()));
        
        return finishFetch(context.getDef(), result);
    }
    
    /**
     * Will get the value that matches the given context exactly (or having a context that contains the whole context).
     *
     * @param context the context instance
     * @param exact true if the match should be exact, false if a context containing the whole context is allowed
     * @return the values (exactly) matching the context instance, or an empty list if no match found
     */
    private static <T extends Value, V extends ValueDef<T>> List<ConfigValueEntity<T, V>> lookup(final Context<V> context,
                                                                                                 final boolean exact,
                                                                                                 final boolean allowExactContextMatch) {
        final List<RecordHolder<LibDimConfigValueRecord, LibDimConfigValueDimensionRecord>> result = DimensionalHelper.lookup(context,
            exact,
            allowExactContextMatch,
            LIB_DIM_CONFIG_VALUE,
            LIB_DIM_CONFIG_VALUE_DIMENSION,
            JOOQ_TX.get().select().from(LIB_DIM_CONFIG_VALUE),
            LIB_DIM_CONFIG_VALUE.VALUE_KEY.eq(context.getDef().getKey()));
        
        return finishFetch(context.getDef(), result);
    }
    
    private static <T extends Value, V extends ValueDef<T>> List<ConfigValueEntity<T, V>> finishFetch(final V valueDef,
                                                                                                      final List<RecordHolder<LibDimConfigValueRecord, LibDimConfigValueDimensionRecord>> result) {
        return result.stream()
            .map(r -> new ConfigValueEntity<>(valueDef, r.getMainRecord(), r.getDimensionRecords()))
            .collect(Collectors.toList());
    }
    
    private final Context<V> context;
    private final LibDimConfigValueRecord valueRecord;
    private T value;
    private boolean contextStored;
    
    public ConfigValueEntity(final Context<V> context, final T value) {
        this(JOOQ_TX.get().newRecord(LIB_DIM_CONFIG_VALUE), context);
        valueRecord.setId(UniqueIdGenerator.generate());
        valueRecord.setValueKey(context.getDef().getKey());
        valueRecord.setContextDepth(context.getDepth());
        setValue(value);
    }
    
    public ConfigValueEntity(final V def, final LibDimConfigValueRecord value, final Collection<LibDimConfigValueDimensionRecord> dimensions) {
        this(value,
            Context.newBuilder(def)
                .from(dimensions.stream()
                    .filter(d -> d.getId() != null)
                    .collect(Collectors.toMap(LibDimConfigValueDimensionRecord::getDimensionName, d -> new PersistedDimensionValue(d.getLongValue(), d.getStringValue()))))
                .build());
        this.value = def.getValueBuilder().buildFromPersisted(new PersistedValue(value.getLongValue(), value.getStringValue()));
        contextStored = true;
    }
    
    private ConfigValueEntity(final LibDimConfigValueRecord value, final Context<V> context) {
        this.valueRecord = value;
        this.context = context;
        contextStored = false;
    }
    
    public Context<V> getContext() {
        return context;
    }
    
    public T getValue() {
        return value;
    }
    
    public void setValue(final T value) {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        this.value = value;
        final PersistedValue persistedValue = value.getPersistedValue();
        this.valueRecord.setLongValue(persistedValue.getLongValue());
        this.valueRecord.setStringValue(persistedValue.getStringValue());
    }
    
    public ConfigValue toProtobuf() {
        return ConfigValue.newBuilder().setContext(context.toProtobuf()).setValue(value.toProtobuf()).build();
    }
    
    @Override
    public ConfigValueEntity<T, V> store() {
        attachAndStore(valueRecord);
        if (!contextStored) {
            attachAndStore(context.values()
                .stream()
                .map(contextDimension -> {
                    final PersistedDimensionValue persistedValue = contextDimension.getPersistedValue();
                    return new LibDimConfigValueDimensionRecord(
                        valueRecord.getId(),
                        contextDimension.getDefinition().getKey(),
                        persistedValue.getLongValue(),
                        persistedValue.getStringValue());
                })
                .collect(Collectors.toList()));
            contextStored = true;
        }
        return this;
    }
    
    @Override
    public ConfigValueEntity<T, V> delete() {
        JOOQ_TX.get().delete(LIB_DIM_CONFIG_VALUE_DIMENSION).where(LIB_DIM_CONFIG_VALUE_DIMENSION.ID.eq(valueRecord.getId())).execute();
        attachAndDelete(valueRecord);
        return this;
    }
    
}
