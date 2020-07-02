package com.ixaris.commons.jooq.persistence;

import java.util.Collection;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.BiConsumer;
import java.util.function.BiFunction;
import java.util.function.BiPredicate;
import java.util.function.Function;
import java.util.stream.Collectors;
import java.util.stream.Stream;

abstract class AbstractRecordMap<K, R, M extends AbstractRecordMap<K, R, M>> extends Entity<M> {
    
    @Deprecated
    @FunctionalInterface
    public interface ApplyFrom<T, K, R> {
        
        ApplyFromMap<T, R> withKeyFunction(Function<T, K> keyFunction);
        
    }
    
    @Deprecated
    public interface ApplyFromMap<T, R> {
        
        void withApplyToRecord(BiConsumer<R, T> applyItemToRecordConsumer);
        
        default void withNoOpApplyToRecord() {
            withApplyToRecord((r, t) -> {});
        }
        
    }
    
    @Deprecated
    @FunctionalInterface
    public interface RequireKeyFunction<T, K> {
        
        void withKeyFunction(Function<T, K> keyFunction);
        
    }
    
    private final Function<K, R> newRecordFunction;
    protected final Map<K, R> map;
    final Map<K, R> added = new HashMap<>();
    final Map<K, R> updated = new HashMap<>();
    final Map<K, R> removed = new HashMap<>();
    
    AbstractRecordMap(final Map<K, R> map, final Function<K, R> newRecordFunction) {
        if (map == null) {
            throw new IllegalArgumentException("map is null");
        }
        if (newRecordFunction == null) {
            throw new IllegalArgumentException("newRecordFunction is null");
        }
        this.newRecordFunction = newRecordFunction;
        this.map = map;
    }
    
    public final R get(final K key) {
        return map.get(key);
    }
    
    public final boolean containsKey(final K key) {
        return map.containsKey(key);
    }
    
    public final boolean isEmpty() {
        return map.isEmpty();
    }
    
    public final Set<K> keySet() {
        return Collections.unmodifiableSet(map.keySet());
    }
    
    public final Collection<R> values() {
        return Collections.unmodifiableCollection(map.values());
    }
    
    public final Set<Entry<K, R>> entrySet() {
        return Collections.unmodifiableSet(map.entrySet());
    }
    
    public void forEach(final BiConsumer<? super K, ? super R> action) {
        map.forEach(action);
    }
    
    public final Map<K, R> getMap() {
        return Collections.unmodifiableMap(map);
    }
    
    @Deprecated
    public final <T> Stream<T> map(final Function<? super R, ? extends T> mapper) {
        return map.values().stream().map(mapper);
    }
    
    public final <T> Map<K, T> map(final BiFunction<? super K, ? super R, ? extends T> mapper) {
        return map.entrySet().stream().collect(Collectors.toMap(Entry::getKey, e -> mapper.apply(e.getKey(), e.getValue())));
    }
    
    public final <T, U> Map<T, U> map(final Function<K, T> keyMapper, final BiFunction<? super K, ? super R, ? extends U> mapper) {
        return map.entrySet().stream().collect(Collectors.toMap(e -> keyMapper.apply(e.getKey()), e -> mapper.apply(e.getKey(), e.getValue())));
    }
    
    public final <T> List<T> mapValues(final Function<? super R, ? extends T> mapper) {
        return map.values().stream().map(mapper).collect(Collectors.toList());
    }
    
    /**
     * @deprecated use fromExisting(records, keyFunction)
     */
    @Deprecated
    public final RequireKeyFunction<R, K> fromExisting(final Collection<R> records) {
        return keyFunction -> fromExisting(records, keyFunction);
    }
    
    /**
     * @deprecated use fromExisting(records, keyFunction)
     */
    @Deprecated
    public final RequireKeyFunction<R, K> fromExisting(final Stream<R> records) {
        return keyFunction -> fromExisting(records, keyFunction);
    }
    
