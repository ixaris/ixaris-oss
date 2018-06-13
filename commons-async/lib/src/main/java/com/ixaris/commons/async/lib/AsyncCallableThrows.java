package com.ixaris.commons.async.lib;

import java.util.concurrent.Callable;

import com.ixaris.commons.misc.lib.function.CallableThrows;

public interface AsyncCallableThrows<T, E extends Throwable> extends CallableThrows<Async<T>, E> {

    @SuppressWarnings("unchecked")
    static <T> AsyncCallableThrows<T, Exception> from(final Callable<Async<T>> c) {
        return c::call;
    }

}
