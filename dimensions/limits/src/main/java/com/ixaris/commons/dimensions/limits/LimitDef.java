/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.limits;

import com.ixaris.commons.dimensions.lib.base.DimensionalDef;
import com.ixaris.commons.dimensions.limits.CommonsDimensionsLimits.LimitCriterion;
import com.ixaris.commons.dimensions.limits.data.AbstractLimitExtensionEntity;
import com.ixaris.commons.dimensions.limits.data.AbstractLimitExtensionEntity.Fetch;
import com.ixaris.commons.misc.lib.registry.NoRegistry;

@NoRegistry
public interface LimitDef<I extends AbstractLimitExtensionEntity<?, I>> extends DimensionalDef {
    
    /**
     * @return {@link LimitCriterion} indicating which criteria (min/max/count) is supported by this limitdef
     */
    LimitCriterion getCriterion();
    
    Fetch<I, ?> getInfoFetch();
    
}
