package com.ixaris.commons.microservices.test.mocks;

import java.util.function.Supplier;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.exception.ClientConflictException;

public final class AsyncMockAnswer<T> implements Answer<Object> {
    
    public static <T> AsyncMockAnswer<T> success(final T result) {
        return new AsyncMockAnswer<T>().replaceWithSuccess(result);
    }
    
    public static <T> AsyncMockAnswer<T> conflict(final ClientConflictException conflict) {
        return new AsyncMockAnswer<T>().replaceWithConflict(conflict);
    }
    
    public static <T> AsyncMockAnswer<T> error(final Throwable error) {
        return new AsyncMockAnswer<T>().replaceWithError(error);
    }
    
    public static <T> AsyncMockAnswer<T> answer(final Supplier<Async<T>> supplier) {
        return new AsyncMockAnswer<>(supplier);
    }
    
    private volatile Supplier<Async<T>> supplier;
    
    private AsyncMockAnswer() {}
    
    public AsyncMockAnswer(final Supplier<Async<T>> supplier) {
        this.supplier = supplier;
    }
    
    public AsyncMockAnswer<T> replaceWithSuccess(final T result) {
        return replace(() -> Async.result(result));
    }
    
    public AsyncMockAnswer<T> replaceWithConflict(final ClientConflictException conflict) {
        return replace(() -> Async.rejected(conflict));
    }
    
    public AsyncMockAnswer<T> replaceWithError(final Throwable error) {
        return replace(() -> Async.rejected(error));
    }
    
    public AsyncMockAnswer<T> replace(final Supplier<Async<T>> supplier) {
        this.supplier = supplier;
        return this;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Object answer(final InvocationOnMock invocation) {
        return supplier.get();
    }
    
}
