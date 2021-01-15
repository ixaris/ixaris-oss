package com.ixaris.commons.dimensions.lib.context;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

import com.ixaris.commons.dimensions.lib.base.AbstractDimensionalDef;
import com.ixaris.commons.dimensions.lib.dimension.LongDimensionDef;

public class ContextTest {
    
    public static final class MockDef extends AbstractDimensionalDef {
        
        public static final String KEY = "MOCK_LIMIT";
        
        private static final MockDef INSTANCE = new MockDef();
        
        private MockDef() {
            super("Mock Limit",
                new ContextDef(
                    CardLevelClassificationDimensionDef.getInstance(),
                    CardBrandDimensionDef.getInstance(),
                    CardRenewedOnExpiryDimensionDef.getInstance(),
                    Long1DimensionDef.getInstance()));
        }
        
        public static MockDef getInstance() {
            return INSTANCE;
        }
        
        @Override
        public String getKey() {
            return KEY;
        }
        
    }
    
    // configured limits
    // CardLevelClassificationDimensionDef CardBrandDimensionDef CardRenewedOnExpiryDimensionDef Long1DimensionDef
    // Limit 1: * * * *
    // Limit 2: CONSUMER * * *
    // Limit 3: CONSUMER VISA * *
    // Limit 4: CORPORATE * * *
    // Limit 5: CA * * *
    // Limit 6: * * * 5
    private static final Context<MockDef> CONFIGURATION_LIMIT_1;
    private static final Context<MockDef> CONFIGURATION_LIMIT_2;
    private static final Context<MockDef> CONFIGURATION_LIMIT_3;
    private static final Context<MockDef> CONFIGURATION_LIMIT_4;
    private static final Context<MockDef> CONFIGURATION_LIMIT_5;
    private static final Context<MockDef> CONFIGURATION_LIMIT_6;
    // querying limit dimensions
    // CardLevelClassificationDimensionDef CardBrandDimensionDef CardRenewedOnExpiryDimensionDef Long1DimensionDef
    // Query 1: * * * *
    // Query 2: CONSUMER * * *
    // Query 3: CONSUMER MASTERCARD * *
    // Query 4: * * true *
    // Query 5: CONSUMER VISA true *
    // Query 6: * * * 5
    // Query 7: * * * 4
    // Query 8: * * * 6
    private static final Context<MockDef> QUERY_1;
    private static final Context<MockDef> QUERY_2;
    private static final Context<MockDef> QUERY_3;
    private static final Context<MockDef> QUERY_4;
    private static final Context<MockDef> QUERY_5;
    private static final Context<MockDef> QUERY_6;
    private static final Context<MockDef> QUERY_7;
    private static final Context<MockDef> QUERY_8;
    
