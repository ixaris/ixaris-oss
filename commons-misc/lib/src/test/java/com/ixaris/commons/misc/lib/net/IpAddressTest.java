package com.ixaris.commons.misc.lib.net;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.fail;

import org.junit.jupiter.api.Test;

public class IpAddressTest {
    
    @Test
    public void testCorrectIP() {
        
        IpAddress ip = IpAddress.parse("192.168.0.254");
        
        assertEquals("192.168.0.254", ip.toString());
        assertEquals(192, ip.getAddress()[12] & 0xFF);
        assertEquals(168, ip.getAddress()[13] & 0xFF);
        assertEquals(0, ip.getAddress()[14] & 0xFF);
        assertEquals(254, ip.getAddress()[15] & 0xFF);
        // assertTrue(ip.isPrivate());
        
        assertEquals(
            IpAddress.parse("0000:0000:0000:0000:0000:0000:0000:0000"),
            IpAddress.parse(IpAddress.parse("::").toString())
        );
        assertEquals(
            IpAddress.parse("FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF"),
            IpAddress.parse(IpAddress.parse("FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF").toString())
        );
        assertEquals(
            IpAddress.parse("fe80::202:b3ff:fe1e:8329"),
            IpAddress.parse(IpAddress.parse("fe80:0000:0000:0000:0202:b3ff:fe1e:8329").toString())
        );
        
        assertEquals("192.168.1.1", IpAddress.parse("0000:0000:0000:0000:0000:FFFF:C0A8:0101").toString());
        assertEquals("::", IpAddress.parse("0000:0000:0000:0000:0000:0000:0000:0000").toString());
        assertEquals("fe80::202:b3ff:fe1e:8329", IpAddress.parse("fe80:0000:0000:0000:0202:b3ff:fe1e:8329").toString());
        assertEquals("fe80::202:b3ff:0:8329", IpAddress.parse("fe80:0000:0000:0000:0202:b3ff:0000:8329").toString());
        assertEquals("fe80:0:202:b3ff::8329", IpAddress.parse("fe80:0000:0202:b3ff:0000:0000:0000:8329").toString());
        assertEquals("fe80::202:b3ff:0:0:8329", IpAddress.parse("fe80:0000:0000:0202:b3ff:0000:0000:8329").toString());
        assertEquals("fe80:0:0:202:b3ff::", IpAddress.parse("fe80:0000:0000:0202:b3ff:0000:0000:000").toString());
    }
    
    @Test
    public void testIncorrectIP() {
        
        try {
            IpAddress.parse("256.0.0.1");
            fail();
        } catch (IllegalArgumentException e) {
        }
        
        try {
            IpAddress.parse("abc");
            fail();
        } catch (IllegalArgumentException e) {
        }
        
        try {
            IpAddress.parse(":::");
            fail();
        } catch (IllegalArgumentException e) {
        }
    }
    
}
