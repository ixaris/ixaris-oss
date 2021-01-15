package com.ixaris.commons.financial.lib;

import static org.assertj.core.api.Assertions.*;

import org.assertj.core.api.Assertions;
import org.junit.Test;

public class CurrencyInfoTest {
    
    private static final String EUR_NUMERIC_CODE = "978";
    private static final String EUR_CURRENCY_CODE = "EUR";
    
    @Test
    public void testCurrencyInfo_ConvertNumeric() {
        final CurrencyNumeric currencyNumeric = CurrencyNumeric.get(EUR_NUMERIC_CODE);
        final CurrencyCode converted = CurrencyInfo.convert(currencyNumeric);
        assertThat(converted.getCode()).isEqualTo(EUR_CURRENCY_CODE);
    }
    
    @Test
    public void testCurrencyInfo_ConvertCurrencyCode() {
        final CurrencyCode currencyCode = CurrencyCode.get(EUR_CURRENCY_CODE);
        final CurrencyNumeric converted = CurrencyInfo.convert(currencyCode);
        assertThat(converted.getCode()).isEqualTo(EUR_NUMERIC_CODE);
    }
    
    @Test
    public void testCurrencyCode_Validate_invalidCurrency() {
        assertThatThrownBy(() -> CurrencyCode.validate("123")).hasMessageContaining("Invalid currency code");
    }
    
    @Test
    public void testCurrencyNumeric_Validate_invalidCurrency() {
        assertThatThrownBy(() -> CurrencyNumeric.validate("EUR")).hasMessageContaining("Invalid currency code");
    }
    
    @Test
    public void testCurrencyCode_Validate_validCurrency() {
        try {
            CurrencyCode.validate(EUR_CURRENCY_CODE);
        } catch (final Exception e) {
            Assertions.fail("Currency should have been treated as valid", e);
        }
    }
    
    @Test
    public void testCurrencyNumeric_Validate_validCurrency() {
        try {
            CurrencyNumeric.validate(EUR_NUMERIC_CODE);
        } catch (final Exception e) {
            Assertions.fail("Currency should have been treated as valid", e);
        }
    }
    
}
