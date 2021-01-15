package com.ixaris.commons.dimensions.limits.support;

import static com.ixaris.commons.async.lib.CompletionStageUtil.block;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;
import static com.ixaris.commons.multitenancy.lib.data.DataUnit.DATA_UNIT;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;

import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import org.junit.AfterClass;
import org.junit.BeforeClass;
import org.junit.Test;

import com.ixaris.commons.async.lib.AsyncLocal;
import com.ixaris.commons.dimensions.lib.context.Context;
import com.ixaris.commons.dimensions.lib.context.Dimension;
import com.ixaris.commons.dimensions.limits.cache.LimitSpecificityComparator;
import com.ixaris.commons.dimensions.limits.data.LimitEntity;
import com.ixaris.commons.dimensions.limits.data.NoLimitExtensionEntity;
import com.ixaris.commons.hikari.test.JooqHikariTestHelper;
import com.ixaris.commons.jooq.persistence.JooqAsyncPersistenceProvider;
import com.ixaris.commons.jooq.persistence.JooqMultiTenancyConfiguration;
import com.ixaris.commons.multitenancy.test.TestTenants;

public class LimitSpecificityComparatorTest {
    
    // Dimensions
    private static final Dimension<String> DIM_1_CATCH_ALL = new Dimension<>(null, Test1DimensionDef.getInstance());
    private static final Dimension<String> DIM_2_CATCH_ALL = new Dimension<>(null, Test2DimensionDef.getInstance());
    private static final Dimension<String> DIM_1_SPECIFIC = new Dimension<>("A", Test1DimensionDef.getInstance());
    private static final Dimension<String> DIM_1_SPECIFIC_2 = new Dimension<>("Z", Test1DimensionDef.getInstance());
    private static final Dimension<String> DIM_2_SPECIFIC = new Dimension<>("A", Test2DimensionDef.getInstance());
    
    // Def1 (*) Def2 (*)
    private static LimitEntity<NoLimitExtensionEntity, TestComparatorLimit> LIMIT_ALL_STAR;
    
    // Def1 (CA) Def2 (CA)
    private static LimitEntity<NoLimitExtensionEntity, TestComparatorLimit> LIMIT_ALL_CATCH_ALL;
    
    // Def1 (CA) Def2 (*)
    private static LimitEntity<NoLimitExtensionEntity, TestComparatorLimit> LIMIT_FIRST_CATCH_ALL;
    
    // Def1 (*) Def2 (CA)
    private static LimitEntity<NoLimitExtensionEntity, TestComparatorLimit> LIMIT_SECOND_CATCH_ALL;
    
    // Def1 (A) Def2 (A)
    private static LimitEntity<NoLimitExtensionEntity, TestComparatorLimit> LIMIT_ALL_SPECIFIC;
    
    // Def1 (A) Def2 (*)
    private static LimitEntity<NoLimitExtensionEntity, TestComparatorLimit> LIMIT_FIRST_SPECIFIC;
    
    // Def1 (*) Def2 (A)
    private static LimitEntity<NoLimitExtensionEntity, TestComparatorLimit> LIMIT_SECOND_SPECIFIC;
    
    // Def1 (Z) Def2 (*)
    private static LimitEntity<NoLimitExtensionEntity, TestComparatorLimit> LIMIT_FIRST_SPECIFIC_2;
    
    private static final Comparator<LimitEntity<NoLimitExtensionEntity, TestComparatorLimit>> COMPARATOR = new LimitSpecificityComparator<>();
    
    private static final String UNIT = "unit_limits";
    private static JooqHikariTestHelper TEST_HELPER = new JooqHikariTestHelper(Collections.singleton(UNIT), TestTenants.DEFAULT);
    private static JooqAsyncPersistenceProvider PROVIDER;
    
