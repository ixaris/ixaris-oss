package com.ixaris.commons.dimensions.lib.base;

import static com.ixaris.commons.dimensions.lib.dimension.LongDimensionDef.MATCH_ANY;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

import org.jooq.Condition;
import org.jooq.Field;
import org.jooq.Record;
import org.jooq.Result;
import org.jooq.SelectConditionStep;
import org.jooq.SelectJoinStep;
import org.jooq.SelectOnConditionStep;
import org.jooq.SelectSeekStepN;
import org.jooq.SortField;
import org.jooq.Table;

import com.ixaris.commons.dimensions.lib.context.Context;
import com.ixaris.commons.dimensions.lib.context.Dimension;
import com.ixaris.commons.dimensions.lib.context.DimensionDef;
import com.ixaris.commons.dimensions.lib.context.PersistedDimensionValue;
import com.ixaris.commons.dimensions.lib.context.hierarchy.HierarchyChildNodeSet;
import com.ixaris.commons.dimensions.lib.context.hierarchy.HierarchyNode;
import com.ixaris.commons.dimensions.lib.context.hierarchy.HierarchySubtreeNodePrefix;
import com.ixaris.commons.dimensions.lib.context.hierarchy.HierarchySubtreeNodes;
import com.ixaris.commons.dimensions.lib.dimension.LongDimensionDef;
import com.ixaris.commons.dimensions.lib.dimension.StringHierarchyDimensionDef;

public final class DimensionalHelper {
    
    public static final class RecordHolder<MAIN_REC extends Record, DIM_REC extends Record> {
        
        private final MAIN_REC mainRecord;
        private final List<DIM_REC> dimensionRecords;
        private final Record record;
        
        public RecordHolder(final MAIN_REC mainRecord, final List<DIM_REC> dimensionRecords, final Record record) {
            this.mainRecord = mainRecord;
            this.dimensionRecords = dimensionRecords;
            this.record = record;
        }
        
        public MAIN_REC getMainRecord() {
            return mainRecord;
        }
        
        public List<DIM_REC> getDimensionRecords() {
            return dimensionRecords;
        }
        
        public Record getRecord() {
            return record;
        }
        
    }
    
    public static void validateForQuery(final Context<?> context) {
        if (context == null) {
            throw new IllegalArgumentException("context is null");
        }
        if (!context.isValidForQuery()) {
            throw new IllegalArgumentException("Context [" + context + "] is not supported by [" + context.getDef() + "] for query");
        }
    }
    
    public static void validateForConfigLookup(final Context<?> context) {
        if (context == null) {
            throw new IllegalArgumentException("context is null");
        }
        if (!context.isValidForConfig(true)) {
            throw new IllegalArgumentException("Context [" + context + "] is not supported by [" + context.getDef() + "] for config lookup");
        }
    }
    
    public static void validateForConfig(final Context<?> context) {
        if (context == null) {
            throw new IllegalArgumentException("context is null");
        }
        if (!context.isValidForConfig(false)) {
            throw new IllegalArgumentException("Context [" + context + "] is not supported by [" + context.getDef() + "] for config");
        }
    }
    
    public static <MAIN_REC extends Record, DIM_REC extends Record, DEF extends DimensionalDef> RecordHolder<MAIN_REC, DIM_REC> fetch(final DEF def,
                                                                                                                                      final Table<MAIN_REC> mainTable,
                                                                                                                                      final Table<DIM_REC> dimensionTable,
                                                                                                                                      final SelectJoinStep<Record> select,
                                                                                                                                      final long id) {
        final Field<Long> idField = mainTable.field("id", Long.class);
        
        SelectJoinStep<Record> join = select;
        final Iterator<DimensionDef<?>> iteratorByImportance = def.getContextDef().iterator();
        // build the query according to dimension importance order
        for (int i = 0; iteratorByImportance.hasNext(); i++) {
            final DimensionDef<?> dimensionDef = iteratorByImportance.next();
            final Table<?> aliasedDimensionTable = dimensionTable.as("cbd" + i);
            join = join
                .leftJoin(aliasedDimensionTable)
                .on(idField.eq(aliasedDimensionTable.field("id", Long.class)),
                    aliasedDimensionTable.field("dimension_name", String.class).eq(dimensionDef.getKey()));
        }
        
        final SelectConditionStep<Record> query = join.where(idField.eq(id));
        
        final Record fetch = query.fetchOne();
        
        return fetch.map(r -> {
            final MAIN_REC main = r.into(mainTable);
            final List<DIM_REC> dimensions = new ArrayList<>(def.getContextDef().size());
            final Iterator<DimensionDef<?>> ii = def.getContextDef().iterator();
            for (int i = 0; ii.hasNext(); i++) {
                ii.next();
                dimensions.add(r.into(dimensionTable.as("cbd" + i)));
            }
            return new RecordHolder<>(main, dimensions, r);
        });
    }
    
