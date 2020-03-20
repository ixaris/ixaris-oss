package com.ixaris.commons.microservices.lib.example2.service;

import static com.ixaris.commons.async.lib.Async.result;

import java.util.List;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.exception.ClientTooManyRequestsException;
import com.ixaris.commons.microservices.lib.common.exception.ServerErrorException;
import com.ixaris.commons.microservices.lib.example.Example.ExampleResponse;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;
import com.ixaris.commons.microservices.lib.service.ServiceOperation;
import com.ixaris.commons.microservices.lib.service.ServiceOperation.BackpressureException;
import com.ixaris.commons.microservices.lib.service.ServiceSkeletonOperation;
import com.ixaris.commons.microservices.lib.service.proxy.ServiceSkeletonProxy;

public final class Example2SkeletonImpl implements Example2Skeleton {
    
    @Override
    public Async<ResponseEnvelope> handle(final ServiceSkeletonOperation operation) {
        final ServiceOperation<?, ?, ?, ?> operationObject;
        try {
            operationObject = operation.getResourceOperationObject();
        } catch (final BackpressureException e) {
            throw new ClientTooManyRequestsException(e);
        }
        
        if (operationObject instanceof Example2Skeleton.ExampleOperation2) {
            System.out.println("Called example operation 2");
            final ExampleOperation2 op = ((ExampleOperation2) operationObject);
            return result(op.responseWrapper.result(ExampleResponse.newBuilder().setId(op.request.getId()).build()));
        } else if (operationObject instanceof Example2Skeleton.IdSkeleton.ExampleOperation) {
            System.out.println("Called example operation 2");
            final IdSkeleton.ExampleOperation op = ((IdSkeleton.ExampleOperation) operationObject);
            return result(op.responseWrapper.result(ExampleResponse.newBuilder().setId(op.id).build()));
        } else {
            throw new ServerErrorException("Unknown operation [" + operationObject + "]");
        }
    }
    
}
