package com.ixaris.commons.microservices.test.mocks;

import org.mockito.invocation.InvocationOnMock;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.test.mocks.SkeletonResourceMock.InvocationPathParams;

@FunctionalInterface
public interface AnswerWithPathParams {
    
    Async<?> answer(InvocationPathParams pathParams, InvocationOnMock invocation) throws Throwable; // NOSONAR generic signature
    
}
