package com.ixaris.commons.dimensions.lib.context;

import com.ixaris.commons.dimensions.lib.dimension.BooleanDimensionDef;

public final class CardRenewedOnExpiryDimensionDef extends BooleanDimensionDef {
    
    private static final CardRenewedOnExpiryDimensionDef INSTANCE = new CardRenewedOnExpiryDimensionDef();
    
    public static CardRenewedOnExpiryDimensionDef getInstance() {
        return INSTANCE;
    }
    
    private CardRenewedOnExpiryDimensionDef() {}
    
}
