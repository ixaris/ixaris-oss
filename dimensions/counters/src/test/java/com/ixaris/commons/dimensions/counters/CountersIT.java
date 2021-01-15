package com.ixaris.commons.dimensions.counters;

import static com.ixaris.commons.async.lib.CompletionStageUtil.block;
import static com.ixaris.commons.dimensions.counters.jooq.tables.LibDimCounterEventQueue.LIB_DIM_COUNTER_EVENT_QUEUE;
import static com.ixaris.commons.jooq.persistence.TransactionalDSLContext.JOOQ_TX;
import static com.ixaris.commons.misc.lib.object.Tuple.tuple;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;
import static com.ixaris.commons.multitenancy.lib.data.DataUnit.DATA_UNIT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotSame;
import static org.junit.Assert.fail;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.concurrent.atomic.AtomicLong;

import org.junit.AfterClass;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixaris.commons.async.lib.AsyncLocal;
import com.ixaris.commons.clustering.lib.extra.LocalCluster;
import com.ixaris.commons.clustering.lib.service.ClusterRouteTimeoutException;
import com.ixaris.commons.dimensions.counters.admin.CountersAdminHelper;
import com.ixaris.commons.dimensions.counters.cache.ClusterShardedCounterCacheProvider;
import com.ixaris.commons.dimensions.counters.support.ADimensionDef;
import com.ixaris.commons.dimensions.counters.support.AEnum;
import com.ixaris.commons.dimensions.counters.support.BDimensionDef;
import com.ixaris.commons.dimensions.counters.support.CDimensionDef;
import com.ixaris.commons.dimensions.counters.support.TestCounterDef;
import com.ixaris.commons.dimensions.counters.support.TestEventEntity;
import com.ixaris.commons.dimensions.lib.context.Context;
import com.ixaris.commons.dimensions.lib.context.Dimension;
import com.ixaris.commons.hikari.test.JooqHikariTestHelper;
import com.ixaris.commons.jooq.persistence.JooqAsyncPersistenceProvider;
import com.ixaris.commons.jooq.persistence.JooqMultiTenancyConfiguration;
import com.ixaris.commons.jooq.persistence.JooqSyncPersistenceProvider;
import com.ixaris.commons.misc.lib.object.Tuple3;
import com.ixaris.commons.misc.lib.registry.Registry;
import com.ixaris.commons.multitenancy.lib.async.ExecutorMultiTenantAtLeastOnceProcessorFactory;
import com.ixaris.commons.multitenancy.test.TestTenants;

/**
 * Context Counters Test
 *
 * @author <a href="mailto:andrew.calafato@ixaris.com">Andrew Calafato</a>
 */
public class CountersIT {
    
    private static Logger LOG = LoggerFactory.getLogger(CountersIT.class);
    
    private static final String UNIT = "unit_counters";
    private static JooqHikariTestHelper TEST_HELPER = new JooqHikariTestHelper(Collections.singleton(UNIT), TestTenants.DEFAULT);
    
    private static final LocalCluster localCluster = new LocalCluster(Collections.emptySet(), Collections.emptySet());
    
    private static final JooqAsyncPersistenceProvider provider = new JooqAsyncPersistenceProvider(JooqMultiTenancyConfiguration.createDslContext(TEST_HELPER
        .getDataSource()));
    private static final JooqSyncPersistenceProvider db = new JooqSyncPersistenceProvider(JooqMultiTenancyConfiguration
        .createDslContext(TEST_HELPER.getDataSource()));
    private static final ClusterShardedCounterCacheProvider cache = new ClusterShardedCounterCacheProvider(
        TEST_HELPER.getMultiTenancy(), localCluster);
    private static final CountersHelper counters = new CountersHelper(provider, cache, localCluster);
    private static final CountersAdminHelper countersAdmin = new CountersAdminHelper(
        provider, cache);
    
    private static AtLeastOnceApplyCounterEventType eventType;
    
    @BeforeClass
    public static void setup() {
        System.setProperty("spring.application.name", UNIT);
        counters.startup();
        CounterDefRegistry.getInstance().postConstruct();
        Registry.registerInApplicableRegistries(TestCounterDef.getInstance());
        eventType = new AtLeastOnceApplyCounterEventType(
            provider,
            cache,
            localCluster,
            new ExecutorMultiTenantAtLeastOnceProcessorFactory(1),
            1000L,
            Collections.singleton(UNIT));
        eventType.start();
        TEST_HELPER.getMultiTenancy().registerTenantLifecycleParticipant(eventType);
        TEST_HELPER.getMultiTenancy().addTenant(TestTenants.DEFAULT);
    }
    
    @AfterClass
    public static void teardown() {
        TENANT.exec(TestTenants.DEFAULT, () -> cache.of(TestCounterDef.getInstance()).clear());
        eventType.stop();
        TEST_HELPER.destroy();
        Registry.unregisterFromApplicableRegistries(TestCounterDef.getInstance());
        CounterDefRegistry.getInstance().preDestroy();
        counters.shutdown();
    }
    
