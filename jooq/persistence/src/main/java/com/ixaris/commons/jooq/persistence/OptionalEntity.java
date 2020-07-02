package com.ixaris.commons.jooq.persistence;

import java.util.function.Supplier;

/**
 * Use this class to manage an optional one-to-one relationship to an entity.
 * 
 * Example Settlement related to optional auth, entity with optional failure details
 */
public final class OptionalEntity<E extends Entity<E>> extends AbstractOptionalRecord<E, OptionalEntity<E>> {
    
    public static <E extends Entity<E>> OptionalEntity<E> withNewEntitySupplier(final Supplier<E> newEntitySupplier) {
        return new OptionalEntity<>(newEntitySupplier);
    }
    
    private OptionalEntity(final Supplier<E> newEntitySupplier) {
        super(newEntitySupplier);
    }
    
    @Override
    public OptionalEntity<E> store() {
        if (record != null) {
            try {
                if (!deleted) {
                    record.store();
                } else {
                    record.delete();
                }
            } finally {
                if (deleted) {
                    record = null;
                    deleted = false;
                }
            }
        }
        return this;
    }
    
    @Override
    public OptionalEntity<E> delete() {
        if (record != null) {
            try {
                record.delete();
            } finally {
                record = null;
                deleted = false;
            }
        }
        return this;
    }
    
}
