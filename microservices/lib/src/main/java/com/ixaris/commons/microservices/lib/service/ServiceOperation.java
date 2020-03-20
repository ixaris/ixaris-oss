package com.ixaris.commons.microservices.lib.service;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.CommonsAsyncLib.Correlation;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;
import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.misc.lib.function.RunnableThrows;

public abstract class ServiceOperation<T extends MessageLite, RQ, RS, C> {
    
    public static Correlation extractCorrelation(final RequestEnvelope e) {
        return Correlation.newBuilder().setCorrelationId(e.getCorrelationId()).setIntentId(e.getIntentId()).build();
    }
    
    public static final class BackpressureException extends Exception {
        
        private final ResponseEnvelope responseEnvelope;
        
        public BackpressureException(final ResponseEnvelope responseEnvelope) {
            this.responseEnvelope = responseEnvelope;
        }
        
        public ResponseEnvelope getResponseEnvelope() {
            return responseEnvelope;
        }
        
    }
    
    public interface ServiceOperationAroundAsync {
        
        <T extends MessageLite, X, E extends Exception> X aroundAsync(final ServiceOperationHeader<T> header, final CallableThrows<X, E> callable) throws E;
        
    }
    
    public final ServiceOperationHeader<T> header;
    public final RQ request;
    public final ServiceResponseWrapper<RS, C> responseWrapper;
    
    private final ServiceOperationAroundAsync aroundAsync;
    
    public ServiceOperation(final ServiceOperationHeader<T> header,
                            final RQ request,
                            final ServiceResponseWrapper<RS, C> responseWrapper,
                            final ServiceOperationAroundAsync aroundAsync) {
        this.header = header;
        this.request = request;
        this.responseWrapper = responseWrapper;
        this.aroundAsync = aroundAsync;
    }
    
    public <X, E extends Exception> X aroundAsync(final CallableThrows<X, E> callable) throws E {
        return aroundAsync.aroundAsync(header, callable);
    }
    
    public <E extends Exception> void aroundAsync(final RunnableThrows<E> runnable) throws E {
        aroundAsync.aroundAsync(header, () -> {
            runnable.run();
            return null;
        });
    }
    
}
