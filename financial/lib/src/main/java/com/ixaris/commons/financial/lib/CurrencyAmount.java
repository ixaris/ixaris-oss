/*
 * Copyright 2002, 2006 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.financial.lib;

import java.io.Serializable;
import java.math.BigDecimal;
import java.math.RoundingMode;

import com.ixaris.commons.financial.lib.CommonsFinancialLib.CurrencyAmountMessage;

/**
 * The CurrencyAmount class is the fundamental class of any financial amount in Ixaris systems. We store values as BigDecimals (with scale as
 * specified by the currency itself - through CurrencyInfo).
 *
 * <p>This class historically comes for DHD's CurrencyAmount, without the overly complex code and is now JPA complaint.
 *
 * @author <a href="mailto:jeanpaul.ebejer@ixaris.com">JP</a>
 */
// @Embeddable
// @Access(AccessType.FIELD)
public class CurrencyAmount implements Serializable, Comparable<CurrencyAmount> {
    
    /* The serial verison UID */
    private static final long serialVersionUID = 7087544790239030114L;
    
    public static CurrencyAmount parse(final String amount) {
        return parse(amount, false);
    }
    
    /**
     * Create a CurrencyAmount from a String
     *
     * @param amount
     * @return
     */
    public static CurrencyAmount parse(final String amount, final boolean strict) {
        
        // check amount passed is null
        if (amount == null) {
            throw new IllegalArgumentException("amount should not be null");
        }
        
        // Make sure this starts with a valid currency code.
        final CurrencyCode ccy = CurrencyCode.get(amount.substring(0, 3));
        
        if (strict) {
            // And get the number of decimals.
            final CurrencyInfo currencyInfo = CurrencyInfo.get(ccy);
            
            // Find first digits allowing for optional sign.
            final char sign = amount.charAt(3);
            final int firstDigit = (sign == '-' || sign == '+' || sign == ' ') ? 4 : 3;
            
            // Find the first and last decimal points.
            // Should always be the same since at most one point should be present.
            final int decimalPos = amount.lastIndexOf('.');
            if (amount.indexOf('.') != decimalPos) {
                throw new IllegalArgumentException("amount must not have multiple decimal points");
            }
            
            final int len = amount.length();
            
            // If currency has no minor unit there must be no decimal...
            final int minorDecimals = currencyInfo.getLeftShift();
            if (minorDecimals == 0) {
                if (decimalPos != -1) {
                    throw new IllegalArgumentException("strict parse does not allow decimal for currency with no minor unit");
                }
            } else {
                if (decimalPos <= firstDigit) {
                    throw new IllegalArgumentException("strict parse requires digits before the decimal point");
                }
                if (len - decimalPos - 1 != minorDecimals) {
                    throw new IllegalArgumentException("strict parse requires the exact number of digits after the "
                        + "decimal point ("
                        + ccy
                        + "="
                        + minorDecimals
                        + ")");
                }
            }
            
            // Check that all characters between the currency and
            // the decimal point (if any) are decimal digits.
            for (int i = (decimalPos == -1) ? len : decimalPos; --i >= firstDigit;) {
                final char c = amount.charAt(i);
                if ((c < '0') || (c > '9')) {
                    throw new IllegalArgumentException("strict parse requires plain decimal format (no exponent for example)");
                }
            }
        }
        
        // get the actual value
        final String value = (amount.charAt(3) == ' ') ? amount.substring(4) : amount.substring(3);
        // this will ensure proper scale for the currency
        return new CurrencyAmount(ccy, new BigDecimal(value), RoundingMode.UNNECESSARY);
    }
    
    /**
     * @param currency
     * @return zero value amount for the specified currency
     */
    public static CurrencyAmount zero(final CurrencyCode currency) {
        if (currency == null) {
            throw new IllegalArgumentException("currency should not be null");
        }
        
        return new CurrencyAmount(currency, 0L);
    }
    
    public static CurrencyAmount zero(final String currency) {
        return zero(CurrencyCode.get(currency));
    }
    
    /**
     * @param currency may not be null
     * @param amount may not be null
     * @return new currency amount with the given parameters
     */
    public static CurrencyAmount fromUnscaled(final CurrencyCode currency, final long amount) {
        return new CurrencyAmount(currency, amount);
    }
    
    /**
     * @param currency may not be null
     * @param amount may not be null
     * @return new currency amount with the given parameters
     */
    public static CurrencyAmount fromUnscaled(final String currency, final long amount) {
        return new CurrencyAmount(CurrencyCode.get(currency), amount);
    }
    