    static {
        CONFIGURATION_LIMIT_1 = Context.empty(MockDef.getInstance());
        
        CONFIGURATION_LIMIT_2 = Context.newBuilder(MockDef.getInstance())
            .add(new Dimension<>(CardLevelClassification.CONSUMER, CardLevelClassificationDimensionDef.getInstance()))
            .build();
        
        CONFIGURATION_LIMIT_3 = Context.newBuilder(MockDef.getInstance())
            .add(new Dimension<>(CardLevelClassification.CONSUMER, CardLevelClassificationDimensionDef.getInstance()))
            .add(new Dimension<>(CardBrand.VISA, CardBrandDimensionDef.getInstance()))
            .build();
        
        CONFIGURATION_LIMIT_4 = Context.newBuilder(MockDef.getInstance())
            .add(new Dimension<>(CardLevelClassification.CORPORATE, CardLevelClassificationDimensionDef.getInstance()))
            .build();
        
        CONFIGURATION_LIMIT_5 = Context.newBuilder(MockDef.getInstance())
            .add(new Dimension<>(null, CardLevelClassificationDimensionDef.getInstance()))
            .build();
        
        CONFIGURATION_LIMIT_6 = Context.newBuilder(MockDef.getInstance())
            .add(new Dimension<>(5L, Long1DimensionDef.getInstance()))
            .build();
        
        QUERY_1 = Context.empty(MockDef.getInstance());
        
        QUERY_2 = Context.newBuilder(MockDef.getInstance())
            .add(new Dimension<>(CardLevelClassification.CONSUMER, CardLevelClassificationDimensionDef.getInstance()))
            .build();
        
        QUERY_3 = Context.newBuilder(MockDef.getInstance())
            .add(new Dimension<>(CardLevelClassification.CONSUMER, CardLevelClassificationDimensionDef.getInstance()))
            .add(new Dimension<>(CardBrand.MASTERCARD, CardBrandDimensionDef.getInstance()))
            .build();
        
        QUERY_4 = Context.newBuilder(MockDef.getInstance())
            .add(new Dimension<>(true, CardRenewedOnExpiryDimensionDef.getInstance()))
            .build();
        
        QUERY_5 = Context.newBuilder(MockDef.getInstance())
            .add(new Dimension<>(CardLevelClassification.CONSUMER, CardLevelClassificationDimensionDef.getInstance()))
            .add(new Dimension<>(CardBrand.VISA, CardBrandDimensionDef.getInstance()))
            .add(new Dimension<>(true, CardRenewedOnExpiryDimensionDef.getInstance()))
            .build();
        
        QUERY_6 = Context.newBuilder(MockDef.getInstance())
            .add(new Dimension<>(5L, Long1DimensionDef.getInstance()))
            .build();
        
        QUERY_7 = Context.newBuilder(MockDef.getInstance())
            .add(new Dimension<>(4L, Long1DimensionDef.getInstance()))
            .build();
        
        QUERY_8 = Context.newBuilder(MockDef.getInstance())
            .add(new Dimension<>(6L, Long1DimensionDef.getInstance()))
            .build();
    }
    
