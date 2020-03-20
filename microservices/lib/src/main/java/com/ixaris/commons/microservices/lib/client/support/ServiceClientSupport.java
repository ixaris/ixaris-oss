package com.ixaris.commons.microservices.lib.client.support;

import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.AsyncTrace.NEW_RELIC;
import static com.ixaris.commons.async.lib.logging.AsyncLogging.ASYNC_MDC;
import static com.ixaris.commons.async.lib.logging.AsyncLogging.CORRELATION;
import static com.ixaris.commons.microservices.lib.client.ServiceEvent.extractCorrelation;
import static com.ixaris.commons.microservices.lib.common.ServiceLoggingHelper.KEY_PATH;
import static com.ixaris.commons.microservices.lib.common.ServiceLoggingHelper.KEY_SERVICE_KEY;
import static com.ixaris.commons.microservices.lib.common.ServiceLoggingHelper.KEY_SERVICE_NAME;
import static com.ixaris.commons.misc.lib.object.Tuple.tuple;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ScheduledExecutorService;
import java.util.function.Function;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableList;
import com.google.common.collect.ImmutableMap;
import com.newrelic.api.agent.NewRelic;
import com.newrelic.api.agent.Segment;
import com.newrelic.api.agent.Token;
import com.newrelic.api.agent.Trace;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.AsyncLocal;
import com.ixaris.commons.async.lib.CompletionStageUtil;
import com.ixaris.commons.async.lib.filter.AsyncFilterChain;
import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.microservices.lib.client.ServiceStub;
import com.ixaris.commons.microservices.lib.client.ServiceStubEvent;
import com.ixaris.commons.microservices.lib.client.proxy.ServiceStubProxy;
import com.ixaris.commons.microservices.lib.client.proxy.UntypedOperationInvoker;
import com.ixaris.commons.microservices.lib.client.proxy.UntypedOperationInvoker.KeyAvailableCondition;
import com.ixaris.commons.microservices.lib.common.ServiceEventFilter;
import com.ixaris.commons.microservices.lib.common.ServiceHandlerStrategy;
import com.ixaris.commons.microservices.lib.common.ServiceOperationFilter;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.common.exception.ServerErrorException;
import com.ixaris.commons.microservices.lib.local.LocalOperations;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventAckEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;
import com.ixaris.commons.microservices.lib.service.ServiceSkeletonOperation;
import com.ixaris.commons.microservices.lib.service.support.ServiceAsyncInterceptor;
import com.ixaris.commons.misc.lib.defaults.Defaults;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;
import com.ixaris.commons.misc.lib.object.Ordered;
import com.ixaris.commons.misc.lib.object.Tuple2;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;

public abstract class ServiceClientSupport {
    
    private static final Logger LOG = LoggerFactory.getLogger(ServiceClientSupport.class);
    
    public static long extractTimeout(final RequestEnvelope requestEnvelope) {
        return requestEnvelope.getTimeout() > 0 ? requestEnvelope.getTimeout() : Defaults.DEFAULT_TIMEOUT;
    }
    
    public static EventEnvelope resetCorrelation(final EventEnvelope eventEnvelope) {
        return eventEnvelope.toBuilder()
            .setCorrelationId(UniqueIdGenerator.generate())
            .setCallRef(0)
            .setParentRef(0)
            .build();
    }
    
    private final ScheduledExecutorService executor;
    private final MultiTenancy multiTenancy;
    private final int defaultTimeout;
    private final Function<String, ServiceKeysResolver> serviceKeysResolverFunction;
    private final ServiceAsyncInterceptor serviceAsyncInterceptor;
    private final ServiceHandlerStrategy serviceHandlerStrategy;
    private final List<? extends ServiceClientFilterFactory> filterFactories;
    private final LocalOperations localOperations;
    
    private final Map<String, ServiceStubProxy<?>> proxies = new HashMap<>();
    
    public ServiceClientSupport(final ScheduledExecutorService executor,
                                final MultiTenancy multiTenancy,
                                final int defaultTimeout,
                                final Function<String, ServiceKeysResolver> serviceKeysResolverFunction,
                                final ServiceAsyncInterceptor serviceAsyncInterceptor,
                                final ServiceHandlerStrategy serviceHandlerStrategy,
                                final Set<? extends ServiceClientFilterFactory> filterFactories,
                                final LocalOperations localOperations) {
        this.executor = executor;
        this.multiTenancy = multiTenancy;
        this.defaultTimeout = defaultTimeout;
        this.serviceKeysResolverFunction = serviceKeysResolverFunction;
        this.serviceAsyncInterceptor = serviceAsyncInterceptor;
        this.serviceHandlerStrategy = serviceHandlerStrategy;
        this.filterFactories = new ArrayList<>(filterFactories);
        this.filterFactories.sort(Ordered.COMPARATOR);
        this.localOperations = localOperations;
    }
    
    /**
     * Get a service operation dispatcher for the specified service name
     *
     * @param serviceName The service name
     * @return The service operation dispatcher implementation for the service
     */
    protected abstract ServiceOperationDispatcher createOperationDispatcher(String serviceName);
    
    /**
     * Get a service event handler for the specified service name
     *
     * @param serviceName The service name
     */
    protected abstract void createEventHandler(String serviceName,
                                               String subscriberName,
                                               ServicePathHolder path,
                                               AsyncFilterNext<EventEnvelope, EventAckEnvelope> filterChain);
    