    /**
     * @param currency may not be null
     * @param amount may not be null
     * @return new currency amount with the given parameters
     */
    public static CurrencyAmount fromScaled(final CurrencyCode currency, final BigDecimal amount) {
        return new CurrencyAmount(currency, amount, RoundingMode.UNNECESSARY);
    }
    
    /**
     * @param currency may not be null
     * @param amount may not be null
     * @param rounding the rounding to use when setting the scale
     * @return new currency amount with the given parameters
     */
    public static CurrencyAmount fromScaled(final CurrencyCode currency, final BigDecimal amount, final RoundingMode rounding) {
        return new CurrencyAmount(currency, amount, rounding);
    }
    
    /**
     * Creates a currency amount object from a protobuf currency amount message.
     *
     * @param currencyAmountMessage protobuf representation of currency amount
     * @return Parsed currency amount
     */
    public static CurrencyAmount fromMessage(final CurrencyAmountMessage currencyAmountMessage) {
        return CurrencyCode.get(currencyAmountMessage.getCurrency()).unscaled(currencyAmountMessage.getAmount());
    }
    
    /**
     * @param amount
     * @return the negated currency amount
     */
    public static CurrencyAmount negate(final CurrencyAmount amount) {
        if (amount == null) {
            throw new IllegalArgumentException("amount is null");
        }
        
        return new CurrencyAmount(amount.currency, -amount.unscaledAmount);
    }
    
    /**
     * @param first
     * @param second
     * @return the minimum from two currency amounts
     */
    public static CurrencyAmount min(final CurrencyAmount first, final CurrencyAmount second) {
        validateMatchingCurrencies(first, second);
        
        // return, take any currency since they are both the same
        return new CurrencyAmount(first.currency, Math.min(first.unscaledAmount, second.unscaledAmount));
    }
    
    /**
     * @param first
     * @param second
     * @return the maximum from two currency amounts
     */
    public static CurrencyAmount max(final CurrencyAmount first, final CurrencyAmount second) {
        validateMatchingCurrencies(first, second);
        
        // return, take any currency since they are both the same
        return new CurrencyAmount(first.currency, Math.max(first.unscaledAmount, second.unscaledAmount));
    }
    
    /**
     * @param amount
     * @return the absolute of this amount
     */
    public static CurrencyAmount abs(final CurrencyAmount amount) {
        if (amount == null) {
            throw new IllegalArgumentException("amount is null");
        }
        // return, take any currency since they are both the same
        return new CurrencyAmount(amount.currency, Math.abs(amount.unscaledAmount));
    }
    
    public static void validateMatchingCurrencies(final CurrencyAmount first, final CurrencyAmount second) {
        if (first == null) {
            throw new IllegalArgumentException("first is null");
        }
        if (second == null) {
            throw new IllegalArgumentException("second is null");
        }
        if (!first.currency.equals(second.currency)) {
            throw new IllegalArgumentException("Mismatched currencies: " + first.currency + " and " + second.currency);
        }
    }
    
    /**
     * The currency
     */
    // @Embedded
    private CurrencyCode currency;
    
    /**
     * The amount, as an unscaled value
     *
     * <p>Note that we use a column definition of
     *
     * <ul>
     *   <li>INT for movements (which will be in the range of -2147483648 to 2147483647), consuming 4 bytes in MySQL
     *   <li>BIGINT for balances/aggregates, consuming 8 bytes in MySQL
     * </ul>
     */
    private long unscaledAmount;
    
    protected CurrencyAmount() {}
    
    private CurrencyAmount(final CurrencyCode currency, final long unscaledAmount) {
        if (currency == null) {
            throw new IllegalArgumentException("currency is null");
        }
        
        this.currency = currency;
        this.unscaledAmount = unscaledAmount;
    }
    
    /*
     * Previously used rounding mode was ROUND_UP, inherited from Godly Damon. Then changed to ROUND_HALF_EVEN as this seemed
     * to be the standard in the financial world. Now changed to parameter so business logic decides rounding.
     */
    private CurrencyAmount(final CurrencyCode currency, final BigDecimal amount, final RoundingMode rounding) {
        if (currency == null) {
            throw new IllegalArgumentException("currency is null");
        }
        if (amount == null) {
            throw new IllegalArgumentException("amount is null");
        }
        
        final int shift = CurrencyInfo.get(currency).getLeftShift();
        this.currency = currency;
        this.unscaledAmount = amount.setScale(shift, rounding).movePointRight(shift).longValue();
    }
    
    public final CurrencyCode getCurrency() {
        return this.currency;
    }
    
    public final long getUnscaledAmount() {
        return unscaledAmount;
    }
    
    public final BigDecimal getScaledAmount() {
        return BigDecimal.valueOf(unscaledAmount, CurrencyInfo.get(currency).getLeftShift());
    }
    