    @BeforeClass
    public static void setup() throws Throwable {
        TEST_HELPER.getMultiTenancy().addTenant(TestTenants.DEFAULT);
        PROVIDER = new JooqAsyncPersistenceProvider(JooqMultiTenancyConfiguration.createDslContext(TEST_HELPER.getDataSource()));
        LIMIT_ALL_STAR = initialiseLimit();
        LIMIT_ALL_CATCH_ALL = initialiseLimit(DIM_1_CATCH_ALL, DIM_2_CATCH_ALL);
        LIMIT_FIRST_CATCH_ALL = initialiseLimit(DIM_1_CATCH_ALL);
        LIMIT_SECOND_CATCH_ALL = initialiseLimit(DIM_2_CATCH_ALL);
        LIMIT_ALL_SPECIFIC = initialiseLimit(DIM_1_SPECIFIC, DIM_2_SPECIFIC);
        LIMIT_FIRST_SPECIFIC = initialiseLimit(DIM_1_SPECIFIC);
        LIMIT_SECOND_SPECIFIC = initialiseLimit(DIM_2_SPECIFIC);
        LIMIT_FIRST_SPECIFIC_2 = initialiseLimit(DIM_1_SPECIFIC_2);
    }
    
    @AfterClass
    public static void teardown() {
        TEST_HELPER.destroy();
    }
    
    /**
     * Asserts that the order is obtained: 1: * * 2: CA CA
     */
    @Test
    public void test_StarAll_CatchAll() {
        
        final List<LimitEntity<NoLimitExtensionEntity, TestComparatorLimit>> limitList = Arrays.asList(LIMIT_ALL_STAR, LIMIT_ALL_CATCH_ALL);
        limitList.sort(COMPARATOR);
        
        assertNotNull(limitList);
        assertEquals(LIMIT_ALL_STAR.getContext().getDepth(), limitList.get(0).getContext().getDepth());
        assertEquals(LIMIT_ALL_CATCH_ALL.getContext().getDepth(), limitList.get(1).getContext().getDepth());
        // the following assert is done so as to make sure that the contextHashes are not equal, hence not letting the
        // test pass when it should fail
        assertNotEquals(LIMIT_ALL_CATCH_ALL.getContext().getDepth(), LIMIT_ALL_STAR.getContext().getDepth());
    }
    
    /**
     * Asserts that the order is obtained: 1: * * 2: CA CA
     */
    @Test
    public void test_StarAll_CatchAll_Inverted() {
        final List<LimitEntity<NoLimitExtensionEntity, TestComparatorLimit>> limitList = Arrays.asList(LIMIT_ALL_CATCH_ALL, LIMIT_ALL_STAR);
        limitList.sort(COMPARATOR);
        
        assertNotNull(limitList);
        assertEquals(LIMIT_ALL_STAR.getContext().getDepth(), limitList.get(0).getContext().getDepth());
        assertEquals(LIMIT_ALL_CATCH_ALL.getContext().getDepth(), limitList.get(1).getContext().getDepth());
    }
    
    /**
     * Asserts that the order is obtained: 1: * * 2: CA *
     */
    @Test
    public void test_StarAll_CatchFirst() {
        final List<LimitEntity<NoLimitExtensionEntity, TestComparatorLimit>> limitList = Arrays.asList(LIMIT_ALL_STAR, LIMIT_FIRST_CATCH_ALL);
        limitList.sort(COMPARATOR);
        
        assertNotNull(limitList);
        assertEquals(LIMIT_ALL_STAR.getContext().getDepth(), limitList.get(0).getContext().getDepth());
        assertEquals(LIMIT_FIRST_CATCH_ALL.getContext().getDepth(), limitList.get(1).getContext().getDepth());
    }
    
    /**
     * Asserts that the order is obtained: 1: * * 2: CA *
     */
    @Test
    public void test_StarAll_CatchFirst_Inverted() {
        final List<LimitEntity<NoLimitExtensionEntity, TestComparatorLimit>> limitList = Arrays.asList(LIMIT_FIRST_CATCH_ALL, LIMIT_ALL_STAR);
        limitList.sort(COMPARATOR);
        
        assertNotNull(limitList);
        assertEquals(LIMIT_ALL_STAR.getContext().getDepth(), limitList.get(0).getContext().getDepth());
        assertEquals(LIMIT_FIRST_CATCH_ALL.getContext().getDepth(), limitList.get(1).getContext().getDepth());
    }
    