    /**
     * @deprecated do not create records to place in collection, use update() methods instead
     */
    @Deprecated
    public final RequireKeyFunction<R, K> fromNew(final Collection<R> records) {
        return keyFunction -> fromNew(records, keyFunction);
    }
    
    /**
     * @deprecated do not create records to place in collection, use update() methods instead
     */
    @Deprecated
    public final RequireKeyFunction<R, K> fromNew(final Stream<R> records) {
        return keyFunction -> fromNew(records, keyFunction);
    }
    
    private void fromNew(final Collection<R> records, final Function<R, K> keyFunction) {
        if (records != null) {
            fromNew(records.stream(), keyFunction);
        }
    }
    
    private void fromNew(final Stream<R> records, final Function<R, K> keyFunction) {
        if (records != null) {
            records.forEach(record -> {
                final K key = keyFunction.apply(record);
                this.map.put(key, record);
                this.added.put(key, record);
            });
        }
    }
    
    /**
     * @deprecated do not create records to place in collection, use update() methods instead
     */
    @Deprecated
    public final void fromNew(final Map<K, R> records) {
        if (records != null) {
            map.putAll(records);
            added.putAll(records);
        }
    }
    
    public final void fromExisting(final Collection<R> records, final Function<R, K> keyFunction) {
        if (records != null) {
            fromExisting(records.stream(), keyFunction);
        }
    }
    
    public final <T> void fromExisting(final Collection<T> records, final Function<T, R> recordFunction, final Function<R, K> keyFunction) {
        if (records != null) {
            fromExisting(records.stream(), recordFunction, keyFunction);
        }
    }
    
    public final void fromExisting(final Stream<R> records, final Function<R, K> keyFunction) {
        if (records != null) {
            records.forEach(record -> {
                final K key = keyFunction.apply(record);
                this.map.put(key, record);
            });
        }
    }
    
    public final <T> void fromExisting(final Stream<T> records, final Function<T, R> recordFunction, final Function<R, K> keyFunction) {
        if (records != null) {
            fromExisting(records.map(recordFunction), keyFunction);
        }
    }
    
    public final void fromExisting(final Map<K, R> records) {
        if (records != null) {
            map.putAll(records);
        }
    }
    
    public final <T> void fromExisting(final Map<K, T> records, final Function<T, R> recordFunction) {
        if (records != null) {
            records.forEach((k, t) -> map.put(k, recordFunction.apply(t)));
        }
    }
    
    /**
     * @deprecated use syncWith()
     */
    @Deprecated
    public final <T> ApplyFrom<T, K, R> applyFrom(final Collection<T> items) {
        return keyFunction -> modifyRecord -> syncWith(items, keyFunction, modifyRecord);
    }
    
    /**
     * @deprecated use syncWith()
     */
    @Deprecated
    public final <T> ApplyFrom<T, K, R> applyFrom(final Stream<T> items) {
        return keyFunction -> modifyRecord -> syncWith(items, keyFunction, modifyRecord);
    }
    
    /**
     * @deprecated use syncWith()
     */
    @Deprecated
    public final <T> ApplyFromMap<T, R> applyFrom(final Map<K, T> itemsMap) {
        return modifyRecord -> syncWith(itemsMap, modifyRecord);
    }
    
    public final void syncWith(final Collection<K> items) {
        syncWith(items, Function.identity(), null);
    }
    
    public final void syncWith(final Collection<K> items, final BiConsumer<R, K> modifyRecord) {
        syncWith(items, Function.identity(), modifyRecord);
    }
    
    public final <T> void syncWith(final Collection<T> items, final Function<T, K> keyFunction, final BiConsumer<R, T> modifyRecord) {
        if (items != null) {
            syncWith(items.stream(), keyFunction, modifyRecord);
        }
    }
    
    public final void syncWith(final Stream<K> items) {
        syncWith(items, Function.identity(), null);
    }
    
    public final void syncWith(final Stream<K> items, final BiConsumer<R, K> modifyRecord) {
        syncWith(items, Function.identity(), modifyRecord);
    }
    
