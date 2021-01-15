package com.ixaris.commons.dimensions.lib.dimension;

import static org.junit.Assert.*;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.Answers;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

import com.ixaris.commons.dimensions.lib.base.AbstractDimensionalDef;
import com.ixaris.commons.dimensions.lib.base.DimensionalDef;
import com.ixaris.commons.dimensions.lib.context.Context;
import com.ixaris.commons.dimensions.lib.context.ContextDef;
import com.ixaris.commons.dimensions.lib.context.Dimension;
import com.ixaris.commons.dimensions.lib.context.NotComparableException;

/**
 * Unit tests for the {@link AbstractDimensionDef}.
 *
 * @author <a href="mailto:sarah.cassar@ixaris.com">sarah.cassar</a>
 */
@RunWith(MockitoJUnitRunner.class)
public class DimensionDefTest {
    
    @Mock(answer = Answers.CALLS_REAL_METHODS)
    private AbstractDimensionDef def;
    
    @Test
    public void isMatchAnySupported_byDefaultMatchAnyIsNotSupported_shouldReturnFalse() {
        assertFalse(def.isMatchAnySupported());
    }
    
    @Test(expected = IllegalStateException.class)
    public void createMatchAnyDimension_matchAnyNotSupported_shouldThrowIllegalStateException() {
        
        // Mock the matchAnySupported to return false.
        Mockito.doReturn(false).when(def).isMatchAnySupported();
        def.createMatchAnyDimension();
    }
    
    @Test
    public void createMatchAnyDimension_matchAnySupported_shouldCreateContextDimensionWithMatchAnyValue() {
        
        Mockito.doReturn(true).when(def).isMatchAnySupported();
        
        final Dimension matchAnyDimension = def.createMatchAnyDimension();
        assertNotNull(matchAnyDimension);
        assertEquals(null, matchAnyDimension.getValue());
    }
    
    @Test
    public void testDimensionKey() {
        assertEquals("A", ADimDef.getInstance().getKey());
        assertEquals("B", BDimensionDef.getInstance().getKey());
    }
    
    @Test
    public void testCompare() throws NotComparableException {
        
        final Dimension<?> dimensionA1 = new Dimension<>(HEnum.ROOT, HDimDef.getInstance());
        final Dimension<?> dimensionA2 = new Dimension<>(HEnum.I1, HDimDef.getInstance());
        final Dimension<?> dimensionA3 = new Dimension<>(HEnum.I1_1, HDimDef.getInstance());
        
        assertTrue(DimensionalDef.compare(HDimDef.getInstance(),
            Context.newBuilder(TestProp.getInstance()).add(dimensionA3).build(),
            Context.newBuilder(TestProp.getInstance()).add(dimensionA1).build()) > 0);
        assertTrue(DimensionalDef.compare(HDimDef.getInstance(),
            Context.newBuilder(TestProp.getInstance()).add(dimensionA2).build(),
            Context.newBuilder(TestProp.getInstance()).add(dimensionA1).build()) > 0);
        assertTrue(DimensionalDef.compare(HDimDef.getInstance(),
            Context.newBuilder(TestProp.getInstance()).add(dimensionA3).build(),
            Context.newBuilder(TestProp.getInstance()).add(dimensionA2).build()) > 0);
        
        assertTrue(DimensionalDef.compare(HDimDef.getInstance(),
            Context.newBuilder(TestProp.getInstance()).add(dimensionA3).build(),
            Context.newBuilder(TestProp.getInstance()).add(dimensionA3).build()) == 0);
        assertTrue(DimensionalDef.compare(HDimDef.getInstance(),
            Context.newBuilder(TestProp.getInstance()).add(dimensionA2).build(),
            Context.newBuilder(TestProp.getInstance()).add(dimensionA2).build()) == 0);
        assertTrue(DimensionalDef.compare(HDimDef.getInstance(),
            Context.newBuilder(TestProp.getInstance()).add(dimensionA1).build(),
            Context.newBuilder(TestProp.getInstance()).add(dimensionA1).build()) == 0);
    }
    
    @Test
    public void testContextComparator() throws NotComparableException {
        final ContextDef contextDef1 = new ContextDef(ADimDef.getInstance(), BDimDef.getInstance(), CDimDef.getInstance());
        DimensionalDef dimensionalDef1 = new AbstractDimensionalDef("Test", contextDef1) {
            
            @Override
            public String getKey() {
                return "TEST1";
            }
            
        };
        
        Context<DimensionalDef> context1 = Context.newBuilder(dimensionalDef1).add(ADimDef.getInstance().create("VA")).build();
        Context<DimensionalDef> context2 = Context.newBuilder(dimensionalDef1)
            .addAll(BDimDef.getInstance().create("VB"), CDimDef.getInstance().create(CEnum.VC))
            .build();
        assertTrue(context1.compareTo(context2) > 0);
        assertTrue(context2.compareTo(context1) < 0);
        
        context1 = Context.newBuilder(dimensionalDef1).addAll(ADimDef.getInstance().create("VA"), BDimDef.getInstance().create("VB")).build();
        context2 = Context.newBuilder(dimensionalDef1).add(BDimDef.getInstance().create("VX")).build();
        try {
            context1.compareTo(context2);
            fail("Should raise NotComparableException!");
        } catch (NotComparableException expected) {}
        try {
            context2.compareTo(context1);
            fail("Should raise NotComparableException!");
        } catch (NotComparableException expected) {}
        
        final ContextDef contextDef2 = new ContextDef(ADimDef.getInstance(), BDimDef.getInstance(), PDimDef.getInstance());
        final DimensionalDef dimensionalDef2 = new AbstractDimensionalDef("Test 2", contextDef2) {
            
            @Override
            public String getKey() {
                return "TEST2";
            }
            
        };
        
        context1 = Context.newBuilder(dimensionalDef2)
            .addAll(ADimDef.getInstance().create("VA"), BDimDef.getInstance().create("VB"), PDimDef.getInstance().create(new Part("1", "2")))
            .build();
        context2 = Context.newBuilder(dimensionalDef2)
            .addAll(ADimDef.getInstance().create("VA"), BDimDef.getInstance().create("VB"), PDimDef.getInstance().create(new Part("1", "3")))
            .build();
        try {
            context1.compareTo(context2);
            fail("Should raise NotComparableException!");
        } catch (NotComparableException expected) {}
        try {
            context2.compareTo(context1);
            fail("Should raise NotComparableException!");
        } catch (NotComparableException expected) {}
        
        context1 = Context.newBuilder(dimensionalDef2)
            .addAll(ADimDef.getInstance().create("VA"), BDimDef.getInstance().create("VB"), PDimDef.getInstance().create(new Part("1")))
            .build();
        context2 = Context.newBuilder(dimensionalDef2)
            .addAll(ADimDef.getInstance().create("VA"), BDimDef.getInstance().create("VB"), PDimDef.getInstance().create(new Part("1", "2")))
            .build();
        assertTrue(context1.compareTo(context2) < 0);
        assertTrue(context2.compareTo(context1) > 0);
    }
    
}
