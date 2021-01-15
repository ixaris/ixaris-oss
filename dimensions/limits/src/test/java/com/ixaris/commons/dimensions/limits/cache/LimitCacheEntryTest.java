package com.ixaris.commons.dimensions.limits.cache;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.fail;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;
import java.util.SortedSet;
import java.util.concurrent.Callable;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ixaris.commons.dimensions.lib.context.Context;
import com.ixaris.commons.dimensions.lib.context.Context.Builder;
import com.ixaris.commons.dimensions.lib.context.Dimension;
import com.ixaris.commons.dimensions.limits.data.LimitEntity;
import com.ixaris.commons.dimensions.limits.data.NoLimitExtensionEntity;
import com.ixaris.commons.dimensions.limits.support.Long1DimensionDef;
import com.ixaris.commons.dimensions.limits.support.Long2DimensionDef;
import com.ixaris.commons.dimensions.limits.support.TestOptionalDimensionsLimit;
import com.ixaris.commons.hikari.test.JooqHikariTestHelper;
import com.ixaris.commons.multitenancy.test.TestTenants;

/**
 * @author <a href="keith.spiteri@ixaris.com">keith.spiteri</a>
 */
public class LimitCacheEntryTest {
    
    private static final String UNIT = "unit_limits";
    private static final JooqHikariTestHelper TEST_HELPER = new JooqHikariTestHelper(Collections.singleton(UNIT), TestTenants.DEFAULT);
    
    @BeforeClass
    public static void setup() {
        TEST_HELPER.getMultiTenancy().addTenant(TestTenants.DEFAULT);
    }
    
    @AfterClass
    public static void teardown() {
        TEST_HELPER.destroy();
    }
    
    @Test
    public void getLimitsForContext_multipleLimitsMatched_shouldOverrideMatchAnyLimitsWithMoreSpecificOnes() throws Throwable {
        final long now = System.currentTimeMillis();
        
        final List<LimitEntity<NoLimitExtensionEntity, TestOptionalDimensionsLimit>> limits = new ArrayList<>();
        
        final LimitEntity<NoLimitExtensionEntity, TestOptionalDimensionsLimit> allStarLimit = initialiseLimit();
        allStarLimit.getLimit().setEffectiveFrom(now);
        allStarLimit.getLimit().setMaxAmount(450L);
        limits.add(allStarLimit);
        
        final LimitEntity<NoLimitExtensionEntity, TestOptionalDimensionsLimit> matchAnyLimit = initialiseLimit(new Dimension<>(null,
            Long1DimensionDef.getInstance()),
            new Dimension<>(null, Long2DimensionDef.getInstance()));
        matchAnyLimit.getLimit().setEffectiveFrom(now);
        matchAnyLimit.getLimit().setMaxAmount(20L);
        limits.add(matchAnyLimit);
        
        final LimitEntity<NoLimitExtensionEntity, TestOptionalDimensionsLimit> specificLimit = initialiseLimit(new Dimension<>(123L,
            Long1DimensionDef.getInstance()),
            new Dimension<>(456L, Long2DimensionDef.getInstance()));
        specificLimit.getLimit().setEffectiveFrom(now);
        specificLimit.getLimit().setMaxAmount(400L);
        limits.add(specificLimit);
        
        final LimitCacheEntry<NoLimitExtensionEntity, TestOptionalDimensionsLimit> cacheEntry = new LimitCacheEntry<>(limits);
        
        final Dimension<Long> longDim = new Dimension<>(123L, Long1DimensionDef.getInstance());
        final Dimension<Long> otherLongDim = new Dimension<>(456L, Long2DimensionDef.getInstance());
        
        final SortedSet<LimitEntity<NoLimitExtensionEntity, TestOptionalDimensionsLimit>> limitsForContext = cacheEntry
            .getLimitsForContext(Context.newBuilder(TestOptionalDimensionsLimit.getInstance()).add(longDim).add(otherLongDim).build());
        assertThat(limitsForContext.size()).isEqualTo(2);
        assertThat(limitsForContext).containsExactly(allStarLimit, specificLimit);
    }
    
    @Test
    public void getLimitsForContext_multipleCalls_shouldNotConcurrentlyModifyCachedLimits() throws Throwable {
        final long now = System.currentTimeMillis();
        
        final List<LimitEntity<NoLimitExtensionEntity, TestOptionalDimensionsLimit>> limits = new ArrayList<>();
        final long sleepingTime = 1000L;
        
        final LimitEntity<NoLimitExtensionEntity, TestOptionalDimensionsLimit> effectiveNow = initialiseLimit();
        effectiveNow.getLimit().setEffectiveFrom(now);
        effectiveNow.getLimit().setEffectiveTo(now + sleepingTime);
        limits.add(effectiveNow);
        
        final LimitEntity<NoLimitExtensionEntity, TestOptionalDimensionsLimit> otherEffectiveNow = initialiseLimit();
        otherEffectiveNow.getLimit().setEffectiveFrom(now);
        otherEffectiveNow.getLimit().setEffectiveTo(now + sleepingTime);
        limits.add(otherEffectiveNow);
        
        final LimitEntity<NoLimitExtensionEntity, TestOptionalDimensionsLimit> effectiveFuture = initialiseLimit();
        effectiveFuture.getLimit().setEffectiveFrom(now + (sleepingTime - 1L));
        limits.add(effectiveFuture);
        
        final LimitEntity<NoLimitExtensionEntity, TestOptionalDimensionsLimit> otherEffectiveFuture = initialiseLimit();
        otherEffectiveFuture.getLimit().setEffectiveFrom(now + (sleepingTime - 1L));
        limits.add(otherEffectiveFuture);
        
        final LimitCacheEntry<NoLimitExtensionEntity, TestOptionalDimensionsLimit> cacheEntry = new LimitCacheEntry<>(limits);
        
        Thread.sleep(sleepingTime);
        
        final ExecutorService executorService = Executors.newFixedThreadPool(10);
        final List<Callable<SortedSet<LimitEntity<NoLimitExtensionEntity, TestOptionalDimensionsLimit>>>> getLimitsForContextCallables = IntStream
            .range(0, 10)
            .<Callable<SortedSet<LimitEntity<NoLimitExtensionEntity, TestOptionalDimensionsLimit>>>>mapToObj(
                i -> () -> cacheEntry.getLimitsForContext(Context.empty(TestOptionalDimensionsLimit.getInstance())))
            .collect(Collectors.toList());
        
        executorService
            .invokeAll(getLimitsForContextCallables)
            .forEach(future -> {
                try {
                    future.get();
                } catch (final ExecutionException | InterruptedException ex) {
                    fail(ex.getMessage(), ex);
                }
            });
    }
    
    private static LimitEntity<NoLimitExtensionEntity, TestOptionalDimensionsLimit> initialiseLimit(final Dimension... dimensionValues) throws Throwable {
        final Builder<TestOptionalDimensionsLimit> contextBuilder = Context.newBuilder(TestOptionalDimensionsLimit.getInstance());
        Arrays.stream(dimensionValues).forEach(contextBuilder::add);
        return new LimitEntity<>(contextBuilder.build(), NoLimitExtensionEntity.getCreate());
    }
    
}
