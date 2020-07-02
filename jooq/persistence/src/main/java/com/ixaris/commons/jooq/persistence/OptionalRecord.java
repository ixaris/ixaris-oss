package com.ixaris.commons.jooq.persistence;

import java.util.function.Supplier;

import org.jooq.UpdatableRecord;

/**
 * Use this class to manage an optional one-to-one relationship to a record.
 */
public final class OptionalRecord<R extends UpdatableRecord<R>> extends AbstractOptionalRecord<R, OptionalRecord<R>> {
    
    public static <R extends UpdatableRecord<R>> OptionalRecord<R> withNewRecordSupplier(final Supplier<R> newRecordSupplier) {
        return new OptionalRecord<>(newRecordSupplier);
    }
    
    private OptionalRecord(final Supplier<R> newRecordSuplier) {
        super(newRecordSuplier);
    }
    
    @Override
    public OptionalRecord<R> store() {
        if (record != null) {
            try {
                if (!deleted) {
                    attachAndStore(record);
                } else {
                    attachAndDelete(record);
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
    public OptionalRecord<R> delete() {
        if (record != null) {
            try {
                attachAndDelete(record);
            } finally {
                record = null;
                deleted = false;
            }
        }
        return this;
    }
    
}
