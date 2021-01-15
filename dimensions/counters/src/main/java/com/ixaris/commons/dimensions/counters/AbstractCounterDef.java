/*
 * Copyright 2002, 2011 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.counters;

import org.jooq.UpdatableRecord;

import com.ixaris.commons.dimensions.lib.base.AbstractDimensionalDef;
import com.ixaris.commons.dimensions.lib.context.ContextDef;

/**
 * Base class for Counter event definitions
 *
 * @author <a href="mailto:andrew.calafato@ixaris.com">Andrew Calafato</a>
 */
public abstract class AbstractCounterDef<R extends UpdatableRecord<R>, C extends AbstractCounterDef<R, C>> extends AbstractDimensionalDef implements CounterDef<R, C> {
    
    private final int partitionDimCount;
    private final int cartesianProductDimCount;
    
    /**
     * @param description
     * @param contextDef
     */
    public AbstractCounterDef(final String description,
                              final ContextDef contextDef,
                              final int partitionDimCount,
                              final int cartesianProductDimCount) {
        super(description, contextDef);
        
        if ((partitionDimCount + cartesianProductDimCount) > 6) {
            throw new IllegalArgumentException("Partition and cartesian product context size exceeds 6");
        }
        
        this.partitionDimCount = partitionDimCount;
        this.cartesianProductDimCount = cartesianProductDimCount;
    }
    
    @Override
    public int getPartitionDimensionsCount() {
        return partitionDimCount;
    }
    
    @Override
    public int getCartesianProductDimensionsCount() {
        return cartesianProductDimCount;
    }
    
}
