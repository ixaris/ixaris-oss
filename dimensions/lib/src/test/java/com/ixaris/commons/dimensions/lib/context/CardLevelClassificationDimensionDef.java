package com.ixaris.commons.dimensions.lib.context;

import com.ixaris.commons.dimensions.lib.dimension.EnumBasedDimensionDef;

public final class CardLevelClassificationDimensionDef extends EnumBasedDimensionDef<CardLevelClassification> {
    
    private static final CardLevelClassificationDimensionDef INSTANCE = new CardLevelClassificationDimensionDef();
    
    public static CardLevelClassificationDimensionDef getInstance() {
        return INSTANCE;
    }
    
    private CardLevelClassificationDimensionDef() {}
    
    @Override
    protected Class<CardLevelClassification> getEnumType() {
        return CardLevelClassification.class;
    }
    
    @Override
    protected CardLevelClassification[] getEnumValues() {
        return CardLevelClassification.values();
    }
    
    @Override
    public boolean isMatchAnySupported() {
        return true;
    }
}
