package com.ixaris.commons.async.lib.filter;

import com.ixaris.commons.async.lib.Async;

/**
 * DO NOT IMPLEMENT! This is part of the Async filter chain api, given to filters as a way to invoke the rest of the
 * chain by calling next(), or bypass the chain by not calling next() 
 */
@FunctionalInterface
public interface AsyncFilterNext<IN, OUT> {
    
    Async<OUT> next(final IN in);
    
}
