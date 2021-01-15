/*
 * Copyright 2002, 2008 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.dimensions.limits;

import com.ixaris.commons.dimensions.limits.data.AbstractLimitExtensionEntity;

public interface ValueLimitDef<I extends AbstractLimitExtensionEntity<?, I>> extends LimitDef<I> {
    
    CommonsDimensionsLimits.ValueLimitDef toProtobuf();
    
}
