package com.ixaris.commons.dimensions.config.admin;

import static com.ixaris.commons.async.lib.idempotency.Intent.INTENT;
import static com.ixaris.commons.jooq.persistence.TransactionalDSLContext.JOOQ_TX;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.Set;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.dimensions.config.SetDef;
import com.ixaris.commons.dimensions.config.SetUpdates;
import com.ixaris.commons.dimensions.config.ValueDef;
import com.ixaris.commons.dimensions.config.cache.ConfigCacheProvider;
import com.ixaris.commons.dimensions.config.data.ConfigSetEntity;
import com.ixaris.commons.dimensions.config.data.ConfigValueEntity;
import com.ixaris.commons.dimensions.config.value.Value;
import com.ixaris.commons.dimensions.config.value.validation.CascadeSetValidation;
import com.ixaris.commons.dimensions.config.value.validation.CascadeValueValidation;
import com.ixaris.commons.dimensions.config.value.validation.ContextValidation;
import com.ixaris.commons.dimensions.config.value.validation.SetValidation;
import com.ixaris.commons.dimensions.config.value.validation.SimpleValidation;
import com.ixaris.commons.dimensions.config.value.validation.ValueValidation;
import com.ixaris.commons.dimensions.lib.base.DimensionalHelper;
import com.ixaris.commons.dimensions.lib.context.ConfigValidationException;
import com.ixaris.commons.dimensions.lib.context.Context;
import com.ixaris.commons.jooq.persistence.JooqAsyncPersistenceProvider;

@Component
public class ConfigAdminHelper {
    
    /**
     * Writes a context property value to the database, and invalidates the cache of the property if there is any.
     *
     * <p>The value to set must satisfy the syntactic and semantic constraints of the property.
     *
     * @param context the context instance for which the value is going to be set. Cannot be null.
     * @param value The value to be set. Cannot be null.
     */
    @SuppressWarnings("unchecked")
    public static <T extends Value, V extends ValueDef<T>> T setConfigValue(final ConfigCacheProvider cache,
                                                                            final Context<V> context,
                                                                            final T value) throws ConfigValidationException {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        DimensionalHelper.validateForConfig(context);
        
        final CascadeValueValidation<? super T> cascade = validateValueAndGetCascadeIfApplicable(context, value);
        final ConfigValueEntity<T, V> currentValue = ConfigValueEntity.lookupExactMatch(context).orElse(null);
        if (currentValue != null) {
            if (currentValue.getValue().equals(value)) {
                return currentValue.getValue();
            }
            currentValue.setValue(value);
            currentValue.store();
        } else {
            // not found, so create a new context value
            new ConfigValueEntity<>(context, value).store();
        }
        if (cascade != null) {
            ConfigValueEntity.cascadeUpdate(context, cascade, value);
        }
        JOOQ_TX.get().onCommit(() -> cache.of(context.getDef()).invalidate());
        return currentValue != null ? currentValue.getValue() : null;
    }
    
    /**
     * Removes a context property value from the database, and invalidates the cache of the property if there is any.
     *
     * @param context the context instance for which the value is going to be removed. Cannot be null.
     * @return true if a value is removed, false otherwise
     */
    public static <T extends Value, V extends ValueDef<T>> boolean removeConfigValue(final ConfigCacheProvider cache, final Context<V> context) {
        DimensionalHelper.validateForConfig(context);
        if (!context.getDef().isDeletable()) {
            throw new IllegalStateException("Value does not allow deletion");
        }
        
        return ConfigValueEntity
            .lookupExactMatch(context)
            .map(v -> {
                v.delete();
                JOOQ_TX.get().onCommit(() -> cache.of(context.getDef()).invalidate());
                return true;
            })
            .orElse(false);
    }
    
