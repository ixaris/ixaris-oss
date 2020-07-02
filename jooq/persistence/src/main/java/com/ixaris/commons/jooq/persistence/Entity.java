package com.ixaris.commons.jooq.persistence;

import static com.ixaris.commons.jooq.persistence.TransactionalDSLContext.JOOQ_TX;

import java.util.Arrays;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.Map;

import org.jooq.Configuration;
import org.jooq.Table;
import org.jooq.UpdatableRecord;

public abstract class Entity<T extends Entity<T>> {
    
    @SafeVarargs
    public static <R extends UpdatableRecord<R>> int[] attachAndStore(final R... records) {
        return attachAndStore(Arrays.asList(records));
    }
    
    public static <R extends UpdatableRecord<R>> int[] attachAndStore(final Collection<R> records) {
        if (!records.isEmpty()) {
            final Iterator<R> i = records.iterator();
            R next = i.next();
            if (isOptimisticLocked(next.getTable())) {
                final int[] count = new int[records.size()];
                int ii = 0;
                final Configuration configuration = JOOQ_TX.get().configuration();
                next.attach(configuration);
                count[ii] = next.store();
                while (i.hasNext()) {
                    next = i.next();
                    next.attach(configuration);
                    ii++;
                    count[ii] = next.store();
                }
                return count;
            } else {
                return JOOQ_TX.get().batchStore(records).execute();
            }
        } else {
            return new int[0];
        }
    }
    
    public static <R extends UpdatableRecord<R>> int attachAndStore(final R record) {
        final Configuration configuration = JOOQ_TX.get().configuration();
        record.attach(configuration);
        return record.store();
    }
    
    @SafeVarargs
    public static <R extends UpdatableRecord<R>> int[] attachAndDelete(final R... records) {
        return attachAndDelete(Arrays.asList(records));
    }
    
    public static <R extends UpdatableRecord<R>> int[] attachAndDelete(final Collection<R> records) {
        if (!records.isEmpty()) {
            final Iterator<R> i = records.iterator();
            R next = i.next();
            if (isOptimisticLocked(next.getTable())) {
                final int[] count = new int[records.size()];
                int ii = 0;
                final Configuration configuration = JOOQ_TX.get().configuration();
                next.attach(configuration);
                count[ii] = next.delete();
                while (i.hasNext()) {
                    next = i.next();
                    next.attach(configuration);
                    ii++;
                    count[ii] = next.delete();
                }
                return count;
            } else {
                return JOOQ_TX.get().batchDelete(records).execute();
            }
        } else {
            return new int[0];
        }
    }
    
    public static <R extends UpdatableRecord<R>> int attachAndDelete(final R record) {
        final Configuration configuration = JOOQ_TX.get().configuration();
        record.attach(configuration);
        return record.delete();
    }
    
    private static final Map<Table<?>, Boolean> OPT_TABLES = new HashMap<>();
    
    private static boolean isOptimisticLocked(final Table<?> table) {
        return OPT_TABLES.computeIfAbsent(table, k -> k.getRecordTimestamp() != null || k.getRecordVersion() != null);
    }
    
    // TODO make abstract when getAndClearRecordsToStore() is removed
    @SuppressWarnings("unchecked")
    public T store() {
        getAndClearRecordsToStore();
        return (T) this;
    }
    
    // TODO make abstract when getAndClearRecordsToDelete() is removed
    @SuppressWarnings("unchecked")
    public T delete() {
        getAndClearRecordsToDelete();
        return (T) this;
    }
    
    /**
     * @deprecated use store() instead
     */
    @Deprecated
    public RecordsBatch getAndClearRecordsToStore() {
        return RecordsBatch.INSTANCE;
    }
    
    /**
     * @deprecated use delete() instead
     */
    @Deprecated
    public RecordsBatch getAndClearRecordsToDelete() {
        throw new UnsupportedOperationException();
    }
    
    /**
     * use attachAndStore() or attachAndDelete() in store() and delete() to avoid the need to explicitly attach
     */
    @Deprecated
    public void attach() {}
    
}
