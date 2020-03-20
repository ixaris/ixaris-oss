package com.ixaris.commons.misc.lib.object;

import static org.junit.Assert.*;

import java.util.Map;

import org.junit.Test;

public class GenericsUtilTest {
    
    private static class DClass<D1> extends CClass<D1, EEnum, DClass<D1>> {}
    
    private static class CClass<C1, C2 extends Enum<C2>, C3 extends CClass<C1, C2, C3>> extends BClass<C1, C2, C3> {}
    
    private static class BClass<B1, B2 extends Enum<B2>, B3 extends BClass<B1, B2, B3>> extends AClass<B2, B3> {}
    
    private abstract static class AClass<A1 extends Enum<A1>, A2 extends AClass<A1, A2>> implements IInterface<A1> {}
    
    private interface IInterface<I1 extends Enum<I1>> {}
    
    private abstract static class SimpleClass1 extends AClass<EEnum, SimpleClass1> {}
    
    private abstract static class SimpleClass2 implements IInterface<EEnum> {}
    
    private abstract static class SimpleClass3<E extends Enum<E>> implements IInterface<E> {}
    
    private interface IInterface2 extends IInterface<EEnum> {}
    
    private enum EEnum {
        A,
        B,
        C;
    }
    
    @Test
    public void testResolveSimpleGenericExample() {
        
        Map<String, Class<?>> map;
        
        map = GenericsUtil.resolveGenericTypeArguments(SimpleClass1.class, AClass.class);
        assertEquals(EEnum.class, map.get("A1"));
        assertEquals(SimpleClass1.class, map.get("A2"));
        
        map = GenericsUtil.resolveGenericTypeArguments(SimpleClass1.class, IInterface.class);
        assertEquals(EEnum.class, map.get("I1"));
        
        map = GenericsUtil.resolveGenericTypeArguments(SimpleClass2.class, IInterface.class);
        assertEquals(EEnum.class, map.get("I1"));
    }
    
    @Test
    public void testResolveConvolutedGenericExample() {
        
        Map<String, Class<?>> map;
        
        map = GenericsUtil.resolveGenericTypeArguments(DClass.class, IInterface.class);
        assertEquals(EEnum.class, map.get("I1"));
        
        map = GenericsUtil.resolveGenericTypeArguments(DClass.class, AClass.class);
        assertEquals(EEnum.class, map.get("A1"));
        assertEquals(DClass.class, map.get("A2"));
        
        map = GenericsUtil.resolveGenericTypeArguments(DClass.class, BClass.class);
        assertEquals(null, map.get("B1"));
        assertEquals(EEnum.class, map.get("B2"));
        assertEquals(DClass.class, map.get("B3"));
        
        map = GenericsUtil.resolveGenericTypeArguments(DClass.class, CClass.class);
        assertEquals(null, map.get("C1"));
        assertEquals(EEnum.class, map.get("C2"));
        assertEquals(DClass.class, map.get("C3"));
        
        map = GenericsUtil.resolveGenericTypeArguments(IInterface2.class, IInterface.class);
        assertEquals(EEnum.class, map.get("I1"));
    }
    
    @Test
    public void testUnresolvable() {
        
        Map<String, Class<?>> map;
        
        map = GenericsUtil.resolveGenericTypeArguments(SimpleClass3.class, IInterface.class);
        assertTrue(map.isEmpty());
    }
    
}
