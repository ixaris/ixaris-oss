package com.ixaris.commons.microservices.lib.client.proxy;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.ScheduledExecutorService;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.collections.lib.GuavaCollections;
import com.ixaris.commons.microservices.lib.client.ServiceEventListener;
import com.ixaris.commons.microservices.lib.client.ServiceEventSubscription;
import com.ixaris.commons.microservices.lib.client.ServiceProviderStub;
import com.ixaris.commons.microservices.lib.client.ServiceStub;
import com.ixaris.commons.microservices.lib.client.proxy.UntypedOperationInvoker.KeyAvailableCondition;
import com.ixaris.commons.microservices.lib.client.support.ServiceEventProcessor;
import com.ixaris.commons.microservices.lib.client.support.ServiceKeysResolver;
import com.ixaris.commons.microservices.lib.common.Nil;
import com.ixaris.commons.microservices.lib.common.ServiceConstants;
import com.ixaris.commons.microservices.lib.common.ServiceHandlerStrategy;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.common.ServiceRootResource;
import com.ixaris.commons.microservices.lib.common.annotations.ServicePath;
import com.ixaris.commons.microservices.lib.common.annotations.ServiceSecurity;
import com.ixaris.commons.microservices.lib.common.exception.ClientConflictException;
import com.ixaris.commons.microservices.lib.common.proxy.ProxyInvoker;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventAckEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;
import com.ixaris.commons.microservices.lib.service.support.ServiceAsyncInterceptor;
import com.ixaris.commons.misc.lib.conversion.SnakeCaseHelper;
import com.ixaris.commons.misc.lib.object.GenericsUtil;
import com.ixaris.commons.misc.lib.object.ProxyFactory;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;

public final class ServiceStubProxy<T extends ServiceStub> {
    
    @FunctionalInterface
    public interface CreateEventHandler {
        
        void create(String subscriberName, ServicePathHolder path, ServiceEventProcessor processor);
        
    }
    
    @FunctionalInterface
    public interface DestroyEventHandler {
        
        void destroy(String subscriberName, ServicePathHolder path);
        
    }
    
    final ScheduledExecutorService executor;
    final MultiTenancy multiTenancy;
    final Class<T> serviceStubType;
    final ServiceKeysResolver keysResolver;
    final ServiceAsyncInterceptor asyncInterceptor;
    final ServiceHandlerStrategy handlerStrategy;
    final Class<? extends MessageLite> contextType;
    
    private final boolean spi;
    private final StubResourceInfo rootResourceInfo;
    private final ServiceStubOperationInvoker<T> operationInvoker;
    private final CreateEventHandler createEventHandler;
    private final DestroyEventHandler destroyEventHandler;
    private final ServiceResourceStubProxy<T> stubInvocationHandler;
    
    private volatile ImmutableMap<ListenerKey, ServiceStubEventProcessor<T>> eventProcessors = ImmutableMap.of();
    
    @SuppressWarnings("unchecked")
    public ServiceStubProxy(final ScheduledExecutorService executor,
                            final MultiTenancy multiTenancy,
                            final Class<T> serviceStubType,
                            final ServiceKeysResolver keysResolver,
                            final ServiceAsyncInterceptor asyncInterceptor,
                            final ServiceHandlerStrategy handlerStrategy,
                            final CreateEventHandler createEventHandler,
                            final DestroyEventHandler destroyEventHandler,
                            final AsyncFilterNext<RequestEnvelope, ResponseEnvelope> filterChain,
                            final KeyAvailableCondition keyAvailableCondition,
                            final int defaultTimeout) {
        if (serviceStubType == null) {
            throw new IllegalArgumentException("serviceStubType is null");
        }
        if (!serviceStubType.isInterface()) {
            throw new IllegalArgumentException("[" + serviceStubType + "] is not an interface");
        }
        if (!ServiceRootResource.class.isAssignableFrom(serviceStubType)) {
            throw new IllegalArgumentException("[" + serviceStubType + "] does not extend ServiceRootResource");
        }
        if (keysResolver == null) {
            throw new IllegalArgumentException("keysResolver is null");
        }
        if (asyncInterceptor == null) {
            throw new IllegalArgumentException("asyncInterceptor is null");
        }
        if (handlerStrategy == null) {
            throw new IllegalArgumentException("handlerStrategy is null");
        }
        
        this.executor = executor;
        this.multiTenancy = multiTenancy;
        this.serviceStubType = serviceStubType;
        this.keysResolver = keysResolver;
        this.asyncInterceptor = asyncInterceptor;
        this.handlerStrategy = handlerStrategy;
        this.createEventHandler = createEventHandler;
        this.destroyEventHandler = destroyEventHandler;
        contextType = (Class<? extends MessageLite>) GenericsUtil.resolveGenericTypeArguments(serviceStubType, ServiceRootResource.class).get("C");
        spi = ServiceProviderStub.class.isAssignableFrom(serviceStubType);
        rootResourceInfo = processResourceType(ServicePathHolder.empty(), serviceStubType, null);
        operationInvoker = new ServiceStubOperationInvoker(filterChain, keyAvailableCondition, this, defaultTimeout);
        stubInvocationHandler = new ServiceResourceStubProxy<>(this, ServicePathHolder.empty(), rootResourceInfo);
    }
    
