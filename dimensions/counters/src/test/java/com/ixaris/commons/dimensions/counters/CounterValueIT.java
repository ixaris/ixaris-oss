package com.ixaris.commons.dimensions.counters;

import static com.ixaris.commons.jooq.persistence.TransactionalDSLContext.JOOQ_TX;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;
import static com.ixaris.commons.multitenancy.lib.data.DataUnit.DATA_UNIT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertTrue;

import java.util.Collections;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ixaris.commons.dimensions.counters.data.CounterEntity;
import com.ixaris.commons.dimensions.counters.support.ADimensionDef;
import com.ixaris.commons.dimensions.counters.support.AEnum;
import com.ixaris.commons.dimensions.counters.support.BDimensionDef;
import com.ixaris.commons.dimensions.counters.support.CDimensionDef;
import com.ixaris.commons.dimensions.counters.support.TestCounterDef;
import com.ixaris.commons.dimensions.counters.test.jooq.tables.records.TestEventCounterDimensionsRecord;
import com.ixaris.commons.dimensions.lib.context.Context;
import com.ixaris.commons.hikari.test.JooqHikariTestHelper;
import com.ixaris.commons.jooq.persistence.JooqMultiTenancyConfiguration;
import com.ixaris.commons.jooq.persistence.JooqSyncPersistenceProvider;
import com.ixaris.commons.multitenancy.test.TestTenants;

public class CounterValueIT {
    
    private static final String UNIT = "unit_counters";
    private static JooqHikariTestHelper TEST_HELPER = new JooqHikariTestHelper(Collections.singleton(UNIT), TestTenants.DEFAULT);
    
    private static final JooqSyncPersistenceProvider provider = new JooqSyncPersistenceProvider(JooqMultiTenancyConfiguration
        .createDslContext(TEST_HELPER.getDataSource()));
    
    @BeforeClass
    public static void setup() {
        TEST_HELPER.getMultiTenancy().addTenant(TestTenants.DEFAULT);
    }
    
    @AfterClass
    public static void teardown() {
        TEST_HELPER.destroy();
    }
    
    @Test
    public void createCounterTest() throws Throwable {
        DATA_UNIT.exec(UNIT, () -> TENANT.exec(TestTenants.DEFAULT, () -> provider.transaction(() -> {
            final int windowMultiple = 7;
            // create counter
            CounterEntity<TestEventCounterDimensionsRecord, TestCounterDef> counter = createCounter(Context.newBuilder(TestCounterDef
                .getInstance())
                .add(ADimensionDef.getInstance().create(AEnum.AAA))
                .add(BDimensionDef.getInstance().create(20L))
                .add(CDimensionDef.getInstance().create(20L))
                .build(),
                new WindowWidth((byte) 1, WindowTimeUnit.DAY),
                windowMultiple);
            
            assertEquals(new WindowWidth((byte) 1, WindowTimeUnit.DAY), counter.getNarrowWindowWidth());
            assertEquals(windowMultiple, counter.getWideWindowMultiple());
            checkCounters(counter, 0, 0L, 0L, 0L, 0, 0L);
            
            // update counter for different narrow windows
            // today 1/1 (count/sum)
            // yesterday 2/4
            // 2 days ago 3/9
            // .. up to wideWindowMultiple + 1 ago
            long totCount = 0;
            long totSum = 0;
            long now = System.currentTimeMillis();
            for (int i = windowMultiple + 1; i >= 0; i--) { // starting from 'windowMultiple' ago till today. The extra windows are out of scope.
                for (int j = 0; j <= i; j++) {
                    incrementCounter(counter, i + 1L, now - (WindowTimeUnit.MILLISECONDS_IN_DAY * i));
                    if (i < windowMultiple) { // older ones will be ignored
                        totCount++;
                        totSum += i + 1;
                    }
                    if (i == 0) { // today - 1/1 for today and 2/4 yesterday(lastFull window)
                        checkCounters(counter, totCount, totSum, 1L, 1L, 2L, 4L); // last counter value is 1/1
                    } else if (i == 1) { // yesterday (lastFull) = j+1 / j+1 * 2
                        checkCounters(counter, totCount, totSum, 0L, 0L, j + 1L, (j + 1L) * 2L); // last counter value is 1/1
                    } else { // all previous days do not effect current/lastFull narrow windows
                        checkCounters(counter, totCount, totSum, 0L, 0L, 0L, 0L); // last counter value is 1/1
                    }
                }
            }
            
            // manually insert an older window - entity will not allow us
            JOOQ_TX.get().execute("INSERT INTO lib_dim_counter_narrow "
                + "(SELECT id + 1, counter_id, window_number - "
                + windowMultiple
                + ", 8, 64 "
                + "FROM lib_dim_counter_narrow "
                + "WHERE counter_id = "
                + counter.getCounter().getId()
                + " AND count = 1)");
            JOOQ_TX.get().execute("UPDATE lib_dim_counter "
                + "SET count = count + 8, sum = sum + 64, start_narrow_window_number = start_narrow_window_number - 1 "
                + "WHERE id = "
                + counter.getCounter().getId());
            
            counter = lookupCounter(counter.getCounter().getId());
            checkCounters(counter, totCount, totSum, 1L, 1L); // query will dropExpiredWindow
            
            // Test Reverse
            incrementCounter(counter, 10L, now);
            checkCounters(counter, totCount + 1L, totSum + 10L, 2L, 11L);
            incrementCounter(counter, 10L, now);
            checkCounters(counter, totCount + 2L, totSum + 20L, 3L, 21L);
            reverseCounter(counter, 10L, now);
            checkCounters(counter, totCount + 1L, totSum + 10L, 2L, 11L); // back to previous
            reverseCounter(counter, 10L, now);
            checkCounters(counter, totCount, totSum, 1L, 1L); // back to original values
            
            return null;
        })));
    }
    
