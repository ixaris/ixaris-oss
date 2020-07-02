package com.ixaris.commons.jooq.persistence;

import java.util.Optional;
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Supplier;

abstract class AbstractOptionalRecord<R, O extends AbstractOptionalRecord<R, O>> extends Entity<O> {
    
    private final Supplier<R> newRecordFunction;
    protected R record;
    boolean deleted = false;
    
    AbstractOptionalRecord(final Supplier<R> newRecordSupplier) {
        if (newRecordSupplier == null) {
            throw new IllegalArgumentException("newRecordSupplier is null");
        }
        this.newRecordFunction = newRecordSupplier;
    }
    
    public final Optional<R> getOptional() {
        return Optional.ofNullable(get());
    }
    
    public final R get() {
        return deleted ? null : record;
    }
    
    public final boolean isEmpty() {
        return record == null || deleted;
    }
    
    public <U> Optional<U> map(final Function<? super R, ? extends U> mapper) {
        return getOptional().map(mapper);
    }
    
    public void ifPresent(final Consumer<? super R> action) {
        ifPresentOrElse(action, null);
    }
    
    public void ifPresentOrElse(final Consumer<? super R> action, final Runnable emptyAction) {
        if (record != null && !deleted) {
            action.accept(record);
        } else if (emptyAction != null) {
            emptyAction.run();
        }
    }
    
    @SuppressWarnings("unchecked")
    public final O fromExisting(final R record) {
        this.record = record;
        return (O) this;
    }
    
    @SuppressWarnings("unchecked")
    public final O apply(final Consumer<R> consumer) {
        if (deleted) {
            deleted = false;
        } else if (record == null) {
            record = newRecordFunction.get();
        }
        consumer.accept(record);
        return (O) this;
    }
    
    @SuppressWarnings("unchecked")
    public final O remove() {
        if (record != null) {
            deleted = true;
        }
        return (O) this;
    }
    
    @Override
    public RecordsBatch getAndClearRecordsToStore() {
        store();
        return RecordsBatch.INSTANCE;
    }
    
    @Override
    public RecordsBatch getAndClearRecordsToDelete() {
        delete();
        return RecordsBatch.INSTANCE;
    }
    
}
