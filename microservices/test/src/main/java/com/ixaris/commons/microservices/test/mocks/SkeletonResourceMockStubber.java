package com.ixaris.commons.microservices.test.mocks;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;
import java.util.concurrent.CompletionStage;

import org.mockito.Mockito;
import org.mockito.internal.stubbing.answers.CallsRealMethods;
import org.mockito.internal.stubbing.answers.DoesNothing;
import org.mockito.internal.stubbing.answers.Returns;
import org.mockito.internal.stubbing.answers.ThrowsException;
import org.mockito.stubbing.Answer;
import org.mockito.stubbing.Stubber;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.Nil;
import com.ixaris.commons.microservices.test.mocks.SkeletonResourceMock.InvocationPathParams;

/**
 * We need to delay calling mockito until the when() method is called with the mock, otherwise mockito will intercept the mock resource calls and
 * throw an UnfinishedStubbingException
 */
public final class SkeletonResourceMockStubber implements Stubber {
    
    private final InvocationPathParams pathParams;
    private final List<Answer<?>> answers = new LinkedList<>();
    
    SkeletonResourceMockStubber(final InvocationPathParams pathParams) {
        this.pathParams = pathParams;
        SkeletonResourceMock.setStubbing(true);
    }
    
    SkeletonResourceMockStubber internalDoAnswer(final Answer answer) {
        answers.add(answer);
        return this;
    }
    
    public SkeletonResourceMockStubber doAnswer(final AnswerWithPathParams answer) {
        return internalDoAnswer(invocation -> answer.answer(pathParams, invocation));
    }
    
    public SkeletonResourceMockStubber doAnswer(final AnswerWithPathParamsAndHeaderAndRequest answer) {
        return internalDoAnswer(invocation -> answer.answer(
            pathParams,
            invocation.getArgument(0),
            invocation.getMethod().getParameterCount() == 1 ? Nil.getInstance() : invocation.getArgument(1)));
    }
    
    @Override
    public SkeletonResourceMockStubber doAnswer(final Answer answer) {
        return internalDoAnswer(answer);
    }
    
    @Override
    public SkeletonResourceMockStubber doCallRealMethod() {
        doAnswer(new CallsRealMethods());
        return this;
    }
    
    @Override
    public SkeletonResourceMockStubber doNothing() {
        doAnswer(DoesNothing.doesNothing());
        return this;
    }
    
    @Override
    public SkeletonResourceMockStubber doReturn(final Object toBeReturned) {
        doAnswer(new Returns(fixReturn(toBeReturned)));
        return this;
    }
    
    @Override
    public SkeletonResourceMockStubber doReturn(final Object toBeReturned, final Object... nextToBeReturned) {
        doAnswer(new Returns(fixReturn(toBeReturned)));
        if (nextToBeReturned == null) {
            doAnswer(new Returns(null));
        } else {
            Arrays.stream(nextToBeReturned).forEach(r -> doAnswer(new Returns(fixReturn(r))));
        }
        return this;
    }
    
    private Object fixReturn(final Object toBeReturned) {
        if (toBeReturned instanceof CompletionStage) {
            return Async.from((CompletionStage<?>) toBeReturned);
        }
        return toBeReturned;
    }
    
    @Override
    public SkeletonResourceMockStubber doThrow(final Class<? extends Throwable> toBeThrown) {
        try {
            doAnswer(new ThrowsException(toBeThrown.getConstructor().newInstance()));
            return this;
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
    
    @SafeVarargs
    @Override
    public final SkeletonResourceMockStubber doThrow(final Class<? extends Throwable> toBeThrown,
                                                     final Class<? extends Throwable>... nextToBeThrown) {
        doThrow(toBeThrown);
        Arrays.stream(nextToBeThrown).forEach(this::doThrow);
        return this;
    }
    
    @Override
    public SkeletonResourceMockStubber doThrow(final Throwable... toBeThrown) {
        if (toBeThrown == null) {
            doAnswer(new ThrowsException(null));
        } else {
            Arrays.stream(toBeThrown).forEach(t -> doAnswer(new ThrowsException(t)));
        }
        return this;
    }
    
    @Override
    public <T> T when(T mock) {
        try {
            final Stubber stubber = Mockito.doAnswer(answers.remove(0));
            for (final Answer answer : answers) {
                stubber.doAnswer(answer);
            }
            return stubber.when(mock);
        } finally {
            SkeletonResourceMock.setStubbing(false);
        }
    }
    
}
