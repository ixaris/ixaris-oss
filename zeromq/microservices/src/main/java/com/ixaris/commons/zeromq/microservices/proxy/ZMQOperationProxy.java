package com.ixaris.commons.zeromq.microservices.proxy;

import java.util.Collections;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQException;

import com.google.protobuf.InvalidProtocolBufferException;

import com.ixaris.commons.microservices.lib.client.dynamic.DynamicServiceFactory;
import com.ixaris.commons.microservices.lib.client.support.ServiceClientSupport;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;
import com.ixaris.commons.misc.lib.object.Tuple2;
import com.ixaris.commons.zeromq.microservices.ZMQGlobal;
import com.ixaris.commons.zeromq.microservices.ZMQGlobal.ZMQAfterContextShutdownThread;
import com.ixaris.commons.zeromq.microservices.common.ZMQUrlHelper;
import com.ixaris.commons.zeromq.microservices.common.ZMQWakeThread;

public class ZMQOperationProxy extends ZMQAfterContextShutdownThread {
    
    private static final Logger LOG = LoggerFactory.getLogger(ZMQOperationProxy.class);
    
    private static final class PendingResponse {
        
        private final ResponseEnvelope response;
        private final byte[] address;
        
        private PendingResponse(final ResponseEnvelope response, final byte[] address) {
            this.response = response;
            this.address = address;
        }
        
    }
    
    private final ZMQUrlHelper urlHelper;
    private final Consumer<Map<String, String>> nodeAttributesConsumer;
    private final DynamicServiceFactory serviceFactory;
    private final ZMQWakeThread wakeThread;
    private final Queue<PendingResponse> pendingResponsesQueue = new ConcurrentLinkedQueue<>();
    
    private String operationUrl;
    private ZMQ.Socket router;
    
    public ZMQOperationProxy(final ZMQUrlHelper urlHelper, final ServiceClientSupport serviceClientSupport, final Consumer<Map<String, String>> nodeAttributesConsumer) {
        super(ZMQOperationProxy.class.getSimpleName());
        this.urlHelper = urlHelper;
        this.nodeAttributesConsumer = nodeAttributesConsumer;
        serviceFactory = new DynamicServiceFactory(serviceClientSupport, true);
        wakeThread = new ZMQWakeThread(getName() + "-WAKE");
        
        start();
    }
    
    public String getOperationUrl() {
        return operationUrl;
    }
    
    @Override
    public void run() {
        // context is not within try-with-resources since it is managed by ZMQ Global with shutdown functionality
        final Context context = ZMQGlobal.getContext();
        try (final ZMQ.Socket pull = context.socket(ZMQ.PULL)) {
            
            final ZMQ.Poller poller = context.poller(2);
            pull.bind("inproc://" + wakeThread.getName());
            poller.register(pull, ZMQ.Poller.POLLIN);
            
            final Tuple2<Socket, String> socketAndOperationUrl = getRouterSocket();
            this.router = socketAndOperationUrl.get1();
            operationUrl = socketAndOperationUrl.get2();
            poller.register(router, ZMQ.Poller.POLLIN);
            
            wakeThread.start();
            
            nodeAttributesConsumer.accept(Collections.singletonMap(ZMQGlobal.OPERATION_URL_KEY, operationUrl));
            
            LOG.info("Started ZeroMQ operation proxy bound to URL: {}", this.operationUrl);
            
            while (!interrupted()) {
                
                // if poll interrupted returns value less than 0
                if (poller.poll() < 0) {
                    break;
                }
                
                handleRequestsAndResponses(poller);
                
                if (poller.pollin(0)) {
                    pull.recv(); // consume all wait data
                }
            }
        } catch (final ZMQException e) {
            if (e.getErrorCode() != ZMQ.Error.ETERM.getCode()) {
                LOG.error("error in " + getClass().getName(), e);
                throw e;
            }
        } catch (final Exception e) {
            LOG.error("error in " + getClass().getName(), e);
            throw e;
        } finally {
            if (router != null) {
                try {
                    router.close();
                } catch (final Exception e) {
                    LOG.warn("Error closing handler router ZMQ socket", e);
                }
                router = null;
            }
        }
    }
    