    /* Counter with context <*, *, *> */
    private CounterValue getCounter1() throws InterruptedException, ClusterRouteTimeoutException {
        return block(counters.getCounter(Context.empty(TestCounterDef.getInstance()), new WindowWidth(1, WindowTimeUnit.DAY), 7));
    }
    
    private CounterValue getCounter1LastFull() throws InterruptedException, ClusterRouteTimeoutException {
        return block(counters.getCounter(Context.empty(TestCounterDef.getInstance()), new WindowWidth(1, WindowTimeUnit.DAY), 7, true));
    }
    
    /* Counter with context <"An A Value", *, *> */
    private CounterValue getCounter2() throws InterruptedException, ClusterRouteTimeoutException {
        return block(counters.getCounter(Context.newBuilder(TestCounterDef.getInstance())
            .add(ADimensionDef.getInstance().create(AEnum.AAA))
            .build(),
            new WindowWidth(1, WindowTimeUnit.DAY),
            7));
    }
    
    private CounterValue getCounter2LastFull() throws InterruptedException, ClusterRouteTimeoutException {
        return block(counters.getCounter(Context.newBuilder(TestCounterDef.getInstance())
            .add(ADimensionDef.getInstance().create(AEnum.AAA))
            .build(),
            new WindowWidth(1, WindowTimeUnit.DAY),
            7,
            true));
    }
    
    /* Counter with context <"An A Value", 10, 20> */
    private CounterValue getCounter3() throws InterruptedException, ClusterRouteTimeoutException {
        return block(counters.getCounter(Context.newBuilder(TestCounterDef.getInstance())
            .add(ADimensionDef.getInstance().create(AEnum.AAA))
            .add(BDimensionDef.getInstance().create(10L))
            .add(CDimensionDef.getInstance().create(20L))
            .build(),
            new WindowWidth(1, WindowTimeUnit.DAY),
            7));
    }
    
    private CounterValue getCounter3LastFull() throws InterruptedException, ClusterRouteTimeoutException {
        return block(counters.getCounter(Context.newBuilder(TestCounterDef.getInstance())
            .add(ADimensionDef.getInstance().create(AEnum.AAA))
            .add(BDimensionDef.getInstance().create(10L))
            .add(CDimensionDef.getInstance().create(20L))
            .build(),
            new WindowWidth(1, WindowTimeUnit.DAY),
            7,
            true));
    }
    
    private CounterValue getCounterForAsync() throws InterruptedException, ClusterRouteTimeoutException {
        return block(counters.getCounter(Context.newBuilder(TestCounterDef.getInstance())
            .add(ADimensionDef.getInstance().create(AEnum.ASY))
            .build(),
            new WindowWidth(1, WindowTimeUnit.DAY),
            7));
    }
    
    private CounterValue getCounterForAsyncConcurrent() throws InterruptedException, ClusterRouteTimeoutException {
        return block(counters.getCounter(Context.newBuilder(TestCounterDef.getInstance())
            .add(ADimensionDef.getInstance().create(AEnum.ASC))
            .build(),
            new WindowWidth(1, WindowTimeUnit.DAY),
            7));
    }
    
    private CounterValue getCounterForCleanup() throws InterruptedException, ClusterRouteTimeoutException {
        return block(counters.getCounter(Context.newBuilder(TestCounterDef.getInstance())
            .add(ADimensionDef.getInstance().create(AEnum.CLN))
            .build(),
            new WindowWidth(1, WindowTimeUnit.DAY),
            7));
    }
    
    private CounterValue getCounterForStress() throws InterruptedException, ClusterRouteTimeoutException {
        return block(counters.getCounter(Context.newBuilder(TestCounterDef.getInstance())
            .add(ADimensionDef.getInstance().create(AEnum.STR))
            .build(),
            new WindowWidth(1, WindowTimeUnit.DAY),
            7));
    }
    
    private CounterValue getCounterSingle() throws InterruptedException, ClusterRouteTimeoutException {
        return block(counters.getCounter(Context.empty(TestCounterDef.getInstance()), new WindowWidth(1, WindowTimeUnit.DAY), 1));
    }
    
    private CounterValue getCounterAlways() throws InterruptedException, ClusterRouteTimeoutException {
        return block(counters.getCounter(Context.empty(TestCounterDef.getInstance()), new WindowWidth(1, WindowTimeUnit.ALWAYS), 1));
    }
    
    private WindowValue addDelta(final WindowValue windowValue, long deltaCount, long deltaSum) {
        return new WindowValue(windowValue.getCount() + deltaCount, windowValue.getSum() + deltaSum);
    }
    
    @SafeVarargs
    private final List<TestEventEntity> createAndStoreEvent(final boolean wait, final Tuple3<Context<TestCounterDef>, Long, Long>... events) throws InterruptedException {
        final List<TestEventEntity> r = db.transaction(() -> {
            final List<TestEventEntity> entities = new ArrayList<>(events.length);
            for (Tuple3<Context<TestCounterDef>, Long, Long> event : events) {
                final Context<TestCounterDef> context = event.get1();
                final AEnum a = Dimension.valueOrNull(context.getDimension(ADimensionDef.getInstance()));
                final Long b = Dimension.valueOrNull(context.getDimension(BDimensionDef.getInstance()));
                final Long c = Dimension.valueOrNull(context.getDimension(CDimensionDef.getInstance()));
                final TestEventEntity entity = new TestEventEntity(event.get2(), event.get3(), true, a, b, c).store();
                entity.store();
                CountersHelper.queueEvent(entity, localCluster);
                entities.add(entity);
            }
            return entities;
        });
        if (wait) {
            waitForPendingEventsToBeProcessed();
        }
        return r;
    }
    
