/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.financial.lib;

import java.io.InvalidObjectException;
import java.io.ObjectInputValidation;
import java.io.Serializable;
import java.math.BigDecimal;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

/**
 * Immutable object that holds one ISO/Swift three-letter (uppercase ASCII) currency code. These are the alphabetic codes as defined in ISO 4217.
 */
public class CurrencyCode implements Serializable, ObjectInputValidation, Comparable<CurrencyCode> {
    
    private static final long serialVersionUID = 2230780004647462089L;
    
    /**
     * The ISO/SWIFT code length in (uppercase 7-bit ASCII) characters.
     */
    public static final int CODE_LENGTH = 3;
    
    private static final Map<String, CurrencyCode> CACHE = new ConcurrentHashMap<>(64);
    
    public static CurrencyCode get(final String code) {
        final CurrencyCode currencyCode = CACHE.get(code);
        if (currencyCode != null) {
            return currencyCode;
        } else {
            final CurrencyCode newCurrencyCode = new CurrencyCode(code);
            CACHE.put(code, newCurrencyCode);
            return newCurrencyCode;
        }
    }
    
    /**
     * Validates the currency code. A currency code is a three (ASCII) letter uppercase string.
     *
     * @param currencyCode the currency code
     */
    public static void validate(final String currencyCode) {
        
        if (currencyCode == null) {
            throw new IllegalArgumentException("Currency should not be null");
        }
        
        if (currencyCode.length() != CODE_LENGTH) {
            throw new IllegalArgumentException("Invalid currency code: must be 3 letters long");
        }
        for (int i = CODE_LENGTH; --i >= 0;) {
            final char c = currencyCode.charAt(i);
            if ((c < 'A') || (c > 'Z')) {
                throw new IllegalArgumentException("Invalid currency code: must be uppercase ASCII letters");
            }
        }
    }
    
    /**
     * The currency code; never null.
     */
    // @Column(columnDefinition = "char(3)")
    private final String currency;
    
    /**
     * Construct an amount with this currency. This facilitates easy transition from a currency to an amount
     *
     * @param amount the scaled amount
     * @return A currency amount with this currency and the specified amount
     */
    public CurrencyAmount amount(final BigDecimal amount) {
        return CurrencyAmount.fromScaled(this, amount);
    }
    
    /**
     * Construct a Currency Amount from an unscaled value with this currency. This facilitates easy transition from a currency to an amount
     *
     * @param unscaledAmount the unscaled amount
     * @return A currency amount with this currency and the specified unscaled amount
     */
    public CurrencyAmount unscaled(final long unscaledAmount) {
        return CurrencyAmount.fromUnscaled(this, unscaledAmount);
    }
    
    /**
     * Make a CcyISOSwift object. Currency Codes should always be a three letter uppercase string.
     *
     * @param code the currency code
     */
    private CurrencyCode(final String code) {
        validate(code);
        this.currency = code;
    }
    
    /**
     * IIFT
     *
     * @return the currency code as a (3-letter uppercase ASCII) String; never null.
     */
    public final String getCode() {
        return currency;
    }
    
    /**
     * @return a String representation of this currency code for human consumption.
     */
    @Override
    public final String toString() {
        return currency;
    }
    
    /**
     * Check equality
     *
     * @param o the object with which to check equality
     * @return true if the given object is equal to this, false otherwise
     * @success return true when o == this
     * @success return false when o is null
     * @success return false when o is not of the same type
     * @success return false when o is of the same type but attributes do not match
     * @success return true when o is of the same type and attributes match
     */
    @Override
    public final boolean equals(final Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (!(o instanceof CurrencyCode)) {
            return false;
        } else {
            return currency.equals(((CurrencyCode) o).currency);
        }
    }
    
    /**
     * The hash code depends on that of the String representation.
     *
     * @return the hash code
     */
    @Override
    public final int hashCode() {
        return currency.hashCode();
    }
    
    /**
     * Compares this object with the specified object for order.
     *
     * <p>Based on the default lexical ordering of the underlying String representation.
     *
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.
     */
    @Override
    public final int compareTo(final CurrencyCode o) {
        return currency.compareTo(o.getCode());
    }
    
    /**
     * Validates the object.
     *
     * @throws InvalidObjectException If the object cannot validate itself.
     */
    @Override
    public final void validateObject() throws InvalidObjectException {
        try {
            validate(currency);
        } catch (final IllegalArgumentException e) {
            final InvalidObjectException ex = new InvalidObjectException("bad object: invalid currency code");
            ex.initCause(e);
            throw ex; // NOSONAR fancy way of attaching the cause exception because there is no constructor
        }
    }
    
}
