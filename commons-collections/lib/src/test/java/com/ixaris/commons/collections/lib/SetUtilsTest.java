package com.ixaris.commons.collections.lib;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Tests for Set Utils.
 *
 * @author daniel.grech
 */
public class SetUtilsTest {
    
    /**
     * combinations(null, 2) = Exception (S is null)
     */
    @Test
    public void testCombinationsNullSet() {
        assertThrows(IllegalArgumentException.class, () -> SetUtils.combinations(null, 2));
    }
    
    /**
     * combinations({1,2,3,4,5}, -5) = Exception (k is negative)
     */
    @Test
    public void testCombinationsSizeNegative() {
        assertThrows(IllegalArgumentException.class, () ->
            SetUtils.combinations(SetBuilder.with(new HashSet<Integer>()).add(1, 2, 3, 4, 5).build(), -5)
        );
    }
    
    /**
     * combinations({}, 5) = {}
     */
    @Test
    public void testCombinationsEmptySet() {
        assertTrue(SetUtils.combinations(Collections.emptySet(), 5).isEmpty());
    }
    
    /**
     * combinations({1,2,3,4,5}, 0) = {}
     */
    @Test
    public void testCombinationsSizeZero() {
        assertTrue(
            SetUtils.combinations(SetBuilder.with(new HashSet<Integer>()).add(1, 2, 3, 4, 5).build(), 0).isEmpty()
        );
    }
    
    /**
     * combinations({1,2,3}, 1) = {{1},{2},{3}}
     */
    @Test
    public void testCombinationsSizeOne() {
        final Set<Set<Integer>> combinations = SetUtils.combinations(
            SetBuilder.with(new HashSet<Integer>()).add(1, 2, 3).build(), 1
        );
        assertEquals(
            SetBuilder
                .with(new HashSet<Set<Integer>>())
                .add(
                    SetBuilder.with(new HashSet<Integer>()).add(1).build(),
                    SetBuilder.with(new HashSet<Integer>()).add(2).build(),
                    SetBuilder.with(new HashSet<Integer>()).add(3).build()
                )
                .build(),
            combinations
        );
    }
    
    /**
     * combinations({1,2,3}, 2) = {{1,2}, {2,3}, {1,3}}
     */
    @Test
    public void testCombinations_ThreeElements_SizeTwo() {
        final Set<Set<Integer>> combinations = SetUtils.combinations(
            SetBuilder.with(new HashSet<Integer>()).add(1, 2, 3).build(), 2
        );
        assertEquals(
            SetBuilder
                .with(new HashSet<Set<Integer>>())
                .add(
                    SetBuilder.with(new HashSet<Integer>()).add(1, 2).build(),
                    SetBuilder.with(new HashSet<Integer>()).add(2, 3).build(),
                    SetBuilder.with(new HashSet<Integer>()).add(1, 3).build()
                )
                .build(),
            combinations
        );
    }
    
    /**
     * combinations({1,2,3,4}, 2) = {{1,2}, {1,3}, {1,4} {2,3}, {2,4}, {3,4}}
     */
    @Test
    public void testCombinations_FourElements_SizeTwo() {
        final Set<Set<Integer>> combinations = SetUtils.combinations(
            SetBuilder.with(new HashSet<Integer>()).add(1, 2, 3, 4).build(), 2
        );
        assertEquals(
            SetBuilder
                .with(new HashSet<Set<Integer>>())
                .add(
                    SetBuilder.with(new HashSet<Integer>()).add(1, 2).build(),
                    SetBuilder.with(new HashSet<Integer>()).add(1, 3).build(),
                    SetBuilder.with(new HashSet<Integer>()).add(1, 4).build(),
                    SetBuilder.with(new HashSet<Integer>()).add(2, 3).build(),
                    SetBuilder.with(new HashSet<Integer>()).add(2, 4).build(),
                    SetBuilder.with(new HashSet<Integer>()).add(3, 4).build()
                )
                .build(),
            combinations
        );
    }
    
    /**
     * combinations({1,2,3}, 3) = {{1,2,3}}
     */
    @Test
    public void testCombinations_ThreeElements_SizeThree() {
        final Set<Set<Integer>> combinations = SetUtils.combinations(
            SetBuilder.with(new HashSet<Integer>()).add(1, 2, 3).build(), 3
        );
        assertEquals(
            SetBuilder
                .with(new HashSet<Set<Integer>>())
                .add(SetBuilder.with(new HashSet<Integer>()).add(1, 2, 3).build())
                .build(),
            combinations
        );
    }
    
    /**
     * combinations({1,2,3,4}, 3) = {{1,2,3}, {1,2,4}, {1,3,4}, {2,3,4}}
     */
    @Test
    public void testCombinations_FourElements_SizeThree() {
        final Set<Set<Integer>> combinations = SetUtils.combinations(
            SetBuilder.with(new HashSet<Integer>()).add(1, 2, 3, 4).build(), 3
        );
        assertEquals(
            SetBuilder
                .with(new HashSet<Set<Integer>>())
                .add(
                    SetBuilder.with(new HashSet<Integer>()).add(1, 2, 3).build(),
                    SetBuilder.with(new HashSet<Integer>()).add(1, 2, 4).build(),
                    SetBuilder.with(new HashSet<Integer>()).add(1, 3, 4).build(),
                    SetBuilder.with(new HashSet<Integer>()).add(2, 3, 4).build()
                )
                .build(),
            combinations
        );
    }
    
}