    /**
     * Writes a context set to the database, and invalidates the cache of the set if there is any.
     *
     * <p>The set of values to set must satisfy the syntactic and semantic contraints of the set.
     *
     * @param context the context instance for which the set of values are going to be set. Cannot be null.
     * @param set The set of values to be set. Cannot be null.
     * @throws ConfigValidationException if a value in the set is not valid for this set
     */
    public static <T extends Value, S extends SetDef<T>> SetUpdates<T> setConfigSet(final ConfigCacheProvider cache,
                                                                                    final Context<S> context,
                                                                                    final Set<T> set) throws ConfigValidationException {
        if (set == null) {
            throw new IllegalArgumentException("set is null");
        }
        
        DimensionalHelper.validateForConfig(context);
        final CascadeSetValidation<? super T> cascade = validateSetAndGetCascadeIfApplicable(context, set);
        final ConfigSetEntity<T, S> currentSet = ConfigSetEntity.lookupExactMatch(context).orElse(null);
        final SetUpdates<T> updates;
        if (currentSet != null) {
            updates = currentSet.setSet(set);
            if (updates.getAdded().isEmpty() && updates.getRemoved().isEmpty()) {
                return updates;
            }
            currentSet.store();
        } else {
            // not found, so create a new context value
            updates = new SetUpdates<>(set, Collections.emptySet());
            new ConfigSetEntity<>(context, set).store();
        }
        if (cascade != null) {
            ConfigSetEntity.cascadeUpdate(context, cascade, set, updates);
        }
        if (context.getDef().isCacheable()) {
            JOOQ_TX.get().onCommit(() -> cache.of(context.getDef()).invalidate());
        }
        return updates;
    }
    
    /**
     * Adds a value to the context set. If the set does not exist, it is created to the database.
     *
     * <p>The value to set must satisfy the syntactic and semantic contraints of the property.
     *
     * @param context the context instance for which the value is going to be added. Cannot be null.
     * @param setUpdates
     * @return true if at least one value is added, false if all values are already in the set
     * @throws ConfigValidationException if a value is not valid for this property or a set with the given context instance is undefined
     */
    public static <T extends Value, S extends SetDef<T>> SetUpdates<T> updateConfigSet(final ConfigCacheProvider cache,
                                                                                       final Context<S> context,
                                                                                       final SetUpdates<T> setUpdates) throws ConfigValidationException {
        if (setUpdates == null) {
            throw new IllegalArgumentException("setUpdates is null");
        }
        
        DimensionalHelper.validateForConfig(context);
        final ConfigSetEntity<T, S> currentSet = ConfigSetEntity.lookupExactMatch(context).orElse(null);
        final SetUpdates<T> updates;
        final Set<T> set;
        final CascadeSetValidation<? super T> validation;
        if (currentSet != null) {
            updates = currentSet.updateSet(setUpdates);
            if (updates.getAdded().isEmpty() && updates.getRemoved().isEmpty()) {
                return updates;
            }
            currentSet.store();
            set = currentSet.getSet();
            validation = validateSetAndGetCascadeIfApplicable(context, set);
        } else {
            // not found, so create a new context value
            set = setUpdates.getAdded();
            updates = new SetUpdates<>(set, Collections.emptySet());
            new ConfigSetEntity<>(context, set).store();
            validation = validateSetAndGetCascadeIfApplicable(context, set);
        }
        if (validation != null) {
            ConfigSetEntity.cascadeUpdate(context, validation, set, updates);
        }
        if (context.getDef().isCacheable()) {
            JOOQ_TX.get().onCommit(() -> cache.of(context.getDef()).invalidate()); // invalidate the cache on commit
        }
        return updates;
    }
    
    /**
     * Removes a context set from the database, and invalidates the cache of the set if there is any.
     *
     * @param context the context instance for which the value is going to be removed. Cannot be null.
     * @return true if a value is removed, false otherwise
     * @success remove the set, invalidate cache and return true
     * @success return false if no set defined for the given context
     */
    public static <T extends Value, S extends SetDef<T>> boolean removeConfigSet(final ConfigCacheProvider cache, final Context<S> context) {
        DimensionalHelper.validateForConfig(context);
        return ConfigSetEntity
            .lookupExactMatch(context)
            .map(s -> {
                s.delete();
                if (context.getDef().isCacheable()) {
                    JOOQ_TX.get().onCommit(() -> cache.of(context.getDef()).invalidate());
                }
                return true;
            })
            .orElse(false);
    }
    