    /**
     * Asserts that the order is obtained: 1: * * 2: * CA
     */
    @Test
    public void test_StarAll_CatchSecond() {
        final List<LimitEntity<NoLimitExtensionEntity, TestComparatorLimit>> limitList = Arrays.asList(LIMIT_ALL_STAR, LIMIT_SECOND_CATCH_ALL);
        limitList.sort(COMPARATOR);
        
        assertNotNull(limitList);
        assertEquals(LIMIT_ALL_STAR.getContext().getDepth(), limitList.get(0).getContext().getDepth());
        assertEquals(LIMIT_SECOND_CATCH_ALL.getContext().getDepth(), limitList.get(1).getContext().getDepth());
    }
    
    /**
     * Asserts that the order is obtained: 1: * * 2: * CA
     */
    @Test
    public void test_StarAll_CatchSecond_Inverted() {
        final List<LimitEntity<NoLimitExtensionEntity, TestComparatorLimit>> limitList = Arrays.asList(LIMIT_SECOND_CATCH_ALL, LIMIT_ALL_STAR);
        limitList.sort(COMPARATOR);
        
        assertNotNull(limitList);
        assertEquals(LIMIT_ALL_STAR.getContext().getDepth(), limitList.get(0).getContext().getDepth());
        assertEquals(LIMIT_SECOND_CATCH_ALL.getContext().getDepth(), limitList.get(1).getContext().getDepth());
    }
    
    /**
     * Asserts that the order is obtained: 1: * * 2: A A
     */
    @Test
    public void test_StarAll_SpecificAll() {
        final List<LimitEntity<NoLimitExtensionEntity, TestComparatorLimit>> limitList = Arrays.asList(LIMIT_ALL_STAR, LIMIT_ALL_SPECIFIC);
        limitList.sort(COMPARATOR);
        
        assertNotNull(limitList);
        assertEquals(LIMIT_ALL_STAR.getContext().getDepth(), limitList.get(0).getContext().getDepth());
        assertEquals(LIMIT_ALL_SPECIFIC.getContext().getDepth(), limitList.get(1).getContext().getDepth());
    }
    
    /**
     * Asserts that the order is obtained: 1: * * 2: A A
     */
    @Test
    public void test_StarAll_SpecificAll_Inverted() {
        final List<LimitEntity<NoLimitExtensionEntity, TestComparatorLimit>> limitList = Arrays.asList(LIMIT_ALL_SPECIFIC, LIMIT_ALL_STAR);
        limitList.sort(COMPARATOR);
        
        assertNotNull(limitList);
        assertEquals(LIMIT_ALL_STAR.getContext().getDepth(), limitList.get(0).getContext().getDepth());
        assertEquals(LIMIT_ALL_SPECIFIC.getContext().getDepth(), limitList.get(1).getContext().getDepth());
    }
    
    /**
     * Asserts that the order is obtained: 1: * * 2: A *
     */
    @Test
    public void test_StarAll_SpecificFirst() {
        final List<LimitEntity<NoLimitExtensionEntity, TestComparatorLimit>> limitList = Arrays.asList(LIMIT_ALL_STAR, LIMIT_FIRST_SPECIFIC);
        limitList.sort(COMPARATOR);
        
        assertNotNull(limitList);
        assertEquals(LIMIT_ALL_STAR.getContext().getDepth(), limitList.get(0).getContext().getDepth());
        assertEquals(LIMIT_FIRST_SPECIFIC.getContext().getDepth(), limitList.get(1).getContext().getDepth());
    }
    
    /**
     * Asserts that the order is obtained: 1: * * 2: A *
     */
    @Test
    public void test_StarAll_SpecificFirst_Inverted() {
        final List<LimitEntity<NoLimitExtensionEntity, TestComparatorLimit>> limitList = Arrays.asList(LIMIT_FIRST_SPECIFIC, LIMIT_ALL_STAR);
        limitList.sort(COMPARATOR);
        
        assertNotNull(limitList);
        assertEquals(LIMIT_ALL_STAR.getContext().getDepth(), limitList.get(0).getContext().getDepth());
        assertEquals(LIMIT_FIRST_SPECIFIC.getContext().getDepth(), limitList.get(1).getContext().getDepth());
    }
    
