package com.ixaris.commons.collections.lib;

import static org.junit.Assert.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import org.junit.Test;

public class ExtendedCollectorsTest {
    
    @Test
    public void testReduce() throws Exception {
        
        final List<Integer> list = new ArrayList<>();
        list.add(1);
        list.add(2);
        list.add(3);
        list.add(4);
        list.add(5);
        list.add(6);
        
        Optional<String> collected = list.stream().collect(ExtendedCollectors.reducing(i -> "FIRST " + i, (s, i) -> s + " " + i));
        assertEquals("FIRST 1 2 3 4 5 6", collected.get());
    }
}
