/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.financial.lib;

import java.io.InvalidObjectException;
import java.io.ObjectInputValidation;
import java.io.Serializable;
import java.util.HashMap;
import java.util.Map;

/**
 * Immutable store for three-digit numeric country/currency code as dictated by ISO4217
 */
public class CurrencyNumeric implements Serializable, ObjectInputValidation, Comparable<CurrencyNumeric> {
    
    private static final long serialVersionUID = 4134147499080240419L;
    
    /**
     * Length of one of these codes as a String; always three digits.
     */
    public static final int CODE_LENGTH = 3;
    
    private static final Map<String, CurrencyNumeric> CACHE = new HashMap<>(256);
    
    public static CurrencyNumeric get(final String code) {
        final CurrencyNumeric currencyNumeric = CACHE.get(code);
        if (currencyNumeric != null) {
            return currencyNumeric;
        } else {
            final CurrencyNumeric newCurrencyNumeric = new CurrencyNumeric(code);
            CACHE.put(code, newCurrencyNumeric);
            return newCurrencyNumeric;
        }
    }
    
    /**
     * Validates the object.
     *
     * @param currencyCode the ISO 8853 code
     * @throws IllegalArgumentException
     */
    public static void validate(final String currencyCode) {
        
        if (currencyCode == null) {
            throw new IllegalArgumentException("Currency code should not be null");
        }
        
        // length must be 3
        if (currencyCode.length() != CODE_LENGTH) {
            throw new IllegalArgumentException("Invalid currency code: must be length 3");
        }
        
        for (int i = CODE_LENGTH; --i >= 0;) {
            final char c = currencyCode.charAt(i);
            if ((c < '0') || (c > '9')) {
                throw new IllegalArgumentException("Invalid currency code: must be pure decimal digits 000 - 999");
            }
        }
    }
    
    /**
     * The 3-digit currency/country code; never null.
     */
    // @Column(columnDefinition = "char(3)")
    private final String currency;
    
    /**
     * Construct an instance of this wrapped numeric currency code.
     *
     * @param code the ISO 8853 code
     */
    private CurrencyNumeric(final String code) {
        validate(code);
        this.currency = code;
    }
    
    /**
     * @return the numeric currency code as a String; never null.
     */
    public final String getCode() {
        return currency;
    }
    
    /**
     * @return a human-readable String representation; never null.
     */
    @Override
    public final String toString() {
        return currency;
    }
    
    /**
     * @return the hash code depends on that of the String representation.
     */
    @Override
    public final int hashCode() {
        return currency.hashCode();
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
    public final boolean equals(final Object o) {
        if (o == this) {
            return true;
        } else if (o == null) {
            return false;
        } else if (!(o instanceof CurrencyNumeric)) {
            return false;
        } else {
            return currency.equals(((CurrencyNumeric) o).currency);
        }
    }
    
    /**
     * Compares this object with the specified object for order.
     *
     * <p>Based on the default lexical ordering of the underlying String representation.
     *
     * @return a negative integer, zero, or a positive integer as this object is less than, equal to, or greater than the specified object.
     */
    @Override
    public final int compareTo(final CurrencyNumeric o) {
        return currency.compareTo(o.getCode());
    }
    
    /**
     * Validates the object.
     *
     * @throws InvalidObjectException if the object is in an invalid state
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