    /**
     * (best) match criteria
     *
     * @param context the context
     * @param mostSpecific
     * @param allowExactContextMatch
     */
    public static <MAIN_REC extends Record, DIM_REC extends Record, DEF extends DimensionalDef> List<RecordHolder<MAIN_REC, DIM_REC>> match(final Context<DEF> context,
                                                                                                                                            final boolean mostSpecific,
                                                                                                                                            final boolean allowExactContextMatch,
                                                                                                                                            final Table<MAIN_REC> mainTable,
                                                                                                                                            final Table<DIM_REC> dimensionTable,
                                                                                                                                            final SelectJoinStep<Record> select,
                                                                                                                                            final Condition condition,
                                                                                                                                            final SortField<?>... orderBy) {
        /*
         * The query is built as follows:
         * Q1 SELECT cb FROM ContextValueEntity/Set cb
         * Q1 LEFT OUTER JOIN cb.context cbd0 WITH cbd0.dimensionName='A'
         * Q1 LEFT OUTER JOIN cb.context cbd1 WITH cbd1.dimensionName='DEF'
         * Q1 LEFT OUTER JOIN cb.context cbd2 WITH cbd2.dimensionName='C'
         * Q2 WHERE cb.key='Prop' AND cb.contextHash < / <= X (!allowExactContextMatch / allowExactContextMatch)
         * Q2 AND (cbd0.value='VA' OR cbd0.value IS NULL)
         * Q2 AND (cbd1.value='VB' OR cbd1.value IS NULL)
         * Q2 AND (cbd2.value='VC' OR cbd2.value IS NULL)
         * Q3 ORDER BY cb.contextHash DESC
         */
        final DEF def = context.getDef();
        final Field<Long> idField = mainTable.field("id", Long.class);
        final Field<Long> contextDepthField = mainTable.field("context_depth", Long.class);
        
        final List<Condition> allCond = new ArrayList<>(def.getContextDef().size() + 2);
        allCond.add(condition);
        if (allowExactContextMatch) {
            allCond.add(contextDepthField.le(context.getDepth()));
        } else {
            allCond.add(contextDepthField.lt(context.getDepth()));
        }
        
        final List<SortField<?>> allOrderBy = new ArrayList<>(def.getContextDef().size() + (((orderBy == null) || (orderBy.length == 0)) ? 1 : orderBy.length));
        SelectJoinStep<Record> join = select;
        final Iterator<DimensionDef<?>> iteratorByImportance = def.getContextDef().iterator();
        // buildList the query according to dimension importance order
        for (int i = 0; iteratorByImportance.hasNext(); i++) {
            join = joinDimensionForMatch(iteratorByImportance.next(), context, i, dimensionTable, idField, select, allCond, allOrderBy);
        }
        
        if ((orderBy == null) || (orderBy.length == 0)) {
            allOrderBy.add(contextDepthField.desc());
        } else {
            allOrderBy.addAll(Arrays.asList(orderBy));
        }
        
        final SelectSeekStepN<Record> query = join.where(allCond).orderBy(allOrderBy);
        
        final Result<Record> fetch;
        if (mostSpecific) {
            fetch = query.limit(1).fetch();
        } else {
            fetch = query.fetch();
        }
        
        return fetch.map(r -> {
            final MAIN_REC main = r.into(mainTable);
            final List<DIM_REC> dimensions = new ArrayList<>(def.getContextDef().size());
            final Iterator<DimensionDef<?>> ii = def.getContextDef().iterator();
            for (int i = 0; ii.hasNext(); i++) {
                ii.next();
                dimensions.add(r.into(dimensionTable.as("cbd" + i)));
            }
            return new RecordHolder<>(main, dimensions, r);
        });
    }
    