    public boolean isSpi() {
        return spi;
    }
    
    public Class<T> getStubType() {
        return serviceStubType;
    }
    
    public Class<T> getServiceStubType() {
        return serviceStubType;
    }
    
    public StubResourceInfo getRootResourceInfo() {
        return rootResourceInfo;
    }
    
    public UntypedOperationInvoker getOperationInvoker() {
        return operationInvoker;
    }
    
    public ProxyInvoker getStubInvocationHandler() {
        return stubInvocationHandler;
    }
    
    @SuppressWarnings("unchecked")
    public T createProxy() {
        return (T) rootResourceInfo.proxyFactory.newInstance(getStubInvocationHandler());
    }
    
    public Async<?> invoke(final ServicePathHolder path,
                           final ServicePathHolder params,
                           final StubResourceMethodInfo<?, ?, ?> methodInfo,
                           final ServiceOperationHeader<?> header,
                           final MessageLite request) throws ClientConflictException {
        return operationInvoker.invoke(path, params, methodInfo, header, request);
    }
    
    public synchronized ServiceEventSubscription subscribe(final ServiceEventListener<?, ?> listener, final StubResourceInfo resource) {
        final ServiceStubEventProcessor<T> processor = new ServiceStubEventProcessor<>(this, listener, resource);
        createEventHandler.create(listener.getName(), resource.path, processor);
        eventProcessors = GuavaCollections.copyOfMapAdding(eventProcessors, new ListenerKey(listener.getName(), resource.path), processor);
        
        return () -> unsubscribe(listener, resource);
    }
    
    private synchronized void unsubscribe(final ServiceEventListener<?, ?> listener, final StubResourceInfo resource) {
        destroyEventHandler.destroy(listener.getName(), resource.path);
        eventProcessors = GuavaCollections.copyOfMapRemoving(eventProcessors, new ListenerKey(listener.getName(), resource.path));
    }
    
    public ServiceStubEventProcessor<T> getEventProcessor(final ServicePathHolder path, final String name) {
        return eventProcessors.get(new ListenerKey(name, path));
    }
    
    public Async<EventAckEnvelope> process(final EventEnvelope eventEnvelope, final String name) {
        return eventProcessors.get(new ListenerKey(name, ServicePathHolder.of(eventEnvelope.getPathList())))
            .process(eventEnvelope);
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + " for " + serviceStubType;
    }
    
