package com.ixaris.commons.zookeeper.clustering;

import static com.ixaris.commons.zookeeper.clustering.ZookeeperClusterSequences.findFirstGap;
import static org.assertj.core.api.Assertions.assertThat;

import org.junit.jupiter.api.Test;

public class ZookeeperClusterSequencesTest {
    
    @Test
    public void testFindFirstGap_emptyArray() {
        assertThat(findFirstGap(new int[0])).isEqualTo(0);
    }
    
    @Test
    public void testFindFirstGap_gapAtEnd() {
        assertThat(findFirstGap(new int[] { 0 })).isEqualTo(1);
        
        assertThat(findFirstGap(new int[] { 0, 1, 2 })).isEqualTo(3);
        
        assertThat(findFirstGap(new int[] { 0, 1, 2, 3, 4 })).isEqualTo(5);
        
        assertThat(findFirstGap(new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9 })).isEqualTo(10);
    }
    
    @Test
    public void testFindFirstGap_gapAtStart() {
        assertThat(findFirstGap(new int[] { 1, 2, 3 })).isEqualTo(0);
        
        assertThat(findFirstGap(new int[] { 2, 3, 4, 7, 9, 20 })).isEqualTo(0);
        
        assertThat(findFirstGap(new int[] { 5, 6, 7, 8, 9 })).isEqualTo(0);
    }
    
    @Test
    public void testFindFirstGap_gapInMiddle() {
        assertThat(findFirstGap(new int[] { 0, 1, 3 })).isEqualTo(2);
        
        assertThat(findFirstGap(new int[] { 0, 2, 3 })).isEqualTo(1);
        
        assertThat(findFirstGap(new int[] { 0, 1, 2, 4, 5, 6 })).isEqualTo(3);
        
        assertThat(findFirstGap(new int[] { 0, 1, 2, 3, 5, 6 })).isEqualTo(4);
        
        assertThat(findFirstGap(new int[] { 0, 5, 6, 7, 8, 9 })).isEqualTo(1);
        
        assertThat(findFirstGap(new int[] { 0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 20 })).isEqualTo(10);
    }
    
}