    public final <T> void syncWith(final Stream<T> items, final Function<T, K> keyFunction, final BiConsumer<R, T> modifyRecord) {
        if (items != null) {
            // merge common items
            final Set<K> keys = internalUpdate(
                items,
                keyFunction,
                modifyRecord != null
                    ? (r, t) -> {
                        modifyRecord.accept(r, t);
                        return true;
                    }
                    : null);
            
            // remove extra items
            final HashSet<K> toRemove = new HashSet<>(map.keySet());
            toRemove.removeAll(keys);
            for (final K key : toRemove) {
                internalRemove(key);
            }
        }
    }
    
    public final <T> void syncWith(final Map<K, T> items) {
        syncWith(items, null);
    }
    
    public final <T> void syncWith(final Map<K, T> items, final BiConsumer<R, T> modifyRecord) {
        if (items != null) {
            syncWith(items.entrySet().stream(), Entry::getKey, modifyRecord != null ? (r, t) -> modifyRecord.accept(r, t.getValue()) : null);
        }
    }
    
    /**
     * @deprecated use update()
     */
    @Deprecated
    public final <T> ApplyFrom<T, K, R> addOrUpdate(final T item) {
        return keyFunction -> modifyRecord -> update(item, keyFunction, (r, t) -> {
            modifyRecord.accept(r, t);
            return true;
        });
    }
    
    /**
     * @deprecated use update()
     */
    @Deprecated
    public final <T> ApplyFrom<T, K, R> addOrUpdate(final Collection<T> items) {
        return keyFunction -> modifyRecord -> update(items, keyFunction, (r, t) -> {
            modifyRecord.accept(r, t);
            return true;
        });
    }
    
    /**
     * @deprecated use update()
     */
    @Deprecated
    public final <T> ApplyFrom<T, K, R> addOrUpdate(final Stream<T> items) {
        return keyFunction -> modifyRecord -> update(items, keyFunction, (r, t) -> {
            modifyRecord.accept(r, t);
            return true;
        });
    }
    
    /**
     * @deprecated use update()
     */
    @Deprecated
    public final <T> ApplyFromMap<T, R> addOrUpdate(final Map<K, T> itemsMap) {
        return modifyRecord -> update(itemsMap, (r, t) -> {
            modifyRecord.accept(r, t);
            return true;
        });
    }
    
    public final void update(final K key) {
        update(key, Function.identity(), null);
    }
    
    public final void update(final K key, final BiPredicate<R, K> modifyRecord) {
        update(key, Function.identity(), modifyRecord);
    }
    
    public final <T> void update(final T item, final Function<T, K> keyFunction, final BiPredicate<R, T> modifyRecord) {
        if (item != null) {
            update(Stream.of(item), keyFunction, modifyRecord);
        }
    }
    
    public final void update(final Collection<K> items) {
        update(items, Function.identity(), null);
    }
    
    public final void update(final Collection<K> items, final BiPredicate<R, K> modifyRecord) {
        update(items, Function.identity(), modifyRecord);
    }
    
    public final <T> void update(final Collection<T> items, final Function<T, K> keyFunction, final BiPredicate<R, T> modifyRecord) {
        if (items != null) {
            update(items.stream(), keyFunction, modifyRecord);
        }
    }
    
    public final void update(final Stream<K> items) {
        update(items, Function.identity(), null);
    }
    
    public final void update(final Stream<K> items, final BiPredicate<R, K> modifyRecord) {
        update(items, Function.identity(), modifyRecord);
    }
    
    public final <T> void update(final Stream<T> items, final Function<T, K> keyFunction, final BiPredicate<R, T> modifyRecord) {
        if (items != null) {
            internalUpdate(items, keyFunction, modifyRecord);
        }
    }
    
    public final <T> void update(final Map<K, T> items, final BiPredicate<R, T> modifyRecord) {
        if (items != null) {
            update(items.entrySet().stream(), (Function<Entry<K, T>, K>) Entry::getKey, modifyRecord != null ? (r, t) -> modifyRecord.test(r, t.getValue()) : null);
        }
    }
    
