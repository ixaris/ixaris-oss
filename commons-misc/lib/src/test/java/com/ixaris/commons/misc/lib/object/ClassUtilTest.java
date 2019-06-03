package com.ixaris.commons.misc.lib.object;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

public class ClassUtilTest {
    
    private static class A {}
    
    private static class B extends A {}
    
    private static class C extends B {}
    
    private static class D extends A {}
    
    @Test
    public void testResolve() {
        
        assertTrue(ClassUtil.isSameOrSubtypeOf(A.class, A.class));
        assertFalse(ClassUtil.isSameOrSubtypeOf(A.class, B.class));
        assertFalse(ClassUtil.isSameOrSubtypeOf(A.class, C.class));
        assertFalse(ClassUtil.isSameOrSubtypeOf(A.class, D.class));
        assertTrue(ClassUtil.isSameOrSubtypeOf(B.class, A.class));
        assertTrue(ClassUtil.isSameOrSubtypeOf(B.class, B.class));
        assertFalse(ClassUtil.isSameOrSubtypeOf(B.class, C.class));
        assertFalse(ClassUtil.isSameOrSubtypeOf(B.class, D.class));
        assertTrue(ClassUtil.isSameOrSubtypeOf(C.class, A.class));
        assertTrue(ClassUtil.isSameOrSubtypeOf(C.class, B.class));
        assertTrue(ClassUtil.isSameOrSubtypeOf(C.class, C.class));
        assertFalse(ClassUtil.isSameOrSubtypeOf(C.class, D.class));
        assertTrue(ClassUtil.isSameOrSubtypeOf(D.class, A.class));
        assertFalse(ClassUtil.isSameOrSubtypeOf(D.class, B.class));
        assertFalse(ClassUtil.isSameOrSubtypeOf(D.class, C.class));
        assertTrue(ClassUtil.isSameOrSubtypeOf(D.class, D.class));
        
        assertTrue(ClassUtil.isSameOrSubtypeOf(A.class, Object.class));
        assertFalse(ClassUtil.isSameOrSubtypeOf(Object.class, A.class));
    }
    
}
