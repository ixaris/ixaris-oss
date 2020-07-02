package com.ixaris.commons.jooq.persistence;

import java.util.HashMap;
import java.util.Map;
import java.util.function.Function;
import java.util.function.Supplier;

import org.jooq.UpdatableRecord;

/**
 * Use this class to manage a one-to-many child relationship in an entity. This class serves a similar role as Hibernate's @ElementCollection.
 *
 * <p>This class provides a means to apply a new collection of items (and figure out what needs to be inserted, updated and deleted) or just add
 * or remove items. Internally uses jooq's bulk store and delete methods.
 *
 * <p>For a usage example see com.ixaris.samples.persistence.poc.impl.data.AuthorEntity
 */
public final class RecordMap<K, R extends UpdatableRecord<R>> extends AbstractRecordMap<K, R, RecordMap<K, R>> {
    
    public static <K, R extends UpdatableRecord<R>> RecordMap<K, R> withNewRecordFunction(final Function<K, R> newRecordFunction) {
        return new RecordMap<>(new HashMap<>(), newRecordFunction);
    }
    
    public static <K, R extends UpdatableRecord<R>> RecordMap<K, R> withMapSupplierAndNewRecordFunction(final Supplier<? extends Map<K, R>> mapSupplier,
                                                                                                        final Function<K, R> newRecordFunction) {
        return new RecordMap<>(mapSupplier.get(), newRecordFunction);
    }
    
    private RecordMap(final Map<K, R> map, final Function<K, R> newRecordFunction) {
        super(map, newRecordFunction);
    }
    
    @Override
    public RecordMap<K, R> store() {
        try {
            attachAndStore(added.values());
            attachAndStore(updated.values());
            attachAndDelete(removed.values());
        } finally {
            added.clear();
            updated.clear();
            removed.clear();
        }
        return this;
    }
    
    @Override
    public RecordMap<K, R> delete() {
        try {
            attachAndDelete(map.values());
        } finally {
            afterDelete();
        }
        return this;
    }
    
}
