package com.ixaris.commons.collections.lib;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class IntHashMapTest {
    
    @Test
    public void testMap() {
        final IntMap<String> map = new IntHashMap<>();
        
        assertEquals(0, map.size());
        
        map.put(12, "12");
        map.put(64, "64");
        
        assertEquals(2, map.size());
        assertEquals("12", map.get(12));
        assertEquals("64", map.get(64));
        
        map.remove(64);
        
        assertEquals(1, map.size());
        assertEquals("12", map.get(12));
    }
    
    @Test
    public void testCopy() {
        final IntMap<String> map = new IntHashMap<>();
        map.put(12, "12");
        map.put(64, "64");
        
        final IntMap<String> map2 = new IntHashMap<>(map);
        
        assertEquals(2, map2.size());
        assertEquals("12", map2.get(12));
        assertEquals("64", map2.get(64));
    }
    
}
