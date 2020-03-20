package com.ixaris.commons.misc.lib.conversion;

import static org.junit.Assert.*;

import org.junit.Test;

public class HexUtilTest {
    
    @Test
    public void testHexUtil() {
        
        assertEquals("0123456789ABCDEF", HexUtil.encode(HexUtil.decode("0123456789ABCDEF")));
        assertEquals("FEDCBA9876543210", HexUtil.encode(HexUtil.decode("FEDCBA9876543210")));
        assertEquals("123456789ABCDEF0", HexUtil.encode(HexUtil.decode("123456789ABCDEF")));
        assertEquals("123456789ABCDEF", HexUtil.encode(HexUtil.decode("123456789ABCDEF"), false));
        assertEquals("CD", HexUtil.encode(HexUtil.decode("ABCDEF"), 1, 1));
        assertEquals("CDE", HexUtil.encode(HexUtil.decode("ABCDEF"), 1, 2, false));
        
        assertEquals("0123456789ABCDEF", HexUtil.encode(0x123456789ABCDEFL));
        assertEquals("FEDCBA9876543210", HexUtil.encode(0xFEDCBA9876543210L));
    }
    
}
