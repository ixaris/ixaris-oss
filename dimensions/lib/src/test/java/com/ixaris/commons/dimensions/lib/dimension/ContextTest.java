package com.ixaris.commons.dimensions.lib.dimension;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

import com.ixaris.commons.dimensions.lib.base.AbstractDimensionalDef;
import com.ixaris.commons.dimensions.lib.context.Context;
import com.ixaris.commons.dimensions.lib.context.ContextDef;

public class ContextTest {
    
    public static class ABHProp extends AbstractDimensionalDef {
        
        private static final ABHProp INSTANCE = new ABHProp();
        
        public static ABHProp getInstance() {
            return INSTANCE;
        }
        
        public static final String KEY = "ABH_PROP";
        
        private ABHProp() {
            super("Test String Property", new ContextDef(ADimDef.getInstance(), BDimDef.getInstance(), HDimDef.getInstance()));
        }
        
        @Override
        public String getKey() {
            return KEY;
        }
        
    }
    
    @Test
    public void getDepth_emptyContext_expectCorrectDepth() {
        assertThat(Context.empty(ABHProp.getInstance()).getDepth()).isEqualTo(0L);
    }
    
    @Test
    public void getDepth_singleDimension_expectCorrectDepth() {
        assertThat(Context.newBuilder(ABHProp.getInstance()).add(ADimDef.getInstance().create("A")).build().getDepth())
            .isEqualTo(0x4000000000000000L);
        assertThat(Context.newBuilder(ABHProp.getInstance()).add(BDimDef.getInstance().create("B")).build().getDepth())
            .isEqualTo(0x2000000000000000L);
    }
    
    @Test
    public void getDepth_2SimpleDimensions_expectCorrectDepth() {
        assertThat(Context.newBuilder(ABHProp.getInstance())
            .add(ADimDef.getInstance().create("A"))
            .add(BDimDef.getInstance().create("B"))
            .build()
            .getDepth())
                .isEqualTo(0x6000000000000000L);
    }
    
    @Test
    public void getDepth_hierarchicalDimensions_expectCorrectDepth() {
        assertThat(Context.newBuilder(ABHProp.getInstance()).add(HDimDef.getInstance().create(HEnum.ROOT)).build().getDepth())
            .isEqualTo(0x800000000000000L);
        assertThat(Context.newBuilder(ABHProp.getInstance()).add(HDimDef.getInstance().create(HEnum.I1)).build().getDepth())
            .isEqualTo(0x1000000000000000L);
        assertThat(Context.newBuilder(ABHProp.getInstance()).add(HDimDef.getInstance().create(HEnum.I1_1)).build().getDepth())
            .isEqualTo(0x1800000000000000L);
    }
    
    @Test
    public void getDepth_simpleAndHierarchicalDimensions_expectCorrectDepth() {
        assertThat(Context.newBuilder(ABHProp.getInstance())
            .add(ADimDef.getInstance().create("A"))
            .add(HDimDef.getInstance().create(HEnum.ROOT))
            .build()
            .getDepth())
                .isEqualTo(0x4800000000000000L);
        assertThat(Context.newBuilder(ABHProp.getInstance())
            .add(ADimDef.getInstance().create("A"))
            .add(HDimDef.getInstance().create(HEnum.I1))
            .build()
            .getDepth())
                .isEqualTo(0x5000000000000000L);
        assertThat(Context.newBuilder(ABHProp.getInstance())
            .add(ADimDef.getInstance().create("A"))
            .add(HDimDef.getInstance().create(HEnum.I1_1))
            .build()
            .getDepth())
                .isEqualTo(0x5800000000000000L);
    }
    
}
