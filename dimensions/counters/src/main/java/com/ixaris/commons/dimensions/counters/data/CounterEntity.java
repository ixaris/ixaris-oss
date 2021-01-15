/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.counters.data;

import static com.ixaris.commons.dimensions.counters.jooq.tables.LibDimCounter.LIB_DIM_COUNTER;
import static com.ixaris.commons.dimensions.counters.jooq.tables.LibDimCounterNarrow.LIB_DIM_COUNTER_NARROW;
import static com.ixaris.commons.jooq.persistence.TransactionalDSLContext.JOOQ_TX;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.List;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.SelectOnConditionStep;
import org.jooq.Table;
import org.jooq.UpdatableRecord;

import com.google.common.collect.ImmutableBiMap;

import com.ixaris.commons.dimensions.counters.CounterDef;
import com.ixaris.commons.dimensions.counters.CounterValue;
import com.ixaris.commons.dimensions.counters.WindowTimeUnit;
import com.ixaris.commons.dimensions.counters.WindowValue;
import com.ixaris.commons.dimensions.counters.WindowWidth;
import com.ixaris.commons.dimensions.counters.jooq.tables.records.LibDimCounterNarrowRecord;
import com.ixaris.commons.dimensions.counters.jooq.tables.records.LibDimCounterRecord;
import com.ixaris.commons.dimensions.lib.context.Context;
import com.ixaris.commons.dimensions.lib.context.Dimension;
import com.ixaris.commons.dimensions.lib.context.DimensionDef;
import com.ixaris.commons.dimensions.lib.context.PersistedDimensionValue;
import com.ixaris.commons.dimensions.lib.context.hierarchy.HierarchyNode;
import com.ixaris.commons.dimensions.lib.dimension.StringHierarchyDimensionDef;
import com.ixaris.commons.jooq.persistence.Entity;
import com.ixaris.commons.jooq.persistence.RecordMap;
import com.ixaris.commons.misc.lib.conversion.SnakeCaseHelper;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;
import com.ixaris.commons.persistence.lib.mapper.EnumCodeMapper;

/**
 * Stores counter wide & narrow window definitions, and the wide window cumulative values (count/sum). Each counter definition (EventDef) should
 * extend this entity and add the dimensions as columns.
 *
 * <p>Dimensions should be added in the following way: - column name should be equal to the dimension def class without "DimensionDef" and with
 * snake case e.g. CardBrandDimensionDef becomes card_brand - Use basic types INT, BIGINT, CHAR, VARCHAR - Use the shortest string length
 * required, e.g. when storing a StringEnumHolder with code of 3 characters, use char(3)
 *
 * @author <a href="mailto:andrew.calafato@ixaris.com">Andrew Calafato</a>
 */
public final class CounterEntity<R extends UpdatableRecord<R>, C extends CounterDef<R, C>> extends Entity<CounterEntity<R, C>> {
    
    public static final EnumCodeMapper<WindowTimeUnit, Character> WINDOW_TIME_UNIT_MAPPING = EnumCodeMapper.with(ImmutableBiMap
        .<WindowTimeUnit, Character>builder()
        .put(WindowTimeUnit.MINUTE, 'N')
        .put(WindowTimeUnit.HOUR, 'H')
        .put(WindowTimeUnit.DAY, 'D')
        .put(WindowTimeUnit.WEEK, 'W')
        .put(WindowTimeUnit.MONTH, 'M')
        .put(WindowTimeUnit.YEAR, 'Y')
        .put(WindowTimeUnit.ALWAYS, 'A')
        .build());
    
    public static <R extends UpdatableRecord<R>, C extends CounterDef<R, C>> CounterEntity<R, C> lookup(final C def, final long id) {
        final Table<R> contextTable = def.getContextTable();
        return JOOQ_TX.get()
            .select()
            .from(LIB_DIM_COUNTER)
            .join(contextTable)
            .on(contextTable.field("id", Long.class).eq(LIB_DIM_COUNTER.ID))
            .where(LIB_DIM_COUNTER.ID.eq(id))
            .fetchOne()
            .map(r -> mapToCounterEntity(r, def, contextTable));
    }
    
