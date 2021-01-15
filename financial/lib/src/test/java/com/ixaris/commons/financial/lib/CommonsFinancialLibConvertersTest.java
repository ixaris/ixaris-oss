package com.ixaris.commons.financial.lib;

import java.math.BigDecimal;
import java.math.BigInteger;
import java.math.MathContext;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.ixaris.commons.financial.lib.CommonsFinancialLib.ScaledAmount;

/**
 * @author lazar.agatonovic
 */
public class CommonsFinancialLibConvertersTest {
    
    @Test
    public void convert_ScaledAmountWithPositiveScaleToBigDecimal_Success() {
        final long value = 12345L;
        final int scale = 3;
        
        final ScaledAmount scaledAmount = buildScaledAmount(value, scale);
        final BigDecimal decimalAmount = CommonsFinancialLibConverters.convertScaledAmount(scaledAmount);
        
        final BigDecimal expectedAmount = BigDecimal.valueOf(value).multiply(BigDecimal.TEN.pow(-scale, MathContext.DECIMAL32));
        Assertions.assertThat(decimalAmount).isEqualByComparingTo(expectedAmount);
    }
    
    @Test
    public void convert_ScaledAmountWithNegativeScaleToBigDecimal_Success() {
        final long value = 12345L;
        final int scale = -2;
        
        final ScaledAmount scaledAmount = buildScaledAmount(value, scale);
        final BigDecimal decimalAmount = CommonsFinancialLibConverters.convertScaledAmount(scaledAmount);
        
        final BigDecimal expectedAmount = BigDecimal.valueOf(value).multiply(BigDecimal.TEN.pow(-scale));
        Assertions.assertThat(decimalAmount).isEqualByComparingTo(expectedAmount);
    }
    
    @Test
    public void convert_BigDecimalToScaledAmount_Success() {
        final long unscaledAmount = 12345678L;
        final int scale = 3;
        final BigDecimal decimalAmount = new BigDecimal(BigInteger.valueOf(unscaledAmount), scale);
        final ScaledAmount scaledAmount = CommonsFinancialLibConverters.convertScaledAmount(decimalAmount);
        
        Assertions.assertThat(scaledAmount.getValue()).isEqualTo(unscaledAmount);
        Assertions.assertThat(scaledAmount.getScale()).isEqualTo(scale);
    }
    
    @Test
    public void convert_BigDecimalToScaledAmountAndBack_Success() {
        final long unscaledAmount = 12345678L;
        final int scale = 3;
        final BigDecimal amountBefore = new BigDecimal(BigInteger.valueOf(unscaledAmount), scale);
        final ScaledAmount scaledAmount = CommonsFinancialLibConverters.convertScaledAmount(amountBefore);
        final BigDecimal amountAfter = CommonsFinancialLibConverters.convertScaledAmount(scaledAmount);
        
        Assertions.assertThat(amountAfter).isEqualByComparingTo(amountBefore);
    }
    
    private static ScaledAmount buildScaledAmount(final long value, final int scale) {
        return ScaledAmount.newBuilder().setValue(value).setScale(scale).build();
    }
}
