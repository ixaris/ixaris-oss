/*
 * Copyright 2002, 2011 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.limits.support;

import static com.ixaris.commons.dimensions.limits.test.jooq.tables.TestEvent.*;
import static com.ixaris.commons.dimensions.limits.test.jooq.tables.TestEventCounterDimensions.*;
import static com.ixaris.commons.jooq.persistence.TransactionalDSLContext.*;

import org.jooq.Table;

import com.ixaris.commons.dimensions.counters.AbstractCounterDef;
import com.ixaris.commons.dimensions.counters.WindowValue;
import com.ixaris.commons.dimensions.lib.context.Context;
import com.ixaris.commons.dimensions.lib.context.ContextDef;
import com.ixaris.commons.dimensions.lib.context.Dimension;
import com.ixaris.commons.dimensions.limits.test.jooq.tables.records.TestEventCounterDimensionsRecord;

public final class TestCounterDef extends AbstractCounterDef<TestEventCounterDimensionsRecord, TestCounterDef> {
    
    private static final TestCounterDef INSTANCE = new TestCounterDef();
    
    public static TestCounterDef getInstance() {
        return INSTANCE;
    }
    
    public static final String KEY = "TEST_COUNTER";
    
    private TestCounterDef() {
        super("Test counter", new ContextDef(Long1DimensionDef.getInstance(), Long2DimensionDef.getInstance()), 0, 1);
    }
    
    @Override
    public String getKey() {
        return KEY;
    }
    
    @Override
    public WindowValue fetchWindow(final Context context, final long from, final Long to) {
        return new WindowValue(0L, 0L);
    }
    
    @Override
    public Table<TestEventCounterDimensionsRecord> getContextTable() {
        return TEST_EVENT_COUNTER_DIMENSIONS;
    }
    
    @Override
    public Context<TestCounterDef> extractContextInstance(final TestEventCounterDimensionsRecord contextRecord) {
        final Context.Builder<TestCounterDef> builder = Context.newBuilder(TestCounterDef.getInstance());
        if (contextRecord.getLong1() != null) {
            builder.add(Long1DimensionDef.getInstance().create(contextRecord.getLong1()));
        }
        if (contextRecord.getLong2() != null) {
            builder.add(Long2DimensionDef.getInstance().create(contextRecord.getLong2()));
        }
        return builder.build();
    }
    
    @Override
    public TestEventCounterDimensionsRecord newContextRecord(final long id, final Context<TestCounterDef> context) {
        final TestEventCounterDimensionsRecord record = new TestEventCounterDimensionsRecord(
            id,
            Dimension.valueOrNull(context.get(Long1DimensionDef.getInstance())),
            Dimension.valueOrNull(context.get(Long2DimensionDef.getInstance())));
        return record;
    }
    
    @Override
    public TestEventEntity lookupCounterEvent(final long eventId) {
        return TestEventEntity.lookup(eventId);
    }
    
}
