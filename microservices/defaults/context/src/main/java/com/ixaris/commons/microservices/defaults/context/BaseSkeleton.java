package com.ixaris.commons.microservices.defaults.context;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.clustering.lib.service.ClusterRegistry.SHARD;
import static com.ixaris.commons.multitenancy.lib.data.DataUnit.DATA_UNIT;

import java.util.Set;
import java.util.function.Function;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.client.ServiceEventListener;
import com.ixaris.commons.microservices.lib.client.ServiceStub;
import com.ixaris.commons.microservices.lib.client.ServiceStubEvent;
import com.ixaris.commons.microservices.lib.client.support.ServiceEventClusterRouteHandler;
import com.ixaris.commons.microservices.lib.client.support.ServiceEventClusterRouteHandlerFactory;
import com.ixaris.commons.microservices.lib.common.ServiceEventHeader;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventAckEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;
import com.ixaris.commons.microservices.lib.service.ServiceProviderSkeleton;
import com.ixaris.commons.microservices.lib.service.ServiceSkeleton;
import com.ixaris.commons.microservices.lib.service.ServiceSkeletonOperation;
import com.ixaris.commons.microservices.lib.service.support.ServiceKeys;
import com.ixaris.commons.microservices.lib.service.support.ServiceOperationClusterRouteHandler;
import com.ixaris.commons.microservices.lib.service.support.ServiceOperationClusterRouteHandlerFactory;
import com.ixaris.commons.misc.lib.function.CallableThrows;

public abstract class BaseSkeleton implements ServiceSkeleton {
    
    protected final String name;
    private final ServiceOperationClusterRouteHandler operationRoute;
    private final ServiceEventClusterRouteHandlerFactory eventRouteFactory;
    
    public BaseSkeleton(final ServiceKeys serviceKeys, final ServiceOperationClusterRouteHandlerFactory operationRouteFactory) {
        this(serviceKeys, operationRouteFactory, null);
    }
    
    public BaseSkeleton(final ServiceKeys serviceKeys,
                        final ServiceOperationClusterRouteHandlerFactory operationRouteFactory,
                        final ServiceEventClusterRouteHandlerFactory eventRouteFactory) {
        final Class<? extends ServiceSkeleton> type = determineType();
        name = determineName(type, serviceKeys);
        operationRoute = operationRouteFactory.create(type, name);
        this.eventRouteFactory = eventRouteFactory;
    }
    
    public BaseSkeleton(final String name, final ServiceOperationClusterRouteHandlerFactory operationRouteFactory) {
        this(name, operationRouteFactory, null);
    }
    
    public BaseSkeleton(final String name,
                        final ServiceOperationClusterRouteHandlerFactory operationRouteFactory,
                        final ServiceEventClusterRouteHandlerFactory eventRouteFactory) {
        if (name == null) {
            throw new IllegalArgumentException("name is null");
        }
        this.name = name;
        this.operationRoute = operationRouteFactory.create(determineType(), name);
        this.eventRouteFactory = eventRouteFactory;
    }
    
    private Class<? extends ServiceSkeleton> determineType() {
        final Set<Class<? extends ServiceSkeleton>> types = ServiceSkeleton.determineServiceSkeletonTypes(this);
        if (types.isEmpty()) {
            throw new IllegalStateException("No service skeleton interface implemented");
        }
        if (types.size() > 1) {
            throw new IllegalStateException("Multiple service skeleton interfaces implemented");
        }
        return types.iterator().next();
    }
    
    private String determineName(final Class<? extends ServiceSkeleton> type, final ServiceKeys serviceKeys) {
        if (ServiceProviderSkeleton.class.isAssignableFrom(type)) {
            final Set<String> keys = serviceKeys.get(type, this);
            if (keys.size() == 1) {
                return keys.iterator().next();
            } else {
                throw new IllegalStateException("cannot determine data unit to use for " + this);
            }
        } else {
            return ServiceSkeleton.extractServiceName(type);
        }
    }
    
    /**
     * Default implementation assumes that the data unit name matches the service name for non-spis and the service key
     * for spis. Override in case of non default behaviour
     */
    @Override
    public <T, E extends Exception> T aroundAsync(final CallableThrows<T, E> callable) throws E {
        return DATA_UNIT.exec(name, callable);
    }
    
    @Override
    public final Async<ResponseEnvelope> handle(final ServiceSkeletonOperation operation) {
        if (SHARD.get() == null) {
            final Long shardKey = operation.getShardKey();
            if (shardKey != null) {
                return operationRoute.route(shardKey, operation.getRequestEnvelope());
            }
        }
        return operation.invokeOnResourceProxy();
    }
    
    public <C extends MessageLite, T extends MessageLite> ServiceEventListener<C, T> wrapListener(final ServiceEventListener<C, T> listener) {
        return new ServiceEventListener<C, T>() {
            
            @Override
            public Async<Void> onEvent(final ServiceEventHeader<C> header, final T event) {
                return listener.onEvent(header, event);
            }
            
            @Override
            public String getName() {
                return name + listener.getName();
            }
            
            @Override
            public Async<EventAckEnvelope> handle(final ServiceStubEvent event) {
                return listener.handle(event);
            }
            
            @Override
            public <X, E extends Exception> X aroundAsync(final CallableThrows<X, E> callable) throws E {
                return BaseSkeleton.this.aroundAsync(callable);
            }
            
        };
    }
    
    public <C extends MessageLite, T extends MessageLite> ServiceEventListener<C, T> wrapListener(final Class<? extends ServiceStub> stubType,
                                                                                                  final Function<ServiceStubEvent, Async<Long>> resolveShardKey,
                                                                                                  final ServiceEventListener<C, T> listener) {
        if (eventRouteFactory == null) {
            throw new IllegalStateException(
                "cannot create event route handler. Use BaseSkeleton constructor that takes ServiceEventClusterRouteHandlerFactory for "
                    + this);
        }
        
        return new ServiceEventListener<C, T>() {
            
            final ServiceEventClusterRouteHandler eventRoute;
            
            {
                eventRoute = eventRouteFactory.create(stubType, getName());
            }
            
            @Override
            public Async<Void> onEvent(final ServiceEventHeader<C> header, final T event) {
                return listener.onEvent(header, event);
            }
            
            @Override
            public String getName() {
                return name + listener.getName();
            }
            
            @Override
            public Async<EventAckEnvelope> handle(final ServiceStubEvent event) {
                if (SHARD.get() == null) {
                    final Long shardKey = await(resolveShardKey.apply(event));
                    if (shardKey != null) {
                        return eventRoute.route(shardKey, event.getEventEnvelope());
                    }
                }
                return listener.handle(event);
            }
            
            @Override
            public <X, E extends Exception> X aroundAsync(final CallableThrows<X, E> callable) throws E {
                return BaseSkeleton.this.aroundAsync(callable);
            }
            
        };
    }
    
}
