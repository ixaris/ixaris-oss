package com.ixaris.commons.microservices.lib.client.proxy;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.CompletionStageUtil.whenDone;
import static com.ixaris.commons.async.lib.idempotency.Intent.INTENT;
import static com.ixaris.commons.async.lib.logging.AsyncLogging.ASYNC_MDC;
import static com.ixaris.commons.async.lib.logging.AsyncLogging.CORRELATION;
import static com.ixaris.commons.microservices.lib.client.ServiceStubEvent.wrapError;
import static com.ixaris.commons.microservices.lib.common.ServiceConstants.WATCH_METHOD_NAME;
import static com.ixaris.commons.microservices.lib.common.ServiceHeader.extractCorrelation;
import static com.ixaris.commons.microservices.lib.common.ServiceLoggingHelper.KEY_SERVICE_KEY;
import static com.ixaris.commons.microservices.lib.common.ServiceLoggingHelper.KEY_SERVICE_NAME;
import static com.ixaris.commons.misc.lib.object.Tuple.tuple;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;

import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.AsyncLocal;
import com.ixaris.commons.async.lib.CompletableFutureUtil;
import com.ixaris.commons.async.lib.FutureAsync;
import com.ixaris.commons.async.lib.idempotency.Intent;
import com.ixaris.commons.microservices.lib.client.ServiceEvent;
import com.ixaris.commons.microservices.lib.client.ServiceEvent.BackpressureException;
import com.ixaris.commons.microservices.lib.client.ServiceEvent.ServiceEventAroundAsync;
import com.ixaris.commons.microservices.lib.client.ServiceEventAckWrapper;
import com.ixaris.commons.microservices.lib.client.ServiceEventListener;
import com.ixaris.commons.microservices.lib.client.ServiceStub;
import com.ixaris.commons.microservices.lib.client.ServiceStubEvent;
import com.ixaris.commons.microservices.lib.client.support.ServiceEventProcessor;
import com.ixaris.commons.microservices.lib.common.ServiceEventHeader;
import com.ixaris.commons.microservices.lib.common.ServiceHeader;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.common.exception.ClientInvalidRequestException;
import com.ixaris.commons.microservices.lib.common.exception.ClientTooManyRequestsException;
import com.ixaris.commons.microservices.lib.common.exception.ServerErrorException;
import com.ixaris.commons.microservices.lib.common.exception.ServerTimeoutException;
import com.ixaris.commons.microservices.lib.common.exception.ServerUnavailableException;
import com.ixaris.commons.microservices.lib.common.exception.ServiceException;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventAckEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;
import com.ixaris.commons.misc.lib.defaults.Defaults;
import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.misc.lib.object.Tuple3;
import com.ixaris.commons.multitenancy.lib.TenantInactiveException;
import com.ixaris.commons.protobuf.lib.MessageHelper;

public final class ServiceStubEventProcessor<T extends ServiceStub> implements ServiceEventProcessor {
    
    private static final Logger LOG = LoggerFactory.getLogger(ServiceStubEventProcessor.class);
    
    private static EventAckEnvelope.Builder newEventAckEnvelopeBuilder(final EventEnvelope eventEnvelope) {
        return EventAckEnvelope.newBuilder()
            .setCorrelationId(eventEnvelope.getCorrelationId())
            .setCallRef(eventEnvelope.getCallRef());
    }
    
    private final ServiceStubProxy<T> proxy;
    private final ServiceEventListener<?, ?> listener;
    private final StubResourceInfo resource;
    
    ServiceStubEventProcessor(final ServiceStubProxy<T> proxy,
                              final ServiceEventListener<?, ?> listener,
                              final StubResourceInfo resource) {
        this.proxy = proxy;
        this.listener = listener;
        this.resource = resource;
    }
    