    /**
     * Will get the counter from db, which exactly matches the given context instance.
     *
     * <p>We assume that concrete Counters have dimension names matching fields.
     *
     * @param context the context instance
     * @param narrowWindowWidth The width of the narrow window
     * @param wideWindowMultiple Wide window width as a multiple of narrowWindowWidth (Wide window width is effectively narrowWindowWidth *
     *     wideWindowMultiple)
     * @return the exact match cached counter, or null if none found
     */
    public static <R extends UpdatableRecord<R>, C extends CounterDef<R, C>> CounterEntity<R, C> lookup(final Context<C> context,
                                                                                                        final WindowWidth narrowWindowWidth,
                                                                                                        final int wideWindowMultiple) {
        final C def = context.getDef();
        final Table<R> contextTable = def.getContextTable();
        
        final SelectOnConditionStep<Record> select = JOOQ_TX.get()
            .select()
            .from(LIB_DIM_COUNTER)
            .join(contextTable)
            .on(contextTable.field("id", Long.class).eq(LIB_DIM_COUNTER.ID));
        
        final List<Condition> allCond = new ArrayList<>(def.getContextDef().size() + 3);
        allCond.add(LIB_DIM_COUNTER.NARROW_WINDOW_WIDTH.eq(narrowWindowWidth.getWidth()));
        allCond.add(LIB_DIM_COUNTER.NARROW_WINDOW_UNIT.eq(WINDOW_TIME_UNIT_MAPPING.codify(narrowWindowWidth.getUnit())));
        allCond.add(LIB_DIM_COUNTER.WIDE_WINDOW_MULTIPLE.eq(wideWindowMultiple));
        
        // build the query according to dimension importance order
        for (DimensionDef<?> dimensionDef : def.getContextDef()) {
            final Dimension<?> dimension = context.getDimension(dimensionDef);
            
            // convert the dimension name from SomeDimension to some_dimension
            final String dimensionField = SnakeCaseHelper.camelToSnakeCase(dimensionDef.getKey());
            
            if (dimension != null) {
                // if this dimension is defined, add an equality clause
                final PersistedDimensionValue persistedValue = dimension.getPersistedValue();
                if (persistedValue.getStringValue() != null) {
                    final Field<String> longValueField = contextTable.field(dimensionField, String.class);
                    allCond.add(longValueField.eq(persistedValue.getStringValue()));
                } else {
                    final Field<Long> longValueField = contextTable.field(dimensionField, Long.class);
                    allCond.add(longValueField.eq(persistedValue.getLongValue()));
                }
            } else {
                // otherwise, add a clause that this dimension has to be null
                final Field<?> longValueField = contextTable.field(dimensionField);
                allCond.add(longValueField.isNull());
            }
        }
        
        return select.where(allCond).fetchOptional().map(r -> mapToCounterEntity(r, def, contextTable)).orElse(null);
    }
    