    @SuppressWarnings("unchecked")
    private static <T> SelectJoinStep<Record> joinDimensionForMatch(final DimensionDef<T> dimensionDef,
                                                                    final Context<?> context,
                                                                    final int i,
                                                                    final Table<?> dimensionTable,
                                                                    final Field<Long> idField,
                                                                    final SelectJoinStep<Record> select,
                                                                    final List<Condition> allCond,
                                                                    final List<SortField<?>> allOrderBy) {
        
        final Dimension<T> dimension = context.getDimension(dimensionDef);
        
        final Table<?> aliasedDimensionTable = dimensionTable.as("cbd" + i);
        final SelectOnConditionStep<Record> join = select
            .leftJoin(aliasedDimensionTable)
            .on(idField.eq(aliasedDimensionTable.field("id", Long.class)),
                aliasedDimensionTable.field("dimension_name", String.class).eq(dimensionDef.getKey()));
        
        // if this dimension is defined, proceed
        if (dimension != null) {
            final PersistedDimensionValue persistedValue = dimension.getPersistedValue();
            
            switch (dimensionDef.getValueMatch()) {
                case LONG_RANGE:
                    final Field<Long> longRangeValueField = aliasedDimensionTable.field("long_value", Long.class);
                    final LongDimensionDef<T> longDimensionDef = (LongDimensionDef<T>) dimensionDef;
                    
                    Condition longCondition;
                    if (dimension.getValue() != null) {
                        switch (longDimensionDef.getRange()) {
                            case MATCH_ANY:
                                longCondition = longRangeValueField.eq(persistedValue.getLongValue()).or(longRangeValueField.eq(MATCH_ANY));
                                break;
                            case LARGER:
                                longCondition = longRangeValueField.ge(persistedValue.getLongValue());
                                allOrderBy.add(longRangeValueField.asc());
                                break;
                            case SMALLER:
                                longCondition = longRangeValueField.le(persistedValue.getLongValue());
                                allOrderBy.add(longRangeValueField.desc());
                                break;
                            default:
                                throw new UnsupportedOperationException(longDimensionDef.getRange().name());
                        }
                    } else {
                        longCondition = longRangeValueField.eq(persistedValue.getLongValue());
                    }
                    longCondition = longCondition.or(longRangeValueField.isNull());
                    allCond.add(longCondition);
                    break;
                
                case LONG:
                    final Field<Long> longValueField = aliasedDimensionTable.field("long_value", Long.class);
                    allCond.add(longValueField.eq(persistedValue.getLongValue()).or(longValueField.isNull()));
                    break;
                
                case STRING_HIERARCHY:
                    final Field<String> stringHierarchyValueField = aliasedDimensionTable.field("string_value", String.class);
                    final StringHierarchyDimensionDef<T> hierarchyDimensionDef = (StringHierarchyDimensionDef<T>) dimensionDef;
                    
                    Condition stringCondition = stringHierarchyValueField.eq(persistedValue.getStringValue());
                    if (dimension.getValue() != null) {
                        if (hierarchyDimensionDef.isMatchAnySupported()) {
                            stringCondition.or(stringHierarchyValueField.eq(StringHierarchyDimensionDef.MATCH_ANY));
                        }
                        if (hierarchyDimensionDef.isHierarchical()) {
                            HierarchyNode<?> parent = ((HierarchyNode<?>) dimension.getValue()).getParent();
                            while (parent != null) {
                                stringCondition = stringCondition.or(stringHierarchyValueField.eq(parent.getKey()));
                                parent = parent.getParent();
                            }
                        }
                    }
                    stringCondition = stringCondition.or(stringHierarchyValueField.isNull());
                    allCond.add(stringCondition);
                    break;
                
                case STRING:
                    final Field<String> stringValueField = aliasedDimensionTable.field("string_value", String.class);
                    allCond.add(stringValueField.eq(persistedValue.getStringValue()).or(stringValueField.isNull()));
                    break;
                
                default:
                    throw new UnsupportedOperationException(dimensionDef.getValueMatch().name());
            }
            
        } else {
            switch (dimensionDef.getValueMatch()) {
                case LONG_RANGE:
                case LONG:
                    final Field<Long> longValueField = aliasedDimensionTable.field("long_value", Long.class);
                    allCond.add(longValueField.isNull());
                    break;
                
                case STRING_HIERARCHY:
                case STRING:
                    final Field<String> stringValueField = aliasedDimensionTable.field("string_value", String.class);
                    allCond.add(stringValueField.isNull());
                    break;
                
                default:
                    throw new UnsupportedOperationException(dimensionDef.getValueMatch().name());
            }
        }
        
        return join;
    }
    
