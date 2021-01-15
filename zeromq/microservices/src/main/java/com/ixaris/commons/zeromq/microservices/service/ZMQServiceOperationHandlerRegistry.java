package com.ixaris.commons.zeromq.microservices.service;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.CompletableFutureUtil.reject;
import static com.ixaris.commons.microservices.lib.service.support.ServiceLoggingFilterFactory.OPERATION_CHANNEL;
import static com.ixaris.commons.misc.lib.object.Tuple.tuple;

import java.util.Collections;
import java.util.HashMap;
import java.util.Map;
import java.util.Queue;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentLinkedQueue;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Context;
import org.zeromq.ZMQ.Poller;
import org.zeromq.ZMQ.Socket;
import org.zeromq.ZMQException;

import com.google.protobuf.InvalidProtocolBufferException;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.AsyncTrace;
import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.microservices.lib.common.ServiceId;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;
import com.ixaris.commons.microservices.lib.service.discovery.ServiceRegistry;
import com.ixaris.commons.misc.lib.object.Tuple2;
import com.ixaris.commons.misc.lib.object.Tuple3;
import com.ixaris.commons.zeromq.microservices.ZMQGlobal;
import com.ixaris.commons.zeromq.microservices.ZMQGlobal.ZMQAfterContextShutdownThread;
import com.ixaris.commons.zeromq.microservices.common.ZMQUrlHelper;
import com.ixaris.commons.zeromq.microservices.common.ZMQWakeThread;

import zmq.ZError;

public class ZMQServiceOperationHandlerRegistry extends ZMQAfterContextShutdownThread {
    
    private static final class PendingResponse {
        
        private final ResponseEnvelope response;
        private final byte[] address;
        
        private PendingResponse(final ResponseEnvelope response, final byte[] address) {
            this.response = response;
            this.address = address;
        }
        
    }
    
    private static final Logger LOG = LoggerFactory.getLogger(ZMQServiceOperationHandlerRegistry.class);
    
    private static final int MAX_NO_OF_BIND_TRIES = 50;
    
    private final ScheduledExecutorService executor;
    private final ZMQUrlHelper urlHelper;
    private final ServiceRegistry serviceRegistry;
    
    private final ZMQWakeThread wakeThread;
    private final Map<ServiceId, ZMQServiceOperationHandler> handlers = new HashMap<>();
    private final Queue<Tuple3<ZMQServiceOperationHandler, CompletableFuture<ZMQServiceOperationHandler>, AsyncTrace>> handlerRegistrations = new ConcurrentLinkedQueue<>();
    private final Queue<Tuple3<ServiceId, CompletableFuture<Void>, AsyncTrace>> handlerDeregistrations = new ConcurrentLinkedQueue<>();
    private final Queue<PendingResponse> pendingResponsesQueue = new ConcurrentLinkedQueue<>();
    
    private String operationUrl;
    private ZMQ.Socket router;
    
    public ZMQServiceOperationHandlerRegistry(final ScheduledExecutorService executor, final ZMQUrlHelper urlHelper, final ServiceRegistry serviceRegistry) {
        super(ZMQServiceOperationHandlerRegistry.class.getSimpleName());
        
        this.executor = executor;
        this.urlHelper = urlHelper;
        this.serviceRegistry = serviceRegistry;
        this.wakeThread = new ZMQWakeThread(getName() + "-WAKE");
        
        start();
    }
    
    public ZMQServiceOperationHandler register(final String serviceName,
                                               final String serviceKey,
                                               final AsyncFilterNext<RequestEnvelope, ResponseEnvelope> filterChain) {
        final ZMQServiceOperationHandler handler = new ZMQServiceOperationHandler(new ServiceId(serviceName, serviceKey), filterChain);
        final CompletableFuture<ZMQServiceOperationHandler> future = new CompletableFuture<>();
        final AsyncTrace trace = AsyncTrace.get();
        handlerRegistrations.offer(tuple(handler, future, trace));
        wakeThread.offerWake();
        
        try {
            return future.get(ZMQGlobal.REGISTRATION_TIMEOUT, TimeUnit.SECONDS);
        } catch (final InterruptedException | TimeoutException | ExecutionException e) {
            throw new IllegalStateException("Error while waiting for registration confirmation", e);
        }
    }
    
    public void deregister(final String serviceName, final String serviceKey) {
        final CompletableFuture<Void> future = new CompletableFuture<>();
        final AsyncTrace trace = AsyncTrace.get();
        handlerDeregistrations.offer(tuple(new ServiceId(serviceName, serviceKey), future, trace));
        wakeThread.offerWake();
        
        try {
            future.get(ZMQGlobal.REGISTRATION_TIMEOUT, TimeUnit.SECONDS);
        } catch (final InterruptedException | TimeoutException | ExecutionException e) {
            throw new IllegalStateException("Error while waiting for registration confirmation", e);
        }
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
            this.operationUrl = socketAndOperationUrl.get2();
            
            poller.register(router, ZMQ.Poller.POLLIN);
            
            wakeThread.start();
            
            while (!interrupted()) {
                
                // if poll interrupted returns value less than 0
                if (poller.poll() < 0) {
                    break;
                }
                
                handleRequestsResponsesAndRegistrations(poller);
                
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
            handlers.clear();
        }
    }
    
