package com.ixaris.commons.async.lib.filter;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;

import com.ixaris.commons.async.lib.Async;

/**
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public interface AsyncFilter<IN, OUT> {
    
    Async<OUT> doFilter(IN in, AsyncFilterChain<IN, OUT> chain);
    
}
