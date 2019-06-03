package com.ixaris.commons.misc.lib.net;

import com.ixaris.commons.misc.lib.conversion.HexUtil;
import com.ixaris.commons.misc.lib.object.EqualsUtil;
import java.net.InetAddress;
import java.util.Arrays;
import java.util.Locale;
import java.util.regex.Pattern;

/**
 * IP address object. Supports both ipv4 and ipv6
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
@SuppressWarnings("squid:S1191")
public class IpAddress implements Comparable<IpAddress> {
    
    /**
     * factory method from {@link InetAddress}
     */
    public static IpAddress forInetAddress(final InetAddress inetAddress) {
        if (inetAddress == null) {
            throw new IllegalArgumentException("inetAddress is null");
        }
        final byte[] address = inetAddress.getAddress();
        if (address == null) {
            throw new IllegalArgumentException("inetAddress.getAddress() is null");
        }
        return new IpAddress(address);
    }
    
    /**
     * factory method from bytes
     */
    public static IpAddress forBytes(final byte[] address) {
        return new IpAddress(address);
    }
    
    /**
     * Parse string representation of an ip
     */
    public static IpAddress parse(final String ip) {
        if (ip == null) {
            throw new IllegalArgumentException("ip is null");
        }
        
        byte[] address = textToNumericFormatV4(ip);
        if (address == null) {
            address = textToNumericFormatV6(ip);
            if (address == null) {
                throw new IllegalArgumentException("IP address [" + ip + "] is not a valid IP v4 or v6 address");
            }
        }
        return new IpAddress(address);
    }
    
    @SuppressWarnings({ "fallthrough", "squid:MethodCyclomaticComplexity" })
    private static byte[] textToNumericFormatV4(final String src) {
        byte[] res = new byte[4];
        
        long tmpValue = 0;
        int currByte = 0;
        boolean newOctet = true;
        
        int len = src.length();
        if (len == 0 || len > 15) {
            return null;
        }
        
        /*
         * When only one part is given, the value is stored directly in
         * the network address without any byte rearrangement.
         *
         * When a two part address is supplied, the last part is
         * interpreted as a 24-bit quantity and placed in the right
         * most three bytes of the network address. This makes the
         * two part address format convenient for specifying Class A
         * network addresses as net.host.
         *
         * When a three part address is specified, the last part is
         * interpreted as a 16-bit quantity and placed in the right
         * most two bytes of the network address. This makes the
         * three part address format convenient for specifying
         * Class B net- work addresses as 128.net.host.
         *
         * When four parts are specified, each is interpreted as a
         * byte of data and assigned, from left to right, to the
         * four bytes of an IPv4 address.
         *
         * We determine and parse the leading parts, if any, as single
         * byte values in one pass directly into the resulting byte[],
         * then the remainder is treated as a 8-to-32-bit entity and
         * translated into the remaining bytes in the array.
         */
        for (int i = 0; i < len; i++) {
            char c = src.charAt(i);
            if (c == '.') {
                if (newOctet || tmpValue < 0 || tmpValue > 0xFF || currByte == 3) {
                    return null;
                }
                res[currByte++] = (byte) (tmpValue & 0xFF);
                tmpValue = 0;
                newOctet = true;
            } else {
                int digit = Character.digit(c, 10);
                if (digit < 0) {
                    return null;
                }
                tmpValue *= 10;
                tmpValue += digit;
                newOctet = false;
            }
        }
        if (newOctet || tmpValue < 0 || tmpValue >= (1L << ((4 - currByte) * 8))) {
            return null;
        }
        switch (currByte) {
            case 0:
                res[0] = (byte) ((tmpValue >> 24) & 0xFF);
            case 1:
                res[1] = (byte) ((tmpValue >> 16) & 0xFF);
            case 2:
                res[2] = (byte) ((tmpValue >> 8) & 0xFF);
            case 3:
                res[3] = (byte) (tmpValue & 0xFF);
        }
        return res;
    }
    
    /*
     * Convert IPv6 presentation level address to network order binary form.
     * credit:
     *  Converted from C code from Solaris 8 (inet_pton)
     *
     * Any component of the string following a per-cent % is ignored.
     *
     * @param src a String representing an IPv6 address in textual format
     * @return a byte array representing the IPv6 numeric address
     */
    @SuppressWarnings({ "squid:MethodCyclomaticComplexity", "squid:S138", "squid:S1168" })
    public static byte[] textToNumericFormatV6(final String src) {
        // Shortest valid string is "::", hence at least 2 chars
        if (src.length() < 2) {
            return null;
        }
        
        char[] bytes = src.toCharArray();
        
        int length = bytes.length;
        int percentIndex = src.indexOf('%');
        if (percentIndex == length - 1) {
            return null;
        }
        
        if (percentIndex != -1) {
            length = percentIndex;
        }
        
        int colonPosition = -1;
        int i = 0, j = 0;
        /* Leading :: requires some special handling. */
        if (bytes[i] == ':') {
            if (bytes[++i] != ':') {
                return null;
            }
        }
        
        byte[] dst = new byte[16];
        int curtok = i;
        char ch;
        boolean sawXdigit = false;
        int val = 0;
        while (i < length) {
            ch = bytes[i++];
            int chval = Character.digit(ch, 16);
            if (chval != -1) {
                val <<= 4;
                val |= chval;
                if (val > 0xFFFF) {
                    return null;
                }
                sawXdigit = true;
                continue;
            }
            if (ch == ':') {
                curtok = i;
                if (!sawXdigit) {
                    if (colonPosition != -1) {
                        return null;
                    }
                    colonPosition = j;
                    continue;
                } else if (i == length) {
                    return null;
                }
                if (j + 2 > 16) {
                    return null;
                }
                dst[j++] = (byte) ((val >> 8) & 0xFF);
                dst[j++] = (byte) (val & 0xFF);
                sawXdigit = false;
                val = 0;
                continue;
            }
            if (ch == '.' && ((j + 4) <= 16)) {
                String ia4 = src.substring(curtok, length);
                /* check this IPv4 address has 3 dots, ie. A.B.C.D */
                int dot_count = 0, index = 0;
                while ((index = ia4.indexOf('.', index)) != -1) {
                    dot_count++;
                    index++;
                }
                if (dot_count != 3) {
                    return null;
                }
                byte[] v4addr = textToNumericFormatV4(ia4);
                if (v4addr == null) {
                    return null;
                }
                for (int k = 0; k < 4; k++) {
                    dst[j++] = v4addr[k];
                }
                sawXdigit = false;
                break; /* '\0' was seen by inet_pton4(). */
            }
            return null;
        }
        if (sawXdigit) {
            if (j + 2 > 16) {
                return null;
            }
            dst[j++] = (byte) ((val >> 8) & 0xFF);
            dst[j++] = (byte) (val & 0xFF);
        }
        
        if (colonPosition != -1) {
            if (j == 16) {
                return null;
            }
            
            int n = j - colonPosition;
            for (i = 1; i <= n; i++) {
                dst[16 - i] = dst[colonPosition + n - i];
                dst[colonPosition + n - i] = 0;
            }
            j = 16;
        }
        if (j != 16) {
            return null;
        }
        
        return dst;
    }
    
    /**
     * Utility routine to check if the InetAddress is an IPv4 mapped IPv6 address.
     *
     * @return a <code>boolean</code> indicating if the InetAddress is an IPv4 mapped IPv6 address; or false if address
     *     is IPv4 address.
     */
    private static boolean isIPv4MappedAddress(final byte[] address) {
        for (int i = 0; i <= 9; i++) {
            if (address[i] != 0x00) {
                return false;
            }
        }
        return (address[10] == (byte) 0xFF) && (address[11] == (byte) 0xFF);
    }
    
    private final byte[] address;
    
    private IpAddress(final byte[] address) {
        if (address == null) {
            throw new IllegalArgumentException("address is null");
        }
        
        if (address.length == 4) {
            this.address = new byte[4];
            System.arraycopy(address, 0, this.address, 0, 4);
        } else if (address.length == 16) {
            if (isIPv4MappedAddress(address)) {
                this.address = new byte[4];
                System.arraycopy(address, 12, this.address, 0, 4);
            } else {
                this.address = new byte[16];
                System.arraycopy(address, 0, this.address, 0, 16);
            }
        } else {
            throw new IllegalArgumentException("address bytes length should be 4 or 16");
        }
    }
    
    public final boolean isV4() {
        return address.length == 4;
    }
    
    final byte[] getAddress() {
        final byte[] copy = new byte[16];
        if (address.length == 4) {
            copy[10] = (byte) 0xFF;
            copy[11] = (byte) 0xFF;
            System.arraycopy(address, 0, copy, 12, 4);
        } else {
            System.arraycopy(address, 0, copy, 0, 16);
        }
        return copy;
    }
    
    final byte getAddressByte(final int index) {
        if ((index < 0) || (index > 15)) {
            throw new IllegalArgumentException("index out of range");
        }
        if (address.length == 4) {
            if (index <= 9) {
                return (byte) 0x00;
            } else if (index <= 11) {
                return (byte) 0xFF;
            } else {
                return address[index - 12];
            }
        } else {
            return address[index];
        }
    }
    
    /**
     * @return true if loopback, false otherwise
     */
    public final boolean isLoopback() {
        if (address.length == 4) {
            // 127.x.x.x
            return (address[0] & 0xFF) == 127;
        } else {
            for (int i = 0; i < 15; i++) {
                if (address[i] != 0x00) {
                    return false;
                }
            }
            return address[15] == 0x01;
        }
    }
    
    /**
     * @return true if private, false otherwise
     */
    public final boolean isPrivate() {
        if (address.length == 4) {
            // link-local unicast in IPv4 (169.254.0.0/16)
            // site-local 10/8 prefix 172.16/12 prefix 192.168/16 prefix
            return ((address[0] == (byte) 169) && (address[1] == (byte) 254))
                || (address[0] == (byte) 10)
                || ((address[0] == (byte) 172) && ((address[1] & 0xF0) == 16))
                || ((address[0] == (byte) 192) && (address[1] == (byte) 168));
        } else {
            return (address[0] == (byte) 0xFC)
                || (address[0] == (byte) 0xFD)
                || ((address[0] == (byte) 0xFE) && (((address[1] & 0xC0) == 0x80) || ((address[1] & 0xC0) == 0xC0)));
        }
    }
    
    private static final Pattern LEADING_ZEROS = Pattern.compile("0+([0-9A-F]+$)");
    
    @Override
    public String toString() {
        return (address.length == 4) ? toStringV4() : toStringV6();
    }
    
    private String toStringV4() {
        return (address[0] & 0xFF) + "." + (address[1] & 0xFF) + "." + (address[2] & 0xFF) + "." + (address[3] & 0xFF);
    }
    
    @SuppressWarnings("squid:MethodCyclomaticComplexity")
    private String toStringV6() {
        // According to http://en.wikipedia.org/wiki/IPv6_address - Two or more consecutive groups of zero value may
        // be replaced with a single empty group using two consecutive colons (::). Substitution may only be applied
        // once in an address, because multiple occurrences would create an ambiguous representation. If more than
        // one such substitution could be applied, the substitution that replaces the most groups must be used; if
        // the number of groups are equal then the leftmost substitution must be used.
        final String[] p = new String[8];
        boolean inZeros = false;
        int inZerosStart = -1;
        int inZerosEnd;
        
        int longestInZerosStart = -1;
        int longestInZerosEnd = -1;
        
        for (int i = 0; i < 16; i += 2) {
            if ((address[i] == 0) && (address[i + 1] == 0)) {
                p[i >> 1] = "0";
                if (!inZeros) {
                    inZeros = true;
                    inZerosStart = i >> 1;
                } else {
                    inZerosEnd = i >> 1;
                    if ((inZerosEnd - inZerosStart) > (longestInZerosEnd - longestInZerosStart)) {
                        longestInZerosEnd = inZerosEnd;
                        longestInZerosStart = inZerosStart;
                    }
                }
            } else {
                p[i >> 1] =
                    LEADING_ZEROS
                        .matcher(HexUtil.encode(address, i, i + 1))
                        .replaceAll("$1")
                        .toLowerCase(Locale.ENGLISH);
                inZeros = false;
            }
        }
        
        final StringBuilder v6 = new StringBuilder();
        boolean first = true;
        for (int i = 0; i < p.length; i++) {
            if (i == longestInZerosEnd) {
                v6.append("::");
                first = true;
            } else if ((i < longestInZerosStart) || (i > longestInZerosEnd)) {
                if (first) {
                    first = false;
                } else {
                    v6.append(":");
                }
                v6.append(p[i]);
            }
        }
        
        return v6.toString();
    }
    
    @Override
    public int compareTo(final IpAddress o) {
        for (int i = 0; i < 16; i++) {
            int result = (getAddressByte(i) & 0xFF) - (o.getAddressByte(i) & 0xFF);
            if (result != 0) {
                return result;
            }
        }
        
        return 0;
    }
    
    @Override
    public boolean equals(final Object o) {
        return EqualsUtil.equals(this, o, other -> Arrays.equals(address, other.address));
    }
    
    @Override
    public int hashCode() {
        int hash = (address[0] << 24) + (address[2] << 16) + (address[2] << 8) + address[3];
        if (address.length > 4) {
            hash +=
                (address[4] << 24)
                    + (address[5] << 16)
                    + (address[6] << 8)
                    + address[7]
                    + (address[8] << 24)
                    + (address[9] << 16)
                    + (address[10] << 8)
                    + address[11]
                    + (address[12] << 24)
                    + (address[13] << 16)
                    + (address[14] << 8)
                    + address[15];
        }
        
        return hash;
    }
    
}