    /**
     * @param context
     * @param partitionDimensionDef the partition dimension being used
     * @return counters matching the given the context
     */
    public static <R extends UpdatableRecord<R>, C extends CounterDef<R, C>> List<CounterEntity<R, C>> lookupMatching(final Context<C> context,
                                                                                                                      final DimensionDef<?> partitionDimensionDef) {
        final C def = context.getDef();
        final Table<R> contextTable = def.getContextTable();
        
        /*
         * For performance reasons we union the cartesian product of combinations for the dimensions whose set of possible value is large.
         *
         * So instead of sending:
         * SELECT * from counters WHERE (dim1 = A OR dim1 IS NULL) AND (dim2 = A OR dim2 IS NULL) ...
         *
         * we send:
         * SELECT * from counters WHERE dim1 = A AND dim2 = A
         * UNION ALL
         * SELECT * from counters WHERE dim1 = A AND dim2 IS NULL
         * UNION ALL
         * SELECT * from counters WHERE dim1 IS NULL AND dim2 = A
         * UNION ALL
         * SELECT * from counters WHERE dim1 IS NULL AND dim2 IS NULL
         *
         * This allows more efficient use of database compound indexes.
         */
        final List<List<Condition>> queriesToUnion = new ArrayList<>();
        queriesToUnion.add(new ArrayList<>(def.getPartitionDimensionsCount()));
        final List<Condition> allCond = new ArrayList<>(def.getContextDef().size());
        final int cartesianProdSize = def.getPartitionDimensionsCount() + def.getCartesianProductDimensionsCount();
        final Iterator<DimensionDef<?>> iteratorByImportance = def.getContextDef().iterator();
        boolean partitionDimensionFound = partitionDimensionDef == null;
        
        // build the query according to dimension importance order
        for (int i = 0; iteratorByImportance.hasNext(); i++) {
            final DimensionDef<?> dimensionDef = iteratorByImportance.next();
            
            if (i < cartesianProdSize) {
                if (!partitionDimensionFound && (i < def.getPartitionDimensionsCount())) {
                    if (dimensionDef.equals(partitionDimensionDef)) {
                        partitionDimensionFound = true;
                        // given partition dimension should match exactly, hence checkValue = true, checkNull = false
                        addDimension(context, dimensionDef, contextTable, allCond, true, false);
                    } else {
                        // all partition dimensions before the given one should be null, hence checkValue = false,
                        // checkNull = true
                        addDimension(context, dimensionDef, contextTable, allCond, false, true);
                    }
                } else {
                    // create cartesian product: double the queries to union
                    int size = queriesToUnion.size();
                    int j = 0;
                    for (final Iterator<List<Condition>> iterator = queriesToUnion.iterator(); j < size; j++) {
                        final List<Condition> nextWithValue = iterator.next();
                        final List<Condition> nextWithNull = new ArrayList<>(def.getPartitionDimensionsCount());
                        nextWithNull.addAll(nextWithValue);
                        queriesToUnion.add(nextWithNull);
                        addDimension(context, dimensionDef, contextTable, nextWithValue, true, false);
                        addDimension(context, dimensionDef, contextTable, nextWithNull, false, true);
                    }
                }
            } else {
                addDimension(context, dimensionDef, contextTable, allCond, true, true);
            }
        }
        
        final List<CounterEntity<R, C>> result = new ArrayList<>();
        // SelectOrderByStep<Record> fullQuery = null;
        for (final List<Condition> conditions : queriesToUnion) {
            conditions.addAll(allCond);
            final SelectConditionStep<Record> query = JOOQ_TX.get()
                .select()
                .from(LIB_DIM_COUNTER)
                .join(contextTable)
                .on(contextTable.field("id", Long.class).eq(LIB_DIM_COUNTER.ID))
                .where(conditions);
            result.addAll(query.fetch().map(r -> mapToCounterEntity(r, def, contextTable)));
            // disabled union all due to jooq issue https://github.com/jOOQ/jOOQ/issues/4856
            // fullQuery = fullQuery == null ? query : fullQuery.unionAll(query);
        }
        
        return result;
        
        // return fullQuery
        // .fetch()
        // .map(r -> mapToCounterEntity(r, def, contextTable));
    }
    
