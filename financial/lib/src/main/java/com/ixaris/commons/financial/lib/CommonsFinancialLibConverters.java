package com.ixaris.commons.financial.lib;

import java.math.BigDecimal;
import java.math.BigInteger;

import com.ixaris.commons.financial.lib.CommonsFinancialLib.CurrencyAmountMessage;
import com.ixaris.commons.financial.lib.CommonsFinancialLib.ScaledAmount;

/**
 * @author daniel.grech
 */
public class CommonsFinancialLibConverters {
    
    public static CurrencyAmountMessage convertCurrencyAmount(final String currency, final long amount) {
        return CurrencyAmount.fromUnscaled(currency, amount).asMessage();
    }
    
    public static CurrencyAmountMessage convertCurrencyAmount(final CurrencyAmount amt) {
        return amt.asMessage();
    }
    
    public static CurrencyAmount convertCurrencyAmount(final CurrencyAmountMessage amt) {
        return CurrencyAmount.fromMessage(amt);
    }
    
    public static CurrencyAmountMessage zero(final String currency) {
        return CurrencyAmount.zero(currency).asMessage();
    }
    
    public static BigDecimal convertScaledAmount(final ScaledAmount scaledAmount) {
        return new BigDecimal(BigInteger.valueOf(scaledAmount.getValue()), scaledAmount.getScale());
    }
    
    public static ScaledAmount convertScaledAmount(final BigDecimal amount) {
        return ScaledAmount.newBuilder().setValue(amount.unscaledValue().longValueExact()).setScale(amount.scale()).build();
    }
}
