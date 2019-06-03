package com.ixaris.commons.misc.lib.object;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ToStringUtilTest {
    
    @Test
    public void testEqualsNullable() {
        
        // both null
        assertEquals("ToStringObject {a = a, b = 1}", new ToStringObject().toString());
    }
    
    private static class ToStringObject {
        
        @Override
        public String toString() {
            return ToStringUtil.of(this).with("a", "a").with("b", 1).toString();
        }
    }
    
}