    private void handleRequestsAndResponses(final ZMQ.Poller poller) {
        try {
            sendPendingResponses();
            
            handlePendingRequests(poller);
        } catch (final RuntimeException e) {
            LOG.error("Error in " + getClass().getName(), e);
        }
    }
    
    private Tuple2<ZMQ.Socket, String> getRouterSocket() {
        final ZMQ.Socket socket = ZMQGlobal.getContext().socket(ZMQ.ROUTER); // NOSONAR closeable handled by ZMQGlobal
        try {
            final String generatedOperationUrl = urlHelper.generateServiceOperationUrl();
            LOG.debug("Trying to bind with [{}]", generatedOperationUrl);
            socket.bind(generatedOperationUrl);
            LOG.info("Bound with [{}]", generatedOperationUrl);
            return new Tuple2<>(socket, generatedOperationUrl);
        } catch (final ZMQException e) {
            socket.close();
            throw e;
        }
    }
    
    private void sendPendingResponses() {
        PendingResponse pendingResponse;
        while ((pendingResponse = pendingResponsesQueue.poll()) != null) {
            final ResponseEnvelope responseEnvelope = pendingResponse.response;
            final byte[] address = pendingResponse.address;
            try {
                doUninterruptableWork(true,
                    () -> router.sendMore(address) && router.send(responseEnvelope.toByteArray()));
            } catch (final RuntimeException e) {
                LOG.error("Sending response [{}:{}] at [{}] failed",
                    responseEnvelope.getCorrelationId(),
                    responseEnvelope.getCallRef(),
                    operationUrl,
                    e);
            }
        }
    }
    
    private void handlePendingRequests(final ZMQ.Poller poller) {
        if (poller.pollin(1)) {
            try {
                final byte[] address = doUninterruptableWork(true, router::recv); // get address
                final byte[] payload = doUninterruptableWork(true, router::recv); // get request
                
                if (payload != null) {
                    try {
                        final RequestEnvelope requestEnvelope = RequestEnvelope.parseFrom(payload);
                        final String serviceName = requestEnvelope.getServiceName();
                        try {
                            serviceFactory
                                .getProxy(serviceName)
                                .map(proxy -> proxy
                                    .invoke(requestEnvelope)
                                    .map(responseEnvelope -> {
                                        queueResponse(responseEnvelope, address);
                                        return null;
                                    }));
                        } catch (final Exception e) {
                            LOG.error("Unable to forward request envelope from proxy to service [{}/{}] with method [{}]!",
                                serviceName,
                                requestEnvelope.getServiceKey(),
                                requestEnvelope.getMethod(),
                                e);
                            final ResponseEnvelope.Builder responseEnvelope = ResponseEnvelope.newBuilder()
                                .setCorrelationId(requestEnvelope.getCorrelationId())
                                .setCallRef(requestEnvelope.getCallRef())
                                .setStatusCode(ResponseStatusCode.SERVER_ERROR)
                                .setStatusMessage(e.getMessage());
                            if (requestEnvelope.getJsonPayload()) {
                                responseEnvelope.setJsonPayload(true);
                            }
                            queueResponse(responseEnvelope.build(), address);
                        }
                        
                    } catch (final InvalidProtocolBufferException e) {
                        // we consume this by logging, ignoring and letting it timeout
                        LOG.error("Parsing error for ResponseEnvelope payload", e);
                    }
                }
            } catch (final RuntimeException e) {
                LOG.error("Error handling proxy request", e);
            }
        }
    }
    
    private void queueResponse(final ResponseEnvelope responseEnvelope, final byte[] address) {
        pendingResponsesQueue.offer(new PendingResponse(responseEnvelope, address));
        wakeThread.offerWake();
    }
    
}