    /**
     * Asserts that the order is obtained: 1: * * 2: * A
     */
    @Test
    public void test_StarAll_SpecificSecond() {
        final List<LimitEntity<NoLimitExtensionEntity, TestComparatorLimit>> limitList = Arrays.asList(LIMIT_ALL_STAR, LIMIT_SECOND_SPECIFIC);
        limitList.sort(COMPARATOR);
        
        assertNotNull(limitList);
        assertEquals(LIMIT_ALL_STAR.getContext().getDepth(), limitList.get(0).getContext().getDepth());
        assertEquals(LIMIT_SECOND_SPECIFIC.getContext().getDepth(), limitList.get(1).getContext().getDepth());
    }
    
    /**
     * Asserts that the order is obtained: 1: * * 2: * A
     */
    @Test
    public void test_StarAll_SpecificSecond_Inverted() {
        final List<LimitEntity<NoLimitExtensionEntity, TestComparatorLimit>> limitList = Arrays.asList(LIMIT_SECOND_SPECIFIC, LIMIT_ALL_STAR);
        limitList.sort(COMPARATOR);
        
        assertNotNull(limitList);
        assertEquals(LIMIT_ALL_STAR.getContext().getDepth(), limitList.get(0).getContext().getDepth());
        assertEquals(LIMIT_SECOND_SPECIFIC.getContext().getDepth(), limitList.get(1).getContext().getDepth());
    }
    
    /**
     * Asserts that the order is obtained: 1: CA CA 2: A A
     */
    @Test
    public void test_CatchAll_SpecificAll() {
        final List<LimitEntity<NoLimitExtensionEntity, TestComparatorLimit>> limitList = Arrays.asList(LIMIT_ALL_CATCH_ALL, LIMIT_ALL_SPECIFIC);
        limitList.sort(COMPARATOR);
        
        assertNotNull(limitList);
        assertEquals(LIMIT_ALL_CATCH_ALL.getContext().getDepth(), limitList.get(0).getContext().getDepth());
        assertEquals(LIMIT_ALL_SPECIFIC.getContext().getDepth(), limitList.get(1).getContext().getDepth());
    }
    
    /**
     * Asserts that the order is obtained: 1: CA CA 2: A A
     */
    @Test
    public void test_CatchAll_SpecificAll_Inverted() {
        final List<LimitEntity<NoLimitExtensionEntity, TestComparatorLimit>> limitList = Arrays.asList(LIMIT_ALL_SPECIFIC, LIMIT_ALL_CATCH_ALL);
        limitList.sort(COMPARATOR);
        
        assertNotNull(limitList);
        assertEquals(LIMIT_ALL_CATCH_ALL.getContext().getDepth(), limitList.get(0).getContext().getDepth());
        assertEquals(LIMIT_ALL_SPECIFIC.getContext().getDepth(), limitList.get(1).getContext().getDepth());
    }
    
    /**
     * Asserts that the order is obtained: 1: CA CA 2: A *
     */
    @Test
    public void test_CatchAll_SpecificFirst() {
        final List<LimitEntity<NoLimitExtensionEntity, TestComparatorLimit>> limitList = Arrays.asList(LIMIT_ALL_CATCH_ALL, LIMIT_FIRST_SPECIFIC);
        limitList.sort(COMPARATOR);
        
        assertNotNull(limitList);
        assertEquals(LIMIT_ALL_CATCH_ALL.getContext().getDepth(), limitList.get(0).getContext().getDepth());
        assertEquals(LIMIT_FIRST_SPECIFIC.getContext().getDepth(), limitList.get(1).getContext().getDepth());
    }
    
