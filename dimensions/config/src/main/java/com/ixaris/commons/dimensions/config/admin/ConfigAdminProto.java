package com.ixaris.commons.dimensions.config.admin;

import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.dimensions.config.CommonsDimensionsConfig;
import com.ixaris.commons.dimensions.config.CommonsDimensionsConfig.ConfigSetDef;
import com.ixaris.commons.dimensions.config.CommonsDimensionsConfig.ConfigSetDefs;
import com.ixaris.commons.dimensions.config.CommonsDimensionsConfig.ConfigSets;
import com.ixaris.commons.dimensions.config.CommonsDimensionsConfig.ConfigValueDef;
import com.ixaris.commons.dimensions.config.CommonsDimensionsConfig.ConfigValueDefs;
import com.ixaris.commons.dimensions.config.CommonsDimensionsConfig.ConfigValues;
import com.ixaris.commons.dimensions.config.ConfigHelper;
import com.ixaris.commons.dimensions.config.SetDef;
import com.ixaris.commons.dimensions.config.SetDefRegistry;
import com.ixaris.commons.dimensions.config.SetUpdates;
import com.ixaris.commons.dimensions.config.ValueDef;
import com.ixaris.commons.dimensions.config.ValueDefRegistry;
import com.ixaris.commons.dimensions.config.data.ConfigSetEntity;
import com.ixaris.commons.dimensions.config.data.ConfigValueEntity;
import com.ixaris.commons.dimensions.config.value.Value;
import com.ixaris.commons.dimensions.lib.CommonsDimensionsLib;
import com.ixaris.commons.dimensions.lib.context.ConfigValidationException;
import com.ixaris.commons.dimensions.lib.context.Context;

@Component
public final class ConfigAdminProto {
    
    private final ConfigHelper config;
    private final ConfigAdminHelper configAdmin;
    
    @Autowired
    public ConfigAdminProto(final ConfigHelper config, final ConfigAdminHelper configAdmin) {
        this.config = config;
        this.configAdmin = configAdmin;
    }
    
    public ConfigValueDefs getConfigValueDefs() {
        return ValueDefRegistry.getInstance().toProtobuf();
    }
    
    public ConfigValueDef getConfigValueDef(final String key) {
        return ValueDefRegistry.getInstance().resolve(key).toProtobuf();
    }
    
    public Async<CommonsDimensionsConfig.Value> getExactMatchValue(final String key, final CommonsDimensionsLib.Context context) {
        return configAdmin
            .getExactMatchValue(getValueContext(key, context))
            .map(o -> o.map(e -> e.getValue().toProtobuf()).orElse(CommonsDimensionsConfig.Value.getDefaultInstance()));
    }
    
    public Async<ConfigValues> getAllValuesMatchingContext(final String key, final CommonsDimensionsLib.Context context) {
        return configAdmin.getAllValuesMatchingContext(getValueContext(key, context)).map(ConfigValueEntity::collectionToProtobuf);
    }
    
    public Async<ConfigValues> getAllValuesContainingContext(final String key, final CommonsDimensionsLib.Context context) {
        return configAdmin.getAllValuesContainingContext(getValueContext(key, context)).map(ConfigValueEntity::collectionToProtobuf);
    }
    