    /**
     * Convert this currency amount object to a protobuf {@link CurrencyAmountMessage}
     */
    public CurrencyAmountMessage asMessage() {
        return CurrencyAmountMessage.newBuilder().setCurrency(currency.getCode()).setAmount(getUnscaledAmount()).build();
    }
    
    /**
     * @return the negated currency amount
     */
    public final CurrencyAmount negate() {
        return negate(this);
    }
    
    /**
     * Adds the passed amount to this currency amount. Convenience method really. (since it calls the static add in a underlying manner)
     *
     * @param augend The amount to add, may not be null
     * @return a new currency amount which is this + toAdd
     */
    public final CurrencyAmount add(final CurrencyAmount augend) {
        
        validateAmountParam(augend);
        
        return new CurrencyAmount(currency, unscaledAmount + augend.unscaledAmount);
    }
    
    /**
     * Subtracts the passed amount from this currency amount. Convenience method really. (since it calls the static sub in a underlying manner)
     *
     * @param subtrahend The amount to subtract, may not be null
     * @return a new currency amount which is this - toSub
     */
    public final CurrencyAmount subtract(final CurrencyAmount subtrahend) {
        
        validateAmountParam(subtrahend);
        
        return new CurrencyAmount(currency, unscaledAmount - subtrahend.unscaledAmount);
    }
    
    private void validateAmountParam(final CurrencyAmount param) {
        
        if (param == null) {
            throw new IllegalArgumentException("second should not be null");
        }
        if (!currency.equals(param.currency)) {
            throw new IllegalArgumentException("Cannot operate on different currencies: " + currency + " and " + param.currency);
        }
    }
    
    /**
     * Multiply this currency amount by a multiplicand
     *
     * @param multiplicand
     * @return this * multiplicand
     */
    public final CurrencyAmount multiply(final BigDecimal multiplicand, final RoundingMode rounding) {
        
        if (multiplicand == null) {
            throw new IllegalArgumentException("multiplicand should not be null");
        }
        
        return new CurrencyAmount(currency, BigDecimal.valueOf(unscaledAmount).multiply(multiplicand).setScale(0, rounding).longValue());
    }
    
    /**
     * Divide this currency amount by a divisor
     *
     * @param divisor
     * @return this / divisor
     */
    public final CurrencyAmount divide(final BigDecimal divisor, final RoundingMode rounding) {
        
        if (divisor == null) {
            throw new IllegalArgumentException("divisor should not be null");
        }
        
        if (BigDecimal.ZERO.compareTo(divisor) == 0) {
            throw new IllegalArgumentException("divisor cannot be zero");
        }
        
        return new CurrencyAmount(currency, BigDecimal.valueOf(unscaledAmount).divide(divisor, 0, rounding).longValue());
    }
    
    /**
     * @param other
     * @return the result of the comparison, as defined by {@link BigDecimal}
     * @throws IllegalArgumentException
     */
    public final int compareTo(final CurrencyAmount other) {
        
        if (other == null) {
            throw new IllegalArgumentException("other is null");
        }
        if (!this.currency.equals(other.currency)) {
            throw new IllegalArgumentException("Cannot compare different currencies: " + this.currency + " and " + other.currency);
        }
        
        // really return whatever is compared by the amounts
        return Long.compare(unscaledAmount, other.unscaledAmount);
    }
    
    /**
     * @return the hashcode, based on currency and amount
     */
    @Override
    public int hashCode() {
        return Long.hashCode(unscaledAmount) + currency.hashCode();
    }
    
    /**
     * Check equality
     *
     * @param o the object with which to check equality
     * @return true if the given object is equal to this, false otherwise
     * @business.requirement SUCCESS return true when o == this
     * @business.requirement SUCCESS return false when o is null
     * @business.requirement SUCCESS return false when o is not of the same type
     * @business.requirement SUCCESS return false when o is of the same type but attributes do not match
     * @business.requirement SUCCESS return true when o is of the same type and attributes match
     */
    @Override
    public boolean equals(final Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (!(this.getClass().equals(o.getClass()))) {
            return false;
        } else {
            final CurrencyAmount other = (CurrencyAmount) o;
            return (this.unscaledAmount == other.unscaledAmount) && this.currency.equals(other.currency);
        }
    }
    
    /**
     * Provides single toString representation such as GBP-122.00 or USD 5
     */
    @Override
    public String toString() {
        if (unscaledAmount < 0L) {
            return currency + getScaledAmount().toPlainString();
        } else {
            return currency + " " + getScaledAmount().toPlainString();
        }
    }
    
}