    private static <T, R extends UpdatableRecord<R>> void addDimension(final Context<?> context,
                                                                       final DimensionDef<T> dimensionDef,
                                                                       final Table<R> contextTable,
                                                                       final List<Condition> allCond,
                                                                       final boolean checkValue,
                                                                       final boolean checkNull) {
        final Dimension<T> dimension = context.getDimension(dimensionDef);
        // convert the dimension name from SomeDimension to some_dimension
        final String dimensionField = SnakeCaseHelper.camelToSnakeCase(dimensionDef.getKey());
        
        if (dimension != null) {
            final PersistedDimensionValue persistedValue = dimension.getPersistedValue();
            
            switch (dimensionDef.getValueMatch()) {
                case LONG_RANGE:
                case LONG:
                    final Field<Long> longValueField = contextTable.field(dimensionField, Long.class);
                    if (!checkValue) {
                        allCond.add(longValueField.isNull());
                    } else if (!checkNull) {
                        allCond.add(longValueField.eq(persistedValue.getLongValue()));
                    } else {
                        allCond.add(longValueField.eq(persistedValue.getLongValue()).or(longValueField.isNull()));
                    }
                    break;
                case STRING_HIERARCHY:
                    final Field<String> stringHierarchyValueField = contextTable.field(dimensionField, String.class);
                    final StringHierarchyDimensionDef<T> hierarchyDimensionDef = (StringHierarchyDimensionDef<T>) dimensionDef;
                    if (!checkValue) {
                        allCond.add(stringHierarchyValueField.isNull());
                    } else {
                        Condition stringCondition = stringHierarchyValueField.eq(persistedValue.getStringValue());
                        if ((dimension.getValue() != null) && hierarchyDimensionDef.isHierarchical()) {
                            HierarchyNode<?> parent = ((HierarchyNode<?>) dimension.getValue()).getParent();
                            while (parent != null) {
                                stringCondition = stringCondition.or(stringHierarchyValueField.eq(parent.getKey()));
                                parent = parent.getParent();
                            }
                        }
                        if (checkNull) {
                            stringCondition = stringCondition.or(stringHierarchyValueField.isNull());
                        }
                        allCond.add(stringCondition);
                    }
                    break;
                case STRING:
                    final Field<String> stringValueField = contextTable.field(dimensionField, String.class);
                    if (!checkValue) {
                        allCond.add(stringValueField.isNull());
                    } else if (!checkNull) {
                        allCond.add(stringValueField.eq(persistedValue.getStringValue()));
                    } else {
                        allCond.add(stringValueField.eq(persistedValue.getStringValue()).or(stringValueField.isNull()));
                    }
                    break;
            }
            
        } else {
            // otherwise, add a clause that this dimension has to be null
            final Field<?> longValueField = contextTable.field(dimensionField);
            allCond.add(longValueField.isNull());
        }
    }
    
    private static <R extends UpdatableRecord<R>, C extends CounterDef<R, C>> CounterEntity<R, C> mapToCounterEntity(final Record r, final C def, final Table<R> contextTable) {
        final LibDimCounterRecord counter = r.into(LIB_DIM_COUNTER);
        final R context = r.into(contextTable);
        final Result<LibDimCounterNarrowRecord> narrowWindows = JOOQ_TX.get().fetch(
            LIB_DIM_COUNTER_NARROW, LIB_DIM_COUNTER_NARROW.COUNTER_ID.eq(counter.getId()));
        return new CounterEntity<>(def, counter, context, narrowWindows);
    }
    
    private static long getWideWindowStartTime(final WindowWidth narrowWindowWidth, final int wideWindowMultiple, final long timestamp) {
        if (wideWindowMultiple == 1) {
            // start of narrow == start of wide
            return narrowWindowWidth.getStartTimestampForWindowNumber(narrowWindowWidth.getWindowNumber(timestamp));
        } else {
            // start of currentNarrowWindowNumber - wideWindowMultiple + 1
            return narrowWindowWidth.getStartTimestampForWindowNumber(
                narrowWindowWidth.getWindowNumber(timestamp) - wideWindowMultiple + 1);
        }
    }
    
    private static LibDimCounterRecord newCounterRecord() {
        final LibDimCounterRecord record = new LibDimCounterRecord();
        record.setId(UniqueIdGenerator.generate());
        return record;
    }
    
    private final Context<C> context;
    private final LibDimCounterRecord counter;
    private final R contextRecord;
    private RecordMap<Long, LibDimCounterNarrowRecord> narrowMap;
    private boolean contextStored;
    
