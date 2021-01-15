package com.ixaris.commons.dimensions.limits.cache;

import java.util.Comparator;

import com.ixaris.commons.dimensions.limits.LimitDef;
import com.ixaris.commons.dimensions.limits.data.AbstractLimitExtensionEntity;
import com.ixaris.commons.dimensions.limits.data.LimitEntity;

/**
 * A {@link LimitEntity} comparator which compares two limits on the basis of their specificity. The specificity of a limit is determined by its
 * context hash (which also takes into account hierarchical dimensions).
 *
 * @author <a href="mailto:juan.buhagiar@ixaris.com">juan.buhagiar</a>
 */
public class LimitSpecificityComparator<I extends AbstractLimitExtensionEntity<?, I>, L extends LimitDef<I>> implements Comparator<LimitEntity<I, L>> {
    
    @Override
    public int compare(final LimitEntity<I, L> o1, final LimitEntity<I, L> o2) {
        return Long.compare(o1.getContext().getDepth(), o2.getContext().getDepth());
    }
    
}
