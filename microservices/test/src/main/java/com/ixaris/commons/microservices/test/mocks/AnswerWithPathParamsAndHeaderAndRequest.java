package com.ixaris.commons.microservices.test.mocks;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.microservices.test.mocks.SkeletonResourceMock.InvocationPathParams;

@FunctionalInterface
public interface AnswerWithPathParamsAndHeaderAndRequest {
    
    Async<?> answer(InvocationPathParams pathParams, ServiceOperationHeader<?> header, Object request) throws Exception; // NOSONAR generic signature
    
}
