package com.ixaris.commons.dimensions.limits;

import static com.ixaris.commons.async.lib.CompletionStageUtil.block;
import static com.ixaris.commons.dimensions.counters.jooq.tables.LibDimCounterEventQueue.LIB_DIM_COUNTER_EVENT_QUEUE;
import static com.ixaris.commons.jooq.persistence.TransactionalDSLContext.JOOQ_TX;
import static com.ixaris.commons.misc.lib.object.Tuple.tuple;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;
import static com.ixaris.commons.multitenancy.lib.data.DataUnit.DATA_UNIT;
import static java.util.function.Function.identity;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.Assert.assertEquals;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.assertj.core.api.ObjectAssert;
import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ixaris.commons.async.test.CompletionStageAssert;
import com.ixaris.commons.clustering.lib.extra.LocalCluster;
import com.ixaris.commons.dimensions.counters.AtLeastOnceApplyCounterEventType;
import com.ixaris.commons.dimensions.counters.CounterDefRegistry;
import com.ixaris.commons.dimensions.counters.CountersHelper;
import com.ixaris.commons.dimensions.counters.WindowTimeUnit;
import com.ixaris.commons.dimensions.counters.WindowWidth;
import com.ixaris.commons.dimensions.counters.cache.ClusterShardedCounterCacheProvider;
import com.ixaris.commons.dimensions.lib.context.Context;
import com.ixaris.commons.dimensions.lib.context.Dimension;
import com.ixaris.commons.dimensions.limits.CommonsDimensionsLimits.LimitValue;
import com.ixaris.commons.dimensions.limits.admin.LimitsAdminHelper;
import com.ixaris.commons.dimensions.limits.admin.LimitsAdminProto;
import com.ixaris.commons.dimensions.limits.cache.LocalLimitCacheWithClusterInvalidateProvider;
import com.ixaris.commons.dimensions.limits.data.LimitEntity;
import com.ixaris.commons.dimensions.limits.data.NoLimitExtensionEntity;
import com.ixaris.commons.dimensions.limits.jooq.tables.records.LibDimLimitRecord;
import com.ixaris.commons.dimensions.limits.support.Long1DimensionDef;
import com.ixaris.commons.dimensions.limits.support.Long2DimensionDef;
import com.ixaris.commons.dimensions.limits.support.TestAggregateLimit;
import com.ixaris.commons.dimensions.limits.support.TestCounterDef;
import com.ixaris.commons.dimensions.limits.support.TestEventEntity;
import com.ixaris.commons.dimensions.limits.support.TestLimit;
import com.ixaris.commons.dimensions.limits.support.TestOptionalDimensionsLimit;
import com.ixaris.commons.hikari.test.JooqHikariTestHelper;
import com.ixaris.commons.jooq.persistence.JooqAsyncPersistenceProvider;
import com.ixaris.commons.jooq.persistence.JooqMultiTenancyConfiguration;
import com.ixaris.commons.jooq.persistence.JooqSyncPersistenceProvider;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;
import com.ixaris.commons.misc.lib.object.Tuple3;
import com.ixaris.commons.misc.lib.registry.Registry;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.lib.async.ExecutorMultiTenantAtLeastOnceProcessorFactory;
import com.ixaris.commons.multitenancy.test.TestTenants;
import com.ixaris.commons.protobuf.lib.CommonsProtobufLib.NullableInt64;

public class LimitsIT {
    
    private static final String UNIT = "unit_limits";
    private static JooqHikariTestHelper TEST_HELPER = new JooqHikariTestHelper(Collections.singleton(UNIT), TestTenants.DEFAULT);
    
    private static final LocalCluster localCluster = new LocalCluster(Collections.emptySet(), Collections.emptySet());
    
    private static final JooqAsyncPersistenceProvider provider = new JooqAsyncPersistenceProvider(JooqMultiTenancyConfiguration.createDslContext(TEST_HELPER
        .getDataSource()));
    private static final JooqSyncPersistenceProvider syncProvider = new JooqSyncPersistenceProvider(JooqMultiTenancyConfiguration.createDslContext(TEST_HELPER
        .getDataSource()));
    private static final ClusterShardedCounterCacheProvider counterCache = new ClusterShardedCounterCacheProvider(
        TEST_HELPER.getMultiTenancy(), localCluster);
    private static final MultiTenancy multiTenancy = new MultiTenancy();
    private static final CountersHelper counters = new CountersHelper(provider, counterCache, localCluster);
    