    private void handleRequestsResponsesAndRegistrations(final Poller poller) {
        try {
            sendPendingResponses();
            
            handlePendingRequests(poller);
            
            handlePendingRegistrations();
            
            handlePendingDeregistrations();
        } catch (final RuntimeException e) {
            LOG.error("Error in " + getClass().getName(), e);
        }
    }
    
    private Tuple2<ZMQ.Socket, String> getRouterSocket() {
        int tries = 0;
        while (true) {
            final ZMQ.Socket socket = ZMQGlobal.getContext().socket(ZMQ.ROUTER);
            try {
                final String operationUrl = urlHelper.generateServiceOperationUrl();
                LOG.debug("Trying to bind with [{}]", operationUrl);
                socket.bind(operationUrl);
                LOG.info("Bound with [{}]", operationUrl);
                serviceRegistry.mergeAttributes(Collections.singletonMap(ZMQGlobal.OPERATION_URL_KEY, operationUrl));
                return tuple(socket, operationUrl);
            } catch (final ZMQException e) {
                socket.close();
                if (e.getErrorCode() != ZError.EADDRINUSE) {
                    // Only keep retrying if registration failed due to address already in use error
                    throw e;
                } else if (++tries == MAX_NO_OF_BIND_TRIES) {
                    throw new IllegalStateException("Did not manage to find port after " + MAX_NO_OF_BIND_TRIES + " tries");
                }
            }
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
    
    private void handlePendingRequests(final Poller poller) {
        if (poller.pollin(1)) {
            try {
                final byte[] address = doUninterruptableWork(true, () -> router.recv()); // get address
                final byte[] payload = doUninterruptableWork(true, () -> router.recv()); // get request
                
                if (payload != null) {
                    try {
                        final RequestEnvelope requestEnvelope = RequestEnvelope.parseFrom(payload);
                        final ZMQServiceOperationHandler handler = handlers.get(new ServiceId(requestEnvelope));
                        if (handler != null) {
                            executor.execute(() -> handler.handle(requestEnvelope, address));
                        } // if there is no handler, ignore and let it timeout
                    } catch (final InvalidProtocolBufferException e) {
                        // if payload is invalid, ignore and let it timeout
                        LOG.error("Parsing error for ResponseEnvelope payload", e);
                    }
                }
            } catch (final RuntimeException e) {
                LOG.error("Error receiving from [{}]", operationUrl, e);
            }
        }
    }
    
    private void handlePendingRegistrations() {
        Tuple3<ZMQServiceOperationHandler, CompletableFuture<ZMQServiceOperationHandler>, AsyncTrace> registration;
        while ((registration = handlerRegistrations.poll()) != null) {
            final ZMQServiceOperationHandler handler = registration.get1();
            final CompletableFuture<ZMQServiceOperationHandler> future = registration.get2();
            AsyncTrace.exec(registration.get3(), () -> {
                if (handlers.putIfAbsent(handler.id, handler) == null) {
                    try {
                        future.complete(handler); // Confirm registration.
                    } catch (final RuntimeException e) {
                        LOG.error("Registration on service discovery failed for {}", handler.id, e);
                        reject(future, e); // reject registration
                    }
                } else {
                    // reject registration
                    reject(future, new IllegalStateException(String.format("Already registered operation handler for %s", handler.id)));
                }
            });
        }
    }
    
    private void handlePendingDeregistrations() {
        Tuple3<ServiceId, CompletableFuture<Void>, AsyncTrace> deregistration;
        while ((deregistration = handlerDeregistrations.poll()) != null) {
            final ServiceId id = deregistration.get1();
            final CompletableFuture<Void> future = deregistration.get2();
            AsyncTrace.exec(deregistration.get3(), () -> {
                if (handlers.remove(id) != null) {
                    future.complete(null); // NOSONAR Confirm deregistration. It is ok to complete this Void with null
                } else {
                    // reject deregistration
                    reject(future, new IllegalStateException(String.format("No operation handler registered for %s", id)));
                }
            });
        }
    }
    
    private final class ZMQServiceOperationHandler {
        
        private final ServiceId id;
        private final AsyncFilterNext<RequestEnvelope, ResponseEnvelope> filterChain;
        
        private ZMQServiceOperationHandler(final ServiceId id, final AsyncFilterNext<RequestEnvelope, ResponseEnvelope> filterChain) {
            this.id = id;
            this.filterChain = filterChain;
        }
        
        private Async<Void> handle(final RequestEnvelope requestEnvelope, final byte[] address) {
            final ResponseEnvelope responseEnvelope = await(OPERATION_CHANNEL.exec("ZMQ", () -> filterChain.next(requestEnvelope)));
            pendingResponsesQueue.offer(new PendingResponse(responseEnvelope, address));
            wakeThread.offerWake();
            return result();
        }
        
        @Override
        public String toString() {
            return getClass().getSimpleName() + " for " + id;
        }
        
    }
    
}
