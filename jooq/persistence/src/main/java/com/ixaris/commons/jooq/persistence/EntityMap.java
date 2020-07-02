package com.ixaris.commons.jooq.persistence;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

/**
 * Use this class to manage a one-to-many child relationship in an entity. This class serves a similar role as Hibernate's @ElementCollection.
 *
 * <p>This class provides a means to apply a new collection of items (and figure out what needs to be inserted, updated and deleted) or just add
 * or remove items. Internally uses jooq's bulk store and delete methods.
 *
 * <p>For a usage example see com.ixaris.samples.persistence.poc.impl.data.AuthorEntity
 */
public final class EntityMap<K, E extends Entity<E>> extends AbstractRecordMap<K, E, EntityMap<K, E>> {
    
    public static <K, E extends Entity<E>> EntityMap<K, E> withNewEntityFunction(final Function<K, E> newEntityFunction) {
        return new EntityMap<>(new HashMap<>(), newEntityFunction);
    }
    
    public static <K, E extends Entity<E>> EntityMap<K, E> withMapSupplierAndNewEntityFunction(final Supplier<? extends Map<K, E>> mapSupplier,
                                                                                               final Function<K, E> newEntityFunction) {
        return new EntityMap<>(mapSupplier.get(), newEntityFunction);
    }
    
    private EntityMap(final Map<K, E> map, final Function<K, E> newRecordFunction) {
        super(map, newRecordFunction);
    }
    
    @Override
    public EntityMap<K, E> store() {
        try {
            for (final E entity : added.values()) {
                entity.store();
            }
            for (final E entity : updated.values()) {
                entity.store();
            }
            for (final E entity : removed.values()) {
                entity.delete();
            }
        } finally {
            added.clear();
            updated.clear();
            removed.clear();
        }
        return this;
    }
    
    @Override
    public EntityMap<K, E> delete() {
        try {
            for (final E entity : map.values()) {
                entity.delete();
            }
        } finally {
            afterDelete();
        }
        return this;
    }
    
}
