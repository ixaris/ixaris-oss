/*
 * Copyright 2002, 2007 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.misc.lib.object;

import java.io.Serializable;
import java.util.Objects;

public class Tuple4<T1, T2, T3, T4> implements Serializable {
    
    private static final long serialVersionUID = -3244327721423966548L;
    
    private final T1 t1;
    private final T2 t2;
    private final T3 t3;
    private final T4 t4;
    
    public Tuple4(final T1 t1, final T2 t2, final T3 t3, final T4 t4) {
        this.t1 = t1;
        this.t2 = t2;
        this.t3 = t3;
        this.t4 = t4;
    }
    
    public final T1 get1() {
        return t1;
    }
    
    public final T2 get2() {
        return t2;
    }
    
    public final T3 get3() {
        return t3;
    }
    
    public final T4 get4() {
        return t4;
    }
    
    @Override
    public boolean equals(final Object o) {
        return EqualsUtil.equals(
            this,
            o,
            other ->
                Objects.equals(this.t1, other.t1)
                    && Objects.equals(this.t2, other.t2)
                    && Objects.equals(this.t3, other.t3)
                    && Objects.equals(this.t4, other.t4)
        );
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(t1, t2, t3, t4);
    }
    
    @Override
    public String toString() {
        return ToStringUtil.of(this).with("1", t1).with("2", t2).with("3", t3).with("4", t4).toString();
    }
}
