package com.ixaris.commons.microservices.lib.service.support;

import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.AsyncTrace.NEW_RELIC;
import static com.ixaris.commons.async.lib.logging.AsyncLogging.ASYNC_MDC;
import static com.ixaris.commons.async.lib.logging.AsyncLogging.CORRELATION;
import static com.ixaris.commons.microservices.lib.common.ServiceLoggingHelper.KEY_PATH;
import static com.ixaris.commons.microservices.lib.common.ServiceLoggingHelper.KEY_SERVICE_KEY;
import static com.ixaris.commons.microservices.lib.common.ServiceLoggingHelper.KEY_SERVICE_NAME;
import static com.ixaris.commons.microservices.lib.service.ServiceOperation.extractCorrelation;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

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
import com.ixaris.commons.microservices.lib.client.ServiceStubEvent;
import com.ixaris.commons.microservices.lib.common.ServiceEventFilter;
import com.ixaris.commons.microservices.lib.common.ServiceHandlerStrategy;
import com.ixaris.commons.microservices.lib.common.ServiceOperationFilter;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.common.exception.ServerErrorException;
import com.ixaris.commons.microservices.lib.common.tracing.TracingServiceOperationFilter;
import com.ixaris.commons.microservices.lib.local.LocalOperations;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventAckEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;
import com.ixaris.commons.microservices.lib.service.ServiceProviderSkeleton;
import com.ixaris.commons.microservices.lib.service.ServiceSkeleton;
import com.ixaris.commons.microservices.lib.service.ServiceSkeletonOperation;
import com.ixaris.commons.microservices.lib.service.discovery.ServiceRegistry;
import com.ixaris.commons.microservices.lib.service.proxy.ServiceSkeletonProxy;
import com.ixaris.commons.misc.lib.object.Ordered;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;

public abstract class ServiceSupport {
    
    public static final String NO_KEY = "";
    
    private static final Logger LOG = LoggerFactory.getLogger(ServiceSupport.class);
    
    private final MultiTenancy multiTenancy;
    private final ServiceRegistry serviceRegistry;
    private final ServiceSecurityChecker serviceSecurityChecker;
    private final ServiceAsyncInterceptor serviceAsyncInterceptor;
    private final ServiceHandlerStrategy serviceHandlerStrategy;
    private final Set<? extends ServiceExceptionTranslator<?>> serviceExceptionTranslators;
    private final ServiceKeys serviceKeys;
    private final List<? extends ServiceFilterFactory> serviceFilterFactories;
    private final LocalOperations localOperations;
    
    private final Map<String, ServiceSkeletonProxy<?>> proxies = new HashMap<>();
    
    public ServiceSupport(final MultiTenancy multiTenancy,
                          final ServiceRegistry serviceRegistry,
                          final ServiceSecurityChecker serviceSecurityChecker,
                          final ServiceAsyncInterceptor serviceAsyncInterceptor,
                          final ServiceHandlerStrategy serviceHandlerStrategy,
                          final Set<? extends ServiceExceptionTranslator<?>> serviceExceptionTranslators,
                          final Set<? extends ServiceFilterFactory> serviceFilterFactories,
                          final ServiceKeys serviceKeys,
                          final LocalOperations localOperations) {
        this.multiTenancy = multiTenancy;
        this.serviceRegistry = serviceRegistry;
        this.serviceSecurityChecker = serviceSecurityChecker;
        this.serviceAsyncInterceptor = serviceAsyncInterceptor;
        this.serviceHandlerStrategy = serviceHandlerStrategy;
        this.serviceExceptionTranslators = serviceExceptionTranslators;
        this.serviceFilterFactories = new ArrayList<>(serviceFilterFactories);
        this.serviceFilterFactories.sort(Ordered.COMPARATOR);
        this.serviceKeys = serviceKeys;
        this.localOperations = localOperations;
    }
    
    /**
     * Create a service operation handler for the specified service name and key
     *
     * @param serviceName The service name
     * @param serviceKey The service key
     */
    protected abstract void createOperationHandler(final String serviceName,
                                                   final String serviceKey,
                                                   final AsyncFilterNext<RequestEnvelope, ResponseEnvelope> filterChain);
    
    /**
     * Destroy the service operation handler for the specified service name and key
     *
     * @param serviceName The service name
     * @param serviceKey The service key
     */
    protected abstract void destroyOperationHandler(final String serviceName, final String serviceKey);
    