    public Async<CommonsDimensionsConfig.Value> simulateValue(final String key, final CommonsDimensionsLib.Context context) {
        return config.getConfigValue(getValueContext(key, context)).map(o -> o.map(Value::toProtobuf).orElse(CommonsDimensionsConfig.Value.getDefaultInstance()));
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Value> Async<Void> setConfigValue(final String key, final CommonsDimensionsConfig.ConfigValue value) throws ConfigValidationException {
        final ValueDef<T> def = (ValueDef<T>) ValueDefRegistry.getInstance().resolve(key);
        return configAdmin
            .setConfigValue(def.getContextDef().contextFromProtobuf(def, value.getContext()),
                def.getValueBuilder().buildFromProtobuf(value.getValue()))
            .map(v -> null);
    }
    
    public Async<Void> removeConfigValue(final String key, final CommonsDimensionsLib.Context context) {
        return configAdmin.removeConfigValue(getValueContext(key, context)).map(v -> null);
    }
    
    public ConfigSetDefs getConfigSetDefs() {
        return SetDefRegistry.getInstance().toProtobuf();
    }
    
    public ConfigSetDef getConfigSetDef(final String key) {
        return SetDefRegistry.getInstance().resolve(key).toProtobuf();
    }
    
    public Async<CommonsDimensionsConfig.Set> getExactMatchSet(final String key, final CommonsDimensionsLib.Context context) {
        return configAdmin
            .getExactMatchSet(getSetContext(key, context))
            .map(o -> o.map(e -> Value.setToProtobuf(e.getSet())).orElseGet(CommonsDimensionsConfig.Set::getDefaultInstance));
    }
    
    public Async<ConfigSets> getAllSetsMatchingContext(final String key, final CommonsDimensionsLib.Context context) {
        return configAdmin.getAllSetsMatchingContext(getSetContext(key, context)).map(ConfigSetEntity::collectionToProtobuf);
    }
    
    public Async<ConfigSets> getAllSetsContainingContext(final String key, final CommonsDimensionsLib.Context context) {
        return configAdmin.getAllSetsContainingContext(getSetContext(key, context)).map(ConfigSetEntity::collectionToProtobuf);
    }
    
    public Async<CommonsDimensionsConfig.Set> simulateSet(final String key, final CommonsDimensionsLib.Context context) {
        return config.getConfigSet(getSetContext(key, context)).map(Value::setToProtobuf);
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Value> Async<Void> setConfigSet(final String key, final CommonsDimensionsConfig.ConfigSet set) throws ConfigValidationException {
        final SetDef<T> def = (SetDef<T>) SetDefRegistry.getInstance().resolve(key);
        return configAdmin
            .setConfigSet(def.getContextDef().contextFromProtobuf(def, set.getContext()), convertSet(def, set.getSet()))
            .map(v -> null);
    }
    
    @SuppressWarnings("unchecked")
    public <T extends Value> Async<Void> updateConfigSet(final String key, final CommonsDimensionsConfig.ConfigSetUpdates set) throws ConfigValidationException {
        final SetDef<T> def = (SetDef<T>) SetDefRegistry.getInstance().resolve(key);
        return configAdmin
            .updateConfigSet(def.getContextDef().contextFromProtobuf(def, set.getContext()),
                new SetUpdates<>(convertSet(def, set.getAdded()), convertSet(def, set.getRemoved())))
            .map(v -> null);
    }
    
    public Async<Void> removeConfigSet(final String key, final CommonsDimensionsLib.Context context) {
        return configAdmin.removeConfigSet(getSetContext(key, context)).map(v -> null);
    }
    
    @SuppressWarnings("unchecked")
    private <T extends Value> Context<? extends ValueDef<T>> getValueContext(final String key, final CommonsDimensionsLib.Context context) {
        final ValueDef<T> def = (ValueDef<T>) ValueDefRegistry.getInstance().resolve(key);
        return def.getContextDef().contextFromProtobuf(def, context);
    }
    
    @SuppressWarnings("unchecked")
    private <T extends Value> Context<? extends SetDef<T>> getSetContext(final String key, final CommonsDimensionsLib.Context context) {
        final SetDef<T> def = (SetDef<T>) SetDefRegistry.getInstance().resolve(key);
        return def.getContextDef().contextFromProtobuf(def, context);
    }
    
    private <T extends Value> Set<T> convertSet(final SetDef<T> def, final CommonsDimensionsConfig.Set set) {
        final Value.Builder<T> valueBuilder = def.getValueBuilder();
        return set.getValuesList().stream().map(valueBuilder::buildFromProtobuf).collect(Collectors.toSet());
    }
    
}
