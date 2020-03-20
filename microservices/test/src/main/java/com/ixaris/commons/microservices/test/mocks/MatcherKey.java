package com.ixaris.commons.microservices.test.mocks;

import org.mockito.ArgumentMatcher;
import org.mockito.internal.matchers.Any;
import org.mockito.internal.matchers.InstanceOf;

import com.ixaris.commons.misc.lib.object.EqualsUtil;

/**
 * Since not all mockito matchers implement equals and hashcode, this wrapper fills the implementation. This is used as a map key
 */
final class MatcherKey {
    
    final ArgumentMatcher<?> matcher;
    
    MatcherKey(final ArgumentMatcher<?> matcher) {
        this.matcher = matcher;
    }
    
    @Override
    public boolean equals(final Object o) {
        return EqualsUtil.equals(this, o, other -> {
            if (matcher instanceof Any) {
                return other.matcher instanceof Any;
            } else if (matcher instanceof InstanceOf) {
                return other.matcher instanceof InstanceOf;
            } else {
                return matcher.equals(other.matcher);
            }
        });
    }
    
    @Override
    public int hashCode() {
        if ((matcher instanceof Any) || (matcher instanceof InstanceOf)) {
            return getClass().hashCode();
        } else {
            return matcher.hashCode();
        }
    }
    
}