    @SuppressWarnings("unchecked")
    private static <T extends Value, V extends ValueDef<T>> CascadeValueValidation<? super T> validateValueAndGetCascadeIfApplicable(final Context<V> context,
                                                                                                                                     final T newValue) throws ConfigValidationException {
        
        // validate the value (if available)
        final ValueValidation<? super T> validation = context.getDef().getValidation();
        if (validation != null) {
            switch (validation.getType()) {
                case SIMPLE:
                    ((SimpleValidation<? super T>) validation).validate(newValue);
                    return null;
                case CONTEXT:
                    ((ContextValidation<? super T>) validation).validate(newValue, context);
                    return null;
                case CASCADE:
                    final CascadeValueValidation<? super T> cascadeValidation = (CascadeValueValidation<? super T>) validation;
                    final Optional<ConfigValueEntity<T, V>> v = ConfigValueEntity.lookupNextMatchingContext(context);
                    cascadeValidation.validate(newValue, v.map(ConfigValueEntity::getValue).orElse(null));
                    return cascadeValidation;
                default:
                    throw new UnsupportedOperationException("Validation type [" + validation.getType() + "] not implemented");
            }
        } else {
            return null;
        }
    }
    
    @SuppressWarnings("unchecked")
    private static <T extends Value, S extends SetDef<T>> CascadeSetValidation<? super T> validateSetAndGetCascadeIfApplicable(final Context<S> context,
                                                                                                                               final Set<T> newSet) throws ConfigValidationException {
        final SetValidation<? super T> validation = context.getDef().getValidation();
        if (validation != null) {
            switch (validation.getType()) {
                case SIMPLE:
                    ((SimpleValidation<? super T>) validation).validate(newSet);
                    return null;
                case CONTEXT:
                    ((ContextValidation<? super T>) validation).validate(newSet, context);
                    return null;
                case CASCADE:
                    final CascadeSetValidation<? super T> cascadeValidation = (CascadeSetValidation<? super T>) validation;
                    final Optional<ConfigSetEntity<T, S>> s = ConfigSetEntity.lookupNextMatchingContext(context);
                    cascadeValidation.validate(newSet, s.map(ConfigSetEntity::getSet).orElse(null));
                    return cascadeValidation;
                default:
                    throw new UnsupportedOperationException("Validation type [" + validation.getType() + "] not implemented");
            }
        } else {
            return null;
        }
    }
    
    private final JooqAsyncPersistenceProvider db;
    private final ConfigCacheProvider cache;
    
    @Autowired
    public ConfigAdminHelper(final JooqAsyncPersistenceProvider db, final ConfigCacheProvider cache) {
        this.db = db;
        this.cache = cache;
    }
    
    /**
     * Retrieves the value exactly matching the context. Used for configuration purposes
     *
     * @param context The Context of the set of values to retrieve. Cannot be null.
     * @return the matching value if one has been defined, a default value if one has been defined or null if no value has been defined
     */
    public <T extends Value, V extends ValueDef<T>> Async<Optional<ConfigValueEntity<T, V>>> getExactMatchValue(final Context<V> context) {
        DimensionalHelper.validateForConfigLookup(context);
        return db.transactionRequired(() -> ConfigValueEntity.lookupExactMatch(context));
    }
    
    /**
     * Gets all values and corresponding context defined for a property that match the given context.
     *
     * @param context the context
     * @return all matching defined values. Never null
     */
    public <T extends Value, V extends ValueDef<T>> Async<List<ConfigValueEntity<T, V>>> getAllValuesMatchingContext(final Context<V> context) {
        DimensionalHelper.validateForConfigLookup(context);
        return db.transactionRequired(() -> ConfigValueEntity.lookupAllMatchingContext(context));
    }
    
