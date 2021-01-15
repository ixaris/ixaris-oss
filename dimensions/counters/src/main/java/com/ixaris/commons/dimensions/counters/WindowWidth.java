/*
 * Copyright 2002, 2011 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.counters;

import java.util.Objects;

import com.ixaris.commons.misc.lib.object.EqualsUtil;

/**
 * Window Width includes amount and unit, example 2 days or 3 months. This class is immutable.
 *
 * @author <a href="mailto:andrew.calafato@ixaris.com">Andrew Calafato</a>
 */
public final class WindowWidth {
    
    private int width;
    private WindowTimeUnit unit;
    
    public WindowWidth(final int width, final WindowTimeUnit unit) {
        if (unit == null) {
            throw new IllegalArgumentException("unit is null");
        }
        
        this.width = width;
        this.unit = unit;
    }
    
    public int getWidth() {
        return width;
    }
    
    public WindowTimeUnit getUnit() {
        return unit;
    }
    
    /**
     * Utility method returning the window number for a particular timestamp using this window duration setup.
     */
    public long getWindowNumber(final long timestamp) {
        return unit.getWindowNumber(timestamp, width);
    }
    
    /**
     * Utility method returning the window start Timestamp for a particular timestamp using this window duration setup.
     */
    public long getStartTimestamp(final long timestamp) {
        return unit.getStartTimestamp(timestamp, width);
    }
    
    /**
     * Utility method returning the window start Timestamp for a particular timestamp using this window duration setup.
     */
    public long getStartTimestampForWindowNumber(final long windowNumber) {
        return unit.getStartTimestampForWindowNumber(windowNumber, width);
    }
    
    @Override
    public int hashCode() {
        return Objects.hash(width, unit);
    }
    
    @Override
    public boolean equals(final Object o) {
        return EqualsUtil.equals(this, o, other -> (width == other.width) && unit.equals(other.unit));
    }
    
    @Override
    public String toString() {
        return getWidth() + " " + getUnit().name();
    }
}
