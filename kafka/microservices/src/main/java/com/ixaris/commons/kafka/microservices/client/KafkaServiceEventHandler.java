package com.ixaris.commons.kafka.microservices.client;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.microservices.lib.client.support.ServiceClientLoggingFilterFactory.EVENT_CHANNEL;

import java.util.Map;
import java.util.Objects;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ScheduledExecutorService;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.InvalidProtocolBufferException;

import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.kafka.multitenancy.KafkaConnectionHandler;
import com.ixaris.commons.kafka.multitenancy.KafkaMultiTenantConnection;
import com.ixaris.commons.kafka.multitenancy.KafkaUtil;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventAckEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;

/**
 * The Kafka Service Operation dispatcher represents Kafka Consumers that receives consumes messages from a Kafka topic
 *
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public final class KafkaServiceEventHandler {
    
    private static final Logger LOG = LoggerFactory.getLogger(KafkaServiceEventHandler.class);
    
    private final String name;
    private final String subscriberName;
    private final ServicePathHolder path;
    private final AsyncFilterNext<EventEnvelope, EventAckEnvelope> filterChain;
    private final KafkaConnectionHandler kafkaMultiTenantConnection;
    
    /**
     * This failedMessageTimer is used to allow retrying with exponential backoff for failed handling of kafka messages. The failedMessageTimer
     * is marked as daemon since on JVM restart, any failed messages (and not committed to kafka) will be automatically retried.
     */
    private final Map<String, Integer> currentFailedMessageRetryCount = new ConcurrentHashMap<>();
    
    public KafkaServiceEventHandler(final String name,
                                    final String subscriberName,
                                    final ServicePathHolder path,
                                    final AsyncFilterNext<EventEnvelope, EventAckEnvelope> filterChain,
                                    final KafkaConnectionHandler kafkaMultiTenantConnection) {
        this.name = name;
        this.subscriberName = subscriberName;
        this.path = path;
        this.filterChain = filterChain;
        this.kafkaMultiTenantConnection = kafkaMultiTenantConnection;
    }
    
    public synchronized void subscribe() {
        final String qualifiedEventName = KafkaUtil.resolveTopicName(name, path);
        kafkaMultiTenantConnection.subscribe(subscriberName, qualifiedEventName, (__, msg) -> {
            try {
                final EventEnvelope eventEnvelope = EventEnvelope.parseFrom(msg);
                final EventAckEnvelope eventAckEnvelope = await(EVENT_CHANNEL.exec("kafka", () -> filterChain.next(eventEnvelope)));
                
                if (Objects.equals(ResponseStatusCode.OK, eventAckEnvelope.getStatusCode())) {
                    return result();
                } else {
                    final EventAckEnvelope retryEventAckEnvelope = await(EVENT_CHANNEL.exec("kafka", () -> filterChain.next(eventEnvelope)));
                    if (Objects.equals(ResponseStatusCode.OK, retryEventAckEnvelope.getStatusCode())) {
                        return result();
                    } else {
                        throw new IllegalStateException(String.format(
                            "Failed while handling event %s:%s",
                            retryEventAckEnvelope.getStatusCode(),
                            retryEventAckEnvelope.getStatusMessage()));
                    }
                }
            } catch (final InvalidProtocolBufferException e) {
                LOG.error("Error while parsing event", e);
                throw new IllegalStateException("Error while parsing event", e);
            }
        });
        LOG.info("Subscribed to event [{}] for all tenants", qualifiedEventName);
    }
    
    public synchronized void unsubscribe() {
        final String qualifiedEventName = KafkaUtil.resolveTopicName(name, path);
        kafkaMultiTenantConnection.unsubscribe(subscriberName, qualifiedEventName);
        
        LOG.info("Unsubscribed from event [{}] for all tenants", qualifiedEventName);
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + " for " + name;
    }
    
}
