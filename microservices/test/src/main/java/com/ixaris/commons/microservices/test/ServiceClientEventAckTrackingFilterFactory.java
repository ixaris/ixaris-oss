package com.ixaris.commons.microservices.test;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.CompletionStageUtil.join;
import static com.ixaris.commons.microservices.lib.common.ServiceConstants.WATCH_METHOD_NAME;
import static com.ixaris.commons.misc.lib.object.Tuple.tuple;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.springframework.stereotype.Component;

import com.ixaris.commons.microservices.lib.client.support.ServiceClientFilterFactory;
import com.ixaris.commons.microservices.lib.common.ServiceEventFilter;
import com.ixaris.commons.microservices.lib.common.ServiceHeader;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.common.exception.ServiceException;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventAckEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;
import com.ixaris.commons.microservices.lib.service.ServiceEventPublisher;
import com.ixaris.commons.misc.lib.object.Tuple3;

@Component
public final class ServiceClientEventAckTrackingFilterFactory implements ServiceClientFilterFactory {
    
    private final Map<Tuple3<String, Long, String>, CompletableFuture<Void>> waitingAcknowledgements = new HashMap<>();
    
    @Override
    public ServiceEventFilter createEventFilter(final String name) {
        return (in, next) -> {
            final EventAckEnvelope eventAckEnvelope = await(next.next(in));
            final CompletableFuture<Void> future;
            synchronized (waitingAcknowledgements) {
                future = waitingAcknowledgements.remove(extractIntent(in));
            }
            if (future != null) {
                if (ResponseStatusCode.OK.equals(eventAckEnvelope.getStatusCode())) {
                    future.complete(null);
                } else {
                    future.completeExceptionally(ServiceException.from(eventAckEnvelope.getStatusCode(),
                        eventAckEnvelope.getStatusMessage(),
                        null,
                        false));
                }
            }
            return result(eventAckEnvelope);
        };
    }
    
    public CompletionStage<Void> register(final ServiceHeader<?> header, final ServiceEventPublisher<?, ?, ?> publisher) {
        synchronized (waitingAcknowledgements) {
            return waitingAcknowledgements.computeIfAbsent(tuple(header.getTenantId(),
                header.getIntentId(),
                WATCH_METHOD_NAME + " " + publisher.getPath()), i -> new CompletableFuture<>());
        }
    }
    
    private Tuple3<String, Long, String> extractIntent(final EventEnvelope eventEnvelope) {
        return tuple(eventEnvelope.getTenantId(),
            eventEnvelope.getIntentId(),
            WATCH_METHOD_NAME + " " + ServicePathHolder.of(eventEnvelope.getPathList()));
    }
    
    @Override
    public int getOrder() {
        return 0;
    }
    
}
