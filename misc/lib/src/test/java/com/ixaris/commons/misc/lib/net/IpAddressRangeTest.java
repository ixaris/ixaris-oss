package com.ixaris.commons.misc.lib.net;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class IpAddressRangeTest {
    
    @Test
    public void testIpAddressRange() {
        
        final IpAddressRange ipRange = new IpAddressRange(
            IpAddress.parse("192.10.0.50"), IpAddress.parse("192.50.0.255"));
        
        assertThat(ipRange.toString()).isEqualTo("192.10.0.50-192.50.0.255");
        assertThat(ipRange.implies(IpAddress.parse("192.10.0.50"))).isTrue();
        assertThat(ipRange.implies(IpAddress.parse("192.47.0.52"))).isTrue();
        assertThat(ipRange.implies(IpAddress.parse("192.10.0.49"))).isFalse();
        assertThat(ipRange.implies(IpAddress.parse("192.50.1.0"))).isFalse();
        assertThat(ipRange.implies(IpAddress.parse("192.51.0.52"))).isFalse();
        assertThat(ipRange.implies(IpAddress.parse("192.9.0.52"))).isFalse();
        assertThat(ipRange.implies(IpAddress.parse("192.10.1.52"))).isFalse();
        
        final IpAddressRange ipRange2 = new IpAddressRange(
            IpAddress.parse("0.0.0.0"), IpAddress.parse("255.255.255.255"));
        
        assertThat(ipRange2.toString()).isEqualTo("0.0.0.0-255.255.255.255");
        assertThat(ipRange2.implies(IpAddress.parse("192.10.0.52"))).isTrue();
        assertThat(ipRange2.implies(IpAddress.parse("192.47.0.52"))).isTrue();
        assertThat(ipRange2.implies(IpAddress.parse("192.51.0.52"))).isTrue();
        assertThat(ipRange2.implies(IpAddress.parse("192.9.0.52"))).isTrue();
        assertThat(ipRange2.implies(IpAddress.parse("192.10.1.52"))).isTrue();
        assertThat(ipRange2.implies(IpAddress.parse("0.0.0.0"))).isTrue();
        assertThat(ipRange2.implies(IpAddress.parse("255.255.255.255"))).isTrue();
        
        final IpAddressRange ipRange3 = new IpAddressRange(
            IpAddress.parse("127.0.0.10"), IpAddress.parse("127.0.0.100"));
        
        assertThat(ipRange3.implies(IpAddress.parse("127.0.0.10"))).isTrue();
        assertThat(ipRange3.implies(IpAddress.parse("127.0.0.100"))).isTrue();
        assertThat(ipRange3.implies(IpAddress.parse("127.0.0.9"))).isFalse();
        assertThat(ipRange3.implies(IpAddress.parse("127.0.0.101"))).isFalse();
        assertThat(ipRange3.implies(IpAddress.parse("127.0.0.200"))).isFalse();
        
        final IpAddressRange ipRange4 = new IpAddressRange(IpAddress.parse("127.0.0.1"), IpAddress.parse("127.0.0.1"));
        
        assertThat(ipRange4.toString()).isEqualTo("127.0.0.1");
        assertThat(ipRange4.implies(IpAddress.parse("127.0.0.1"))).isTrue();
        assertThat(ipRange4.implies(IpAddress.parse("192.168.0.1"))).isFalse();
    }
    
}