    /**
     * Get a service event handler for the specified service name
     *
     * @param serviceName The service name
     */
    protected abstract void destroyEventHandler(String serviceName, String subscriberName, ServicePathHolder path);
    
    @SuppressWarnings("unchecked")
    public final <T extends ServiceStub> ServiceStubProxy<T> getOrCreate(final Class<T> serviceStubType) {
        final String name = ServiceStub.extractServiceName(serviceStubType);
        synchronized (proxies) {
            return (ServiceStubProxy<T>) proxies.computeIfAbsent(name, n -> {
                final Tuple2<AsyncFilterNext<RequestEnvelope, ResponseEnvelope>, KeyAvailableCondition> filter = createOperationFilterChain(n);
                return new ServiceStubProxy<>(executor,
                    multiTenancy,
                    serviceStubType,
                    serviceKeysResolverFunction.apply(n),
                    serviceAsyncInterceptor,
                    serviceHandlerStrategy,
                    (subscriberName, path, processor) -> createEventHandler(n, subscriberName, path, processor),
                    (subscriberName, path) -> destroyEventHandler(n, subscriberName, path),
                    filter.get1(),
                    filter.get2(),
                    defaultTimeout);
            });
        }
    }
    
    public final ServiceStubProxy<?> get(final String name) {
        synchronized (proxies) {
            return proxies.get(name);
        }
    }
    
    /**
     * Assuming this method wil be called after initialisation, so any available service will have been initialised
     */
    public final UntypedOperationInvoker getOperationInvoker(final String serviceName) {
        final Optional<? extends ServiceStubProxy<?>> proxy;
        synchronized (proxies) {
            proxy = Optional.ofNullable(proxies.get(serviceName));
        }
        return proxy
            .map(ServiceStubProxy::getOperationInvoker)
            .orElseGet(() -> {
                final Tuple2<AsyncFilterNext<RequestEnvelope, ResponseEnvelope>, KeyAvailableCondition> filter = createOperationFilterChain(serviceName);
                return new UntypedOperationInvoker(filter.get1(), filter.get2());
            });
    }
    
    public final int getDefaultTimeout() {
        return defaultTimeout;
    }
    
    private Tuple2<AsyncFilterNext<RequestEnvelope, ResponseEnvelope>, KeyAvailableCondition> createOperationFilterChain(final String name) {
        final ImmutableList.Builder<ServiceOperationFilter> builder = new ImmutableList.Builder<>();
        for (final ServiceClientFilterFactory processorFactory : filterFactories) {
            final ServiceOperationFilter filter = processorFactory.createOperationFilter(name);
            if (filter != null) {
                builder.add(filter);
            }
        }
        
        final ServiceOperationDispatcher dispatcher = (localOperations != null)
            ? new MultiplexingServiceOperationDispatcher(localOperations.getOperationDispatcher(name),
                createOperationDispatcher(name))
            : createOperationDispatcher(name);
        
        return tuple(new AsyncFilterChain<>(builder.build()).with(in -> dispatch(in, dispatcher), (in, t) -> {
            LOG.error("Unexpected exception while dispatching operation", t);
            return result(ServiceSkeletonOperation.wrapError(in, new ServerErrorException(t)));
        }),
            dispatcher::isKeyAvailable);
    }
    
    private Async<ResponseEnvelope> dispatch(final RequestEnvelope in, final ServiceOperationDispatcher dispatcher) {
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("dispatch_request");
        
        final Async<ResponseEnvelope> f = dispatcher.dispatch(in);
        
        CompletionStageUtil.whenDone(f, (r, t) -> segment.end());
        
        return f;
    }
    
    private void createEventHandler(final String name,
                                    final String subscriberName,
                                    final ServicePathHolder path,
                                    final ServiceEventProcessor processor) {
        final ImmutableList.Builder<ServiceEventFilter> builder = new ImmutableList.Builder<>();
        // add first filter to set logging context, before anything else
        builder.add(this::handle);
        for (final ServiceClientFilterFactory processorFactory : filterFactories) {
            final ServiceEventFilter filter = processorFactory.createEventFilter(name);
            if (filter != null) {
                builder.add(filter);
            }
        }
        
        final AsyncFilterNext<EventEnvelope, EventAckEnvelope> filterChain = new AsyncFilterChain<>(builder.build())
            .with(processor::process, (in, t) -> {
                LOG.error("Unexpected exception while handling event", t);
                return result(ServiceStubEvent.wrapError(in, new ServerErrorException(t)));
            });
        
        createEventHandler(name, subscriberName, path, filterChain);
    }
    
    @Trace(dispatcher = true)
    private Async<EventAckEnvelope> handle(final EventEnvelope in,
                                           final AsyncFilterNext<EventEnvelope, EventAckEnvelope> next) {
        final Token token = NewRelic.getAgent().getTransaction().getToken();
        
        final EventEnvelope resetIn = resetCorrelation(in);
        final Async<EventAckEnvelope> f = AsyncLocal
            .with(NEW_RELIC, token)
            .with(CORRELATION, extractCorrelation(in))
            .with(TENANT, in.getTenantId())
            .with(ASYNC_MDC,
                ImmutableMap.of(
                    KEY_SERVICE_NAME,
                    in.getServiceName(),
                    KEY_SERVICE_KEY,
                    in.getServiceKey(),
                    KEY_PATH,
                    String.join("/", in.getPathList()) + "/watch"))
            .exec(() -> next.next(resetIn));
        
        CompletionStageUtil.whenDone(f, (r, t) -> token.expire());
        return f;
    }
    
}
