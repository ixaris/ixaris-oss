/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.config;

import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.jooq.persistence.TransactionalDSLContext.JOOQ_TX;

import java.util.Collections;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.dimensions.config.admin.UndefinedPropertyException;
import com.ixaris.commons.dimensions.config.cache.ConfigCache;
import com.ixaris.commons.dimensions.config.cache.ConfigCacheProvider;
import com.ixaris.commons.dimensions.config.data.ConfigSetEntity;
import com.ixaris.commons.dimensions.config.data.ConfigValueEntity;
import com.ixaris.commons.dimensions.config.value.NullMarker;
import com.ixaris.commons.dimensions.config.value.Value;
import com.ixaris.commons.dimensions.lib.base.DimensionalHelper;
import com.ixaris.commons.dimensions.lib.context.ConfigValidationException;
import com.ixaris.commons.dimensions.lib.context.Context;
import com.ixaris.commons.jooq.persistence.JooqAsyncPersistenceProvider;

/**
 * ContextDef Properties Impl
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
@Component
public final class ConfigHelper {
    
    private final JooqAsyncPersistenceProvider db;
    private final ConfigCacheProvider cache;
    
    @Autowired
    public ConfigHelper(final JooqAsyncPersistenceProvider db, final ConfigCacheProvider cache) {
        this.db = db;
        this.cache = cache;
    }
    
    /**
     * Retrieves the value of a property.
     *
     * @param context The Context of the value to retrieve. Cannot be null.
     * @return A typed value of the property for the given context instance, or null if no value is found
     */
    @SuppressWarnings("unchecked")
    public <T extends Value, V extends ValueDef<T>> Async<Optional<T>> getConfigValue(final Context<V> context) {
        DimensionalHelper.validateForQuery(context);
        final V def = context.getDef();
        final ConfigCache defCache = cache.of(def);
        
        // first try the cache. We have a 2 level cache, property > context instances > value
        final Object cached = defCache.get(context);
        
        if (cached != null) {
            return result(cached.equals(NullMarker.NULL) ? Optional.empty() : Optional.of((T) cached));
        }
        
        return db.transactionRequired(() -> {
            // next try the database
            final Optional<T> value = ConfigValueEntity.lookupBestMatch(context);
            if (!value.isPresent() && !def.isNullExpected()) {
                throw new UndefinedPropertyException("Missing configuration for set [" + def + "] and context [" + context + "]");
            }
            
            // cache the retrieved value. If we arrived here, context value is not cached
            // replace null with a custom null object to distinguish between no cached value and cached as null
            JOOQ_TX.get().onCommit(() -> defCache.put(context, value.map(v -> (Object) v).orElse(NullMarker.NULL)));
            return value;
        });
    }
    
    public <T extends Value, V extends ValueDef<T>> Async<Map<Context<V>, T>> getAllValuesMatchingContext(final Context<V> context) {
        DimensionalHelper.validateForQuery(context);
        return db.transactionRequired(() -> ConfigValueEntity
            .lookupAllMatchingContext(context)
            .stream()
            .collect(Collectors.toMap(ConfigValueEntity::getContext, ConfigValueEntity::getValue)));
    }
    
    public <T extends Value, V extends ValueDef<T>> Async<Map<Context<V>, T>> getAllValuesByValue(final V def, final T value) {
        return db.transactionRequired(() -> ConfigValueEntity
            .lookupByValue(def, value)
            .stream()
            .collect(Collectors.toMap(ConfigValueEntity::getContext, ConfigValueEntity::getValue)));
    }
    
    /**
     * Retrieves the set of values matching the context.
     *
     * @param context The Context of the set of values to retrieve. Cannot be null.
     * @return A typed set of values of the property for the given context instance, or an empty set if no set is found. Never returns null.
     */
    @SuppressWarnings("unchecked")
    public <T extends Value, S extends SetDef<T>> Async<Set<T>> getConfigSet(final Context<S> context) {
        DimensionalHelper.validateForQuery(context);
        final S def = context.getDef();
        final ConfigCache defCache = cache.of(def);
        
        // first try the cache. We have a 2 level cache, property > context instances > value
        if (def.isCacheable()) {
            final Object cached = defCache.get(context);
            
            if (cached != null) {
                return result((Set<T>) cached);
            }
        }
        
        return db.transactionRequired(() -> {
            // next try the database
            final Optional<Set<T>> set = ConfigSetEntity.lookupBestMatch(context);
            if (!set.isPresent() && !def.isNullExpected()) {
                throw new UndefinedPropertyException("Missing configuration for set [" + def + "] and context [" + context + "]");
            }
            
            if (def.isCacheable()) {
                // cache the retrieved set. If we arrived here, context set is not cached
                JOOQ_TX.get().onCommit(() -> defCache.put(context, set.orElse(Collections.emptySet())));
            }
            return set.orElse(Collections.emptySet());
        });
    }
    
    public <T extends Value, S extends SetDef<T>> Async<Map<Context<S>, Set<T>>> getAllSetsMatchingContext(final Context<S> context) {
        DimensionalHelper.validateForQuery(context);
        return db.transactionRequired(() -> ConfigSetEntity
            .lookupAllMatchingContext(context)
            .stream()
            .collect(Collectors.toMap(ConfigSetEntity::getContext, ConfigSetEntity::getSet)));
    }
    
    /**
     * Checks if the given value is contained in the set best matching the given context
     *
     * @param context the context instance for which the set is going to be checked. Cannot be null.
     * @param value the value to check for. Cannot be null
     * @return true if the set contains the given value, false otherwise
     * @throws ConfigValidationException if the value is not valid for this property
     */
    @SuppressWarnings("unchecked")
    public <T extends Value, S extends SetDef<T>> Async<Boolean> isValueInSet(final Context<S> context, final T value) {
        if (value == null) {
            throw new IllegalArgumentException("value is null");
        }
        
        DimensionalHelper.validateForQuery(context);
        final S def = context.getDef();
        
        // first try the cache. We have a 2 level cache, property > context instances > value
        if (def.isCacheable()) {
            final ConfigCache defCache = cache.of(def);
            final Object cached = defCache.get(context);
            
            if (cached != null) {
                return result(!cached.equals(NullMarker.NULL) && ((Set<T>) cached).contains(value));
            }
            
            return db.transactionRequired(() -> {
                // if we get here, there is no cached value
                // next try the database. Since set is cacheable, we will try to get the set and cache it
                final Optional<Set<T>> set = ConfigSetEntity.lookupBestMatch(context);
                if (!set.isPresent() && !def.isNullExpected()) {
                    throw new UndefinedPropertyException("Missing configuration for set [" + def + "] and context [" + context + "]");
                }
                
                // cache the retrieved set. If we arrived here, context set is not cached
                JOOQ_TX.get().onCommit(() -> defCache.put(context, set.orElse(Collections.emptySet())));
                return set.map(s -> s.contains(value)).orElse(false);
            });
            
        } else {
            // If the set is not cacheable, we will not get the whole set, to avoid unnecessary db reads (sets may be
            // large)
            // we therefore let the database check if the value is in the set
            return db.transactionRequired(() -> ConfigSetEntity.isValueInBestMatchSet(context, value));
        }
    }
    
}
