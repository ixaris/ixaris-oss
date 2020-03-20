package com.ixaris.commons.collections.lib;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.google.common.collect.ImmutableMap;
import com.google.common.collect.ImmutableSet;

public class GuavaCollectionsTest {
    
    @Test
    public void testCopyOfSetAdding() {
        final ImmutableSet<String> set = ImmutableSet.of("A", "B");
        Assertions.assertThat(set.size()).isEqualTo(2);
        final ImmutableSet<String> set2 = GuavaCollections.copyOfSetAdding(set, "C");
        Assertions.assertThat(set2.size()).isEqualTo(3);
        Assertions.assertThat(set2.contains("C")).isTrue();
        final ImmutableSet<String> set3 = GuavaCollections.copyOfSetAdding(set2, "D", "E");
        Assertions.assertThat(set3.size()).isEqualTo(5);
        Assertions.assertThat(set3.contains("D")).isTrue();
        Assertions.assertThat(set3.contains("E")).isTrue();
    }
    
    @Test
    public void testCopyOfSetRemoving() {
        final ImmutableSet<String> set = ImmutableSet.of("A", "B", "C", "D");
        Assertions.assertThat(set.size()).isEqualTo(4);
        final ImmutableSet<String> set2 = GuavaCollections.copyOfSetRemoving(set, "C", "D");
        Assertions.assertThat(set2.size()).isEqualTo(2);
        Assertions.assertThat(set2.contains("C")).isFalse();
        Assertions.assertThat(set2.contains("D")).isFalse();
    }
    
    @Test
    public void testCopyOfMapAdding() {
        final ImmutableMap<String, String> map = ImmutableMap.of("A", "A", "B", "B");
        Assertions.assertThat(map.size()).isEqualTo(2);
        final ImmutableMap<String, String> map2 = GuavaCollections.copyOfMapAdding(map, "C", "C");
        Assertions.assertThat(map2.size()).isEqualTo(3);
        Assertions.assertThat(map2.get("C")).isEqualTo("C");
        final ImmutableMap<String, String> map3 = GuavaCollections.copyOfMapAdding(map2, ImmutableMap.of("D", "D", "E", "E"));
        Assertions.assertThat(map3.size()).isEqualTo(5);
        Assertions.assertThat(map3.get("D")).isEqualTo("D");
        Assertions.assertThat(map3.get("E")).isEqualTo("E");
    }
    
    @Test
    public void testCopyOfMapRemoving() {
        final ImmutableMap<String, String> map = ImmutableMap.of("A", "A", "B", "B", "C", "C", "D", "D");
        Assertions.assertThat(map.size()).isEqualTo(4);
        final ImmutableMap<String, String> map2 = GuavaCollections.copyOfMapRemoving(map, "C", "D");
        Assertions.assertThat(map2.size()).isEqualTo(2);
        Assertions.assertThat(map2.get("C")).isNull();
        Assertions.assertThat(map2.get("D")).isNull();
    }
    
}
