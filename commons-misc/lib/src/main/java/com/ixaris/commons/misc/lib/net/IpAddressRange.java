package com.ixaris.commons.misc.lib.net;

import com.ixaris.commons.misc.lib.object.EqualsUtil;
import java.io.Serializable;
import java.util.Objects;

/**
 * IP address range object to checks if a given ip address falls in between 2 ip addresses (inclusive)
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
public final class IpAddressRange implements Serializable {
    
    private static final long serialVersionUID = 4459557979409300626L;
    
    public static IpAddressRange parse(final String range) {
        final String[] ips = range.split("-");
        if (ips.length > 2) {
            throw new IllegalArgumentException("Invalid range");
        }
        if (ips.length == 2) {
            return new IpAddressRange(IpAddress.parse(ips[0].trim()), IpAddress.parse(ips[1].trim()));
        } else {
            final IpAddress ip = IpAddress.parse(ips[0].trim());
            return new IpAddressRange(ip, ip);
        }
    }
    
    private IpAddress rangeStart;
    private IpAddress rangeEnd;
    
    public IpAddressRange(final IpAddress rangeStart, final IpAddress rangeEnd) {
        if (rangeStart == null) {
            throw new IllegalArgumentException("rangeStart is null");
        }
        if (rangeEnd == null) {
            throw new IllegalArgumentException("rangeEnd is null");
        }
        
        if (rangeStart.compareTo(rangeEnd) <= 0) {
            this.rangeStart = rangeStart;
            this.rangeEnd = rangeEnd;
        } else {
            this.rangeStart = rangeEnd;
            this.rangeEnd = rangeStart;
        }
    }
    
    public IpAddress getRangeStart() {
        return rangeStart;
    }
    
    public IpAddress getRangeEnd() {
        return rangeEnd;
    }
    
    public boolean implies(final IpAddress ip) {
        int i;
        if (rangeStart.isV4() && rangeEnd.isV4() && ip.isV4()) {
            // we know that the first 96 bits are the same
            i = 12;
        } else {
            i = 0;
        }
        
        boolean startSatisfied = false;
        
        for (; i < 16; i++) {
            // first check that given ip is larger than start (if equal, skip to the next part)
            if (!startSatisfied) {
                final int startResult = (ip.getAddressByte(i) & 0xFF) - (rangeStart.getAddressByte(i) & 0xFF);
                if (startResult < 0) {
                    return false;
                } else if (startResult > 0) {
                    // no need to keep checking start since we know that the given ip must be smaller
                    startSatisfied = true;
                } else {
                    continue;
                }
            }
            
            // then check that the given ip is smaller than end (if equal, skip to next part)
            final int endResult = (rangeEnd.getAddressByte(i) & 0xFF) - (ip.getAddressByte(i) & 0xFF);
            if (endResult < 0) {
                return false;
            } else if (endResult > 0) {
                // at this point we know that this part is in between start and end, so the ip is within range
                // no need for further checking
                return true;
            }
        }
        
        return true;
    }
    
    @Override
    public String toString() {
        return rangeStart.equals(rangeEnd) ? rangeStart.toString() : (rangeStart.toString() + "-" + rangeEnd);
    }
    
    @Override
    public final boolean equals(final Object o) {
        return EqualsUtil.equals(
            this, o, other -> rangeStart.equals(other.rangeStart) && rangeEnd.equals(other.rangeEnd)
        );
    }
    
    @Override
    public final int hashCode() {
        return Objects.hash(rangeStart, rangeEnd);
    }
    
}
