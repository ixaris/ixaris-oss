package com.ixaris.commons.collections.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class HashSetMultiValueMapTest {
    
    @Test
    public void test() {
        
        final HashSetMultiMap<String, String> map = new HashSetMultiMap<String, String>();
        
        assertTrue(map.put("1", "1"));
        assertTrue(map.put("1", "2"));
        assertTrue(map.put("1", "3"));
        assertTrue(map.put("1", "4"));
        assertTrue(map.put("1", "5"));
        
        assertEquals(1, map.size());
        assertEquals(5, map.get("1").size());
        
        assertFalse(map.put("1", "5"));
        
        assertEquals(5, map.get("1").size());
        
        assertTrue(map.remove("1", "5"));
        assertFalse(map.remove("1", "5"));
        
        assertEquals(4, map.get("1").size());
        
        assertNull(map.get("2"));
        
        assertEquals(1, map.size());
        
        assertTrue(map.put("2", "1"));
        assertTrue(map.put("2", "2"));
        assertTrue(map.put("2", "3"));
        assertFalse(map.put("2", "3"));
        
        assertEquals(2, map.size());
        
        assertEquals(3, map.remove("2").size());
        assertEquals(1, map.size());
    }
    
}
