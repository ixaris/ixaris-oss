package com.ixaris.commons.microservices.test.mocks;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.ListIterator;
import java.util.Map.Entry;

import org.mockito.ArgumentMatcher;
import org.mockito.internal.matchers.Any;
import org.mockito.internal.matchers.InstanceOf;

final class ParameterisedMocks {
    
    private final LinkedHashMap<MatcherKey, Object> mocks;
    
    ParameterisedMocks() {
        mocks = new LinkedHashMap<>();
    }
    
    void addMock(final ArgumentMatcher<?> matcher, Object mock) {
        if ((matcher instanceof Any) || (matcher instanceof InstanceOf)) {
            // since these match anything, previous matchers can be safely cleared
            mocks.clear();
        }
        mocks.put(new MatcherKey(matcher), mock);
    }
    
    Object getMock(final ArgumentMatcher<?> matcher) {
        return mocks.get(new MatcherKey(matcher));
    }
    
    Object getMock(final Object param) {
        final List<Entry<MatcherKey, Object>> entries = new ArrayList<>(mocks.entrySet());
        final ListIterator<Entry<MatcherKey, Object>> iterator = entries.listIterator(entries.size());
        // iterate in reverse order
        while (iterator.hasPrevious()) {
            final Entry<MatcherKey, Object> entry = iterator.previous();
            if (matches(entry.getKey().matcher, param)) {
                return entry.getValue();
            }
        }
        return null;
    }
    
    @SuppressWarnings("unchecked")
    private <T> boolean matches(final ArgumentMatcher<T> matcher, final Object param) {
        return matcher.matches((T) param);
    }
    
}
