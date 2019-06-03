package com.ixaris.commons.misc.lib.object;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class WrapperTest {
    
    public static class Wrapper1 implements Wrapper<Object> {
        
        private final Object wrapped;
        
        public Wrapper1(final Object wrapped) {
            this.wrapped = wrapped;
        }
        
        @Override
        public Object unwrap() {
            return wrapped;
        }
        
    }
    
    public static class Wrapper2 implements Wrapper<Object> {
        
        private final Object wrapped;
        
        public Wrapper2(final Object wrapped) {
            this.wrapped = wrapped;
        }
        
        @Override
        public Object unwrap() {
            return wrapped;
        }
        
    }
    
    public static class Wrapper3 implements Wrapper<Object> {
        
        private final Object wrapped;
        
        public Wrapper3(final Object wrapped) {
            this.wrapped = wrapped;
        }
        
        @Override
        public Object unwrap() {
            return wrapped;
        }
        
    }
    
    @Test
    public void testIsWrappedBy() {
        final Object o = new Wrapper1(new Wrapper2(new Object()));
        assertTrue(Wrapper.isWrappedBy(o, Wrapper1.class));
        assertTrue(Wrapper.isWrappedBy(o, Wrapper2.class));
        assertFalse(Wrapper.isWrappedBy(o, Wrapper3.class));
    }
    
    @Test
    public void testUnwrap() {
        final Object o1 = new Object();
        final Object o2 = new Wrapper1(o1);
        final Object o3 = new Wrapper2(o2);
        assertEquals(o1, Wrapper.unwrap(o1));
        assertEquals(o1, Wrapper.unwrap(o2));
        assertEquals(o1, Wrapper.unwrap(o3));
    }
    
}
