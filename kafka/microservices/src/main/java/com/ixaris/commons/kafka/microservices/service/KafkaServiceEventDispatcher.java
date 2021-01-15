package com.ixaris.commons.kafka.microservices.service;

import static com.ixaris.commons.async.lib.Async.awaitExceptions;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.AsyncExecutor.relay;

import java.util.Set;
import java.util.concurrent.TimeoutException;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.kafka.multitenancy.KafkaConnectionHandler;
import com.ixaris.commons.kafka.multitenancy.KafkaUtil;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventAckEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;
import com.ixaris.commons.microservices.lib.service.support.ServiceEventDispatcher;

/**
 * Kafka Implementation dispatching service events
 */
public final class KafkaServiceEventDispatcher implements ServiceEventDispatcher {
    
    private static final Logger LOG = LoggerFactory.getLogger(KafkaServiceEventDispatcher.class);
    
    private final String name;
    private final KafkaConnectionHandler connection;
    
    public KafkaServiceEventDispatcher(final String name, final Set<ServicePathHolder> paths, final KafkaConnectionHandler connection) {
        this.name = name;
        this.connection = connection;
        connection.register(paths.stream().map(p -> KafkaUtil.resolveTopicName(name, p)).collect(Collectors.toSet()));
    }
    
    @SuppressWarnings({ "squid:S1181", "squid:S1193" })
    @Override
    public Async<EventAckEnvelope> dispatch(final EventEnvelope eventEnvelope) {
        final String topicName = KafkaUtil.resolveTopicName(name, eventEnvelope.getPathList());
        
        try {
            return awaitExceptions(relay(connection.publish(topicName, eventEnvelope.getPartitionId(), eventEnvelope.toByteArray())))
                .map(rm -> EventAckEnvelope.newBuilder()
                    .setCorrelationId(eventEnvelope.getCorrelationId())
                    .setCallRef(eventEnvelope.getCallRef())
                    .setStatusCode(ResponseStatusCode.OK)
                    .build());
        } catch (final Throwable t) {
            LOG.error("Received error message for tenant [{}] event [{}:{}]",
                eventEnvelope.getTenantId(),
                eventEnvelope.getCorrelationId(),
                eventEnvelope.getCallRef(),
                t);
            return result(EventAckEnvelope.newBuilder()
                .setCorrelationId(eventEnvelope.getCorrelationId())
                .setCallRef(eventEnvelope.getCallRef())
                .setStatusCode(t instanceof TimeoutException ? ResponseStatusCode.SERVER_TIMEOUT : ResponseStatusCode.SERVER_ERROR)
                .setStatusMessage(t.getMessage())
                .build());
        }
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + " for " + name;
    }
    
}