    public CounterEntity(final Context<C> context,
                         final WindowWidth narrowWindowWidth,
                         final int wideWindowMultiple,
                         final WindowValue wideValue,
                         final WindowValue narrowValue,
                         final WindowValue narrowValueLastFull,
                         final long timestamp,
                         final int shard) {
        this(context, newCounterRecord());
        counter.setNarrowWindowWidth(narrowWindowWidth.getWidth());
        counter.setNarrowWindowUnit(WINDOW_TIME_UNIT_MAPPING.codify(narrowWindowWidth.getUnit()));
        counter.setWideWindowMultiple(wideWindowMultiple);
        counter.setLastQueried(timestamp);
        counter.setQueriedUpdatedDiff(0L);
        counter.setStartNarrowWindowNumber(narrowWindowWidth.getWindowNumber(timestamp) - wideWindowMultiple + 1);
        counter.setCount(wideValue.getCount());
        counter.setSum(wideValue.getSum());
        counter.setShard(shard);
        
        if (wideWindowMultiple != 1) {
            final long currentNarrowWindowNumber = narrowWindowWidth.getWindowNumber(timestamp);
            narrowMap.update(currentNarrowWindowNumber, (r, t) -> {
                r.setCount(narrowValue.getCount());
                r.setSum(narrowValue.getSum());
                return true;
            });
            final long lastNarrowWindowNumber = currentNarrowWindowNumber - 1;
            narrowMap.update(lastNarrowWindowNumber, (r, t) -> {
                r.setCount(narrowValueLastFull.getCount());
                r.setSum(narrowValueLastFull.getSum());
                return true;
            });
        }
    }
    
    public CounterEntity(final C def,
                         final LibDimCounterRecord counter,
                         final R contextRecord,
                         final Collection<LibDimCounterNarrowRecord> narrowRecords) {
        this(def.extractContextInstance(contextRecord), counter, contextRecord);
        narrowMap.fromExisting(narrowRecords, LibDimCounterNarrowRecord::getWindowNumber);
        contextStored = true;
    }
    
    private CounterEntity(final Context<C> context, final LibDimCounterRecord counter) {
        this(context, counter, context.getDef().newContextRecord(counter.getId(), context));
    }
    
    private CounterEntity(final Context<C> context, final LibDimCounterRecord counter, final R contextRecord) {
        this.context = context;
        this.counter = counter;
        this.contextRecord = contextRecord;
        narrowMap = RecordMap.withMapSupplierAndNewRecordFunction(LinkedHashMap::new, i -> new LibDimCounterNarrowRecord(UniqueIdGenerator.generate(), this.counter.getId(), i, 0L, 0L));
        contextStored = false;
    }
    
    public Context<C> getContext() {
        return context;
    }
    
    @Override
    public CounterEntity<R, C> store() {
        attachAndStore(counter);
        if (!contextStored) {
            attachAndStore(contextRecord);
            contextStored = true;
        }
        narrowMap.store();
        return this;
    }
    
    @Override
    public CounterEntity<R, C> delete() {
        attachAndDelete(contextRecord);
        JOOQ_TX.get().delete(LIB_DIM_COUNTER_NARROW).where(LIB_DIM_COUNTER_NARROW.COUNTER_ID.eq(counter.getId())).execute();
        attachAndDelete(counter);
        return this;
    }
    
    public LibDimCounterRecord getCounter() {
        return counter;
    }
    
    public final WindowValue getWideValue() {
        return new WindowValue(counter.getCount(), counter.getSum());
    }
    
    /**
     * Returns count/sum value of current narrow window. Returns zero values if narrow window does not exist.
     */
    public final WindowValue getNarrowValue() {
        if (counter.getWideWindowMultiple() == 1) {
            return getWideValue();
        }
        final LibDimCounterNarrowRecord record = narrowMap.get(getNarrowWindowWidth().getWindowNumber(System.currentTimeMillis()));
        return record == null ? new WindowValue(0L, 0L) : new WindowValue(record.getCount(), record.getSum());
    }
    
