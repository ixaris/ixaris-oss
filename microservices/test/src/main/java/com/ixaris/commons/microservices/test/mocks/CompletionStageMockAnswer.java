package com.ixaris.commons.microservices.test.mocks;

import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;

import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.exception.ClientConflictException;

@Deprecated
public final class CompletionStageMockAnswer<T> implements Answer<Object> {
    
    private static final Logger LOG = LoggerFactory.getLogger(CompletionStageMockAnswer.class);
    
    public static <T> CompletionStageMockAnswer<T> success(final T result) {
        return new CompletionStageMockAnswer<T>().replaceWithSuccess(result);
    }
    
    public static <T> CompletionStageMockAnswer<T> conflict(final ClientConflictException conflict) {
        return new CompletionStageMockAnswer<T>().replaceWithConflict(conflict);
    }
    
    public static <T> CompletionStageMockAnswer<T> error(final Throwable error) {
        return new CompletionStageMockAnswer<T>().replaceWithError(error);
    }
    
    public static <T> CompletionStageMockAnswer<T> answer(final Callable<CompletionStage<T>> callable) {
        return new CompletionStageMockAnswer<>(callable);
    }
    
    private volatile Callable<CompletionStage<T>> callable;
    
    private CompletionStageMockAnswer() {}
    
    public CompletionStageMockAnswer(final Callable<CompletionStage<T>> callable) {
        this.callable = callable;
    }
    
    public CompletionStageMockAnswer<T> replaceWithSuccess(final T result) {
        return replace(() -> Async.result(result));
    }
    
    public CompletionStageMockAnswer<T> replaceWithConflict(final ClientConflictException conflict) {
        return replace(() -> Async.rejected(conflict));
    }
    
    public CompletionStageMockAnswer<T> replaceWithError(final Throwable error) {
        return replace(() -> Async.rejected(error));
    }
    
    public CompletionStageMockAnswer<T> replace(final Callable<CompletionStage<T>> callable) {
        this.callable = callable;
        return this;
    }
    
    @Override
    @SuppressWarnings("unchecked")
    public Object answer(final InvocationOnMock invocation) {
        LOG.info("Returning mock successful answer {} {}", invocation.getMock().getClass().getName(), invocation.getMethod().getName());
        try {
            return Async.from(callable.call());
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }
    
}
