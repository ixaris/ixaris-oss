/*
 * Licensed to the Apache Software Foundation (ASF) under one or more
 * contributor license agreements. See the NOTICE file distributed with
 * this work for additional information regarding copyright ownership.
 * The ASF licenses this file to You under the Apache License, Version 2.0
 * (the "License"); you may not use this file except in compliance with
 * the License. You may obtain a copy of the License at
 * http://www.apache.org/licenses/LICENSE-2.0
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.ixaris.commons.misc.lib.conversion;

/**
 * Tables useful when converting byte arrays to and from strings of hexadecimal digits. Code from Ajp11, from Apache's
 * JServ.
 *
 * @author Craig R. McClanahan
 */
public final class HexUtil {
    
    /**
     * Table for HEX to DEC byte translation.
     */
    private static final byte[] DEC = {
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        0, 1, 2, 3, 4, 5, 6, 7, 8, 9, -1, -1, -1, -1, -1, -1,
        -1, 10, 11, 12, 13, 14, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, 10, 11, 12, 13, 14, 15, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
        -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1, -1,
    };
    
    /**
     * Table for byte to hex string translation.
     */
    private static final char[] HEX = "0123456789ABCDEF".toCharArray();
    
    public static String encode(final long l) {
        final byte[] b = new byte[8];
        b[0] = (byte) (l >>> 56);
        b[1] = (byte) (l >>> 48);
        b[2] = (byte) (l >>> 40);
        b[3] = (byte) (l >>> 32);
        b[4] = (byte) (l >>> 24);
        b[5] = (byte) (l >>> 16);
        b[6] = (byte) (l >>> 8);
        b[7] = (byte) (l);
        return encode(b, 0, 7, true);
    }
    
    public static String encode(final byte b) {
        return encode(new byte[] { b }, 0, 0, true);
    }
    
    public static String encode(final byte... in) {
        return encode(in, 0, in.length - 1, true);
    }
    
    public static String encode(final byte[] in, final boolean even) {
        return encode(in, 0, in.length - 1, even);
    }
    
    public static String encode(final byte[] in, final int start, final int end) {
        return encode(in, start, end, true);
    }
    
    public static String encode(final byte[] in, final int start, final int end, final boolean even) {
        if (in == null) {
            return null;
        }
        
        final char[] c = new char[(end - start + 1) << 1];
        int pos = 0;
        
        for (int i = start; i < end; i++) {
            c[pos++] = HEX[(in[i] >>> 4) & 0xF];
            c[pos++] = HEX[in[i] & 0xF];
        }
        
        c[pos++] = HEX[(in[end] >>> 4) & 0xF];
        if (even) {
            c[pos++] = HEX[in[end] & 0x0F];
        }
        
        return new String(c, 0, pos);
    }
    
    public static byte[] decode(final String in) {
        if (in == null) {
            return null;
        }
        
        final int len = in.length();
        final byte[] out;
        if ((len & 1) == 0x0) {
            out = new byte[len >>> 1];
            
            for (int i = 0; i < out.length; i++) {
                out[i] = (byte) ((DEC[in.charAt(i << 1)] << 4) | DEC[in.charAt((i << 1) + 1)]);
            }
            
        } else {
            out = new byte[(len >>> 1) + 1];
            
            for (int i = 0; i < out.length - 1; i++) {
                out[i] = (byte) ((DEC[in.charAt(i << 1)] << 4) | DEC[in.charAt((i << 1) + 1)]);
            }
            
            out[out.length - 1] = (byte) (DEC[in.charAt(in.length() - 1)] << 4);
        }
        
        return out;
    }
    
}
