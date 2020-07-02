package com.ixaris.commons.jooq.persistence;

import static com.ixaris.commons.jooq.persistence.Entity.attachAndDelete;
import static com.ixaris.commons.jooq.persistence.Entity.attachAndStore;

import java.util.Collection;

import org.jooq.UpdatableRecord;

@Deprecated
public final class RecordsBatch {
    
    static final RecordsBatch INSTANCE = new RecordsBatch();
    
    public RecordsBatch store(final UpdatableRecord<?>... records) {
        attachAndStore((UpdatableRecord[]) records);
        return this;
    }
    
    public RecordsBatch store(final Collection<? extends UpdatableRecord<?>> records) {
        attachAndStore((Collection) records);
        return this;
    }
    
    public RecordsBatch store(final Entity<?> entity) {
        entity.store();
        return this;
    }
    
    public RecordsBatch delete(final UpdatableRecord<?>... records) {
        attachAndDelete((UpdatableRecord[]) records);
        return this;
    }
    
    public RecordsBatch delete(final Collection<? extends UpdatableRecord<?>> records) {
        attachAndDelete((Collection) records);
        return this;
    }
    
    public RecordsBatch delete(final Entity<?> entity) {
        entity.delete();
        return this;
    }
    
    public RecordsBatch addAll(final RecordsBatch recordsBatch) {
        return this;
    }
    
    public void execute() {}
    
}
