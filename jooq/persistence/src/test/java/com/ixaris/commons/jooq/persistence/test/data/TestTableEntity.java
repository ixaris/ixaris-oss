package com.ixaris.commons.jooq.persistence.test.data;

import java.util.function.Consumer;

import com.ixaris.commons.jooq.persistence.Entity;
import com.ixaris.commons.jooq.persistence.test.jooq.tables.records.TestTableRecord;

public class TestTableEntity extends Entity<TestTableEntity> {
    
    private final TestTableRecord record;
    
    TestTableEntity(final TestTableRecord record) {
        this.record = record;
    }
    
    public TestTableEntity(final String name) {
        this(new TestTableRecord());
        record.setName(name);
    }
    
    @Override
    public TestTableEntity store() {
        attachAndStore(record);
        return this;
    }
    
    @Override
    public TestTableEntity delete() {
        attachAndDelete(record);
        return this;
    }
    
    public TestTableEntity apply(final Consumer<TestTableRecord> consumer) {
        if (consumer != null) {
            consumer.accept(record);
        }
        return this;
    }
    
    public TestTableRecord getRecord() {
        return record;
    }
    
}