    @Override
    public Async<EventAckEnvelope> process(final EventEnvelope eventEnvelope) {
        if (eventEnvelope == null) {
            throw new IllegalArgumentException("eventEnvelope is null");
        }
        
        LOG.debug("Received event [{}] for [{}]", eventEnvelope.getCorrelationId(), proxy.serviceStubType);
        
        EventAckEnvelope eventAckEnvelope;
        try {
            proxy.multiTenancy.verifyTenantIsActive(eventEnvelope.getTenantId());
            final FutureAsync<EventAckEnvelope> future = new FutureAsync<>();
            final ScheduledFuture<?> scheduledTask = proxy.executor.schedule(() -> {
                LOG.debug("Publishing timeout acknowledgement [{}:{}] for [{}]",
                    eventEnvelope.getCorrelationId(),
                    eventEnvelope.getCallRef(),
                    eventEnvelope.getServiceName());
                future.complete(EventAckEnvelope.newBuilder()
                    .setCorrelationId(eventEnvelope.getCorrelationId())
                    .setCallRef(eventEnvelope.getCallRef())
                    .setStatusCode(ResponseStatusCode.SERVER_TIMEOUT)
                    .setStatusMessage(String.format("Timed out handling [%s:%s]",
                        eventEnvelope.getServiceName(),
                        eventEnvelope.getServiceKey()))
                    .build());
            },
                Defaults.DEFAULT_TIMEOUT,
                TimeUnit.MILLISECONDS);
            whenDone(listener.handle(getServiceStubEvent(eventEnvelope)), (r, t) -> {
                scheduledTask.cancel(false); // try to cancel the timeout
                CompletableFutureUtil.complete(future, r, t);
            });
            eventAckEnvelope = await(future);
        } catch (final TenantInactiveException e) {
            eventAckEnvelope = wrapError(eventEnvelope, new ServerUnavailableException(e));
        } catch (final ServiceException e) {
            eventAckEnvelope = wrapError(eventEnvelope, e);
        } catch (final Throwable e) { // NOSONAR framework code
            eventAckEnvelope = wrapError(eventEnvelope, new ServerErrorException(e));
        }
        
        LOG.debug("Event Ack [{}] [{}:{}] for [{}",
            eventAckEnvelope.getStatusCode(),
            eventEnvelope.getCorrelationId(),
            eventEnvelope.getCallRef(),
            proxy.serviceStubType);
        
        return result(eventAckEnvelope);
    }
    
    private ServiceStubEvent getServiceStubEvent(final EventEnvelope eventEnvelope) {
        return new ServiceStubEvent() {
            
            @Override
            public Async<EventAckEnvelope> invokeOnListener() {
                return ServiceStubEventProcessor.this.invokeOnListener(eventEnvelope);
            }
            
            @Override
            public ServiceEvent<?, ?> getResourceEventObject() throws BackpressureException {
                return ServiceStubEventProcessor.this.getResourceEventObject(eventEnvelope);
            }
            
            @Override
            public EventAckEnvelope wrapError(final ServiceException e) {
                return ServiceStubEvent.wrapError(eventEnvelope, e);
            }
            
            @Override
            public Class<?> getResourceType() {
                return resource.resourceType;
            }
            
            @Override
            public EventEnvelope getEventEnvelope() {
                return eventEnvelope;
            }
            
            @Override
            public MessageLite getEvent() {
                try {
                    return MessageHelper.parse(resource.eventType, eventEnvelope.getPayload());
                } catch (final InvalidProtocolBufferException e) {
                    throw new ClientInvalidRequestException(e);
                }
            }
            
        };
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + " for " + proxy.serviceStubType;
    }
    
    private Async<EventAckEnvelope> invokeOnListener(final EventEnvelope eventEnvelope) {
        final ServiceEventHeader<?> header;
        final MessageLite event;
        final Intent intent;
        try {
            final Tuple3<ServiceEventHeader<?>, MessageLite, Intent> extracted = validateAndExtractFromEnvelope(eventEnvelope);
            header = extracted.get1();
            event = extracted.get2();
            intent = extracted.get3();
        } catch (final ServiceException e) {
            return result(wrapError(eventEnvelope, e));
        }
        
        if (!proxy.handlerStrategy.getRequestStrategy().startMessage()) {
            return result(wrapError(eventEnvelope, new ClientTooManyRequestsException()));
        }
        
        Async<Void> future;
        try {
            future = aroundAsync(header, intent, () -> listener.aroundAsync(() -> onEvent(listener, header, event)));
        } catch (final Throwable e) { // NOSONAR framework code catches all throwables
            future = Async.rejected(e);
        }
        
        final ServiceEventAckWrapperImpl eventAckWrapper = new ServiceEventAckWrapperImpl(eventEnvelope);
        if (future != null) {
            try {
                await(future);
                return result(eventAckWrapper.success());
            } catch (final Throwable t) { // NOSONAR framework code needs to catch everything
                return result(eventAckWrapper.error(t));
            }
        } else {
            return result(eventAckWrapper.error(new ServerTimeoutException()));
        }
    }
    
