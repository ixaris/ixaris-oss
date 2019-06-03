/*
 * Copyright 2002, 2007 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.misc.lib.object;

import java.io.Serializable;
import java.util.Objects;

/**
 * A Pair Object to hold 2 Objects. This is a convenience Object to avoid creating an Object when there is the need to
 * hold or return 2 Objects. This class should not be abused! {@code Pair<List<Object>, Map<String, Pair<Pair<Object,
 * Object>, Object>>>} is an example of abusing this class. In cases like these, a specific class should be created to
 * help maintainability and code clarity.
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
public class Tuple2<T1, T2> implements Serializable {
    
    private static final long serialVersionUID = -3244327721423966548L;
    
    private final T1 t1;
    private final T2 t2;
    
    public Tuple2(final T1 t1, final T2 t2) {
        this.t1 = t1;
        this.t2 = t2;
    }
    
    public final T1 get1() {
        return t1;
    }
    
    public final T2 get2() {
        return t2;
    }
    
    @Override
    public boolean equals(final Object o) {
        return EqualsUtil.equals(
            this, o, other -> Objects.equals(this.t1, other.t1) && Objects.equals(this.t2, other.t2)
        );
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(t1, t2);
    }
    
    @Override
    public String toString() {
        return ToStringUtil.of(this).with("1", t1).with("2", t2).toString();
    }
    
}