    /**
     * Create a service event dispatcher for the specified service name and key
     *
     * @param serviceName The service name
     * @param serviceKey The service key
     * @return The service event dispatcher implementation for the service
     */
    protected abstract ServiceEventDispatcher createEventDispatcher(final String serviceName, final String serviceKey, final Set<ServicePathHolder> paths);
    
    /**
     * Destroy the service event dispatcher for the specified service name and key
     *
     * @param serviceName The service name
     * @param serviceKey The service key
     */
    protected abstract void destroyEventDispatcher(final String serviceName, final String serviceKey);
    
    @SuppressWarnings("unchecked")
    public final <T extends ServiceSkeleton> ServiceSkeletonProxy<T> getOrCreate(final Class<T> serviceSkeletonType) {
        synchronized (proxies) {
            final String name = ServiceSkeleton.extractServiceName(serviceSkeletonType);
            return (ServiceSkeletonProxy<T>) proxies.computeIfAbsent(name, n -> new ServiceSkeletonProxy<>(multiTenancy,
                serviceSkeletonType,
                serviceSecurityChecker,
                serviceAsyncInterceptor,
                serviceHandlerStrategy,
                serviceExceptionTranslators));
        }
    }
    
    public final ServiceSkeletonProxy<?> get(final String name) {
        synchronized (proxies) {
            return proxies.get(name);
        }
    }
    
    public final void init(final ServiceSkeleton serviceSkeleton) {
        final Set<Class<? extends ServiceSkeleton>> serviceSkeletonTypes = ServiceSkeleton
            .determineServiceSkeletonTypes(serviceSkeleton);
        for (final Class<? extends ServiceSkeleton> serviceSkeletonType : serviceSkeletonTypes) {
            final ServiceSkeletonProxy<?> proxy = getOrCreate(serviceSkeletonType);
            if (ServiceProviderSkeleton.class.isAssignableFrom(serviceSkeletonType)) {
                for (final String key : serviceKeys.get(serviceSkeletonType, serviceSkeleton)) {
                    internalInit(serviceSkeleton, serviceSkeletonType, proxy, key);
                }
            } else {
                internalInit(serviceSkeleton, serviceSkeletonType, proxy, NO_KEY);
            }
        }
    }
    
    public final void destroy(final ServiceSkeleton serviceSkeleton) {
        final Set<Class<? extends ServiceSkeleton>> serviceSkeletonTypes = ServiceSkeleton
            .determineServiceSkeletonTypes(serviceSkeleton);
        for (final Class<? extends ServiceSkeleton> serviceSkeletonType : serviceSkeletonTypes) {
            final ServiceSkeletonProxy<?> proxy = getOrCreate(serviceSkeletonType);
            if (ServiceProviderSkeleton.class.isAssignableFrom(serviceSkeletonType)) {
                for (final String key : serviceKeys.get(serviceSkeletonType, serviceSkeleton)) {
                    internalDestroy(serviceSkeleton, serviceSkeletonType, proxy, key);
                }
            } else {
                internalDestroy(serviceSkeleton, serviceSkeletonType, proxy, NO_KEY);
            }
            
            synchronized (proxies) {
                final String serviceName = ServiceSkeleton.extractServiceName(serviceSkeletonType);
                proxies.remove(serviceName);
            }
        }
    }
    
