package com.ixaris.commons.financial.lib;

import static org.assertj.core.api.Assertions.assertThat;

import java.math.BigDecimal;

import org.junit.Test;

public class CurrencyAmountTest {
    
    @Test
    public void testCurrencyAmount() {
        
        CurrencyAmount a = CurrencyAmount.fromUnscaled(CurrencyCode.get("GBP"), 100L);
        CurrencyAmount b = CurrencyAmount.fromScaled(CurrencyCode.get("GBP"), new BigDecimal("1.00"));
        CurrencyAmount c = CurrencyAmount.fromScaled(CurrencyCode.get("GBP"), BigDecimal.ONE);
        CurrencyAmount d = CurrencyAmount.parse("GBP 1.00", true);
        
        assertThat(CurrencyCode.get("GBP")).isEqualTo(a.getCurrency());
        assertThat(100L).isEqualTo(a.getUnscaledAmount());
        assertThat(BigDecimal.ONE.setScale(2)).isEqualTo(a.getScaledAmount());
        
        assertThat(a).isEqualTo(b);
        assertThat(a).isEqualTo(c);
        assertThat(a).isEqualTo(d);
    }
    
    @Test
    public void testCurrencyAmountEquals() {
        
        CurrencyAmount a = CurrencyAmount.fromUnscaled(CurrencyCode.get("GBP"), 100L);
        CurrencyAmount b = CurrencyAmount.fromUnscaled(CurrencyCode.get("GBP"), 100L);
        CurrencyAmount c = CurrencyAmount.fromUnscaled(CurrencyCode.get("USD"), 100L);
        CurrencyAmount d = CurrencyAmount.fromUnscaled(CurrencyCode.get("GBP"), 1000L);
        
        assertThat(a).isEqualTo(b);
        assertThat(a).isNotEqualTo(c);
        assertThat(a).isNotEqualTo(d);
        assertThat(a).isNotNull();
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void add_ArgumentIsNull_ExceptionIsThrown() {
        final CurrencyAmount base = CurrencyAmount.fromUnscaled(CurrencyCode.get("EUR"), 1000L);
        base.add(null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void add_ArgumentIsOfDifferentCurrency_ExceptionIsThrown() {
        final CurrencyAmount base = CurrencyAmount.fromUnscaled(CurrencyCode.get("EUR"), 1000L);
        final CurrencyAmount augend = CurrencyAmount.fromUnscaled(CurrencyCode.get("GBP"), 1000L);
        base.add(augend);
    }
    
    @Test
    public void add_ArgumentIsPositive_AmountAdded() {
        final CurrencyAmount base = CurrencyAmount.fromUnscaled(CurrencyCode.get("EUR"), 1000L);
        final CurrencyAmount augend = CurrencyAmount.fromUnscaled(CurrencyCode.get("EUR"), 1000L);
        final CurrencyAmount expected = CurrencyAmount.fromUnscaled(CurrencyCode.get("EUR"), 2000L);
        assertThat(base.add(augend)).isEqualByComparingTo(expected);
    }
    
    @Test
    public void add_ArgumentIsNegative_AmountAdded() {
        final CurrencyAmount base = CurrencyAmount.fromUnscaled(CurrencyCode.get("EUR"), 1000L);
        final CurrencyAmount augend = CurrencyAmount.fromUnscaled(CurrencyCode.get("EUR"), -1000L);
        final CurrencyAmount expected = CurrencyAmount.fromUnscaled(CurrencyCode.get("EUR"), 0L);
        assertThat(base.add(augend)).isEqualByComparingTo(expected);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void subtract_SubtrahendIsNull_ExceptionIsThrown() {
        final CurrencyAmount base = CurrencyAmount.fromUnscaled(CurrencyCode.get("EUR"), 1000L);
        base.subtract(null);
    }
    
    @Test(expected = IllegalArgumentException.class)
    public void subtract_SubtrahendIsOfDifferentCurrency_ExceptionIsThrown() {
        final CurrencyAmount base = CurrencyAmount.fromUnscaled(CurrencyCode.get("EUR"), 1000L);
        final CurrencyAmount subtrahend = CurrencyAmount.fromUnscaled(CurrencyCode.get("GBP"), 1000L);
        base.subtract(subtrahend);
    }
    
    @Test
    public void subtract_SubtrahendIsPositive_AmountSubtracted() {
        final CurrencyAmount base = CurrencyAmount.fromUnscaled(CurrencyCode.get("EUR"), 1000L);
        final CurrencyAmount subtrahend = CurrencyAmount.fromUnscaled(CurrencyCode.get("EUR"), 1000L);
        final CurrencyAmount expected = CurrencyAmount.fromUnscaled(CurrencyCode.get("EUR"), 0L);
        assertThat(base.subtract(subtrahend)).isEqualByComparingTo(expected);
    }
    
    @Test
    public void subtract_SubtrahendIsNegative_AmountSubtracted() {
        final CurrencyAmount base = CurrencyAmount.fromUnscaled(CurrencyCode.get("EUR"), 1000L);
        final CurrencyAmount subtrahend = CurrencyAmount.fromUnscaled(CurrencyCode.get("EUR"), -1000L);
        final CurrencyAmount expected = CurrencyAmount.fromUnscaled(CurrencyCode.get("EUR"), 2000L);
        assertThat(base.subtract(subtrahend)).isEqualByComparingTo(expected);
    }
    
    @Test
    public void currencyCode_amount_shouldCreateEquivalentCurrencyAmount() {
        final CurrencyCode currencyCode = CurrencyCode.get("GBP");
        assertThat(currencyCode.amount(BigDecimal.ONE)).isEqualTo(CurrencyAmount.fromScaled(currencyCode, BigDecimal.ONE));
    }
    
    @Test
    public void currencyCode_unscaled_shouldCreateEquivalentCurrencyAmount() {
        final CurrencyCode currencyCode = CurrencyCode.get("GBP");
        assertThat(currencyCode.unscaled(100)).isEqualTo(CurrencyAmount.fromScaled(currencyCode, BigDecimal.ONE));
    }
    
    @Test
    public void currencyCode_unscaledAndUnscaledAmount_shouldBeEqual() {
        final CurrencyCode currencyCode = CurrencyCode.get("GBP");
        
        final int unscaledAmount = 100;
        assertThat(currencyCode.unscaled(unscaledAmount).getUnscaledAmount()).isEqualTo(unscaledAmount);
    }
}
