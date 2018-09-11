package com.ixaris.commons.async.lib.filter;

import static com.ixaris.commons.async.lib.Async.awaitExceptions;

import java.util.List;
import java.util.function.BiFunction;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.misc.lib.function.FunctionThrows;

/**
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public final class AsyncFilterChain<IN, OUT> {
    
    private final List<? extends AsyncFilter<IN, OUT>> filters;
    private final FunctionThrows<IN, Async<OUT>, ?> endFunction;
    private final BiFunction<IN, Throwable, Async<OUT>> errorFunction;
    
    private int index = 0;
    
    public AsyncFilterChain(final List<? extends AsyncFilter<IN, OUT>> filters,
                            final FunctionThrows<IN, Async<OUT>, ?> endFunction,
                            final BiFunction<IN, Throwable, Async<OUT>> errorFunction) {
        this.filters = filters;
        this.endFunction = endFunction;
        this.errorFunction = errorFunction;
    }
    
    public Async<OUT> doFilter(final IN in) {
        try {
            final Async<OUT> stage;
            if (filters.size() == index) {
                stage = endFunction.apply(in);
            } else {
                stage = filters.get(index++).doFilter(in, this);
            }
            return awaitExceptions(stage);
        } catch (final Throwable t) { // NOSONAR handling edge error
            return errorFunction.apply(in, t);
        }
    }
    
}
