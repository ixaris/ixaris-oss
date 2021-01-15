/*
 * Copyright 2002, 2011 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.counters.support;

import static com.ixaris.commons.dimensions.counters.test.jooq.tables.TestEvent.*;
import static com.ixaris.commons.dimensions.counters.test.jooq.tables.TestEventCounterDimensions.*;
import static com.ixaris.commons.jooq.persistence.TransactionalDSLContext.*;

import org.jooq.Table;

import com.ixaris.commons.dimensions.counters.AbstractCounterDef;
import com.ixaris.commons.dimensions.counters.WindowTimeUnit;
import com.ixaris.commons.dimensions.counters.WindowValue;
import com.ixaris.commons.dimensions.counters.test.jooq.tables.records.TestEventCounterDimensionsRecord;
import com.ixaris.commons.dimensions.lib.context.Context;
import com.ixaris.commons.dimensions.lib.context.ContextDef;
import com.ixaris.commons.dimensions.lib.context.Dimension;

public final class TestCounterDef extends AbstractCounterDef<TestEventCounterDimensionsRecord, TestCounterDef> {
    
    private static final TestCounterDef INSTANCE = new TestCounterDef();
    
    public static TestCounterDef getInstance() {
        return INSTANCE;
    }
    
    public static final String KEY = "TEST_COUNTER";
    
    private TestCounterDef() {
        super("Test counter", new ContextDef(ADimensionDef.getInstance(), BDimensionDef.getInstance(), CDimensionDef.getInstance()), 0, 1);
    }
    
    @Override
    public String getKey() {
        return KEY;
    }
    
    @Override
    public WindowValue fetchWindow(final Context context, final long from, final Long to) {
        // Here one would make a query on the 'counter event' table
        final long toTime = to == null ? System.currentTimeMillis() : to;
        return new WindowValue((toTime - from) / WindowTimeUnit.MILLISECONDS_IN_HOUR, // test count will return number of hours in window
            (toTime - from) / WindowTimeUnit.MILLISECONDS_IN_HOUR * 10); // 10 times the count
    }
    
    @Override
    public Table<TestEventCounterDimensionsRecord> getContextTable() {
        return TEST_EVENT_COUNTER_DIMENSIONS;
    }
    
    @Override
    public Context<TestCounterDef> extractContextInstance(final TestEventCounterDimensionsRecord contextRecord) {
        final Context.Builder<TestCounterDef> builder = Context.newBuilder(TestCounterDef.getInstance());
        if (contextRecord.getA() != null) {
            builder.add(ADimensionDef.getInstance().create(AEnum.valueOf(contextRecord.getA())));
        }
        if (contextRecord.getB() != null) {
            builder.add(BDimensionDef.getInstance().create(contextRecord.getB()));
        }
        if (contextRecord.getC() != null) {
            builder.add(CDimensionDef.getInstance().create(contextRecord.getC()));
        }
        return builder.build();
    }
    
    @Override
    public TestEventCounterDimensionsRecord newContextRecord(final long id, final Context<TestCounterDef> context) {
        return new TestEventCounterDimensionsRecord(
            id,
            Dimension.mapValueOrNull(context.get(ADimensionDef.getInstance()), Enum::name),
            Dimension.valueOrNull(context.get(BDimensionDef.getInstance())),
            Dimension.valueOrNull(context.get(CDimensionDef.getInstance())));
    }
    
    @Override
    public TestEventEntity lookupCounterEvent(final long eventId) {
        return TestEventEntity.lookup(eventId);
    }
    
}
