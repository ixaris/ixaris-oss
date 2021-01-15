package com.ixaris.commons.dimensions.limits.data;

import java.util.List;
import java.util.Map;

import org.jooq.Record;
import org.jooq.SelectJoinStep;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.dimensions.limits.jooq.tables.records.LibDimLimitRecord;
import com.ixaris.commons.jooq.persistence.Entity;

/**
 * abstract class to be extended to hold more information on a limit
 *
 * @param <T>
 * @param <X>
 */
public abstract class AbstractLimitExtensionEntity<T, X extends AbstractLimitExtensionEntity<T, X>> extends Entity<X> {
    
    public interface Fetch<I extends AbstractLimitExtensionEntity<?, I>, G> {
        
        SelectJoinStep<Record> joinWithInfo(SelectJoinStep<Record> from);
        
        G fetchRelated(long id);
        
        Map<Long, G> fetchRelated(List<Long> ids);
        
        I from(Record record, G related);
        
    }
    
    public abstract Async<Long> getCount(final T extensionParam, final LibDimLimitRecord limitRecord);
    
    public abstract Async<Long> getMin(final T extensionParam, final LibDimLimitRecord limitRecord);
    
    public abstract Async<Long> getMax(final T extensionParam, final LibDimLimitRecord limitRecord);
    
    /**
     * copy the extended information in cases where a limit needs to be split into two limits. This typically happens when a limit is created in
     * the middle of another limit's applicable time window, causing the latter limit to be split in two limits, one applying before the former
     * limit time window, and one applying after it
     *
     * @param id the new limit's id
     * @return
     */
    public abstract X copyForSplitLimit(final Long id);
    
}