    @SuppressWarnings("unchecked")
    private static <T> SelectJoinStep<Record> joinDimensionForLookup(final DimensionDef<T> dimensionDef,
                                                                     final Context<?> context,
                                                                     final boolean exact,
                                                                     final int i,
                                                                     final Table<?> dimensionTable,
                                                                     final Field<Long> idField,
                                                                     final SelectJoinStep<Record> select,
                                                                     final List<Condition> allCond,
                                                                     final List<SortField<?>> allOrderBy) {
        
        final Dimension<T> dimension = context.getDimension(dimensionDef);
        
        final Table<?> aliasedDimensionTable = dimensionTable.as("cbd" + i);
        
        // if this dimension is defined, proceed
        if (dimension != null) {
            final SelectOnConditionStep<Record> join = select
                .join(aliasedDimensionTable)
                .on(idField.eq(aliasedDimensionTable.field("id", Long.class)),
                    aliasedDimensionTable.field("dimension_name", String.class).eq(dimensionDef.getKey()));
            
            final PersistedDimensionValue persistedValue = dimension.getPersistedValue();
            
            switch (dimensionDef.getValueMatch()) {
                case LONG_RANGE:
                    if (!exact) {
                        final Field<Long> longRangeValueField = aliasedDimensionTable.field("long_value", Long.class);
                        final LongDimensionDef<T> longDimensionDef = (LongDimensionDef<T>) dimensionDef;
                        
                        final Condition longCondition;
                        if (dimension.getValue() == null) {
                            longCondition = longRangeValueField.isNotNull();
                        } else {
                            switch (longDimensionDef.getRange()) {
                                case MATCH_ANY:
                                    longCondition = longRangeValueField.eq(persistedValue.getLongValue());
                                    break;
                                case LARGER:
                                    longCondition = longRangeValueField.le(persistedValue.getLongValue());
                                    allOrderBy.add(longRangeValueField.desc());
                                    break;
                                case SMALLER:
                                    longCondition = longRangeValueField.ge(persistedValue.getLongValue());
                                    allOrderBy.add(longRangeValueField.asc());
                                    break;
                                default:
                                    throw new UnsupportedOperationException(longDimensionDef.getRange().name());
                            }
                        }
                        allCond.add(longCondition);
                        break;
                    }
                    // if exact, works like a standard string
                case LONG:
                    final Field<Long> longValueField = aliasedDimensionTable.field("long_value", Long.class);
                    allCond.add(longValueField.eq(persistedValue.getLongValue()));
                    break;
                
                case STRING_HIERARCHY:
                    if (!exact) {
                        final Field<String> stringHierarchyValueField = aliasedDimensionTable.field("string_value", String.class);
                        final StringHierarchyDimensionDef<T> hierarchyDimensionDef = (StringHierarchyDimensionDef<T>) dimensionDef;
                        
                        final Condition stringCondition;
                        if (dimension.getValue() == null) {
                            stringCondition = stringHierarchyValueField.isNotNull();
                        } else if (hierarchyDimensionDef.isHierarchical()) {
                            final HierarchySubtreeNodes<?> subtree = ((HierarchyNode<?>) dimension.getValue()).getSubtree();
                            if (subtree != null) {
                                if (HierarchySubtreeNodes.Resolution.SET.equals(subtree.getResolution())) {
                                    final Set<? extends HierarchyNode<?>> subtreeSet = ((HierarchyChildNodeSet<?>) subtree).getChildNodeSet();
                                    Condition setCondition = null;
                                    for (HierarchyNode<?> node : subtreeSet) {
                                        Condition nodeCondition = stringHierarchyValueField.eq(node.getKey());
                                        setCondition = setCondition == null ? nodeCondition : setCondition.or(nodeCondition);
                                    }
                                    stringCondition = setCondition;
                                } else if (HierarchySubtreeNodes.Resolution.PREFIX.equals(subtree.getResolution())) {
                                    final String subtreePrefix = ((HierarchySubtreeNodePrefix<?>) subtree).getSubtreeNodePrefix() + "%";
                                    stringCondition = stringHierarchyValueField.like(subtreePrefix);
                                } else {
                                    throw new UnsupportedOperationException(subtree.getResolution().name());
                                }
                            } else {
                                // exact match
                                stringCondition = stringHierarchyValueField.eq(persistedValue.getStringValue());
                            }
                        } else {
                            // exact match if dimension is not hierarchical
                            stringCondition = stringHierarchyValueField.eq(persistedValue.getStringValue());
                        }
                        allCond.add(stringCondition);
                        break;
                    }
                    // if exact, works like a standard string
                case STRING:
                    final Field<String> stringValueField = aliasedDimensionTable.field("string_value", String.class);
                    allCond.add(stringValueField.eq(persistedValue.getStringValue()));
                    break;
                
                default:
                    throw new UnsupportedOperationException(dimensionDef.getValueMatch().name());
            }
            
            return join;
            
        } else {
            return select
                .leftJoin(aliasedDimensionTable)
                .on(idField.eq(aliasedDimensionTable.field("id", Long.class)),
                    aliasedDimensionTable.field("dimension_name", String.class).eq(dimensionDef.getKey()));
        }
    }
    
