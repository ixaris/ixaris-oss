/*
 * Copyright 2002, 2012 Ixaris Systems Ltd. All rights reserved. IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to
 * license terms.
 */
package com.ixaris.commons.misc.lib.sensitivedata;

import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;

import com.ixaris.commons.misc.lib.conversion.Base64Util;

/**
 * Message Authentication Code utilities - used for strong integrity checks.
 */
public final class MacUtil {
    
    private static MacUtil INSTANCE = new MacUtil();
    
    public static MacUtil getInstance() {
        return INSTANCE;
    }
    
    private static final String DEFAULT_ALORITHM = "HmacSHA512";
    
    private MacUtil() {}
    
    /**
     * @param data - the value to be maced
     * @return the mac
     */
    public byte[] mac(final byte[] data, final byte[] key) {
        return mac(data, key, DEFAULT_ALORITHM);
    }
    
    /**
     * Mac service
     *
     * @param data the data (utf-8 string) to be maced
     * @return the MAC of data as a btye array
     */
    public byte[] mac(final byte[] data, final byte[] key, final String algorithm) {
        if (data == null) {
            return null;
        }
        
        try {
            // initialise mac key
            final SecretKeySpec secretKeySpec = new SecretKeySpec(key, algorithm);
            
            // generate MAC of the data
            final Mac mac = Mac.getInstance(algorithm);
            mac.init(secretKeySpec);
            return mac.doFinal(data);
            
        } catch (InvalidKeyException e) {
            throw new IllegalStateException(e);
        } catch (NoSuchAlgorithmException e) {
            throw new UnsupportedOperationException(e);
        }
    }
    
    /**
     * @param value - the value to be maced
     * @return the mac
     */
    public String macToString(final byte[] value, final byte[] key) {
        return macToString(value, key, DEFAULT_ALORITHM);
    }
    
    /**
     * mac a byte array
     *
     * @param value - the value to be maced
     * @return the mac
     */
    public String macToString(final byte[] value, final byte[] key, final String algorithm) {
        return Base64Util.encode(mac(value, key, algorithm));
    }
    
    /**
     * @param value - the value to be maced
     * @return the mac of the lowercase value
     */
    public byte[] macCaseInsensitive(final String value, final byte[] key) {
        return macCaseInsensitive(value, key, DEFAULT_ALORITHM);
    }
    
    /**
     * macs a (case-insensitive) value using SHA-512 (default). The value is converted to lowercase before being maced,
     * causing the mac for different cases to be the same
     *
     * @param value - the value to be maced
     * @return the mac of the lowercase value
     */
    public byte[] macCaseInsensitive(final String value, final byte[] key, final String algorithm) {
        return value == null ? null : mac(getBytesFromString(value, false), key, algorithm);
    }
    
    /**
     * @param value - the value to be maced
     * @return the mac of the lowercase value
     */
    public String macCaseInsensitiveToString(final String value, final byte[] key) {
        return macCaseInsensitiveToString(value, key, DEFAULT_ALORITHM);
    }
    
    /**
     * macs a (case-insensitive) value. The value is converted to lowercase before being maced, causing the mac for
     * different cases to be the same
     *
     * @param value - the value to be maced
     * @return the mac of the lowercase value
     */
    public String macCaseInsensitiveToString(final String value, final byte[] key, final String algorithm) {
        return value == null ? null : macToString(getBytesFromString(value, false), key, algorithm);
    }
    
    /**
     * @param value - the value to be maced
     * @return the mac of the value
     */
    public byte[] macCaseSensitive(final String value, final byte[] key) {
        return macCaseSensitive(value, key, DEFAULT_ALORITHM);
    }
    
    /**
     * macs a (case-sensitive) value using SHA-512 (default).
     *
     * @param value - the value to be maced
     * @return the mac of the value
     */
    public byte[] macCaseSensitive(final String value, final byte[] key, final String algorithm) {
        return value == null ? null : mac(getBytesFromString(value, true), key, algorithm);
    }
    
    /**
     * @param value - the value to be maced
     * @return the mac of the value
     */
    public String macCaseSensitiveToString(final String value, final byte[] key) {
        return macCaseSensitiveToString(value, key, DEFAULT_ALORITHM);
    }
    
    /**
     * macs a (case-sensitive) value.
     *
     * @param value - the value to be maced
     * @return the mac of the value
     */
    public String macCaseSensitiveToString(final String value, final byte[] key, final String algorithm) {
        return value == null ? null : macToString(getBytesFromString(value, true), key, algorithm);
    }
    
    private byte[] getBytesFromString(final String value, final boolean caseSensitive) {
        if (caseSensitive) {
            return value.getBytes(StandardCharsets.UTF_8);
        } else {
            return value.toLowerCase().getBytes(StandardCharsets.UTF_8);
        }
    }
    
}
