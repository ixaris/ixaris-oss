package com.ixaris.commons.async.lib.filter;

import com.ixaris.commons.async.lib.Async;

/**
 * An asynchronous filter, for use with {@link AsyncFilterChain}. The filter needs to call next.next() for the chain to
 * procees. Not doing so implies that the rest of the chain is bypassed. This is similar in principle to servlet filters
 * where the filter needs to call chain.doFilter()
 */
@FunctionalInterface
public interface AsyncFilter<IN, OUT> {
    
    Async<OUT> doFilter(IN in, AsyncFilterNext<IN, OUT> next);
    
}
