package com.ixaris.commons.collections.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.Test;

public class ExtendedCollectorsTest {
    
    @Test
    public void testReduce() {
        final List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
        list.add(5);
        list.add(6);
        
        Optional<String> collected = list.stream().collect(ExtendedCollectors.reducing(
            i -> "FIRST " + i, (s, i) -> s + " " + i
        ));
        assertEquals("FIRST 1 2 3 4 5 6", collected.get());
    }
    
    @Test
    public void testToMapOfList() {
        final List<int[]> numbers = new ArrayList<>();
        numbers.add(new int[] { 1, 1 });
        numbers.add(new int[] { 2, 2 });
        numbers.add(new int[] { 2, 3 });
        
        final Map<Integer, List<Integer>> map = numbers.stream().collect(ExtendedCollectors.toMapOfList(
            a -> a[0], a -> a[1]
        ));
        
        assertEquals(1, map.get(1).get(0).intValue());
        assertEquals(2, map.get(2).get(0).intValue());
        assertEquals(3, map.get(2).get(1).intValue());
    }
    
    @Test
    public void testToMapOfMapOfList() {
        final List<int[]> numbers = new ArrayList<>();
        numbers.add(new int[] { 1, 1, 1 });
        numbers.add(new int[] { 1, 2, 2 });
        numbers.add(new int[] { 1, 3, 3 });
        numbers.add(new int[] { 1, 4, 4 });
        numbers.add(new int[] { 2, 1, 5 });
        numbers.add(new int[] { 2, 2, 6 });
        numbers.add(new int[] { 2, 2, 7 });
        
        final Map<Integer, Map<Integer, List<Integer>>> map = numbers.stream().collect(ExtendedCollectors
            .toMapOfMapOfList(a -> a[0], a -> a[1], a -> a[2]));
        
        assertEquals(1, map.get(1).get(1).get(0).intValue());
        assertEquals(2, map.get(1).get(2).get(0).intValue());
        assertEquals(3, map.get(1).get(3).get(0).intValue());
        assertEquals(4, map.get(1).get(4).get(0).intValue());
        assertEquals(5, map.get(2).get(1).get(0).intValue());
        assertEquals(6, map.get(2).get(2).get(0).intValue());
        assertEquals(7, map.get(2).get(2).get(1).intValue());
    }
    
}