    /**
     * Returns count/sum value of last full narrow window. Returns zero values if narrow window does not exist.
     */
    public final WindowValue getNarrowValueLastFull() {
        if (counter.getWideWindowMultiple() == 1) {
            throw new IllegalStateException("Only 1 window is maintained for counter with wideWindowMultiple = 1. Cannot retreive LastFullNarrow window counter.");
        }
        final LibDimCounterNarrowRecord record = narrowMap.get(getNarrowWindowWidth().getWindowNumber(System.currentTimeMillis()) - 1);
        return record == null ? new WindowValue(0L, 0L) : new WindowValue(record.getCount(), record.getSum());
    }
    
    /**
     * Returns Wide & Narrow windows count & sum values. Use current narrow window.
     */
    public final CounterValue getWideAndNarrowValue() {
        return new CounterValue(getWideValue(), getNarrowValue());
    }
    
    /**
     * Returns Wide & Narrow windows count & sum values. Use last full narrow window.
     */
    public final CounterValue getWideAndNarrowValueLastFull() {
        if (counter.getWideWindowMultiple() == 1) {
            throw new IllegalStateException("Only 1 window is maintained for counter with wideWindowMultiple = 1. Cannot retreive LastFullNarrow window counter.");
        }
        return new CounterValue(getWideValue(), getNarrowValueLastFull());
    }
    
    public WindowWidth getNarrowWindowWidth() {
        return new WindowWidth(counter.getNarrowWindowWidth(), WINDOW_TIME_UNIT_MAPPING.resolve(counter.getNarrowWindowUnit()));
    }
    
    public int getWideWindowMultiple() {
        return counter.getWideWindowMultiple();
    }
    
    /**
     * Add a delta to a counter, updating the timestamp's narrow/wide windows. The count is incremented by 1. Ignores update if timestamp is
     * before wide window scope.
     *
     * @param delta The amount to add to counter sum.
     * @param timestamp The timestamp of the event to reflect the narrow window to increment.
     */
    public final void increment(final long delta, final long timestamp) {
        if (timestamp > System.currentTimeMillis()) {
            throw new IllegalArgumentException("Timestamp cannot be in the future: " + timestamp);
        }
        final WindowWidth narrowWindowWidth = getNarrowWindowWidth();
        if (timestamp < getWideWindowStartTime(narrowWindowWidth, counter.getWideWindowMultiple(), System.currentTimeMillis())) {
            // do nothing if timestamp is before wide window scope.
            return;
        }
        
        // if narrow == wide window and delta falls in a new window, we need to start a fresh wide window
        if (counter.getWideWindowMultiple() == 1) {
            dropExpiredWindows(timestamp);
        }
        
        counter.setCount(counter.getCount() + 1);
        counter.setSum(counter.getSum() + delta);
        
        // if narrow == wide window, we do NOT keep/update narrow windows.
        if (counter.getWideWindowMultiple() != 1) {
            final long narrowWindowNumber = getNarrowWindowWidth().getWindowNumber(timestamp);
            narrowMap.update(narrowWindowNumber, (record, d) -> {
                record.setCount(record.getCount() + 1);
                record.setSum(record.getSum() + delta);
                return true;
            });
        }
    }
    
    /**
     * Decrement the counter sum, typically reversing a previous increment, updating the timestamp's narrow/wide windows. The count is
     * decremented by 1. Ignores update if timestamp is before wide window scope.
     *
     * @param delta The amount to subtract from counter sum.
     * @param timestamp The timestamp of the event to reflect the narrow window to decrement.
     */
    public final void decrement(final long delta, final long timestamp) {
        if (timestamp > System.currentTimeMillis()) {
            throw new IllegalArgumentException("Timestamp cannot be in the future.");
        }
        final WindowWidth narrowWindowWidth = getNarrowWindowWidth();
        if (timestamp < getWideWindowStartTime(narrowWindowWidth, counter.getWideWindowMultiple(), System.currentTimeMillis())) {
            // do nothing if timestamp is before wide window scope.
            return;
        }
        if (counter.getCount() == 0) {
            throw new IllegalStateException("Cannot reverse a counter wide window with count 0");
        }
        
        counter.setCount(counter.getCount() - 1);
        counter.setSum(counter.getSum() - delta);
        
        // if narrow == wide window, we do NOT keep/update a narrow Window.
        if (counter.getWideWindowMultiple() > 1) {
            final long narrowWindowNumber = narrowWindowWidth.getWindowNumber(timestamp);
            narrowMap.update(narrowWindowNumber, (record, d) -> {
                if (record.getCount() == 0) {
                    throw new IllegalStateException("Cannot reverse - narrow window does not exist");
                } else {
                    record.setCount(record.getCount() - 1);
                    record.setSum(record.getSum() - delta);
                }
                return true;
            });
        }
    }
    
