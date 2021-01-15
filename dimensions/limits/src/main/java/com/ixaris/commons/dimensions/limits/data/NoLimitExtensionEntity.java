package com.ixaris.commons.dimensions.limits.data;

import static com.ixaris.commons.async.lib.Async.result;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.function.Function;

import org.jooq.Record;
import org.jooq.SelectJoinStep;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.dimensions.limits.jooq.tables.records.LibDimLimitRecord;

public final class NoLimitExtensionEntity extends AbstractLimitExtensionEntity<Void, NoLimitExtensionEntity> {
    
    private static final NoLimitExtensionEntity INSTANCE = new NoLimitExtensionEntity();
    
    public static final Fetch<NoLimitExtensionEntity, Void> FETCH = new Fetch<NoLimitExtensionEntity, Void>() {
        
        @Override
        public SelectJoinStep<Record> joinWithInfo(final SelectJoinStep<Record> from) {
            return from;
        }
        
        @Override
        public Void fetchRelated(final long id) {
            return null;
        }
        
        @Override
        public Map<Long, Void> fetchRelated(final List ids) {
            return Collections.emptyMap();
        }
        
        @Override
        public NoLimitExtensionEntity from(final Record record, final Void related) {
            return INSTANCE;
        }
    };
    
    public static NoLimitExtensionEntity getInstance() {
        return INSTANCE;
    }
    
    public static Function<Long, NoLimitExtensionEntity> getCreate() {
        return id -> INSTANCE;
    }
    
    @Override
    public NoLimitExtensionEntity store() {
        return this;
    }
    
    @Override
    public NoLimitExtensionEntity delete() {
        return this;
    }
    
    @Override
    public Async<Long> getCount(final Void extensionParam, final LibDimLimitRecord limitRecord) {
        return result(limitRecord.getMaxCount());
    }
    
    @Override
    public Async<Long> getMin(final Void extensionParam, final LibDimLimitRecord limitRecord) {
        return result(limitRecord.getMinAmount());
    }
    
    @Override
    public Async<Long> getMax(final Void extensionParam, final LibDimLimitRecord limitRecord) {
        return result(limitRecord.getMaxAmount());
    }
    
    @Override
    public NoLimitExtensionEntity copyForSplitLimit(final Long id) {
        return this;
    }
    
}
