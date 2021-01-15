/*
 * Copyright 2002, 2011 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.counters.support;

import static com.ixaris.commons.dimensions.counters.test.jooq.tables.TestEvent.*;
import static com.ixaris.commons.jooq.persistence.TransactionalDSLContext.*;

import java.util.Objects;

import com.ixaris.commons.dimensions.counters.data.CounterEventEntity;
import com.ixaris.commons.dimensions.counters.test.jooq.tables.records.TestEventRecord;
import com.ixaris.commons.dimensions.lib.context.Context;
import com.ixaris.commons.jooq.persistence.Entity;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;
import com.ixaris.commons.misc.lib.object.EqualsUtil;

public final class TestEventEntity extends Entity<TestEventEntity> implements CounterEventEntity<TestCounterDef> {
    
    public static TestEventEntity lookup(final long eventId) {
        return new TestEventEntity(JOOQ_TX.get().fetchOne(TEST_EVENT, TEST_EVENT.ID.eq(eventId)));
    }
    
    private final TestEventRecord event;
    
    private TestEventEntity(final TestEventRecord event) {
        this.event = event;
    }
    
    public TestEventEntity(final long delta, final long timestamp, final boolean counterAffected, final AEnum a, final Long b, final Long c) {
        this(new TestEventRecord(UniqueIdGenerator.generate(), timestamp, delta, counterAffected, a != null ? a.name() : null, b, c));
    }
    
    public TestEventRecord getEvent() {
        return event;
    }
    
    @Override
    public TestEventEntity store() {
        attachAndStore(event);
        return this;
    }
    
    @Override
    public TestEventEntity delete() {
        attachAndDelete(event);
        return this;
    }
    
    @Override
    public boolean equals(final Object o) {
        return EqualsUtil.equals(this, o, other -> Objects.equals(event, other.event));
    }
    
    @Override
    public int hashCode() {
        return Long.hashCode(event.getId());
    }
    
    @Override
    public long getId() {
        return event.getId();
    }
    
    @Override
    public boolean isCounterAffected() {
        return event.getCounterAffected();
    }
    
    @Override
    public Context<TestCounterDef> getContext() {
        final Context.Builder<TestCounterDef> builder = Context.newBuilder(TestCounterDef.getInstance());
        if (event.getA() != null) {
            builder.add(ADimensionDef.getInstance().create(AEnum.valueOf(event.getA())));
        }
        if (event.getB() != null) {
            builder.add(BDimensionDef.getInstance().create(event.getB()));
        }
        if (event.getC() != null) {
            builder.add(CDimensionDef.getInstance().create(event.getC()));
        }
        return builder.build();
    }
    
    @Override
    public long getDelta() {
        return event.getDelta();
    }
    
    @Override
    public long getTimestamp() {
        return event.getTimestamp();
    }
    
}