    // CONTAINING
    @Test
    public void testContainingQuery1_Limit1() {
        assertTrue(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_1, QUERY_1));
    }
    
    @Test
    public void testContainingQuery1_Limit2() {
        assertTrue(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_2, QUERY_1));
    }
    
    @Test
    public void testContainingQuery1_Limit3() {
        assertTrue(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_3, QUERY_1));
    }
    
    @Test
    public void testContainingQuery1_Limit4() {
        assertTrue(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_4, QUERY_1));
    }
    
    @Test
    public void testContainingQuery1_Limit5() {
        assertTrue(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_5, QUERY_1));
    }
    
    @Test
    public void testContainingQuery2_Limit1() {
        assertFalse(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_1, QUERY_2));
    }
    
    @Test
    public void testContainingQuery2_Limit2() {
        assertTrue(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_2, QUERY_2));
    }
    
    @Test
    public void testContainingQuery2_Limit3() {
        assertTrue(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_3, QUERY_2));
    }
    
    @Test
    public void testContainingQuery2_Limit4() {
        assertFalse(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_4, QUERY_2));
    }
    
    @Test
    public void testContainingQuery2_Limit5() {
        assertFalse(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_5, QUERY_2));
    }
    
    @Test
    public void testContainingQuery3_Limit1() {
        assertFalse(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_1, QUERY_3));
    }
    
    @Test
    public void testContainingQuery3_Limit2() {
        assertFalse(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_2, QUERY_3));
    }
    
    @Test
    public void testContainingQuery3_Limit3() {
        assertFalse(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_3, QUERY_3));
    }
    
    @Test
    public void testContainingQuery3_Limit4() {
        assertFalse(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_4, QUERY_3));
    }
    
    @Test
    public void testContainingQuery3_Limit5() {
        assertFalse(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_5, QUERY_3));
    }
    
    @Test
    public void testContainingQuery4_Limit1() {
        assertFalse(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_1, QUERY_4));
    }
    
    @Test
    public void testContainingQuery4_Limit2() {
        assertFalse(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_2, QUERY_4));
    }
    
    @Test
    public void testContainingQuery4_Limit3() {
        assertFalse(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_3, QUERY_4));
    }
    
    @Test
    public void testContainingQuery4_Limit4() {
        assertFalse(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_4, QUERY_4));
    }
    
    @Test
    public void testContainingQuery4_Limit5() {
        assertFalse(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_5, QUERY_4));
    }
    
    @Test
    public void testContainingQuery5_Limit1() {
        assertFalse(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_1, QUERY_5));
    }
    
    @Test
    public void testContainingQuery5_Limit2() {
        assertFalse(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_2, QUERY_5));
    }
    
    @Test
    public void testContainingQuery5_Limit3() {
        assertFalse(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_3, QUERY_5));
    }
    
    @Test
    public void testContainingQuery5_Limit4() {
        assertFalse(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_4, QUERY_5));
    }
    
    @Test
    public void testContainingQuery5_Limit5() {
        assertFalse(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_5, QUERY_5));
    }
    
    // Query 6 Limit 6
    
    @Test
    public void testContainingQueryLongContextDimDefExact_Query6_Limit6() {
        Long1DimensionDef.getInstance().setRange(LongDimensionDef.Range.EXACT);
        assertTrue(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_6, QUERY_6));
    }
    
    @Test
    public void testContainingQueryLongContextDimDefSmaller_Query6_Limit6() {
        Long1DimensionDef.getInstance().setRange(LongDimensionDef.Range.SMALLER);
        assertTrue(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_6, QUERY_6));
    }
    
    @Test
    public void testContainingQueryLongContextDimDefLarger_Query6_Limit6() {
        Long1DimensionDef.getInstance().setRange(LongDimensionDef.Range.LARGER);
        assertTrue(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_6, QUERY_6));
    }
    
    // Query 7 Limit 6
    
    @Test
    public void testContainingQueryLongContextDimDefExact_Query7_Limit6() {
        Long1DimensionDef.getInstance().setRange(LongDimensionDef.Range.EXACT);
        assertFalse(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_6, QUERY_7));
    }
    
    @Test
    public void testContainingQueryLongContextDimDefSmaller_Query7_Limit6() {
        Long1DimensionDef.getInstance().setRange(LongDimensionDef.Range.SMALLER);
        assertTrue(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_6, QUERY_7));
    }
    
    @Test
    public void testContainingQueryLongContextDimDefLarger_Query7_Limit6() {
        Long1DimensionDef.getInstance().setRange(LongDimensionDef.Range.LARGER);
        assertFalse(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_6, QUERY_7));
    }
    
    // Query 8 Limit 6
    
    @Test
    public void testContainingQueryLongContextDimDefExact_Query8_Limit6() {
        Long1DimensionDef.getInstance().setRange(LongDimensionDef.Range.EXACT);
        assertFalse(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_6, QUERY_8));
    }
    
    @Test
    public void testContainingQueryLongContextDimDefSmaller_Query8_Limit6() {
        Long1DimensionDef.getInstance().setRange(LongDimensionDef.Range.SMALLER);
        assertFalse(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_6, QUERY_8));
    }
    
    @Test
    public void testContainingQueryLongContextDimDefLarger_Query8_Limit6() {
        Long1DimensionDef.getInstance().setRange(LongDimensionDef.Range.LARGER);
        assertTrue(Context.isContaining(MockDef.getInstance(), CONFIGURATION_LIMIT_6, QUERY_8));
    }
    
    // MATCHING
    
    @Test
    public void testMatchingQuery1_Limit1() {
        assertTrue(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_1, QUERY_1));
    }
    
    @Test
    public void testMatchingQuery1_Limit2() {
        assertFalse(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_2, QUERY_1));
    }
    
    @Test
    public void testMatchingQuery1_Limit3() {
        assertFalse(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_3, QUERY_1));
    }
    
    @Test
    public void testMatchingQuery1_Limit4() {
        assertFalse(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_4, QUERY_1));
    }
    
    @Test
    public void testMatchingQuery1_Limit5() {
        assertFalse(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_5, QUERY_1));
    }
    
    @Test
    public void testMatchingQuery2_Limit1() {
        assertTrue(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_1, QUERY_2));
    }
    
    @Test
    public void testMatchingQuery2_Limit2() {
        assertTrue(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_2, QUERY_2));
    }
    
    @Test
    public void testMatchingQuery2_Limit3() {
        assertFalse(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_3, QUERY_2));
    }
    
    @Test
    public void testMatchingQuery2_Limit4() {
        assertFalse(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_4, QUERY_2));
    }
    
    @Test
    public void testMatchingQuery2_Limit5() {
        assertTrue(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_5, QUERY_2));
    }
    
    @Test
    public void testMatchingQuery3_Limit1() {
        assertTrue(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_1, QUERY_3));
    }
    
    @Test
    public void testMatchingQuery3_Limit2() {
        assertTrue(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_2, QUERY_3));
    }
    
    @Test
    public void testMatchingQuery3_Limit3() {
        assertFalse(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_3, QUERY_3));
    }
    
    @Test
    public void testMatchingQuery3_Limit4() {
        assertFalse(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_4, QUERY_3));
    }
    
    @Test
    public void testMatchingQuery3_Limit5() {
        assertTrue(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_5, QUERY_3));
    }
    
    @Test
    public void testMatchingQuery4_Limit1() {
        assertTrue(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_1, QUERY_4));
    }
    
    @Test
    public void testMatchingQuery4_Limit2() {
        assertFalse(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_2, QUERY_4));
    }
    
    @Test
    public void testMatchingQuery4_Limit3() {
        assertFalse(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_3, QUERY_4));
    }
    
    @Test
    public void testMatchingQuery4_Limit4() {
        assertFalse(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_4, QUERY_4));
    }
    
    @Test
    public void testMatchingQuery4_Limit5() {
        assertFalse(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_5, QUERY_4));
    }
    
    @Test
    public void testMatchingQuery5_Limit1() {
        assertTrue(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_1, QUERY_5));
    }
    
    @Test
    public void testMatchingQuery5_Limit2() {
        assertTrue(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_2, QUERY_5));
    }
    
    @Test
    public void testMatchingQuery5_Limit3() {
        assertTrue(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_3, QUERY_5));
    }
    
    @Test
    public void testMatchingQuery5_Limit4() {
        assertFalse(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_4, QUERY_5));
    }
    
    @Test
    public void testMatchingQuery5_Limit5() {
        assertTrue(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_5, QUERY_5));
    }
    
    // Query 6 Limit 6
    
    @Test
    public void testMatchingQueryLongContextDimDefExact_Query6_Limit6() {
        Long1DimensionDef.getInstance().setRange(LongDimensionDef.Range.EXACT);
        assertTrue(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_6, QUERY_6));
    }
    
    @Test
    public void testMatchingQueryLongContextDimDefSmaller_Query6_Limit6() {
        Long1DimensionDef.getInstance().setRange(LongDimensionDef.Range.SMALLER);
        assertTrue(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_6, QUERY_6));
    }
    
    @Test
    public void testMatchingQueryLongContextDimDefLarger_Query6_Limit6() {
        Long1DimensionDef.getInstance().setRange(LongDimensionDef.Range.LARGER);
        assertTrue(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_6, QUERY_6));
    }
    
    // Query 7 Limit 6
    
    @Test
    public void testMatchingQueryLongContextDimDefExact_Query7_Limit6() {
        Long1DimensionDef.getInstance().setRange(LongDimensionDef.Range.EXACT);
        assertFalse(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_6, QUERY_7));
    }
    
    @Test
    public void testMatchingQueryLongContextDimDefSmaller_Query7_Limit6() {
        Long1DimensionDef.getInstance().setRange(LongDimensionDef.Range.SMALLER);
        assertFalse(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_6, QUERY_7));
    }
    
    @Test
    public void testMatchingQueryLongContextDimDefLarger_Query7_Limit6() {
        Long1DimensionDef.getInstance().setRange(LongDimensionDef.Range.LARGER);
        assertTrue(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_6, QUERY_7));
    }
    
    // Query 8 Limit 6
    
    @Test
    public void testMatchingQueryLongContextDimDefExact_Query8_Limit6() {
        Long1DimensionDef.getInstance().setRange(LongDimensionDef.Range.EXACT);
        assertFalse(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_6, QUERY_8));
    }
    
    @Test
    public void testMatchingQueryLongContextDimDefSmaller_Query8_Limit6() {
        Long1DimensionDef.getInstance().setRange(LongDimensionDef.Range.SMALLER);
        assertTrue(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_6, QUERY_8));
    }
    
    @Test
    public void testMatchingQueryLongContextDimDefLarger_Query8_Limit6() {
        Long1DimensionDef.getInstance().setRange(LongDimensionDef.Range.LARGER);
        assertFalse(Context.isMatching(MockDef.getInstance(), CONFIGURATION_LIMIT_6, QUERY_8));
    }
    
}