    private void updateAndStoreEvent(final boolean wait, final List<TestEventEntity> events) throws InterruptedException {
        db.transaction(() -> {
            for (TestEventEntity entity : events) {
                entity.getEvent().setCounterAffected(false);
                entity.store();
                CountersHelper.queueEvent(entity, localCluster);
            }
            return null;
        });
        if (wait) {
            waitForPendingEventsToBeProcessed();
        }
    }
    
    private void waitForPendingEventsToBeProcessed() throws InterruptedException {
        boolean first = false;
        for (int i = 0; i < 200; i++) {
            Thread.sleep(200L); // some time for async processing
            
            final int count = db.transaction(() -> JOOQ_TX.get().fetchCount(LIB_DIM_COUNTER_EVENT_QUEUE));
            if (count == 0) {
                if (first) {
                    // check twice in a row
                    return;
                } else {
                    first = true;
                }
            } else {
                first = false;
            }
        }
        throw new IllegalStateException();
    }
    
    @Before
    public void beforeEach() throws InterruptedException {
        DATA_UNIT.exec(UNIT, () -> TENANT.exec(TestTenants.DEFAULT, this::waitForPendingEventsToBeProcessed));
    }
    
    @Test
    public void testMultiWindowCounters() throws Exception {
        DATA_UNIT.exec(UNIT, () -> TENANT.exec(TestTenants.DEFAULT, () -> {
            /* CREATING COUNTERS ************************************************************************************************** */
            
            // initial values matching ones generated by TestCounterDef
            final WindowValue initNarrowWindowValue = TestCounterDef
                .getInstance()
                .fetchWindow(null, // not used
                    new WindowWidth(1, WindowTimeUnit.DAY).getStartTimestamp(System.currentTimeMillis()),
                    null); // not used
            final WindowValue initNarrowWindowValueLastFull = new WindowValue(24L, 240L);
            final WindowValue initWideWindowValue = new WindowValue(initNarrowWindowValue.getCount() + (6 * 24),
                initNarrowWindowValue.getSum() + (60 * 24));
            
            // create a counter with context <*,*,*>
            assertEquals(new CounterValue(initWideWindowValue, initNarrowWindowValue), getCounter1());
            assertEquals(new CounterValue(initWideWindowValue, initNarrowWindowValueLastFull), getCounter1LastFull());
            
            // create a counter with context <"An A Value", *, *>
            assertEquals(new CounterValue(initWideWindowValue, initNarrowWindowValue), getCounter2());
            assertEquals(new CounterValue(initWideWindowValue, initNarrowWindowValueLastFull), getCounter2LastFull());
            
            // create a counter with context <"An A Value", 10, 20.5>
            assertEquals(new CounterValue(initWideWindowValue, initNarrowWindowValue), getCounter3());
            assertEquals(new CounterValue(initWideWindowValue, initNarrowWindowValueLastFull), getCounter3LastFull());
            
            /* ADDING A BATCH OF EVENTS1 (dated today) ************************************************************************* */
            
            final long currentTimeMillis1 = System.currentTimeMillis();
            final List<TestEventEntity> events1 = createAndStoreEvent(true,
                tuple(Context.empty(TestCounterDef.getInstance()), 5L, currentTimeMillis1), // matches counter 1 only
                tuple(Context.newBuilder(TestCounterDef.getInstance()).add(ADimensionDef.getInstance().create(AEnum.DDD)).build(),
                    32L,
                    currentTimeMillis1), // matches counter 1 only
                tuple(Context.newBuilder(TestCounterDef.getInstance()).add(ADimensionDef.getInstance().create(AEnum.AAA)).build(),
                    21L,
                    currentTimeMillis1), // matches counter 1 and 2
                tuple(Context.newBuilder(TestCounterDef.getInstance())
                    .add(ADimensionDef.getInstance().create(AEnum.AAA))
                    .add(BDimensionDef.getInstance().create(10L))
                    .add(CDimensionDef.getInstance().create(20L))
                    .build(),
                    10L,
                    currentTimeMillis1)); // matches counter 1, 2 and 3
            
            // all 4 events affected counter 1
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 4, 68L), addDelta(initNarrowWindowValue, 4, 68L)), getCounter1());
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 4, 68L), initNarrowWindowValueLastFull), getCounter1LastFull()); // only wide affected
            
            // 2 events affected counter 2
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 2, 31L), addDelta(initNarrowWindowValue, 2, 31L)), getCounter2());
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 2, 31L), initNarrowWindowValueLastFull), getCounter2LastFull()); // only wide affected
            
            // 1 event affected counter 3
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 1, 10L), addDelta(initNarrowWindowValue, 1, 10L)), getCounter3());
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 1, 10L), initNarrowWindowValueLastFull), getCounter3LastFull()); // only wide affected
            
            /* ADDING A BATCH OF EVENTS2 (dated yesterday) ********************************************************************* */
            
            // Do the same with events on yesterday, to update last full narrow window.
            final long currentTimeMillis2 = System.currentTimeMillis() - WindowTimeUnit.MILLISECONDS_IN_DAY;
            final List<TestEventEntity> events2 = createAndStoreEvent(true,
                tuple(Context.empty(TestCounterDef.getInstance()), 5L, currentTimeMillis2), // matches counter 1 only
                tuple(Context.newBuilder(TestCounterDef.getInstance()).add(ADimensionDef.getInstance().create(AEnum.DDD)).build(),
                    32L,
                    currentTimeMillis2), // matches counter 1 only
                tuple(Context.newBuilder(TestCounterDef.getInstance()).add(ADimensionDef.getInstance().create(AEnum.AAA)).build(),
                    21L,
                    currentTimeMillis2), // matches counter 1 and 2
                tuple(Context.newBuilder(TestCounterDef.getInstance())
                    .add(ADimensionDef.getInstance().create(AEnum.AAA))
                    .add(BDimensionDef.getInstance().create(10L))
                    .add(CDimensionDef.getInstance().create(20L))
                    .build(),
                    10L,
                    currentTimeMillis2)); // matches counter 1, 2 and 3
            
            // all 4 events affected counter 1
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 8, 68L * 2), addDelta(initNarrowWindowValue, 4, 68L)), getCounter1());
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 8, 68L * 2), addDelta(initNarrowWindowValueLastFull, 4, 68L)),
                getCounter1LastFull());
            
            // 2 events affected counter 2
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 4, 31L * 2), addDelta(initNarrowWindowValue, 2, 31L)), getCounter2());
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 4, 31L * 2), addDelta(initNarrowWindowValueLastFull, 2, 31L)),
                getCounter2LastFull());
            
            // 1 event affected counter 3
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 2, 10L * 2), addDelta(initNarrowWindowValue, 1, 10L)), getCounter3());
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 2, 10L * 2), addDelta(initNarrowWindowValueLastFull, 1, 10L)),
                getCounter3LastFull());
            
            /* ADDING A BATCH OF EVENTS2 (dated 2 days ago) ******************************************************************** */
            
            // Do the same with events on 2 days ago - only wide window affected
            final long currentTimeMillis3 = System.currentTimeMillis() - WindowTimeUnit.MILLISECONDS_IN_DAY * 2;
            final List<TestEventEntity> events3 = createAndStoreEvent(true,
                tuple(Context.empty(TestCounterDef.getInstance()), 5L, currentTimeMillis3), // matches counter 1 only
                tuple(Context.newBuilder(TestCounterDef.getInstance()).add(ADimensionDef.getInstance().create(AEnum.DDD)).build(),
                    32L,
                    currentTimeMillis3), // matches counter 1 only
                tuple(Context.newBuilder(TestCounterDef.getInstance()).add(ADimensionDef.getInstance().create(AEnum.AAA)).build(),
                    21L,
                    currentTimeMillis3), // matches counter 1 and 2
                tuple(Context.newBuilder(TestCounterDef.getInstance())
                    .add(ADimensionDef.getInstance().create(AEnum.AAA))
                    .add(BDimensionDef.getInstance().create(10L))
                    .add(CDimensionDef.getInstance().create(20L))
                    .build(),
                    10L,
                    currentTimeMillis3)); // matches counter 1, 2 and 3
            
            // all 4 events affected counter 1
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 12, 68L * 3), addDelta(initNarrowWindowValue, 4, 68L)), getCounter1());
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 12, 68L * 3), addDelta(initNarrowWindowValueLastFull, 4, 68L)),
                getCounter1LastFull());
            
            // 2 events affected counter 2
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 6, 31L * 3), addDelta(initNarrowWindowValue, 2, 31L)), getCounter2());
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 6, 31L * 3), addDelta(initNarrowWindowValueLastFull, 2, 31L)),
                getCounter2LastFull());
            
            // 1 event affected counter 3
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 3, 10L * 3), addDelta(initNarrowWindowValue, 1, 10L)), getCounter3());
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 3, 10L * 3), addDelta(initNarrowWindowValueLastFull, 1, 10L)),
                getCounter3LastFull());
            
            /* ADDING A BATCH OF EVENTS4 (dated 8 days ago) ******************************************************************** */
            
            // Do the same with events on 8 days ago - nothing should be affected
            final long currentTimeMillis4 = System.currentTimeMillis() - WindowTimeUnit.MILLISECONDS_IN_DAY * 8;
            final List<TestEventEntity> events4 = createAndStoreEvent(true,
                tuple(Context.empty(TestCounterDef.getInstance()), 5L, currentTimeMillis4), // matches counter 1 only
                tuple(Context.newBuilder(TestCounterDef.getInstance()).add(ADimensionDef.getInstance().create(AEnum.DDD)).build(),
                    32L,
                    currentTimeMillis4), // matches counter 1 only
                tuple(Context.newBuilder(TestCounterDef.getInstance()).add(ADimensionDef.getInstance().create(AEnum.AAA)).build(),
                    21L,
                    currentTimeMillis4), // matches counter 1 and 2
                tuple(Context.newBuilder(TestCounterDef.getInstance())
                    .add(ADimensionDef.getInstance().create(AEnum.AAA))
                    .add(BDimensionDef.getInstance().create(10L))
                    .add(CDimensionDef.getInstance().create(20L))
                    .build(),
                    10L,
                    currentTimeMillis4)); // matches counter 1, 2 and 3
            
            // all 4 events affected counter 1
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 12, 68L * 3), addDelta(initNarrowWindowValue, 4, 68L)), getCounter1());
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 12, 68L * 3), addDelta(initNarrowWindowValueLastFull, 4, 68L)),
                getCounter1LastFull());
            
            // 2 events affected counter 2
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 6, 31L * 3), addDelta(initNarrowWindowValue, 2, 31L)), getCounter2());
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 6, 31L * 3), addDelta(initNarrowWindowValueLastFull, 2, 31L)),
                getCounter2LastFull());
            
            // 1 event affected counter 3
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 3, 10L * 3), addDelta(initNarrowWindowValue, 1, 10L)), getCounter3());
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 3, 10L * 3), addDelta(initNarrowWindowValueLastFull, 1, 10L)),
                getCounter3LastFull());
            
            /* REVERTING ********************************************************************************************************** */
            // Reverting last batch of events
            updateAndStoreEvent(true, events4);
            
            // nothing should be affected
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 12, 68L * 3), addDelta(initNarrowWindowValue, 4, 68L)), getCounter1());
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 12, 68L * 3), addDelta(initNarrowWindowValueLastFull, 4, 68L)),
                getCounter1LastFull());
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 6, 31L * 3), addDelta(initNarrowWindowValue, 2, 31L)), getCounter2());
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 6, 31L * 3), addDelta(initNarrowWindowValueLastFull, 2, 31L)),
                getCounter2LastFull());
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 3, 10L * 3), addDelta(initNarrowWindowValue, 1, 10L)), getCounter3());
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 3, 10L * 3), addDelta(initNarrowWindowValueLastFull, 1, 10L)),
                getCounter3LastFull());
            
            // Reverting batch of events3
            updateAndStoreEvent(true, events3);
            
            // back to were we where after executing batch of events2
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 8, 68L * 2), addDelta(initNarrowWindowValue, 4, 68L)), getCounter1());
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 8, 68L * 2), addDelta(initNarrowWindowValueLastFull, 4, 68L)),
                getCounter1LastFull());
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 4, 31L * 2), addDelta(initNarrowWindowValue, 2, 31L)), getCounter2());
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 4, 31L * 2), addDelta(initNarrowWindowValueLastFull, 2, 31L)),
                getCounter2LastFull());
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 2, 10L * 2), addDelta(initNarrowWindowValue, 1, 10L)), getCounter3());
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 2, 10L * 2), addDelta(initNarrowWindowValueLastFull, 1, 10L)),
                getCounter3LastFull());
            
            // Reverting batch 2
            updateAndStoreEvent(true, events2);
            
            // back to state after executing batch of events1
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 4, 68L), addDelta(initNarrowWindowValue, 4, 68L)), getCounter1());
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 4, 68L), initNarrowWindowValueLastFull), getCounter1LastFull()); // only wide affected
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 2, 31L), addDelta(initNarrowWindowValue, 2, 31L)), getCounter2());
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 2, 31L), initNarrowWindowValueLastFull), getCounter2LastFull()); // only wide affected
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 1, 10L), addDelta(initNarrowWindowValue, 1, 10L)), getCounter3());
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 1, 10L), initNarrowWindowValueLastFull), getCounter3LastFull()); // only wide affected
            
            // Reverting batch 1
            updateAndStoreEvent(true, events1);
            
            // back to initial state
            assertEquals(new CounterValue(initWideWindowValue, initNarrowWindowValue), getCounter1());
            assertEquals(new CounterValue(initWideWindowValue, initNarrowWindowValueLastFull), getCounter1LastFull()); // only wide affected
            assertEquals(new CounterValue(initWideWindowValue, initNarrowWindowValue), getCounter2());
            assertEquals(new CounterValue(initWideWindowValue, initNarrowWindowValueLastFull), getCounter2LastFull()); // only wide affected
            assertEquals(new CounterValue(initWideWindowValue, initNarrowWindowValue), getCounter3());
            assertEquals(new CounterValue(initWideWindowValue, initNarrowWindowValueLastFull), getCounter3LastFull()); // only wide affected
        }));
    }
    
    @Test
    public void testSingleWindowCounters() throws Exception {
        DATA_UNIT.exec(UNIT, () -> TENANT.exec(TestTenants.DEFAULT, () -> {
            // initial values matching ones generated by TestCounterDef
            final WindowValue initNarrowWindowValue = TestCounterDef
                .getInstance()
                .fetchWindow(null, // not used
                    new WindowWidth(1, WindowTimeUnit.DAY).getStartTimestamp(System.currentTimeMillis()),
                    null); // not used
            assertEquals(new CounterValue(initNarrowWindowValue, initNarrowWindowValue), getCounterSingle());
            
            // Create a batch with 2 events dated now
            final long currentTimeMillis1 = System.currentTimeMillis();
            final List<TestEventEntity> events1 = createAndStoreEvent(true,
                tuple(Context.empty(TestCounterDef.getInstance()), 5L, currentTimeMillis1),
                tuple(Context.newBuilder(TestCounterDef.getInstance()).add(ADimensionDef.getInstance().create(AEnum.DDD)).build(),
                    10L,
                    currentTimeMillis1));
            
            assertEquals(new CounterValue(addDelta(initNarrowWindowValue, 2, 15L), addDelta(initNarrowWindowValue, 2, 15L)),
                getCounterSingle());
            
            // Do the same with events on yesterday, will not affect anything.
            final long currentTimeMillis2 = System.currentTimeMillis() - WindowTimeUnit.MILLISECONDS_IN_DAY;
            final List<TestEventEntity> events2 = createAndStoreEvent(true,
                tuple(Context.empty(TestCounterDef.getInstance()), 5L, currentTimeMillis2),
                tuple(Context.newBuilder(TestCounterDef.getInstance()).add(ADimensionDef.getInstance().create(AEnum.DDD)).build(),
                    10L,
                    currentTimeMillis2));
            
            assertEquals(new CounterValue(addDelta(initNarrowWindowValue, 2, 15), addDelta(initNarrowWindowValue, 2, 15L)),
                getCounterSingle());
            
            // Reverting batch 2 - nothing should happen
            updateAndStoreEvent(true, events2);
            assertEquals(new CounterValue(addDelta(initNarrowWindowValue, 2, 15), addDelta(initNarrowWindowValue, 2, 15L)),
                getCounterSingle());
            
            // Reverting batch 1 - back to init
            updateAndStoreEvent(true, events1);
            assertEquals(new CounterValue(initNarrowWindowValue, initNarrowWindowValue), getCounterSingle());
            
            // test Last full for single window
            try {
                block(counters.getCounter(Context.empty(TestCounterDef.getInstance()), new WindowWidth(1, WindowTimeUnit.DAY), 1, true));
                fail();
            } catch (final IllegalArgumentException ex) {
                // expected
            }
        }));
    }
    
    @Test
    public void testWindowLessCounters() throws Exception {
        DATA_UNIT.exec(UNIT, () -> TENANT.exec(TestTenants.DEFAULT, () -> {
            // initial values matching ones generated by TestCounterDef
            final WindowValue initNarrowWindowValue = TestCounterDef
                .getInstance()
                .fetchWindow(null, // not used
                    new WindowWidth(1, WindowTimeUnit.ALWAYS).getStartTimestamp(System.currentTimeMillis()),
                    null); // not used
            assertEquals(new CounterValue(initNarrowWindowValue, initNarrowWindowValue), getCounterAlways());
            
            // Create a batch with 2 events dated now
            final long currentTimeMillis1 = System.currentTimeMillis();
            final List<TestEventEntity> events1 = createAndStoreEvent(true,
                tuple(Context.empty(TestCounterDef.getInstance()), 5L, currentTimeMillis1),
                tuple(Context.newBuilder(TestCounterDef.getInstance()).add(ADimensionDef.getInstance().create(AEnum.DDD)).build(),
                    10L,
                    currentTimeMillis1));
            
            assertEquals(new CounterValue(addDelta(initNarrowWindowValue, 2, 15L), addDelta(initNarrowWindowValue, 2, 15L)),
                getCounterAlways());
            
            CounterValue counterAlways = getCounterAlways();
            
            // Do the same with events on yesterday, will not affect anything.
            final long currentTimeMillis2 = System.currentTimeMillis() - WindowTimeUnit.MILLISECONDS_IN_DAY;
            final List<TestEventEntity> events2 = createAndStoreEvent(true,
                tuple(Context.empty(TestCounterDef.getInstance()), 5L, currentTimeMillis2),
                tuple(Context.newBuilder(TestCounterDef.getInstance()).add(ADimensionDef.getInstance().create(AEnum.DDD)).build(),
                    10L,
                    currentTimeMillis2));
            
            assertEquals(new CounterValue(addDelta(counterAlways.getNarrow(), 2, 15), addDelta(counterAlways.getNarrow(), 2, 15L)),
                getCounterAlways());
            
            // Reverting batch 2 - nothing should happen
            updateAndStoreEvent(true, events2);
            assertEquals(new CounterValue(addDelta(initNarrowWindowValue, 2, 15), addDelta(initNarrowWindowValue, 2, 15L)),
                getCounterAlways());
            
            // Reverting batch 1 - back to init
            updateAndStoreEvent(true, events1);
            assertEquals(new CounterValue(initNarrowWindowValue, initNarrowWindowValue), getCounterAlways());
            
            // test Last full for single window
            try {
                block(counters.getCounter(Context.empty(TestCounterDef.getInstance()), new WindowWidth(1, WindowTimeUnit.ALWAYS), 1, true));
                fail();
            } catch (final IllegalArgumentException ex) {
                // expected
            }
        }));
    }
    
    @Test
    public void testAsync() throws Throwable {
        // TODO remove this test as now async by default
        DATA_UNIT.exec(UNIT, () -> TENANT.exec(TestTenants.DEFAULT, () -> {
            // initial values matching ones generated by TestCounterDef
            final WindowValue initNarrowWindowValue = TestCounterDef
                .getInstance()
                .fetchWindow(null, // not used
                    new WindowWidth(1, WindowTimeUnit.DAY).getStartTimestamp(System.currentTimeMillis()),
                    null); // not used
            final WindowValue initWideWindowValue = new WindowValue(initNarrowWindowValue.getCount() + (6 * 24),
                initNarrowWindowValue.getSum() + (60 * 24));
            assertEquals(new CounterValue(initWideWindowValue, initNarrowWindowValue), getCounterForAsync());
            
            final List<TestEventEntity> events = createAndStoreEvent(true,
                tuple(Context.newBuilder(TestCounterDef.getInstance()).add(ADimensionDef.getInstance().create(AEnum.ASY)).build(),
                    5L,
                    System.currentTimeMillis()));
            
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 1, 5L), addDelta(initNarrowWindowValue, 1, 5L)), getCounterForAsync());
            
            updateAndStoreEvent(true, events);
            
            // back to initial values
            assertEquals(new CounterValue(initWideWindowValue, initNarrowWindowValue), getCounterForAsync());
        }));
    }
    
    @Test
    public void testCleanup() throws Throwable {
        DATA_UNIT.exec(UNIT, () -> TENANT.exec(TestTenants.DEFAULT, () -> {
            createAndStoreEvent(true,
                tuple(Context.newBuilder(TestCounterDef.getInstance()).add(ADimensionDef.getInstance().create(AEnum.CLN)).build(),
                    5L,
                    System.currentTimeMillis()));
            CounterValue counterValue = getCounterForCleanup(); // an updated counter - lastUpdated is now, which is greater than
            // lastQueried by a bit
            
            // countersCleanupScheduledTask.run("", null);
            block(countersAdmin.cleanUp(7 * WindowTimeUnit.MILLISECONDS_IN_DAY));
            
            assertEquals(counterValue, getCounterForCleanup()); // should stay the same - not cleaned up
            
            Thread.sleep(100L);
            
            createAndStoreEvent(true,
                tuple(Context.newBuilder(TestCounterDef.getInstance()).add(ADimensionDef.getInstance().create(AEnum.CLN)).build(),
                    5L,
                    System.currentTimeMillis()));
            
            // countersCleanupScheduledTask.run("", null);
            block(countersAdmin.cleanUp(100L));
            
            // initial values matching ones generated by TestCounterDef
            final WindowValue initNarrowWindowValue = TestCounterDef
                .getInstance()
                .fetchWindow(null, // not used
                    new WindowWidth(1, WindowTimeUnit.DAY).getStartTimestamp(System.currentTimeMillis()),
                    null); // not used
            final WindowValue initWideWindowValue = new WindowValue(initNarrowWindowValue.getCount() + (6 * 24),
                initNarrowWindowValue.getSum() + (60 * 24));
            
            // counter back to initial values
            assertNotSame(counterValue, getCounterForCleanup());
            assertEquals(new CounterValue(initWideWindowValue, initNarrowWindowValue), getCounterForCleanup());
        }));
    }
    
    @Test
    public void testConcurrency() throws Throwable {
        DATA_UNIT.exec(UNIT, () -> TENANT.exec(TestTenants.DEFAULT, () -> {
            // initial values matching ones generated by TestCounterDef
            WindowValue initNarrowWindowValue = TestCounterDef
                .getInstance()
                .fetchWindow(null, // not used
                    new WindowWidth(1, WindowTimeUnit.DAY).getStartTimestamp(System.currentTimeMillis()),
                    null); // not used
            WindowValue initWideWindowValue = new WindowValue(initNarrowWindowValue.getCount() + (6 * 24),
                initNarrowWindowValue.getSum() + (60 * 24));
            
            assertEquals(new CounterValue(initWideWindowValue, initNarrowWindowValue), getCounterForStress());
            
            final int NUM_OF_READ_THREADS = 50;
            final int NUM_OF_WRITE_THREADS = 5;
            final int NUM_OF_EVENTS_PER_READ_THREAD = 10;
            final int NUM_OF_EVENTS_PER_WRITE_THREAD = 10;
            final int NUM_OF_WRITE_EVENTS = NUM_OF_WRITE_THREADS * NUM_OF_EVENTS_PER_READ_THREAD;
            
            Thread[] writeThreads = new Thread[NUM_OF_WRITE_THREADS];
            Thread[] readThreads = new Thread[NUM_OF_READ_THREADS];
            
            long time = System.nanoTime(); // start time
            
            final AtomicLong aggregate = new AtomicLong();
            
            for (int i = 0; i < NUM_OF_READ_THREADS; i++) {
                readThreads[i] =
                    new Thread(AsyncLocal.wrap(() -> {
                        for (int i12 = 0; i12 < NUM_OF_EVENTS_PER_READ_THREAD; i12++) {
                            
                            long time1 = System.nanoTime(); // start time
                            try {
                                getCounterForStress();
                            } catch (final Throwable t) {
                                throw new IllegalStateException(t);
                            }
                            aggregate.addAndGet(System.nanoTime() - time1);
                        }
                    }));
                readThreads[i].start();
            }
            
            final AtomicInteger failures = new AtomicInteger();
            for (int i = 0; i < NUM_OF_WRITE_THREADS; i++) {
                writeThreads[i] =
                    new Thread(AsyncLocal.wrap(() -> {
                        for (int i1 = 0; i1 < NUM_OF_EVENTS_PER_WRITE_THREAD; i1++) {
                            try {
                                createAndStoreEvent(true,
                                    tuple(Context.newBuilder(TestCounterDef.getInstance())
                                        .add(ADimensionDef.getInstance().create(AEnum.STR))
                                        .build(),
                                        10L,
                                        System.currentTimeMillis()));
                            } catch (final Exception e) {
                                e.printStackTrace();
                                failures.incrementAndGet(); // optimistic lock aspect gave up!
                            }
                        }
                    }));
                writeThreads[i].start();
            }
            
            // wait for all threads to complete
            for (int i = 0; i < NUM_OF_READ_THREADS; i++) {
                try {
                    readThreads[i].join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            for (int i = 0; i < NUM_OF_WRITE_THREADS; i++) {
                try {
                    writeThreads[i].join();
                } catch (InterruptedException e) {
                    e.printStackTrace();
                }
            }
            
            LOG.info("Total test time (ms): " + TimeUnit.MILLISECONDS.convert(System.nanoTime() - time, TimeUnit.NANOSECONDS));
            LOG.info("Aggregate read (ms): " + TimeUnit.MILLISECONDS.convert(aggregate.get(), TimeUnit.NANOSECONDS));
            LOG.info("Average read (ms): "
                + TimeUnit.MILLISECONDS.convert(aggregate.get(), TimeUnit.NANOSECONDS)
                    / (NUM_OF_READ_THREADS * NUM_OF_EVENTS_PER_READ_THREAD));
            LOG.info("Failures: " + failures.get());
            
            final CounterValue counterValue = new CounterValue(addDelta(initWideWindowValue,
                NUM_OF_WRITE_EVENTS - failures.get(),
                10L * (NUM_OF_WRITE_EVENTS - failures.get())),
                addDelta(initNarrowWindowValue, NUM_OF_WRITE_EVENTS - failures.get(), 10L * (NUM_OF_WRITE_EVENTS - failures.get())));
            
            assertEquals(counterValue, getCounterForStress());
        }));
        // cacheable set from ADimensionDef
        
        // Time in ms
        // 1000 reads 1 thread 2 threads 3 threads 5 threads
        // cacheable 3000 3058 5641 7000
        // non-cacheable 5300 6000 7400 6600
        
        // 1000 reads & writes 1 thread 2 threads 3 threads 5 threads
        // cacheable 12000 7948 22500
        // non-cacheable 11000 7000 15000
    }
    
    @Test
    public void testAsyncConcurrent() throws Throwable {
        DATA_UNIT.exec(UNIT, () -> TENANT.exec(TestTenants.DEFAULT, () -> {
            // initial values matching ones generated by TestCounterDef
            final WindowValue initNarrowWindowValue = TestCounterDef
                .getInstance()
                .fetchWindow(null, // not used
                    new WindowWidth(1, WindowTimeUnit.DAY).getStartTimestamp(System.currentTimeMillis()),
                    null); // not used
            final WindowValue initWideWindowValue = new WindowValue(initNarrowWindowValue.getCount() + (6 * 24),
                initNarrowWindowValue.getSum() + (60 * 24));
            assertEquals(new CounterValue(initWideWindowValue, initNarrowWindowValue), getCounterForAsyncConcurrent());
            
            long time = System.currentTimeMillis(); // start time
            
            for (int i = 0; i < 10; i++) {
                List<TestEventEntity> events = createAndStoreEvent(false,
                    tuple(Context.newBuilder(TestCounterDef.getInstance()).add(ADimensionDef.getInstance().create(AEnum.ASC)).build(),
                        5L,
                        System.currentTimeMillis()));
                
                updateAndStoreEvent(false, events);
            }
            
            for (int i = 0; i < 10; i++) {
                createAndStoreEvent(false,
                    tuple(Context.newBuilder(TestCounterDef.getInstance()).add(ADimensionDef.getInstance().create(AEnum.ASC)).build(),
                        6L,
                        System.currentTimeMillis()));
            }
            
            for (int i = 0; i < 10; i++) {
                createAndStoreEvent(false,
                    tuple(Context.newBuilder(TestCounterDef.getInstance()).add(ADimensionDef.getInstance().create(AEnum.ASC)).build(),
                        -6L,
                        System.currentTimeMillis()));
            }
            
            waitForPendingEventsToBeProcessed();
            
            LOG.info("Total test time: " + (System.currentTimeMillis() - time));
            
            // back to initial values
            assertEquals(new CounterValue(addDelta(initWideWindowValue, 20L, 0L), addDelta(initNarrowWindowValue, 20L, 0L)),
                getCounterForAsyncConcurrent());
        }));
    }
    
}
