package com.ixaris.commons.dimensions.lib.context;

import com.ixaris.commons.dimensions.lib.dimension.EnumBasedDimensionDef;

public final class CardBrandDimensionDef extends EnumBasedDimensionDef<CardBrand> {
    
    private static final CardBrandDimensionDef INSTANCE = new CardBrandDimensionDef();
    
    public static CardBrandDimensionDef getInstance() {
        return INSTANCE;
    }
    
    private CardBrandDimensionDef() {}
    
    @Override
    protected Class<CardBrand> getEnumType() {
        return CardBrand.class;
    }
    
    @Override
    protected CardBrand[] getEnumValues() {
        return CardBrand.values();
    }
    
}