    /**
     * Gets all values and corresponding context defined for a property that contain the given context. Will return all values if an empty
     * context is passed
     *
     * @param context the context
     * @return all matching defined set. Never null
     */
    public <T extends Value, V extends ValueDef<T>> Async<List<ConfigValueEntity<T, V>>> getAllValuesContainingContext(final Context<V> context) {
        DimensionalHelper.validateForConfigLookup(context);
        return db.transactionRequired(() -> ConfigValueEntity.lookupAllContainingContext(context));
    }
    
    public <T extends Value, V extends ValueDef<T>> Async<Void> setConfigValue(final Context<V> context, final T value) throws ConfigValidationException {
        return db
            .transaction(INTENT.get(), () -> {
                setConfigValue(cache, context, value);
                return (Void) null;
            })
            .onDuplicateIntent(e -> null);
    }
    
    public <T extends Value, V extends ValueDef<T>> Async<Void> removeConfigValue(final Context<V> context) {
        return db
            .transaction(INTENT.get(), () -> {
                removeConfigValue(cache, context);
                return (Void) null;
            })
            .onDuplicateIntent(e -> null);
    }
    
    /**
     * Retrieves the set of values exactly matching the context. Used for configuration purposes
     *
     * @param context The Context of the set of values to retrieve. Cannot be null.
     * @return A typed set of values of the property for the given context instance, or null if no value is found.
     */
    public <T extends Value, S extends SetDef<T>> Async<Optional<ConfigSetEntity<T, S>>> getExactMatchSet(final Context<S> context) {
        DimensionalHelper.validateForConfigLookup(context);
        return db.transactionRequired(() -> ConfigSetEntity.lookupExactMatch(context));
    }
    
    /**
     * Gets all sets and corresponding context defined for a set that match the given context.
     *
     * @param context the context
     * @return all matching defined set. Never null
     */
    public <T extends Value, S extends SetDef<T>> Async<List<ConfigSetEntity<T, S>>> getAllSetsMatchingContext(final Context<S> context) {
        DimensionalHelper.validateForConfigLookup(context);
        return db.transactionRequired(() -> ConfigSetEntity.lookupAllMatchingContext(context));
    }
    
    /**
     * Gets all sets and corresponding context defined for a set that contain the given context. Will return all values if an empty context is
     * passed
     *
     * @param context the context
     * @return all matching defined set. Never null
     */
    public <T extends Value, S extends SetDef<T>> Async<List<ConfigSetEntity<T, S>>> getAllSetsContainingContext(final Context<S> context) {
        DimensionalHelper.validateForConfigLookup(context);
        return db.transactionRequired(() -> ConfigSetEntity.lookupAllContainingContext(context));
    }
    
    public <T extends Value, S extends SetDef<T>> Async<Void> setConfigSet(final Context<S> context, final Set<T> set) throws ConfigValidationException {
        return db
            .transaction(INTENT.get(), () -> {
                setConfigSet(cache, context, set);
                return (Void) null;
            })
            .onDuplicateIntent(e -> null);
    }
    
    public <T extends Value, S extends SetDef<T>> Async<Void> updateConfigSet(final Context<S> context, final SetUpdates<T> setUpdates) throws ConfigValidationException {
        return db
            .transaction(INTENT.get(), () -> {
                updateConfigSet(cache, context, setUpdates);
                return (Void) null;
            })
            .onDuplicateIntent(e -> null);
    }
    
    public <T extends Value, S extends SetDef<T>> Async<Void> removeConfigSet(final Context<S> context) {
        return db
            .transaction(INTENT.get(), () -> {
                removeConfigSet(cache, context);
                return (Void) null;
            })
            .onDuplicateIntent(e -> null);
    }
    
}