    /**
     * Will get the value that matches the given context exactly (or having a context that contains the whole context).
     *
     * @param context the context instance
     * @param exact true if the match should be exact, false if a context containing the whole context is allowed
     * @param allowExactContextMatch
     * @return the values (exactly) matching the context instance, or an empty list if no match found
     */
    public static <MAIN_REC extends Record, DIM_REC extends Record, DEF extends DimensionalDef> List<RecordHolder<MAIN_REC, DIM_REC>> lookup(final Context<DEF> context,
                                                                                                                                             final boolean exact,
                                                                                                                                             final boolean allowExactContextMatch,
                                                                                                                                             final Table<MAIN_REC> mainTable,
                                                                                                                                             final Table<DIM_REC> dimensionTable,
                                                                                                                                             final SelectJoinStep<Record> select,
                                                                                                                                             final Condition condition,
                                                                                                                                             final SortField<?>... orderBy) {
        /*
         * The query is built as follows:
         * Q1 SELECT cb FROM ContextValueEntity/Set cb
         * Q1 JOIN cb.context cbd1 WITH cbd1.dimensionName='A' AND cbd1.value='VA'
         * Q1 JOIN cb.context cbd2 WITH cbd2.dimensionName='DEF' AND cbd2.value='VB'
         * Q1 JOIN cb.context cbd3 WITH cbd3.dimensionName='C' AND cbd3.value='VC'
         * Q2 WHERE cbv.propertyId='Prop' AND cb.contextHash > / >= / = X (!allowExactContextMatch / allowExactContextMatch / exact)
         * Q3 ORDER BY cb.contextHash -- added only if NOT exact
         */
        final DEF def = context.getDef();
        final Field<Long> idField = mainTable.field("id", Long.class);
        final Field<Long> contextDepthField = mainTable.field("context_depth", Long.class);
        
        final List<Condition> allCond = new ArrayList<>(def.getContextDef().size() + 1);
        allCond.add(condition);
        if (exact) {
            allCond.add(contextDepthField.eq(context.getDepth()));
        } else if (allowExactContextMatch) {
            allCond.add(contextDepthField.ge(context.getDepth()));
        } else {
            allCond.add(contextDepthField.gt(context.getDepth()));
        }
        
        final List<SortField<?>> allOrderBy = new ArrayList<>(def.getContextDef().size() + (((orderBy == null) || (orderBy.length == 0)) ? 1 : orderBy.length));
        SelectJoinStep<Record> join = select;
        final Iterator<DimensionDef<?>> iteratorByImportance = def.getContextDef().iterator();
        // build the query according to dimension importance order
        for (int i = 0; iteratorByImportance.hasNext(); i++) {
            join = joinDimensionForLookup(iteratorByImportance.next(), context, exact, i, dimensionTable, idField, select, allCond, allOrderBy);
        }
        
        if ((orderBy == null) || (orderBy.length == 0)) {
            allOrderBy.add(contextDepthField.desc());
        } else {
            allOrderBy.addAll(Arrays.asList(orderBy));
        }
        
        final Result<Record> fetch = join.where(allCond).orderBy(allOrderBy).fetch();
        
        return fetch.map(r -> {
            final MAIN_REC main = r.into(mainTable);
            final List<DIM_REC> dimensions = new ArrayList<>(def.getContextDef().size());
            final Iterator<DimensionDef<?>> ii = def.getContextDef().iterator();
            for (int i = 0; ii.hasNext(); i++) {
                ii.next();
                dimensions.add(r.into(dimensionTable.as("cbd" + i)));
            }
            return new RecordHolder<>(main, dimensions, r);
        });
    }
    
    private DimensionalHelper() {}
    
}