    private void checkCounters(final CounterEntity<TestEventCounterDimensionsRecord, TestCounterDef> counter,
                               final long wideCount,
                               final long wideSum,
                               final long narrowCount,
                               final long narrowSum) {
        
        counter.dropExpiredWindows();
        final WindowValue wideValue = counter.getWideValue();
        final WindowValue narrowValue = counter.getNarrowValue();
        
        assertEquals(wideCount, wideValue.getCount());
        assertTrue("Expected " + wideSum + " got " + wideValue.getSum(), wideSum == wideValue.getSum());
        assertEquals(narrowCount, narrowValue.getCount());
        assertTrue("Expected " + narrowSum + " got " + narrowValue.getSum(), narrowSum == narrowValue.getSum());
        assertEquals(new CounterValue(new WindowValue(wideCount, wideSum), new WindowValue(narrowCount, narrowSum)),
            counter.getWideAndNarrowValue());
    }
    
    private void checkCounters(final CounterEntity<TestEventCounterDimensionsRecord, TestCounterDef> counter,
                               final long wideCount,
                               final long wideSum,
                               final long narrowCount,
                               final long narrowSum,
                               final long narrowCountLastFull,
                               final long narrowSumLastFull) {
        
        checkCounters(counter, wideCount, wideSum, narrowCount, narrowSum);
        
        final WindowValue narrowValueLastFull = counter.getNarrowValueLastFull();
        assertEquals(narrowCountLastFull, counter.getNarrowValueLastFull().getCount());
        assertTrue("Expected " + narrowSumLastFull + " got " + narrowValueLastFull.getSum(), narrowSumLastFull == narrowValueLastFull.getSum());
        assertEquals(new CounterValue(new WindowValue(wideCount, wideSum), new WindowValue(narrowCountLastFull, narrowSumLastFull)),
            counter.getWideAndNarrowValueLastFull());
    }
    
    private CounterEntity<TestEventCounterDimensionsRecord, TestCounterDef> createCounter(final Context<TestCounterDef> context,
                                                                                          final WindowWidth narrowWindowWidth,
                                                                                          final int wideWindowMultiple) {
        final CounterEntity<TestEventCounterDimensionsRecord, TestCounterDef> counter = new CounterEntity<>(context,
            narrowWindowWidth,
            wideWindowMultiple,
            new WindowValue(0L, 0L),
            new WindowValue(0L, 0L),
            new WindowValue(0L, 0L),
            System.currentTimeMillis(),
            0);
        counter.store();
        return counter;
    }
    
    private void incrementCounter(final CounterEntity<TestEventCounterDimensionsRecord, TestCounterDef> counter,
                                  final long delta,
                                  final long timestamp) {
        
        counter.increment(delta, timestamp);
        counter.store();
    }
    
    private void reverseCounter(final CounterEntity<TestEventCounterDimensionsRecord, TestCounterDef> counter,
                                final long delta,
                                final long timestamp) {
        
        counter.decrement(delta, timestamp);
        counter.store();
    }
    
    private CounterEntity<TestEventCounterDimensionsRecord, TestCounterDef> lookupCounter(final long id) {
        return CounterEntity.lookup(TestCounterDef.getInstance(), id);
    }
    
}
