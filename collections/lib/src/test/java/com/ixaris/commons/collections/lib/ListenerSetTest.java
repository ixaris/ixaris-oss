package com.ixaris.commons.collections.lib;

import static org.assertj.core.api.Assertions.assertThat;

import org.junit.Test;

public class ListenerSetTest {
    
    @Test
    public void testListenerSet() {
        final ListenerSet<Object> set = new ListenerSet<>();
        
        final Object o1 = new Object();
        final Object o2 = new Object();
        
        assertThat(set.add(o1)).isTrue();
        assertThat(set.add(o1)).isFalse();
        assertThat(set.add(o2)).isTrue();
        
        set.publish(set::remove);
        
        assertThat(set.isEmpty()).isTrue();
    }
    
}
