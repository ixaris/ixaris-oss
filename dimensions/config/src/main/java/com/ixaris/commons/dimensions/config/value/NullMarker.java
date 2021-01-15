/**
 *
 */
package com.ixaris.commons.dimensions.config.value;

import com.ixaris.commons.misc.lib.object.EqualsUtil;

/**
 * @author <a href="aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public final class NullMarker {
    
    public static final NullMarker NULL = new NullMarker();
    
    private NullMarker() {}
    
    @Override
    public boolean equals(final Object o) {
        return EqualsUtil.equals(this, o, other -> true);
    }
    
    @Override
    public int hashCode() {
        return 1;
    }
    
}
