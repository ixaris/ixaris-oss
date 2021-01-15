package com.ixaris.commons.dimensions.limits.admin;

import java.util.function.Function;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.dimensions.counters.WindowTimeUnit;
import com.ixaris.commons.dimensions.counters.WindowWidth;
import com.ixaris.commons.dimensions.lib.CommonsDimensionsLib;
import com.ixaris.commons.dimensions.lib.context.Context;
import com.ixaris.commons.dimensions.limits.CommonsDimensionsLimits.CounterLimit;
import com.ixaris.commons.dimensions.limits.CommonsDimensionsLimits.CounterLimitDefs;
import com.ixaris.commons.dimensions.limits.CommonsDimensionsLimits.CounterLimitValue;
import com.ixaris.commons.dimensions.limits.CommonsDimensionsLimits.CounterLimits;
import com.ixaris.commons.dimensions.limits.CommonsDimensionsLimits.LimitValue;
import com.ixaris.commons.dimensions.limits.CommonsDimensionsLimits.UnsetLimit;
import com.ixaris.commons.dimensions.limits.CommonsDimensionsLimits.ValueLimit;
import com.ixaris.commons.dimensions.limits.CommonsDimensionsLimits.ValueLimitDefs;
import com.ixaris.commons.dimensions.limits.CommonsDimensionsLimits.ValueLimits;
import com.ixaris.commons.dimensions.limits.CounterLimitDef;
import com.ixaris.commons.dimensions.limits.CounterLimitDefRegistry;
import com.ixaris.commons.dimensions.limits.LimitDef;
import com.ixaris.commons.dimensions.limits.LimitsHelper;
import com.ixaris.commons.dimensions.limits.ValueLimitDef;
import com.ixaris.commons.dimensions.limits.ValueLimitDefRegistry;
import com.ixaris.commons.dimensions.limits.data.AbstractLimitExtensionEntity;
import com.ixaris.commons.dimensions.limits.data.LimitEntity;
import com.ixaris.commons.protobuf.lib.ProtobufConverters;

@Component
public final class LimitsAdminProto {
    
    private final LimitsHelper limits;
    private final LimitsAdminHelper limitsAdmin;
    
    @Autowired
    public LimitsAdminProto(final LimitsHelper limits, final LimitsAdminHelper limitsAdmin) {
        this.limits = limits;
        this.limitsAdmin = limitsAdmin;
    }
    
    public ValueLimitDefs getValueLimitDefs() {
        return ValueLimitDefRegistry.getInstance().toProtobuf();
    }
    
    @SuppressWarnings("unchecked")
    public <T, I extends AbstractLimitExtensionEntity<T, I>> Async<ValueLimit> lookupValueLimit(final String key, final long id) {
        final ValueLimitDef<I> def = (ValueLimitDef<I>) ValueLimitDefRegistry.getInstance().resolve(key);
        return limitsAdmin.lookup(def, id).map(LimitEntity::toValueProtobuf);
    }
    
    @SuppressWarnings("unchecked")
    public <T, I extends AbstractLimitExtensionEntity<T, I>, L extends LimitDef<I>> Async<ValueLimits> getAllValueLimitsMatchingContext(final String key,
                                                                                                                                        final CommonsDimensionsLib.Context context) {
        return limitsAdmin
            .getAllLimitsMatchingContext((Context<L>) getValueLimitContext(key, context))
            .map(LimitEntity::collectionToValueProtobuf);
    }
    
    @SuppressWarnings("unchecked")
    public <T, I extends AbstractLimitExtensionEntity<T, I>, L extends LimitDef<I>> Async<ValueLimits> getAllValueLimitsContainingContext(final String key,
                                                                                                                                          final CommonsDimensionsLib.Context context) {
        return limitsAdmin
            .getAllLimitsContainingContext((Context<L>) getValueLimitContext(key, context))
            .map(LimitEntity::collectionToValueProtobuf);
    }
    
    @SuppressWarnings("unchecked")
    public <T, I extends AbstractLimitExtensionEntity<T, I>, L extends LimitDef<I>> Async<ValueLimits> simulateValueLimit(final String key,
                                                                                                                          final CommonsDimensionsLib.Context context) {
        return limits.getAllMatchingLimits((Context<L>) getValueLimitContext(key, context)).map(LimitEntity::collectionToValueProtobuf);
    }
    
    @SuppressWarnings("unchecked")
    public <T, I extends AbstractLimitExtensionEntity<T, I>> Async<Void> setValueLimit(final String key,
                                                                                       final CommonsDimensionsLib.Context context,
                                                                                       final LimitValue value,
                                                                                       final Function<Long, I> infoCreate) {
        final ValueLimitDef<I> def = (ValueLimitDef<I>) ValueLimitDefRegistry.getInstance().resolve(key);
        return limitsAdmin.setLimit(
            def.getContextDef().contextFromProtobuf(def, context),
            value.getEffectiveFrom() == 0 ? null : value.getEffectiveFrom(),
            value.getEffectiveTo() == 0 ? null : value.getEffectiveTo(),
            ProtobufConverters.toLong(value.getMaxCount()),
            ProtobufConverters.toLong(value.getMinAmount()),
            ProtobufConverters.toLong(value.getMaxAmount()),
            infoCreate);
    }
    
