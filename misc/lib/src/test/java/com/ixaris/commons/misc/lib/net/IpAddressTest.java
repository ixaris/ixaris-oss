package com.ixaris.commons.misc.lib.net;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import org.junit.Test;

public class IpAddressTest {
    
    @Test
    public void testCorrectIP() {
        
        IpAddress ip = IpAddress.parse("192.168.0.254");
        
        assertThat(ip.toString()).isEqualTo("192.168.0.254");
        assertThat(ip.getAddress()[12] & 0xFF).isEqualTo(192);
        assertThat(ip.getAddress()[13] & 0xFF).isEqualTo(168);
        assertThat(ip.getAddress()[14] & 0xFF).isEqualTo(0);
        assertThat(ip.getAddress()[15] & 0xFF).isEqualTo(254);
        assertThat(ip.isPrivate()).isTrue();
        
        assertThat(IpAddress.parse(IpAddress.parse("::").toString()))
            .isEqualTo(IpAddress.parse("0000:0000:0000:0000:0000:0000:0000:0000"));
        assertThat(IpAddress.parse(IpAddress.parse("FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF").toString()))
            .isEqualTo(IpAddress.parse("FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF:FFFF"));
        assertThat(IpAddress.parse(IpAddress.parse("fe80:0000:0000:0000:0202:b3ff:fe1e:8329").toString()))
            .isEqualTo(IpAddress.parse("fe80::202:b3ff:fe1e:8329"));
        
        assertThat(IpAddress.parse("0000:0000:0000:0000:0000:FFFF:C0A8:0101").toString()).isEqualTo("192.168.1.1");
        assertThat(IpAddress.parse("0000:0000:0000:0000:0000:0000:0000:0000").toString()).isEqualTo("::");
        assertThat(IpAddress.parse("fe80:0000:0000:0000:0202:b3ff:fe1e:8329").toString()).isEqualTo("fe80::202:b3ff:fe1e:8329");
        assertThat(IpAddress.parse("fe80:0000:0000:0000:0202:b3ff:0000:8329").toString()).isEqualTo("fe80::202:b3ff:0:8329");
        assertThat(IpAddress.parse("fe80:0000:0202:b3ff:0000:0000:0000:8329").toString()).isEqualTo("fe80:0:202:b3ff::8329");
        assertThat(IpAddress.parse("fe80:0000:0000:0202:b3ff:0000:0000:8329").toString()).isEqualTo("fe80::202:b3ff:0:0:8329");
        assertThat(IpAddress.parse("fe80:0000:0000:0202:b3ff:0000:0000:000").toString()).isEqualTo("fe80:0:0:202:b3ff::");
    }
    
    @Test
    public void testIncorrectIP() {
        assertThatThrownBy(() -> IpAddress.parse("256.0.0.1")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> IpAddress.parse("abc")).isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> IpAddress.parse(":::")).isInstanceOf(IllegalArgumentException.class);
    }
    
}
