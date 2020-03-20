package com.ixaris.commons.async.lib.filter;

import static com.ixaris.commons.async.lib.Async.awaitExceptions;

import java.util.List;
import java.util.function.BiFunction;

import com.google.common.collect.ImmutableList;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.misc.lib.function.FunctionThrows;

/**
 * Asynchronous filter chain. Supports having multiple asynchronous tasks that can change the input and/or output
 * as well as bypass the rest of the chain altogether, e.g. caching. Works in a similar way as servlet filters
 * but every 
 */
public final class AsyncFilterChain<IN, OUT> {
    
    private final List<? extends AsyncFilter<IN, OUT>> filters;
    
    @SafeVarargs
    public AsyncFilterChain(final AsyncFilter<IN, OUT>... filters) {
        this.filters = ImmutableList.copyOf(filters);
    }
    
    public AsyncFilterChain(final List<? extends AsyncFilter<IN, OUT>> filters) {
        this.filters = ImmutableList.copyOf(filters);
    }
    
    public AsyncFilterNext<IN, OUT> with(final FunctionThrows<IN, Async<OUT>, ?> endFunction, final BiFunction<IN, Throwable, Async<OUT>> errorFunction) {
        return in -> exec(in, endFunction, errorFunction);
    }
    
    public Async<OUT> exec(final IN in,
                           final FunctionThrows<IN, Async<OUT>, ?> endFunction,
                           final BiFunction<IN, Throwable, Async<OUT>> errorFunction) {
        return doFilter(in, -1, endFunction, errorFunction);
    }
    
    @SuppressWarnings("squid:S1181")
    private Async<OUT> doFilter(final IN in,
                                final int index,
                                final FunctionThrows<IN, Async<OUT>, ?> endFunction,
                                final BiFunction<IN, Throwable, Async<OUT>> errorFunction) {
        try {
            final Async<OUT> stage;
            if (index == filters.size() - 1) {
                stage = endFunction.apply(in);
            } else {
                final int nextIndex = index + 1;
                stage = filters.get(nextIndex).doFilter(in, nextIn -> doFilter(nextIn, nextIndex, endFunction, errorFunction));
            }
            return awaitExceptions(stage);
        } catch (final Throwable t) {
            return errorFunction.apply(in, t);
        }
    }
    
}