    /**
     * Asserts that the order is obtained: 1: CA CA 2: A *
     */
    @Test
    public void test_CatchAll_SpecificFirst_Inverted() {
        final List<LimitEntity<NoLimitExtensionEntity, TestComparatorLimit>> limitList = Arrays.asList(LIMIT_FIRST_SPECIFIC, LIMIT_ALL_CATCH_ALL);
        limitList.sort(COMPARATOR);
        
        assertNotNull(limitList);
        assertEquals(LIMIT_ALL_CATCH_ALL.getContext().getDepth(), limitList.get(0).getContext().getDepth());
        assertEquals(LIMIT_FIRST_SPECIFIC.getContext().getDepth(), limitList.get(1).getContext().getDepth());
    }
    
    /**
     * Asserts that the order is obtained: 1: * A 2: CA CA
     */
    @Test
    public void test_CatchAll_SpecificSecond() {
        final List<LimitEntity<NoLimitExtensionEntity, TestComparatorLimit>> limitList = Arrays.asList(LIMIT_SECOND_SPECIFIC, LIMIT_ALL_CATCH_ALL);
        limitList.sort(COMPARATOR);
        
        assertNotNull(limitList);
        assertEquals(LIMIT_SECOND_SPECIFIC.getContext().getDepth(), limitList.get(0).getContext().getDepth());
        assertEquals(LIMIT_ALL_CATCH_ALL.getContext().getDepth(), limitList.get(1).getContext().getDepth());
    }
    
    /**
     * Asserts that the order is obtained: 1: * A 2: CA CA
     */
    @Test
    public void test_CatchAll_SpecificSecond_Inverted() {
        final List<LimitEntity<NoLimitExtensionEntity, TestComparatorLimit>> limitList = Arrays.asList(LIMIT_ALL_CATCH_ALL, LIMIT_SECOND_SPECIFIC);
        limitList.sort(COMPARATOR);
        
        assertNotNull(limitList);
        assertEquals(LIMIT_SECOND_SPECIFIC.getContext().getDepth(), limitList.get(0).getContext().getDepth());
        assertEquals(LIMIT_ALL_CATCH_ALL.getContext().getDepth(), limitList.get(1).getContext().getDepth());
    }
    
    /**
     * Asserts that the order is obtained: 1: * A 2: A A
     */
    @Test
    public void test_SpecificAll_SpecificSecond() {
        final List<LimitEntity<NoLimitExtensionEntity, TestComparatorLimit>> limitList = Arrays.asList(LIMIT_SECOND_SPECIFIC, LIMIT_ALL_SPECIFIC);
        limitList.sort(COMPARATOR);
        
        assertNotNull(limitList);
        assertEquals(LIMIT_SECOND_SPECIFIC.getContext().getDepth(), limitList.get(0).getContext().getDepth());
        assertEquals(LIMIT_ALL_SPECIFIC.getContext().getDepth(), limitList.get(1).getContext().getDepth());
    }
    
    /**
     * Asserts that the order is obtained: 1: * A 2: A A
     */
    @Test
    public void test_SpecificAll_SpecificSecond_Inverted() {
        final List<LimitEntity<NoLimitExtensionEntity, TestComparatorLimit>> limitList = Arrays.asList(LIMIT_ALL_SPECIFIC, LIMIT_SECOND_SPECIFIC);
        limitList.sort(COMPARATOR);
        
        assertNotNull(limitList);
        assertEquals(LIMIT_SECOND_SPECIFIC.getContext().getDepth(), limitList.get(0).getContext().getDepth());
        assertEquals(LIMIT_ALL_SPECIFIC.getContext().getDepth(), limitList.get(1).getContext().getDepth());
    }
    
    /**
     * Asserts that the order is obtained: 1: A * 2: A A
     */
    @Test
    public void test_SpecificAll_SpecificFirst() {
        final List<LimitEntity<NoLimitExtensionEntity, TestComparatorLimit>> limitList = Arrays.asList(LIMIT_FIRST_SPECIFIC, LIMIT_ALL_SPECIFIC);
        limitList.sort(COMPARATOR);
        
        assertNotNull(limitList);
        assertEquals(LIMIT_FIRST_SPECIFIC.getContext().getDepth(), limitList.get(0).getContext().getDepth());
        assertEquals(LIMIT_ALL_SPECIFIC.getContext().getDepth(), limitList.get(1).getContext().getDepth());
    }
    
