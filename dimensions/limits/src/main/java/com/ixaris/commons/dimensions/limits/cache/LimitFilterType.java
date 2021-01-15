package com.ixaris.commons.dimensions.limits.cache;

import com.ixaris.commons.dimensions.lib.context.Context;
import com.ixaris.commons.dimensions.limits.LimitDef;
import com.ixaris.commons.dimensions.limits.data.AbstractLimitExtensionEntity;
import com.ixaris.commons.dimensions.limits.data.LimitEntity;

/**
 * <a href="https://jira.ixaris.com/confluence/display/TECH/Context+Property+Matching">Context Property Matching</a> defines examples of how
 * these limit filters work
 *
 * @author gordon.farrugia
 */
public enum LimitFilterType {
    
    /**
     * This filter type will return limits which have a context instance that is less than or equal to in terms of specificity when compared to
     * the querying context instance
     */
    MATCHING(Context::isMatching),
    
    /**
     * This filter type will return limits which have a context instance that is greater than or equal to in terms of specificity when compared
     * to the querying context instance
     */
    CONTAINING(Context::isContaining);
    
    @FunctionalInterface
    private interface LimitContextPredicate {
        
        <I extends AbstractLimitExtensionEntity<?, I>, L extends LimitDef<I>> boolean test(final L limitDef, final Context<L> limitContext, final Context<L> queryContext);
        
    }
    
    private final LimitContextPredicate limitContextPredicate;
    
    LimitFilterType(final LimitContextPredicate limitContextPredicate) {
        this.limitContextPredicate = limitContextPredicate;
    }
    
    public <I extends AbstractLimitExtensionEntity<?, I>, L extends LimitDef<I>> boolean test(final LimitEntity<I, L> limit, final Context<L> queryContext) {
        return limitContextPredicate.test(limit.getContext().getDef(), limit.getContext(), queryContext);
    }
    
}