    /**
     * Drops any Expired windows.
     *
     * @return True if some windows were dropped. False otherwise.
     */
    public boolean dropExpiredWindows() {
        return dropExpiredWindows(System.currentTimeMillis());
    }
    
    /**
     * Drops any Expired windows.
     *
     * @param timestamp
     * @return True if some windows were dropped. False otherwise.
     */
    private boolean dropExpiredWindows(final long timestamp) {
        final WindowWidth narrowWindowWidth = getNarrowWindowWidth();
        if (counter.getWideWindowMultiple() == 1) {
            // if narrow == wide window then we only have 1 window - the wide one
            // start a fresh wide window if old one 'expired'
            final long newWindowNumber = getNarrowWindowWidth().getWindowNumber(timestamp);
            if (newWindowNumber > counter.getStartNarrowWindowNumber()) {
                counter.setStartNarrowWindowNumber(newWindowNumber);
                counter.setCount(0L);
                counter.setSum(0L);
                return true;
            } else {
                return false;
            }
            
        } else {
            // drop narrow windows between (and including) last recorded startNarrowWindowNumber and
            // lastExpiredWindowNumber
            // most recent expired window is current window number - wideWindowMultiple.
            final long lastExpiredWindowNumber = getNarrowWindowWidth().getWindowNumber(timestamp) - counter.getWideWindowMultiple();
            if (lastExpiredWindowNumber < counter.getStartNarrowWindowNumber()) {
                return false; // nothing to drop
            }
            
            // drop narrow windows
            boolean foundNarrowWindow = false; // if this flag becomes true, it indicates that subsequent null narrow windows
            // are 0
            for (long windowNumber = counter.getStartNarrowWindowNumber(); windowNumber <= lastExpiredWindowNumber; windowNumber++) {
                final LibDimCounterNarrowRecord narrowRecord = narrowMap.get(windowNumber);
                if (narrowRecord == null) {
                    if (!foundNarrowWindow) {
                        final WindowValue wideValue = context
                            .getDef()
                            .fetchWindow(context,
                                narrowWindowWidth.getStartTimestampForWindowNumber(windowNumber),
                                narrowWindowWidth.getStartTimestampForWindowNumber(windowNumber + 1));
                        counter.setCount(counter.getCount() - wideValue.getCount());
                        counter.setSum(counter.getSum() - wideValue.getSum());
                    } // else the narrow window must be 0
                } else {
                    counter.setCount(counter.getCount() - narrowRecord.getCount());
                    counter.setSum(counter.getSum() - narrowRecord.getSum());
                    narrowMap.remove(windowNumber);
                    foundNarrowWindow = true;
                }
            }
            
            final long startNarrowWindowNumber = lastExpiredWindowNumber + 1;
            counter.setStartNarrowWindowNumber(startNarrowWindowNumber);
            if (foundNarrowWindow && !narrowMap.containsKey(startNarrowWindowNumber)) {
                // if this narrow window is found in db and next narrow window is not there - we create a ZERO-value
                // window, to avoid asking counter definition to calculate values. This is a normal scenario where no
                // events occurred in the next narrow window (the one being created)
                narrowMap.update(startNarrowWindowNumber);
            }
            
            return true;
        }
    }
}