    /**
     * Asserts that the order is obtained: 1: A * 2: A A
     */
    @Test
    public void test_SpecificAll_SpecificFirst_Inverted() {
        final List<LimitEntity<NoLimitExtensionEntity, TestComparatorLimit>> limitList = Arrays.asList(LIMIT_ALL_SPECIFIC, LIMIT_FIRST_SPECIFIC);
        limitList.sort(COMPARATOR);
        
        assertNotNull(limitList);
        assertEquals(LIMIT_FIRST_SPECIFIC.getContext().getDepth(), limitList.get(0).getContext().getDepth());
        assertEquals(LIMIT_ALL_SPECIFIC.getContext().getDepth(), limitList.get(1).getContext().getDepth());
    }
    
    /**
     * Asserts that the order is obtained: 1: * A 2: A *
     */
    @Test
    public void test_SpecificFirst_SpecificSecond() {
        final List<LimitEntity<NoLimitExtensionEntity, TestComparatorLimit>> limitList = Arrays.asList(LIMIT_SECOND_SPECIFIC,
            LIMIT_FIRST_SPECIFIC);
        limitList.sort(COMPARATOR);
        
        assertNotNull(limitList);
        assertEquals(LIMIT_SECOND_SPECIFIC.getContext().getDepth(), limitList.get(0).getContext().getDepth());
        assertEquals(LIMIT_FIRST_SPECIFIC.getContext().getDepth(), limitList.get(1).getContext().getDepth());
    }
    
    /**
     * Asserts that the order is obtained: 1: * A 2: A *
     */
    @Test
    public void test_SpecificFirst_SpecificSecond_Inverted() {
        final List<LimitEntity<NoLimitExtensionEntity, TestComparatorLimit>> limitList = Arrays.asList(LIMIT_FIRST_SPECIFIC,
            LIMIT_SECOND_SPECIFIC);
        limitList.sort(COMPARATOR);
        
        assertNotNull(limitList);
        assertEquals(LIMIT_SECOND_SPECIFIC.getContext().getDepth(), limitList.get(0).getContext().getDepth());
        assertEquals(LIMIT_FIRST_SPECIFIC.getContext().getDepth(), limitList.get(1).getContext().getDepth());
    }
    
    /**
     * Asserts that the order is obtained: 1: CA * 2: A *
     */
    @Test
    public void test_SpecificFirst_CatchFirst() {
        final List<LimitEntity<NoLimitExtensionEntity, TestComparatorLimit>> limitList = Arrays.asList(LIMIT_FIRST_CATCH_ALL,
            LIMIT_FIRST_SPECIFIC);
        limitList.sort(COMPARATOR);
        
        assertNotNull(limitList);
        assertEquals(LIMIT_FIRST_CATCH_ALL.getContext().getDepth(), limitList.get(0).getContext().getDepth());
        assertEquals(LIMIT_FIRST_SPECIFIC.getContext().getDepth(), limitList.get(1).getContext().getDepth());
    }
    
    /**
     * Asserts that the order is obtained: 1: CA * 2: A *
     */
    @Test
    public void test_SpecificFirst_CatchFirst_Inverted() {
        final List<LimitEntity<NoLimitExtensionEntity, TestComparatorLimit>> limitList = Arrays.asList(LIMIT_FIRST_SPECIFIC,
            LIMIT_FIRST_CATCH_ALL);
        limitList.sort(COMPARATOR);
        
        assertNotNull(limitList);
        assertEquals(LIMIT_FIRST_CATCH_ALL.getContext().getDepth(), limitList.get(0).getContext().getDepth());
        assertEquals(LIMIT_FIRST_SPECIFIC.getContext().getDepth(), limitList.get(1).getContext().getDepth());
    }
    
