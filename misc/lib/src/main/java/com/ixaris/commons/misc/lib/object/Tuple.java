package com.ixaris.commons.misc.lib.object;

/**
 * Factory method for tuples
 */
public final class Tuple {
    
    public static <T1, T2> Tuple2<T1, T2> tuple(T1 t1, T2 t2) {
        return new Tuple2<>(t1, t2);
    }
    
    public static <T1, T2, T3> Tuple3<T1, T2, T3> tuple(T1 t1, T2 t2, T3 t3) {
        return new Tuple3<>(t1, t2, t3);
    }
    
    public static <T1, T2, T3, T4> Tuple4<T1, T2, T3, T4> tuple(T1 t1, T2 t2, T3 t3, T4 t4) {
        return new Tuple4<>(t1, t2, t3, t4);
    }
    
    public static <T1, T2, T3, T4, T5> Tuple5<T1, T2, T3, T4, T5> tuple(T1 t1, T2 t2, T3 t3, T4 t4, T5 t5) {
        return new Tuple5<>(t1, t2, t3, t4, t5);
    }
    
    private Tuple() {}
    
}