    @SuppressWarnings("unchecked")
    private StubResourceInfo processResourceType(final ServicePathHolder resourcePath,
                                                 final Class<?> stubType,
                                                 final String parentSecurity) {
        if (!stubType.isInterface()) {
            throw new IllegalArgumentException("[" + stubType + "] is not an interface");
        }
        
        final String resourceSecurity = Optional.ofNullable(stubType.getAnnotation(ServiceSecurity.class))
            .map(ServiceSecurity::value)
            .orElse(parentSecurity);
        String paramName = null;
        StubResourceInfo paramInfo = null;
        final Map<String, StubResourceInfo> resourceMap = new HashMap<>();
        final Map<String, StubResourceMethodInfo<?, ?, ?>> methodMap = new HashMap<>();
        Class<? extends MessageLite> eventType = null;
        
        for (final Method method : stubType.getMethods()) {
            if (method.isDefault()) {
                // skip
            } else if (isSubResource(method)) {
                // subresource
                final Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == 0) {
                    final ServicePathHolder subResourcePath = resourcePath.push(SnakeCaseHelper.camelToSnakeCase(method
                        .getName()));
                    resourceMap.put(method.getName(),
                        processResourceType(subResourcePath, method.getReturnType(), resourceSecurity));
                } else if (parameterTypes.length == 1) {
                    if (paramName == null) {
                        paramName = method.getName();
                        final ServicePathHolder paramPath = resourcePath.push("_"); // push _ for param
                        paramInfo = processResourceType(paramPath, method.getReturnType(), resourceSecurity);
                    } else {
                        throw new IllegalStateException(String.format("Multiple parametrised paths [%s] in [%s]", method, stubType));
                    }
                } else {
                    throw new IllegalStateException(String.format("Too many parameters for path method [%s] in [%s]", method, stubType));
                }
                
            } else if (isWatch(method)) {
                // resource watch method
                final ParameterizedType genericListenerType = (ParameterizedType) method.getGenericParameterTypes()[0];
                final Class<? extends MessageLite> contextType = (Class<? extends MessageLite>) genericListenerType.getActualTypeArguments()[0];
                if (!this.contextType.equals(contextType)) {
                    throw new IllegalStateException(String.format("Mismatched context in watch method [%s] in [%s]", method, stubType));
                }
                
                eventType = (Class<? extends MessageLite>) genericListenerType.getActualTypeArguments()[1];
                
            } else if (isTransformedResourceMethod(method)) {
                // skip transformed operations
                
            } else if (isResourceMethod(method)) {
                // resource method
                final Type[] genericParameterTypes = method.getGenericParameterTypes();
                final Class<? extends MessageLite> contextType = (Class<? extends MessageLite>) ((ParameterizedType) genericParameterTypes[0]).getActualTypeArguments()[0];
                if (!this.contextType.equals(contextType)) {
                    throw new IllegalStateException(String.format("Mismatched context in operation method [%s] in [%s]", method, stubType));
                }
                final Class<?>[] parameterTypes = method.getParameterTypes();
                final boolean hasRequest = parameterTypes.length == 2;
                if (hasRequest && !MessageLite.class.isAssignableFrom(parameterTypes[1])) {
                    throw new IllegalStateException(String.format("Invalid second parameter for operation method [%s] in [%s]", method, stubType));
                }
                final Class<? extends MessageLite> requestType = hasRequest
                    ? (Class<? extends MessageLite>) parameterTypes[1] : null;
                
                final Type[] returnTypeArgs = ((ParameterizedType) method.getGenericReturnType())
                    .getActualTypeArguments();
                final Class<? extends MessageLite> responseType = returnTypeArgs[0].equals(Nil.class)
                    ? null : (Class<? extends MessageLite>) returnTypeArgs[0];
                
                final Class<?>[] exceptionTypes = method.getExceptionTypes();
                final Constructor<? extends ClientConflictException> conflictConstructor;
                final Class<? extends MessageLite> conflictType;
                if (exceptionTypes.length == 0) {
                    conflictType = null;
                    conflictConstructor = null;
                } else if (exceptionTypes.length == 1) {
                    if (ClientConflictException.class.isAssignableFrom(exceptionTypes[0])) {
                        final Class<? extends ClientConflictException> conflictException = (Class<? extends ClientConflictException>) exceptionTypes[0];
                        try {
                            conflictType = (Class<? extends MessageLite>) conflictException.getDeclaredMethod("getConflict").getReturnType();
                            conflictConstructor = conflictException.getConstructor(conflictType);
                        } catch (NoSuchMethodException e) {
                            throw new IllegalStateException(e);
                        }
                    } else {
                        throw new IllegalStateException(String.format("Invalid operation method [%s] should only throw a conflict exception", method));
                    }
                } else {
                    throw new IllegalStateException(String.format("Invalid operation method [%s] should only throw a conflict exception", method));
                }
                methodMap.put(method.getName(),
                    new StubResourceMethodInfo<>(SnakeCaseHelper.camelToSnakeCase(method.getName()),
                        requestType,
                        responseType,
                        conflictType,
                        conflictConstructor));
                
            } else if (!ServiceProviderStub.class.isAssignableFrom(stubType)
                || !(ServiceProviderStub.KEYS_METHOD_NAME.equals(method.getName())
                    && (method.getParameterCount() == 0))) {
                throw new IllegalStateException("Neither path nor operation method ["
                    + method
                    + "] in ["
                    + stubType
                    + "]");
            }
        }
        
        return new StubResourceInfo(new ProxyFactory(stubType.getClassLoader(), stubType),
            stubType,
            resourcePath,
            paramName,
            paramInfo,
            resourceMap,
            methodMap,
            eventType);
    }
    
    private boolean isSubResource(final Method method) {
        return method.getAnnotation(ServicePath.class) != null;
    }
    
    private boolean isWatch(final Method method) {
        return method.getName().equals(ServiceConstants.WATCH_METHOD_NAME);
    }
    
    private boolean isTransformedResourceMethod(final Method method) {
        final Class<?>[] parameterTypes = method.getParameterTypes();
        return CompletionStage.class.equals(method.getReturnType())
            && (parameterTypes.length >= 1)
            && ServiceOperationHeader.class.equals(parameterTypes[0])
            && (parameterTypes.length <= 2);
    }
    
    private boolean isResourceMethod(final Method method) {
        final Class<?>[] parameterTypes = method.getParameterTypes();
        return Async.class.equals(method.getReturnType())
            && (parameterTypes.length >= 1)
            && ServiceOperationHeader.class.equals(parameterTypes[0])
            && (parameterTypes.length <= 2);
    }
    
}