    /**
     * Asserts that the order is obtained: 1: * CA 2: A *
     */
    @Test
    public void test_SpecificFirst_CatchSecond() {
        final List<LimitEntity<NoLimitExtensionEntity, TestComparatorLimit>> limitList = Arrays.asList(LIMIT_SECOND_CATCH_ALL,
            LIMIT_FIRST_SPECIFIC);
        limitList.sort(COMPARATOR);
        
        assertNotNull(limitList);
        assertEquals(LIMIT_SECOND_CATCH_ALL.getContext().getDepth(), limitList.get(0).getContext().getDepth());
        assertEquals(LIMIT_FIRST_SPECIFIC.getContext().getDepth(), limitList.get(1).getContext().getDepth());
    }
    
    /**
     * Asserts that the order is obtained: 1: * CA 2: A *
     */
    @Test
    public void test_SpecificFirst_CatchSecond_Inverted() {
        final List<LimitEntity<NoLimitExtensionEntity, TestComparatorLimit>> limitList = Arrays.asList(LIMIT_FIRST_SPECIFIC,
            LIMIT_SECOND_CATCH_ALL);
        limitList.sort(COMPARATOR);
        
        assertNotNull(limitList);
        assertEquals(LIMIT_SECOND_CATCH_ALL.getContext().getDepth(), limitList.get(0).getContext().getDepth());
        assertEquals(LIMIT_FIRST_SPECIFIC.getContext().getDepth(), limitList.get(1).getContext().getDepth());
    }
    
    /**
     * Asserts that the order is obtained: 1: * CA 2: * A
     */
    @Test
    public void test_SpecificSecond_CatchSecond() {
        final List<LimitEntity<NoLimitExtensionEntity, TestComparatorLimit>> limitList = Arrays.asList(LIMIT_SECOND_CATCH_ALL,
            LIMIT_SECOND_SPECIFIC);
        limitList.sort(COMPARATOR);
        
        assertNotNull(limitList);
        assertEquals(LIMIT_SECOND_CATCH_ALL.getContext().getDepth(), limitList.get(0).getContext().getDepth());
        assertEquals(LIMIT_SECOND_SPECIFIC.getContext().getDepth(), limitList.get(1).getContext().getDepth());
    }
    
    /**
     * Asserts that the order is obtained: 1: * CA 2: * A
     */
    @Test
    public void test_SpecificSecond_CatchSecond_Inverted() {
        final List<LimitEntity<NoLimitExtensionEntity, TestComparatorLimit>> limitList = Arrays.asList(LIMIT_SECOND_SPECIFIC,
            LIMIT_SECOND_CATCH_ALL);
        limitList.sort(COMPARATOR);
        
        assertNotNull(limitList);
        assertEquals(LIMIT_SECOND_CATCH_ALL.getContext().getDepth(), limitList.get(0).getContext().getDepth());
        assertEquals(LIMIT_SECOND_SPECIFIC.getContext().getDepth(), limitList.get(1).getContext().getDepth());
    }
    
    /**
     * Order does not matter in this case as if 2 limits with different specified values, only a containing query (not "matching") may have been
     * executed, in which case order is not important
     */
    @Test
    public void test_EquallySpecific() {
        final List<LimitEntity<NoLimitExtensionEntity, TestComparatorLimit>> limitList = Arrays.asList(LIMIT_FIRST_SPECIFIC,
            LIMIT_FIRST_SPECIFIC_2);
        limitList.sort(COMPARATOR);
        assertNotNull(limitList);
        assertEquals(2L, limitList.size());
    }
    
    private static LimitEntity<NoLimitExtensionEntity, TestComparatorLimit> initialiseLimit(final Dimension<?>... dimensions) throws Throwable {
        return AsyncLocal
            .with(DATA_UNIT, UNIT)
            .with(TENANT, TestTenants.DEFAULT)
            .exec(() -> block(
                PROVIDER.transaction(() -> new LimitEntity<>(Context.newBuilder(TestComparatorLimit.getInstance()).addAll(dimensions).build(), NoLimitExtensionEntity.getCreate()))));
    }
    
}
