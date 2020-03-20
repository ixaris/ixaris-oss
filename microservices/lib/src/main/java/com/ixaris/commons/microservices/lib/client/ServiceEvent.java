package com.ixaris.commons.microservices.lib.client;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.CommonsAsyncLib.Correlation;
import com.ixaris.commons.microservices.lib.common.ServiceEventHeader;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventAckEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventEnvelope;
import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.misc.lib.function.RunnableThrows;

public final class ServiceEvent<T extends MessageLite, EV extends MessageLite> {
    
    public static Correlation extractCorrelation(final EventEnvelope e) {
        return Correlation.newBuilder().setCorrelationId(e.getCorrelationId()).setIntentId(e.getIntentId()).build();
    }
    
    public static final class BackpressureException extends Exception {
        
        private final EventAckEnvelope eventAckEnvelope;
        
        public BackpressureException(final EventAckEnvelope eventAckEnvelope) {
            this.eventAckEnvelope = eventAckEnvelope;
        }
        
        public EventAckEnvelope getEventAckEnvelope() {
            return eventAckEnvelope;
        }
    }
    
    public interface ServiceEventAroundAsync {
        
        <T extends MessageLite, X, E extends Exception> X aroundAsync(final ServiceEventHeader<T> header, final CallableThrows<X, E> callable) throws E;
        
    }
    
    public final ServiceEventHeader<T> header;
    public final EV event;
    public final ServiceEventAckWrapper eventAckWrapper;
    
    private final ServiceEventAroundAsync aroundAsync;
    
    public ServiceEvent(final ServiceEventHeader<T> header,
                        final EV event,
                        final ServiceEventAckWrapper eventAckWrapper,
                        final ServiceEventAroundAsync aroundAsync) {
        this.header = header;
        this.event = event;
        this.eventAckWrapper = eventAckWrapper;
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
