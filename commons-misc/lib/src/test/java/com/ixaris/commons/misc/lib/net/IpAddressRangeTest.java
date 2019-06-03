package com.ixaris.commons.misc.lib.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class IpAddressRangeTest {
    
    @Test
    public void testIpAddressRange() {
        
        final IpAddressRange ipRange = new IpAddressRange(
            IpAddress.parse("192.10.0.50"), IpAddress.parse("192.50.0.255")
        );
        
        assertEquals("192.10.0.50-192.50.0.255", ipRange.toString());
        assertTrue(ipRange.implies(IpAddress.parse("192.10.0.50")));
        assertTrue(ipRange.implies(IpAddress.parse("192.47.0.52")));
        assertFalse(ipRange.implies(IpAddress.parse("192.10.0.49")));
        assertFalse(ipRange.implies(IpAddress.parse("192.50.1.0")));
        assertFalse(ipRange.implies(IpAddress.parse("192.51.0.52")));
        assertFalse(ipRange.implies(IpAddress.parse("192.9.0.52")));
        assertFalse(ipRange.implies(IpAddress.parse("192.10.1.52")));
        
        final IpAddressRange ipRange2 = new IpAddressRange(
            IpAddress.parse("0.0.0.0"), IpAddress.parse("255.255.255.255")
        );
        
        assertEquals("0.0.0.0-255.255.255.255", ipRange2.toString());
        assertTrue(ipRange2.implies(IpAddress.parse("192.10.0.52")));
        assertTrue(ipRange2.implies(IpAddress.parse("192.47.0.52")));
        assertTrue(ipRange2.implies(IpAddress.parse("192.51.0.52")));
        assertTrue(ipRange2.implies(IpAddress.parse("192.9.0.52")));
        assertTrue(ipRange2.implies(IpAddress.parse("192.10.1.52")));
        assertTrue(ipRange2.implies(IpAddress.parse("0.0.0.0")));
        assertTrue(ipRange2.implies(IpAddress.parse("255.255.255.255")));
        
        final IpAddressRange ipRange3 = new IpAddressRange(
            IpAddress.parse("127.0.0.10"), IpAddress.parse("127.0.0.100")
        );
        
        assertTrue(ipRange3.implies(IpAddress.parse("127.0.0.10")));
        assertTrue(ipRange3.implies(IpAddress.parse("127.0.0.100")));
        assertFalse(ipRange3.implies(IpAddress.parse("127.0.0.9")));
        assertFalse(ipRange3.implies(IpAddress.parse("127.0.0.101")));
        assertFalse(ipRange3.implies(IpAddress.parse("127.0.0.200")));
        
        final IpAddressRange ipRange4 = new IpAddressRange(IpAddress.parse("127.0.0.1"), IpAddress.parse("127.0.0.1"));
        
        assertEquals("127.0.0.1", ipRange4.toString());
        assertTrue(ipRange4.implies(IpAddress.parse("127.0.0.1")));
        assertFalse(ipRange4.implies(IpAddress.parse("192.168.0.1")));
    }
    
}