    public final void updateEach(final BiPredicate<R, K> modifyRecord) {
        final Iterator<Entry<K, R>> i = map.entrySet().iterator();
        while (i.hasNext()) {
            final Entry<K, R> entry = i.next();
            final K key = entry.getKey();
            final R record = entry.getValue();
            if (!added.containsKey(key)) {
                updated.put(key, record);
            }
            if ((modifyRecord != null) && !modifyRecord.test(record, key)) {
                i.remove();
                if (added.remove(key) == null) {
                    updated.remove(key);
                    removed.put(key, record);
                }
            }
        }
    }
    
    public final void remove(final K key) {
        remove(key, Function.identity());
    }
    
    public final <T> void remove(final T item, final Function<T, K> keyFunction) {
        if (item != null) {
            remove(Stream.of(item), keyFunction);
        }
    }
    
    public final void remove(final Collection<K> items) {
        remove(items, Function.identity());
    }
    
    public final <T> void remove(final Collection<T> items, final Function<T, K> keyFunction) {
        if (items != null) {
            remove(items.stream(), keyFunction);
        }
    }
    
    /**
     * used by lotus/impl/src/main/java/com/ixaris/support/lotus/impl/data/InstrumentEntity.java
     * TODO replace with commented method when this use is refactored
     */
    @Deprecated
    public final <T> RequireKeyFunction<T, K> remove(final Stream<T> items) {
        return keyFunction -> remove(items, keyFunction);
    }
    
    // public final void remove(final Stream<K> items) {
    // remove(items, Function.identity());
    // }
    
    public final <T> void remove(final Stream<T> items, final Function<T, K> keyFunction) {
        if (items != null) {
            internalRemove(items, keyFunction);
        }
    }
    
    public final void remove(final Map<K, ?> items) {
        if (items != null) {
            remove(items.keySet().stream(), Function.identity());
        }
    }
    
    /**
     * To be used if records are deleted (e.g. by a bulk delete statement)
     */
    public final void afterDelete() {
        map.clear();
        added.clear();
        updated.clear();
        removed.clear();
    }
    
    public final Map<K, R> getAdded() {
        return Collections.unmodifiableMap(added);
    }
    
    public final Map<K, R> getUpdated() {
        return Collections.unmodifiableMap(updated);
    }
    
    public final Map<K, R> getRemoved() {
        return Collections.unmodifiableMap(removed);
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
    
    private <T> Set<K> internalUpdate(final Stream<T> items, final Function<T, K> keyFunction, final BiPredicate<R, T> modifyRecord) {
        return items
            .map(item -> {
                final K key = keyFunction.apply(item);
                final R record = internalAddOrUpdate(key);
                if ((modifyRecord != null) && !modifyRecord.test(record, item)) {
                    internalRemove(key);
                }
                return key;
            })
            .collect(Collectors.toSet());
    }
    
    private <T> void internalRemove(final Stream<T> items, final Function<T, K> keyFunction) {
        items.forEach(item -> internalRemove(keyFunction.apply(item)));
    }
    
    private R internalAddOrUpdate(final K key) {
        R record = map.get(key);
        if (record == null) {
            record = removed.remove(key);
            if (record == null) {
                // new record
                record = newRecordFunction.apply(key);
                added.put(key, record);
            } else {
                // existing record deleted and readded
                updated.put(key, record);
            }
            map.put(key, record);
        } else if (!added.containsKey(key)) {
            // record added but not yet stored
            updated.put(key, record);
        }
        return record;
    }
    
    private void internalRemove(final K key) {
        final R record = map.remove(key);
        if ((record != null) && (added.remove(key) == null)) {
            // if not a new record, add it to removed set for DELETE,
            // otherwise just remove from added set so INSERT is not done
            updated.remove(key);
            removed.put(key, record);
        }
    }
    
}
