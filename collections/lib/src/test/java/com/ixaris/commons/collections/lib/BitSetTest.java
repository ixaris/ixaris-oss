package com.ixaris.commons.collections.lib;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.NoSuchElementException;

import org.junit.jupiter.api.Test;

public class BitSetTest {
    
    @Test
    public void testSet() {
        final BitSet set = BitSet.of(72);
        
        assertEquals(0, set.size());
        assertArrayEquals(new int[0], set.toArray());
        
        set.add(12);
        set.add(64);
        
        assertEquals(2, set.size());
        assertTrue(set.contains(12));
        assertTrue(set.contains(64));
        
        assertThrows(IndexOutOfBoundsException.class, () -> set.add(72));
        
        assertEquals(2, set.size());
        assertArrayEquals(new int[] { 12, 64 }, set.toArray());
        
        for (int i = 0; i < 72; i++) {
            set.add(i);
        }
        
        assertEquals(72, set.size());
        for (int i = 0; i < 72; i++) {
            assertTrue(set.contains(i));
        }
    }
    
    @Test
    public void testCopy() {
        final BitSet set = BitSet.of(72, 12, 64);
        
        assertEquals(2, set.size());
        assertArrayEquals(new int[] { 12, 64 }, set.toArray());
        
        final BitSet copy = BitSet.copy(set);
        
        assertEquals(2, copy.size());
        assertArrayEquals(new int[] { 12, 64 }, copy.toArray());
        
        assertEquals(set, copy);
    }
    
    @Test
    public void testIterator() {
        final BitSet set = BitSet.of(72);
        
        set.add(12);
        set.add(64);
        
        final BitSetIterator i = set.iterator();
        
        assertTrue(i.hasNext());
        assertEquals(12, i.next());
        assertEquals(64, i.next());
        assertFalse(i.hasNext());
        assertThrows(NoSuchElementException.class, i::next);
        
        final BitSetIterator i2 = set.iterator();
        
        assertTrue(i2.hasNext());
        assertEquals(12, i2.next());
        i2.remove();
        assertEquals(64, i2.next());
        assertFalse(i2.hasNext());
        assertThrows(NoSuchElementException.class, i2::next);
        
        assertEquals(1, set.size());
        assertArrayEquals(new int[] { 64 }, set.toArray());
    }
    
    @Test
    public void testAddAll() {
        final BitSet set1 = BitSet.of(72);
        set1.add(12);
        set1.add(64);
        
        final BitSet set2 = BitSet.of(72);
        set2.add(1);
        set2.add(3);
        set2.add(12);
        set2.add(70);
        
        set1.addAll(set2);
        
        assertEquals(5, set1.size());
        assertArrayEquals(new int[] { 1, 3, 12, 64, 70 }, set1.toArray());
    }
    
    @Test
    public void testRemoveAll() {
        final BitSet set1 = BitSet.of(72);
        set1.add(1);
        set1.add(3);
        set1.add(12);
        set1.add(64);
        
        final BitSet set2 = BitSet.of(72);
        set2.add(1);
        set2.add(3);
        set2.add(70);
        
        set1.removeAll(set2);
        
        assertEquals(2, set1.size());
        assertArrayEquals(new int[] { 12, 64 }, set1.toArray());
    }
    
}