    private void internalInit(final ServiceSkeleton serviceSkeleton,
                              final Class<? extends ServiceSkeleton> serviceSkeletonType,
                              final ServiceSkeletonProxy<?> skeletonProxy,
                              final String key) {
        final String name = ServiceSkeleton.extractServiceName(serviceSkeletonType);
        
        final ImmutableList.Builder<ServiceOperationFilter> oBuilder = new ImmutableList.Builder<>();
        // add first filter to set logging context, before anything else
        // TODO make this a filter factory
        oBuilder.add(this::handle);
        final ImmutableList.Builder<ServiceEventFilter> eBuilder = new ImmutableList.Builder<>();
        for (final ServiceFilterFactory processorFactory : serviceFilterFactories) {
            final ServiceOperationFilter oFilter = processorFactory.createOperationFilter(name, key);
            if (oFilter != null) {
                oBuilder.add(oFilter);
            }
            final ServiceEventFilter eFilter = processorFactory.createEventFilter(name, key);
            if (eFilter != null) {
                eBuilder.add(eFilter);
            }
        }
        
        oBuilder.add(new TracingServiceOperationFilter());
        
        final ServiceEventDispatcher dispatcher = createEventDispatcher(name, key, skeletonProxy.getPublisherPaths());
        
        final AsyncFilterNext<EventEnvelope, EventAckEnvelope> efilterChain = new AsyncFilterChain<>(eBuilder.build())
            .with(in -> dispatch(in, dispatcher), (in, t) -> {
                LOG.error("Unexpected exception while dispatching event", t);
                return result(ServiceStubEvent.wrapError(in, new ServerErrorException(t)));
            });
        
        final ServiceOperationProcessor processor = initServiceSkeletonProxy(skeletonProxy, serviceSkeleton, key, efilterChain);
        
        final AsyncFilterNext<RequestEnvelope, ResponseEnvelope> oFilterChain = new AsyncFilterChain<>(oBuilder.build())
            .with(processor::process, (in, t) -> {
                LOG.error("Unexpected exception while handling operation", t);
                return result(ServiceSkeletonOperation.wrapError(in, new ServerErrorException(t)));
            });
        
        serviceRegistry.register(name, key);
        
        if (localOperations != null) {
            localOperations.registerOperationHandler(name, key, oFilterChain);
        }
        createOperationHandler(name, key, oFilterChain);
        
        LOG.info("Initialised service skeleton [{}] with serviceKey [{}]", serviceSkeletonType, key);
    }
    
    private Async<EventAckEnvelope> dispatch(final EventEnvelope in, final ServiceEventDispatcher dispatcher) {
        Segment segment = NewRelic.getAgent().getTransaction().startSegment("dispatch_event");
        
        final Async<EventAckEnvelope> f = dispatcher.dispatch(in);
        
        CompletionStageUtil.whenDone(f, (r, t) -> segment.end());
        
        return f;
    }
    
    @Trace(dispatcher = true)
    private Async<ResponseEnvelope> handle(final RequestEnvelope in,
                                           final AsyncFilterNext<RequestEnvelope, ResponseEnvelope> next) {
        final Token token = NewRelic.getAgent().getTransaction().getToken();
        
        final Async<ResponseEnvelope> f = AsyncLocal
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
                    String.join("/", in.getPathList()) + "/" + in.getMethod()))
            .exec(() -> next.next(in));
        
        CompletionStageUtil.whenDone(f, (r, t) -> token.expire());
        return f;
    }
    
    private void internalDestroy(final ServiceSkeleton serviceSkeleton,
                                 final Class<? extends ServiceSkeleton> serviceSkeletonType,
                                 final ServiceSkeletonProxy<?> skeletonProxy,
                                 final String serviceKey) {
        final String serviceName = ServiceSkeleton.extractServiceName(serviceSkeletonType);
        
        destroyOperationHandler(serviceName, serviceKey);
        destroyEventDispatcher(serviceName, serviceKey);
        
        if (localOperations != null) {
            localOperations.deregisterOperationHandler(serviceName, serviceKey);
        }
        
        destroyServiceSkeletonProxy(skeletonProxy, serviceSkeleton, serviceKey);
        
        serviceRegistry.deregister(serviceName, serviceKey);
        
        LOG.info("Destroyed service skeleton [{}] with serviceKey [{}]", serviceSkeletonType, serviceKey);
    }
    
    @SuppressWarnings("unchecked")
    private <T extends ServiceSkeleton> ServiceOperationProcessor initServiceSkeletonProxy(final ServiceSkeletonProxy<T> proxy,
                                                                                           final ServiceSkeleton serviceSkeleton,
                                                                                           final String serviceKey,
                                                                                           final AsyncFilterNext<EventEnvelope, EventAckEnvelope> filterChain) {
        return proxy.init((T) serviceSkeleton, serviceKey, filterChain);
    }
    
    @SuppressWarnings("unchecked")
    private <T extends ServiceSkeleton> void destroyServiceSkeletonProxy(final ServiceSkeletonProxy<T> proxy,
                                                                         final ServiceSkeleton serviceSkeleton,
                                                                         final String serviceKey) {
        proxy.destroy((T) serviceSkeleton, serviceKey);
    }
    
}