    @SuppressWarnings("unchecked")
    public <T, I extends AbstractLimitExtensionEntity<T, I>> Async<Void> unsetValueLimit(final String key, final UnsetLimit unsetLimit) {
        final ValueLimitDef<I> def = (ValueLimitDef<I>) ValueLimitDefRegistry.getInstance().resolve(key);
        return limitsAdmin.unsetLimit(def.getContextDef().contextFromProtobuf(def, unsetLimit.getContext()),
            unsetLimit.getEffectiveFrom() == 0 ? null : unsetLimit.getEffectiveFrom(),
            unsetLimit.getEffectiveTo() == 0 ? null : unsetLimit.getEffectiveTo());
    }
    
    public CounterLimitDefs getCounterLimitDefs() {
        return CounterLimitDefRegistry.getInstance().toProtobuf();
    }
    
    @SuppressWarnings("unchecked")
    public <T, I extends AbstractLimitExtensionEntity<T, I>> Async<CounterLimit> lookupCounterLimit(final String key, final long id) {
        final CounterLimitDef<I, ?> def = (CounterLimitDef<I, ?>) CounterLimitDefRegistry.getInstance().resolve(key);
        return limitsAdmin.lookup(def, id).map(LimitEntity::toCounterProtobuf);
    }
    
    @SuppressWarnings("unchecked")
    public <T, I extends AbstractLimitExtensionEntity<T, I>, L extends LimitDef<I>> Async<CounterLimits> getAllCounterLimitsMatchingContext(final String key,
                                                                                                                                            final CommonsDimensionsLib.Context context) {
        return limitsAdmin
            .getAllLimitsMatchingContext((Context<L>) getCounterLimitContext(key, context))
            .map(LimitEntity::collectionToCounterProtobuf);
    }
    
    @SuppressWarnings("unchecked")
    public <T, I extends AbstractLimitExtensionEntity<T, I>, L extends LimitDef<I>> Async<CounterLimits> getAllCounterLimitsContainingContext(final String key,
                                                                                                                                              final CommonsDimensionsLib.Context context) {
        return limitsAdmin
            .getAllLimitsContainingContext((Context<L>) getCounterLimitContext(key, context))
            .map(LimitEntity::collectionToCounterProtobuf);
    }
    
    @SuppressWarnings("unchecked")
    public <T, I extends AbstractLimitExtensionEntity<T, I>, L extends LimitDef<I>> Async<CounterLimits> simulateCounterLimit(final String key,
                                                                                                                              final CommonsDimensionsLib.Context context) {
        return limits.getAllMatchingLimits((Context<L>) getCounterLimitContext(key, context)).map(LimitEntity::collectionToCounterProtobuf);
    }
    
    @SuppressWarnings("unchecked")
    public <T, I extends AbstractLimitExtensionEntity<T, I>> Async<Void> setCounterLimit(final String key,
                                                                                         final CommonsDimensionsLib.Context context,
                                                                                         final CounterLimitValue counterLimitValue,
                                                                                         final Function<Long, I> infoCreate) {
        final CounterLimitDef<I, ?> def = (CounterLimitDef<I, ?>) CounterLimitDefRegistry.getInstance().resolve(key);
        final LimitValue value = counterLimitValue.getValue();
        return limitsAdmin.setLimit(
            def.getContextDef().contextFromProtobuf(def, context),
            value.getEffectiveFrom() == 0 ? null : value.getEffectiveFrom(),
            value.getEffectiveTo() == 0 ? null : value.getEffectiveTo(),
            ProtobufConverters.toLong(value.getMaxCount()),
            ProtobufConverters.toLong(value.getMinAmount()),
            ProtobufConverters.toLong(value.getMaxAmount()),
            new WindowWidth(
                counterLimitValue.getNarrowWindowWidth(),
                WindowTimeUnit.valueOf(counterLimitValue.getNarrowWindowUnit().name())),
            counterLimitValue.getWideWindowMultiple(),
            infoCreate);
    }
    
    @SuppressWarnings("unchecked")
    public <T, I extends AbstractLimitExtensionEntity<T, I>> Async<Void> unsetCounterLimit(final String key, final UnsetLimit unsetLimit) {
        final CounterLimitDef<I, ?> def = (CounterLimitDef<I, ?>) CounterLimitDefRegistry.getInstance().resolve(key);
        return limitsAdmin.unsetLimit(def.getContextDef().contextFromProtobuf(def, unsetLimit.getContext()),
            unsetLimit.getEffectiveFrom() == 0 ? null : unsetLimit.getEffectiveFrom(),
            unsetLimit.getEffectiveTo() == 0 ? null : unsetLimit.getEffectiveTo());
    }
    
    private Context<? extends LimitDef<?>> getValueLimitContext(final String key, final CommonsDimensionsLib.Context context) {
        final ValueLimitDef<?> def = ValueLimitDefRegistry.getInstance().resolve(key);
        return def.getContextDef().contextFromProtobuf(def, context);
    }
    
    private Context<? extends LimitDef<?>> getCounterLimitContext(final String key, final CommonsDimensionsLib.Context context) {
        final CounterLimitDef<?, ?> def = CounterLimitDefRegistry.getInstance().resolve(key);
        return def.getContextDef().contextFromProtobuf(def, context);
    }
    
}