    private static final LocalLimitCacheWithClusterInvalidateProvider cache = new LocalLimitCacheWithClusterInvalidateProvider(multiTenancy);
    private static final LimitsHelper limits = new LimitsHelper(provider, cache, counters);
    private static final LimitsAdminHelper limitsAdmin = new LimitsAdminHelper(provider, cache);
    private static final LimitsAdminProto limitsProto = new LimitsAdminProto(limits, limitsAdmin);
    
    private static AtLeastOnceApplyCounterEventType eventType;
    
    @BeforeClass
    public static void setup() {
        System.setProperty("spring.application.name", UNIT);
        counters.startup();
        counterCache.startup();
        
        ValueLimitDefRegistry.getInstance().postConstruct();
        CounterLimitDefRegistry.getInstance().postConstruct();
        CounterDefRegistry.getInstance().postConstruct();
        Registry.registerInApplicableRegistries(TestLimit.getInstance());
        Registry.registerInApplicableRegistries(TestCounterDef.getInstance());
        eventType = new AtLeastOnceApplyCounterEventType(
            provider,
            counterCache,
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
        TENANT.exec(TestTenants.DEFAULT, () -> counterCache.of(TestCounterDef.getInstance()).clear());
        
        eventType.stop();
        TEST_HELPER.destroy();
        
        Registry.unregisterFromApplicableRegistries(TestCounterDef.getInstance());
        Registry.unregisterFromApplicableRegistries(TestLimit.getInstance());
        ValueLimitDefRegistry.getInstance().preDestroy();
        CounterLimitDefRegistry.getInstance().preDestroy();
        CounterDefRegistry.getInstance().preDestroy();
        
        counterCache.shutdown();
        counters.shutdown();
    }
    
    @Test
    public void testSetLimits() throws Throwable {
        DATA_UNIT.exec(UNIT, () -> TENANT.exec(TestTenants.DEFAULT, () -> {
            long now = System.currentTimeMillis() + 5000L;
            
            block(limitsProto.setValueLimit(TestLimit.getInstance().getKey(),
                Context.empty(TestLimit.getInstance()).toProtobuf(),
                LimitValue.newBuilder()
                    .setEffectiveFrom(now)
                    .setEffectiveTo(now + 19999L)
                    .setMaxCount(NullableInt64.newBuilder().setValue(10L))
                    .setMinAmount(NullableInt64.newBuilder().setHasValue(true))
                    .setMaxAmount(NullableInt64.newBuilder().setValue(1000L))
                    .build(),
                NoLimitExtensionEntity.getCreate()));
            
            block(limitsAdmin.setLimit(Context.empty(TestLimit.getInstance()),
                now + 7500L,
                now + 14999L,
                10L,
                0L,
                1000L,
                NoLimitExtensionEntity.getCreate()));
            
            block(limitsAdmin.setLimit(Context.empty(TestLimit.getInstance()),
                now + 5000L,
                now + 9999L,
                10L,
                0L,
                1000L,
                NoLimitExtensionEntity.getCreate()));
            
            block(limitsAdmin.setLimit(Context.empty(TestLimit.getInstance()),
                now + 20000L,
                now + 24999L,
                10L,
                0L,
                1000L,
                NoLimitExtensionEntity.getCreate()));
            
            block(limitsAdmin.setLimit(Context.empty(TestLimit.getInstance()),
                now + 25000L,
                null,
                10L,
                0L,
                1000L,
                NoLimitExtensionEntity.getCreate()));
            
            while (System.currentTimeMillis() < now) {
                Thread.sleep(50L);
            }
            
            final List<LimitEntity<NoLimitExtensionEntity, TestLimit>> allLimitsMatchingContext = block(limitsAdmin
                .getAllLimitsMatchingContext(Context.empty(TestLimit.getInstance())));
            assertEquals(6, allLimitsMatchingContext.size());
            int haveEffectiveTo = 0;
            for (final LimitEntity<NoLimitExtensionEntity, TestLimit> l : allLimitsMatchingContext) {
                if (l.getLimit().getEffectiveTo() != null) {
                    haveEffectiveTo++;
                    assertEquals(5000L, l.getLimit().getEffectiveTo() - l.getLimit().getEffectiveFrom() + 1L);
                }
            }
            assertEquals(5, haveEffectiveTo);
        }));
    }
    
    @Test
    public void testMostRestrictive() throws Throwable {
        DATA_UNIT.exec(UNIT, () -> TENANT.exec(TestTenants.DEFAULT, () -> {
            long now = System.currentTimeMillis() + 5000L;
            
            block(limitsAdmin.setLimit(Context.empty(TestLimit.getInstance()), now, null, 10L, 0L, 1000L, NoLimitExtensionEntity.getCreate()));
            
            block(limitsAdmin.setLimit(Context.newBuilder(TestLimit.getInstance())
                .add(Long1DimensionDef.getInstance().createMatchAnyDimension())
                .build(),
                now,
                null,
                5L,
                0L,
                1000L,
                NoLimitExtensionEntity.getCreate()));
            
            block(limitsAdmin.setLimit(Context.newBuilder(TestLimit.getInstance())
                .add(Long2DimensionDef.getInstance().createMatchAnyDimension())
                .build(),
                now,
                null,
                10L,
                10L,
                1000L,
                NoLimitExtensionEntity.getCreate()));
            
            block(limitsAdmin.setLimit(Context.newBuilder(TestLimit.getInstance())
                .add(Long1DimensionDef.getInstance().createMatchAnyDimension())
                .add(Long2DimensionDef.getInstance().createMatchAnyDimension())
                .build(),
                now,
                null,
                10L,
                0L,
                100L,
                NoLimitExtensionEntity.getCreate()));
            
            while (System.currentTimeMillis() < now) {
                Thread.sleep(50L);
            }
            
            final LimitBounds<NoLimitExtensionEntity, TestLimit> bounds = block(limits.getMostRestrictive(Context.newBuilder(TestLimit
                .getInstance())
                .add(Long1DimensionDef.getInstance().create(1L))
                .add(Long2DimensionDef.getInstance().create(1L))
                .build(),
                null));
            
            assertEquals(5L, bounds.getCount().longValue());
            assertEquals(10L, bounds.getMin().longValue());
            assertEquals(100L, bounds.getMax().longValue());
        }));
    }
    
    @Test
    public void mostRestrictive_matchAnyAndSpecifiedDimension_shouldApplyLimitForSpecificContext() {
        DATA_UNIT.exec(UNIT, () -> TENANT.exec(TestTenants.DEFAULT, () -> {
            final Context<TestOptionalDimensionsLimit> genericLimitContext = Context.newBuilder(TestOptionalDimensionsLimit.getInstance())
                .add(Long1DimensionDef.getInstance().createMatchAnyDimension())
                .build();
            setLimit(genericLimitContext, 1L, 10L, 100L);
            
            final long specificLimitCount = 10L;
            final long specificLimitMinimum = 0L;
            final long specificLimitMaximum = 1000L;
            
            final long dimensionValue = UniqueIdGenerator.generate();
            final Context<TestOptionalDimensionsLimit> specificLimitContext = Context.newBuilder(TestOptionalDimensionsLimit.getInstance())
                .add(Long1DimensionDef.getInstance().create(dimensionValue))
                .build();
            setLimit(specificLimitContext, specificLimitCount, specificLimitMinimum, specificLimitMaximum);
            
            final LimitBounds<NoLimitExtensionEntity, TestOptionalDimensionsLimit> bounds = CompletionStageAssert
                .assertThat(limits.getMostRestrictive(Context.newBuilder(TestOptionalDimensionsLimit.getInstance())
                    .add(Long1DimensionDef.getInstance().create(dimensionValue))
                    .build(),
                    null))
                .await()
                .isFulfilled(identity());
            
            assertThat(bounds.getCount().longValue()).isEqualTo(specificLimitCount);
            assertThat(bounds.getMin().longValue()).isEqualTo(specificLimitMinimum);
            assertThat(bounds.getMax().longValue()).isEqualTo(specificLimitMaximum);
        }));
    }
    
    @Test
    public void testCounterDeltasToReachLimit() throws Throwable {
        DATA_UNIT.exec(UNIT, () -> TENANT.exec(TestTenants.DEFAULT, () -> {
            long now = System.currentTimeMillis() + 5000L;
            
            block(limitsAdmin.setLimit(Context.empty(TestAggregateLimit.getInstance()),
                now,
                null,
                10L,
                -1000L,
                1000L,
                new WindowWidth(1, WindowTimeUnit.DAY),
                1,
                NoLimitExtensionEntity.getCreate()));
            
            block(limitsAdmin.setLimit(Context.newBuilder(TestAggregateLimit.getInstance())
                .add(Long1DimensionDef.getInstance().createMatchAnyDimension())
                .build(),
                now,
                null,
                5L,
                -1000L,
                1000L,
                new WindowWidth(1, WindowTimeUnit.DAY),
                1,
                NoLimitExtensionEntity.getCreate()));
            
            block(limitsAdmin.setLimit(Context.newBuilder(TestAggregateLimit.getInstance())
                .add(Long2DimensionDef.getInstance().createMatchAnyDimension())
                .build(),
                now,
                null,
                10L,
                -1000L,
                1000L,
                new WindowWidth(1, WindowTimeUnit.DAY),
                1,
                NoLimitExtensionEntity.getCreate()));
            
            block(limitsAdmin.setLimit(Context.newBuilder(TestAggregateLimit.getInstance())
                .add(Long1DimensionDef.getInstance().createMatchAnyDimension())
                .add(Long2DimensionDef.getInstance().createMatchAnyDimension())
                .build(),
                now,
                null,
                10L,
                -100L,
                100L,
                new WindowWidth(1, WindowTimeUnit.DAY),
                1,
                NoLimitExtensionEntity.getCreate()));
            
            while (System.currentTimeMillis() < now) {
                Thread.sleep(50L);
            }
            
            final LimitBounds<NoLimitExtensionEntity, TestAggregateLimit> bounds = block(limits.getDeltasToReactLimit(Context
                .newBuilder(TestAggregateLimit.getInstance())
                .add(Long1DimensionDef.getInstance().create(1L))
                .add(Long2DimensionDef.getInstance().create(1L))
                .build(),
                null));
            
            assertEquals(5L, bounds.getCount().longValue());
            assertEquals(-100L, bounds.getMin().longValue());
            assertEquals(100L, bounds.getMax().longValue());
            
            final List<LimitExceeded> limitExceeded1 = block(limits.validateDeltaAgainstLimit(Context.newBuilder(TestAggregateLimit
                .getInstance())
                .add(Long1DimensionDef.getInstance().create(1L))
                .add(Long2DimensionDef.getInstance().create(1L))
                .build(),
                null,
                6L,
                50L));
            assertEquals(1, limitExceeded1.size());
            assertEquals(limitExceeded1.get(0).getFailedLimit().getId(), bounds.getCountLimit().getId());
            
            final List<LimitExceeded> limitExceeded2 = block(limits.validateDeltaAgainstLimit(Context.newBuilder(TestAggregateLimit
                .getInstance())
                .add(Long1DimensionDef.getInstance().create(1L))
                .add(Long2DimensionDef.getInstance().create(1L))
                .build(),
                null,
                1L,
                200L));
            assertEquals(1, limitExceeded2.size());
            assertEquals(limitExceeded2.get(0).getFailedLimit().getId(), bounds.getMaxLimit().getId());
            
            final List<LimitExceeded> limitExceeded3 = block(limits.validateDeltaAgainstLimit(Context.newBuilder(TestAggregateLimit
                .getInstance())
                .add(Long1DimensionDef.getInstance().create(1L))
                .add(Long2DimensionDef.getInstance().create(1L))
                .build(),
                null,
                1L,
                -200L));
            assertEquals(1, limitExceeded3.size());
            assertEquals(limitExceeded3.get(0).getFailedLimit().getId(), bounds.getMinLimit().getId());
            
            createAndStoreEvent(true,
                tuple(Context.newBuilder(TestCounterDef.getInstance())
                    .add(Long1DimensionDef.getInstance().create(1L))
                    .add(Long2DimensionDef.getInstance().create(1L))
                    .build(),
                    10L,
                    System.currentTimeMillis())); // matches counter 1, 2 and 3
            
            final LimitBounds<NoLimitExtensionEntity, TestAggregateLimit> bounds2 = block(limits.getDeltasToReactLimit(Context
                .newBuilder(TestAggregateLimit.getInstance())
                .add(Long1DimensionDef.getInstance().create(1L))
                .add(Long2DimensionDef.getInstance().create(1L))
                .build(),
                null));
            
            assertEquals(4L, bounds2.getCount().longValue());
            assertEquals(-110L, bounds2.getMin().longValue());
            assertEquals(90L, bounds2.getMax().longValue());
        }));
    }
    
    @Test
    public void setLimits_specific_valueForUndefinedOrMarchAnyConfigRequirement() throws Throwable {
        DATA_UNIT.exec(UNIT, () -> TENANT.exec(TestTenants.DEFAULT, () -> {
            long now = System.currentTimeMillis() + 1000L;
            assertThatThrownBy(() -> block(limitsAdmin.setLimit(Context.newBuilder(TestLimit.getInstance())
                .add(Long1DimensionDef.getInstance().create(123L))
                .build(),
                now + 5000L,
                now + 9999L,
                10L,
                0L,
                1000L,
                NoLimitExtensionEntity.getCreate())))
                    .isInstanceOf(IllegalArgumentException.class);
        }));
    }
    
    @Test
    public void getAllLimitsContainingContext() {
        DATA_UNIT.exec(UNIT, () -> TENANT.exec(TestTenants.DEFAULT, () -> {
            final Context<TestOptionalDimensionsLimit> emptyDimensionLimitContext = Context.empty(TestOptionalDimensionsLimit.getInstance());
            final long emptyLimitMaximum = 1000L;
            setLimit(emptyDimensionLimitContext, null, null, emptyLimitMaximum);
            
            final long matchAnyLimitMaximum = 2000L;
            final Context<TestOptionalDimensionsLimit> matchAnyDimensionLimitContext = Context.newBuilder(TestOptionalDimensionsLimit
                .getInstance())
                .add(Long2DimensionDef.getInstance().createMatchAnyDimension())
                .build();
            setLimit(matchAnyDimensionLimitContext, null, null, matchAnyLimitMaximum);
            
            final long specifiedLimitMaximum = 3000L;
            final long dimensionValue = UniqueIdGenerator.generate();
            final Context<TestOptionalDimensionsLimit> specifiedDimensionLimitContext = Context.newBuilder(TestOptionalDimensionsLimit
                .getInstance())
                .add(Long2DimensionDef.getInstance().create(dimensionValue))
                .build();
            setLimit(specifiedDimensionLimitContext, null, null, specifiedLimitMaximum);
            final long specifiedFutureLimitMaximum = 4000L;
            setLimit(specifiedDimensionLimitContext, System.currentTimeMillis() + 10000L, null, null, null, specifiedFutureLimitMaximum);
            
            final long verySpecificLimitMaximum = 5000L;
            final long otherDimensionValue = UniqueIdGenerator.generate();
            final Context<TestOptionalDimensionsLimit> verySpecificDimensionLimitContext = Context.newBuilder(TestOptionalDimensionsLimit
                .getInstance())
                .add(Long2DimensionDef.getInstance().create(dimensionValue))
                .add(Long1DimensionDef.getInstance().create(otherDimensionValue))
                .build();
            setLimit(verySpecificDimensionLimitContext, null, null, verySpecificLimitMaximum);
            
            CompletionStageAssert
                .assertThat(limitsAdmin.getAllLimitsContainingContext(emptyDimensionLimitContext))
                .await()
                .isFulfilled()
                .satisfies(exactlyMatchingContextLimits -> {
                    assertThat(exactlyMatchingContextLimits).hasSize(5);
                    assertThatLimitHasValues(exactlyMatchingContextLimits.get(0).getLimit(), null, null, verySpecificLimitMaximum);
                    assertThatLimitHasValues(exactlyMatchingContextLimits.get(1).getLimit(), null, null, specifiedFutureLimitMaximum);
                    assertThatLimitHasValues(exactlyMatchingContextLimits.get(2).getLimit(), null, null, specifiedLimitMaximum);
                    assertThatLimitHasValues(exactlyMatchingContextLimits.get(3).getLimit(), null, null, matchAnyLimitMaximum);
                    assertThatLimitHasValues(exactlyMatchingContextLimits.get(4).getLimit(), null, null, emptyLimitMaximum);
                });
                
            CompletionStageAssert
                .assertThat(limitsAdmin.getAllLimitsContainingContext(matchAnyDimensionLimitContext))
                .await()
                .isFulfilled()
                .satisfies(exactlyMatchingContextLimits -> {
                    assertThat(exactlyMatchingContextLimits).hasSize(4);
                    assertThatLimitHasValues(exactlyMatchingContextLimits.get(0).getLimit(), null, null, verySpecificLimitMaximum);
                    assertThatLimitHasValues(exactlyMatchingContextLimits.get(1).getLimit(), null, null, specifiedFutureLimitMaximum);
                    assertThatLimitHasValues(exactlyMatchingContextLimits.get(2).getLimit(), null, null, specifiedLimitMaximum);
                    assertThatLimitHasValues(exactlyMatchingContextLimits.get(3).getLimit(), null, null, matchAnyLimitMaximum);
                });
                
            CompletionStageAssert
                .assertThat(limitsAdmin.getAllLimitsContainingContext(specifiedDimensionLimitContext))
                .await()
                .isFulfilled()
                .satisfies(exactlyMatchingContextLimits -> {
                    assertThat(exactlyMatchingContextLimits).hasSize(3);
                    assertThatLimitHasValues(exactlyMatchingContextLimits.get(0).getLimit(), null, null, verySpecificLimitMaximum);
                    assertThatLimitHasValues(exactlyMatchingContextLimits.get(1).getLimit(), null, null, specifiedFutureLimitMaximum);
                    assertThatLimitHasValues(exactlyMatchingContextLimits.get(2).getLimit(), null, null, specifiedLimitMaximum);
                });
        }));
    }
    
    @Test
    public void getAllLimitsExactlyMatchingContext() {
        DATA_UNIT.exec(UNIT, () -> TENANT.exec(TestTenants.DEFAULT, () -> {
            final Context<TestOptionalDimensionsLimit> emptyDimensionLimitContext = Context.empty(TestOptionalDimensionsLimit.getInstance());
            final long emptyLimitCount = 1L;
            final long emptyLimitMinimum = 10L;
            final long emptyLimitMaximum = 100L;
            setLimit(emptyDimensionLimitContext, emptyLimitCount, emptyLimitMinimum, emptyLimitMaximum);
            
            final long matchAnyLimitCount = 2L;
            final long matchAnyLimitMinimum = 20L;
            final long matchAnyLimitMaximum = 200L;
            final Context<TestOptionalDimensionsLimit> matchAnyDimensionLimitContext = Context.newBuilder(TestOptionalDimensionsLimit
                .getInstance())
                .add(Long1DimensionDef.getInstance().createMatchAnyDimension())
                .build();
            setLimit(matchAnyDimensionLimitContext, matchAnyLimitCount, matchAnyLimitMinimum, matchAnyLimitMaximum);
            
            final long specifiedLimitCount = 3L;
            final long specifiedLimitMinimum = 30L;
            final long specifiedLimitMaximum = 300L;
            final long dimensionValue = UniqueIdGenerator.generate();
            final Context<TestOptionalDimensionsLimit> specifiedDimensionLimitContext = Context.newBuilder(TestOptionalDimensionsLimit
                .getInstance())
                .add(Long1DimensionDef.getInstance().create(dimensionValue))
                .build();
            setLimit(specifiedDimensionLimitContext, specifiedLimitCount, specifiedLimitMinimum, specifiedLimitMaximum);
            final long specifiedFutureLimitCount = 4L;
            final long specifiedFutureLimitMinimum = 40L;
            final long specifiedFutureLimitMaximum = 400L;
            setLimit(specifiedDimensionLimitContext,
                System.currentTimeMillis() + 10000L,
                null,
                specifiedFutureLimitCount,
                specifiedFutureLimitMinimum,
                specifiedFutureLimitMaximum);
            
            CompletionStageAssert
                .assertThat(limitsAdmin.getAllLimitsExactlyMatchingContext(emptyDimensionLimitContext))
                .await()
                .isFulfilled()
                .satisfies(exactlyMatchingContextLimits -> {
                    assertThat(exactlyMatchingContextLimits).hasSize(1);
                    assertThatLimitHasValues(exactlyMatchingContextLimits.get(0).getLimit(),
                        emptyLimitCount,
                        emptyLimitMinimum,
                        emptyLimitMaximum);
                });
                
            CompletionStageAssert
                .assertThat(limitsAdmin.getAllLimitsExactlyMatchingContext(matchAnyDimensionLimitContext))
                .await()
                .isFulfilled()
                .satisfies(exactlyMatchingContextLimits -> {
                    assertThat(exactlyMatchingContextLimits).hasSize(1);
                    assertThatLimitHasValues(exactlyMatchingContextLimits.get(0).getLimit(),
                        matchAnyLimitCount,
                        matchAnyLimitMinimum,
                        matchAnyLimitMaximum);
                });
                
            CompletionStageAssert
                .assertThat(limitsAdmin.getAllLimitsExactlyMatchingContext(specifiedDimensionLimitContext))
                .await()
                .isFulfilled()
                .satisfies(exactlyMatchingContextLimits -> {
                    assertThat(exactlyMatchingContextLimits).hasSize(2);
                    assertThatLimitHasValues(exactlyMatchingContextLimits.get(0).getLimit(),
                        specifiedLimitCount,
                        specifiedLimitMinimum,
                        specifiedLimitMaximum);
                    assertThatLimitHasValues(exactlyMatchingContextLimits.get(1).getLimit(),
                        specifiedFutureLimitCount,
                        specifiedFutureLimitMinimum,
                        specifiedFutureLimitMaximum);
                });
        }));
    }
    
    @Test
    public void expireLimit_limitNotExpired_shouldSetEffectiveToToCurrentTime() throws InterruptedException {
        DATA_UNIT.exec(UNIT, () -> TENANT.exec(TestTenants.DEFAULT, () -> {
            final Context<TestOptionalDimensionsLimit> context = Context.newBuilder(TestOptionalDimensionsLimit.getInstance())
                .add(Long1DimensionDef.getInstance().create(UniqueIdGenerator.generate()))
                .build();
            
            block(limitsAdmin.setLimit(context, null, null, 10L, 0L, 1000L, NoLimitExtensionEntity.getCreate()));
            
            CompletionStageAssert
                .assertThat(limits.getAllMatchingLimits(context))
                .await()
                .isFulfilled()
                .satisfies(expiredLimits -> assertThat(expiredLimits.size()).isEqualTo(1));
            
            final long now = System.currentTimeMillis();
            block(limitsAdmin.unsetLimit(context, now + 200L, null));
            
            Thread.sleep(210L);
            
            CompletionStageAssert
                .assertThat(limits.getAllMatchingLimits(context))
                .await()
                .isFulfilled()
                .satisfies(expiredLimits -> assertThat(expiredLimits.size()).isEqualTo(0));
        }));
    }
    
    @SafeVarargs
    private final List<TestEventEntity> createAndStoreEvent(final boolean wait, final Tuple3<Context<TestCounterDef>, Long, Long>... events) throws InterruptedException {
        final List<TestEventEntity> r = syncProvider.transaction(() -> {
            final List<TestEventEntity> entities = new ArrayList<>(events.length);
            for (Tuple3<Context<TestCounterDef>, Long, Long> event : events) {
                final Context<TestCounterDef> context = event.get1();
                final Long long1 = Dimension.valueOrNull(context.getDimension(Long1DimensionDef.getInstance()));
                final Long long2 = Dimension.valueOrNull(context.getDimension(Long2DimensionDef.getInstance()));
                final TestEventEntity entity = new TestEventEntity(event.get2(), event.get3(), true, long1, long2)
                    .store();
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
    
    private static ObjectAssert<Void> setLimit(final Context<TestOptionalDimensionsLimit> context,
                                               final Long count,
                                               final Long minimum,
                                               final Long maximum) {
        return setLimit(context, null, null, count, minimum, maximum);
    }
    
    private static ObjectAssert<Void> setLimit(final Context<TestOptionalDimensionsLimit> context,
                                               final Long effectiveFrom,
                                               final Long effectiveTo,
                                               final Long count,
                                               final Long minimum,
                                               final Long maximum) {
        return CompletionStageAssert
            .assertThat(limitsAdmin.setLimit(context, effectiveFrom, effectiveTo, count, minimum, maximum, NoLimitExtensionEntity.getCreate()))
            .await()
            .isFulfilled();
    }
    
    private void waitForPendingEventsToBeProcessed() throws InterruptedException {
        boolean first = false;
        for (int i = 0; i < 50; i++) {
            Thread.sleep(200L); // some time for from processing
            
            final int count = syncProvider.transaction(() -> JOOQ_TX.get().fetchCount(LIB_DIM_COUNTER_EVENT_QUEUE));
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
    
    private void assertThatLimitHasValues(final LibDimLimitRecord limit,
                                          final Long expectedCount,
                                          final Long expectedMinimum,
                                          final Long expectedMaximum) {
        assertThat(limit.getMaxCount()).isEqualTo(expectedCount);
        assertThat(limit.getMinAmount()).isEqualTo(expectedMinimum);
        assertThat(limit.getMaxAmount()).isEqualTo(expectedMaximum);
    }
}
