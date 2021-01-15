package com.ixaris.commons.financial.lib;

import com.ixaris.commons.financial.lib.CommonsFinancialLib.CurrencyAmountMessage;

/**
 * @author lazar.agatonovic
 */
public class CommonsFinancialLibCalculator {
    
    public static CurrencyAmountMessage add(final CurrencyAmountMessage amount1, final CurrencyAmountMessage amount2) {
        return CurrencyAmount.fromMessage(amount1).add(CurrencyAmount.fromMessage(amount2)).asMessage();
    }
    
    public static CurrencyAmountMessage subtract(final CurrencyAmountMessage amount1, final CurrencyAmountMessage amount2) {
        return CurrencyAmount.fromMessage(amount1).subtract(CurrencyAmount.fromMessage(amount2)).asMessage();
    }
    
    public static CurrencyAmountMessage negate(final CurrencyAmountMessage amt) {
        return CurrencyAmount.fromMessage(amt).negate().asMessage();
    }
}