    private ServiceEvent<?, ?> getResourceEventObject(final EventEnvelope eventEnvelope) throws BackpressureException {
        final Tuple3<ServiceEventHeader<?>, MessageLite, Intent> extracted = validateAndExtractFromEnvelope(eventEnvelope);
        final ServiceEventHeader<?> header = extracted.get1();
        final MessageLite event = extracted.get2();
        final Intent intent = extracted.get3();
        
        if (!proxy.handlerStrategy.getRequestStrategy().startMessage()) {
            throw new BackpressureException(wrapError(eventEnvelope, new ClientTooManyRequestsException()));
        }
        
        return new ServiceEvent<>(header,
            event,
            new ServiceEventAckWrapperImpl(eventEnvelope),
            new ServiceEventAroundAsync() {
                
                @Override
                public <U extends MessageLite, X, E extends Exception> X aroundAsync(final ServiceEventHeader<U> header,
                                                                                     final CallableThrows<X, E> callable) throws E {
                    return AsyncLocal
                        .with(CORRELATION, extractCorrelation(header))
                        .with(TENANT, header.getTenantId())
                        .with(ASYNC_MDC,
                            ImmutableMap.of(
                                KEY_SERVICE_NAME,
                                header.getServiceName(),
                                KEY_SERVICE_KEY,
                                header.getServiceKey()))
                        .exec(() -> ServiceStubEventProcessor.this.aroundAsync(header, intent, callable));
                }
                
            });
    }
    
    private Tuple3<ServiceEventHeader<?>, MessageLite, Intent> validateAndExtractFromEnvelope(final EventEnvelope eventEnvelope) throws ServiceException {
        try {
            final MessageLite parsedContext = MessageHelper.parse(proxy.contextType, eventEnvelope.getContext());
            final ServiceEventHeader<?> header = ServiceEventHeader.from(eventEnvelope, parsedContext);
            final MessageLite event = MessageHelper.parse(resource.eventType, eventEnvelope.getPayload());
            final ServicePathHolder path = ServicePathHolder.of(eventEnvelope.getPathList());
            final Intent intent = new Intent(eventEnvelope.getIntentId(),
                WATCH_METHOD_NAME + " " + path,
                MessageHelper.fingerprint(event));
            return tuple(header, event, intent);
            
        } catch (final InvalidProtocolBufferException e) {
            throw new ClientInvalidRequestException(e);
            
        } catch (final RuntimeException e) {
            throw new ServerErrorException(e);
        }
    }
    
    public <C extends MessageLite, U, E extends Exception> U aroundAsync(final ServiceHeader<C> header,
                                                                         final Intent intent,
                                                                         final CallableThrows<U, E> callable) throws E {
        return INTENT.exec(intent, () -> proxy.asyncInterceptor.aroundAsync(header, () -> proxy.handlerStrategy.aroundAsync(callable)));
    }
    
    @SuppressWarnings("unchecked")
    private <C extends MessageLite, E extends MessageLite> Async<Void> onEvent(final ServiceEventListener<?, ?> listener,
                                                                               final ServiceEventHeader<C> header,
                                                                               final E event) {
        return ((ServiceEventListener<C, E>) listener).onEvent(header, event);
    }
    
    private final class ServiceEventAckWrapperImpl implements ServiceEventAckWrapper {
        
        private final EventEnvelope eventEnvelope;
        private final AtomicBoolean used = new AtomicBoolean(false);
        
        private ServiceEventAckWrapperImpl(final EventEnvelope eventEnvelope) {
            this.eventEnvelope = eventEnvelope;
        }
        
        @Override
        public EventAckEnvelope success() {
            if (used.compareAndSet(false, true)) {
                proxy.handlerStrategy.getRequestStrategy().finishMessage();
                return newEventAckEnvelopeBuilder(eventEnvelope).setStatusCode(ResponseStatusCode.OK).build();
            } else {
                throw new IllegalStateException("success() or error() already called");
            }
        }
        
        @Override
        public EventAckEnvelope error(final Throwable t) {
            if (used.compareAndSet(false, true)) {
                LOG.error("Event Ack error", t);
                proxy.handlerStrategy.getRequestStrategy().finishMessage();
                final ServiceException ex = (t instanceof ServiceException)
                    ? (ServiceException) t : new ServerErrorException(t.getMessage());
                return wrapError(eventEnvelope, ex);
            } else {
                throw new IllegalStateException("success() or error() already called");
            }
        }
        
        @Override
        protected void finalize() throws Throwable {
            // just in case
            if (used.compareAndSet(false, true)) {
                proxy.handlerStrategy.getRequestStrategy().finishMessage();
                LOG.warn("ServiceEventAckWrapperImpl was not consumed for [{}:{}]",
                    eventEnvelope.getCorrelationId(),
                    eventEnvelope.getCallRef());
            }
            super.finalize();
        }
        
    }
    
}
