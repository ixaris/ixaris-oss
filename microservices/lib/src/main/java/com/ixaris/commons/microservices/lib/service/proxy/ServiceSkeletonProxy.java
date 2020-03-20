package com.ixaris.commons.microservices.lib.service.proxy;

import static com.ixaris.commons.microservices.lib.common.ServiceConstants.WATCH_METHOD_NAME;
import static com.ixaris.commons.microservices.lib.service.support.ServiceSupport.NO_KEY;

import java.lang.reflect.Constructor;
import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Proxy;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.stream.Collectors;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.filter.AsyncFilterNext;
import com.ixaris.commons.collections.lib.GuavaCollections;
import com.ixaris.commons.microservices.lib.common.Nil;
import com.ixaris.commons.microservices.lib.common.ServiceHandlerStrategy;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.common.ServiceRootResource;
import com.ixaris.commons.microservices.lib.common.annotations.ServicePath;
import com.ixaris.commons.microservices.lib.common.annotations.ServiceSecurity;
import com.ixaris.commons.microservices.lib.common.annotations.ServiceTags;
import com.ixaris.commons.microservices.lib.common.exception.ClientConflictException;
import com.ixaris.commons.microservices.lib.common.exception.ClientInvalidRequestException;
import com.ixaris.commons.microservices.lib.common.exception.ClientUnauthorisedException;
import com.ixaris.commons.microservices.lib.common.exception.ServiceException;
import com.ixaris.commons.microservices.lib.common.proxy.ProxyInvoker;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventAckEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;
import com.ixaris.commons.microservices.lib.service.ServiceEventPublisher;
import com.ixaris.commons.microservices.lib.service.ServiceOperation.ServiceOperationAroundAsync;
import com.ixaris.commons.microservices.lib.service.ServiceProviderSkeleton;
import com.ixaris.commons.microservices.lib.service.ServiceSkeleton;
import com.ixaris.commons.microservices.lib.service.support.ServiceAsyncInterceptor;
import com.ixaris.commons.microservices.lib.service.support.ServiceExceptionTranslator;
import com.ixaris.commons.microservices.lib.service.support.ServiceOperationProcessor;
import com.ixaris.commons.microservices.lib.service.support.ServiceSecurityChecker;
import com.ixaris.commons.misc.lib.conversion.SnakeCaseHelper;
import com.ixaris.commons.misc.lib.function.FunctionThrows;
import com.ixaris.commons.misc.lib.object.GenericsUtil;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.protobuf.lib.MessageValidator;

import valid.Valid.MessageValidation;

public final class ServiceSkeletonProxy<T extends ServiceSkeleton> {
    
    final MultiTenancy multiTenancy;
    final Class<T> serviceSkeletonType;
    private final ServiceSecurityChecker securityChecker;
    final ServiceAsyncInterceptor asyncInterceptor;
    final ServiceHandlerStrategy handlerStrategy;
    final Class<? extends MessageLite> contextType;
    private final SkeletonResourceInfo rootResourceInfo;
    final Map<Class<?>, Map<String, SkeletonResourceMethodInfo<?, ?, ?>>> resourceMethodMap;
    private final Map<Class<? extends ServiceEventPublisher<?, ?, ?>>, ServiceEventPublisherProxy<T>> publisherInvocationHandlers;
    final FunctionThrows<Throwable, ServiceException, RuntimeException> serviceExceptionTranslator;
    
    private volatile ImmutableMap<String, ServiceSkeletonOperationProcessor<T>> operationProcessors = ImmutableMap.of();
    private volatile ImmutableMap<String, ServiceSkeletonEventPublisher<T>> eventPublishers = ImmutableMap.of();
    
    @SuppressWarnings("unchecked")
    public ServiceSkeletonProxy(final MultiTenancy multiTenancy,
                                final Class<T> serviceSkeletonType,
                                final ServiceSecurityChecker securityChecker,
                                final ServiceAsyncInterceptor asyncInterceptor,
                                final ServiceHandlerStrategy handlerStrategy,
                                final Set<? extends ServiceExceptionTranslator<?>> exceptionTranslators) {
        if (serviceSkeletonType == null) {
            throw new IllegalArgumentException("serviceSkeletonType is null");
        }
        if (!serviceSkeletonType.isInterface()) {
            throw new IllegalArgumentException("[" + serviceSkeletonType + "] is not an interface");
        }
        if (securityChecker == null) {
            throw new IllegalArgumentException("securityChecker is null");
        }
        if (asyncInterceptor == null) {
            throw new IllegalArgumentException("asyncInterceptor is null");
        }
        if (handlerStrategy == null) {
            throw new IllegalArgumentException("handlerStrategy is null");
        }
        
        this.multiTenancy = multiTenancy;
        this.serviceSkeletonType = serviceSkeletonType;
        this.securityChecker = securityChecker;
        this.asyncInterceptor = asyncInterceptor;
        this.handlerStrategy = handlerStrategy;
        
        final Method resourceMethod;
        try {
            resourceMethod = serviceSkeletonType.getDeclaredMethod("resource");
            if (!ServiceRootResource.class.isAssignableFrom(resourceMethod.getReturnType())) {
                throw new IllegalStateException("Expected resource() to return ServiceRootResource in ["
                    + serviceSkeletonType
                    + "]");
            }
        } catch (NoSuchMethodException e) {
            throw new IllegalStateException("Expected resource() method in [" + serviceSkeletonType + "]", e);
        }
        
        contextType = (Class<? extends MessageLite>) GenericsUtil
            .resolveGenericTypeArguments(resourceMethod.getReturnType(), ServiceRootResource.class)
            .get("C");
        final Map<Class<?>, Map<String, SkeletonResourceMethodInfo<?, ?, ?>>> resourceMethodMap = new HashMap<>();
        final Map<Class<? extends ServiceEventPublisher<?, ?, ?>>, ServiceEventPublisherProxy<T>> publishers = new HashMap<>();
        rootResourceInfo = processSkeletonType(serviceSkeletonType,
            resourceMethod,
            0,
            new ResourcePathParam[0],
            ServicePathHolder.empty(),
            null,
            Collections.emptyList(),
            resourceMethodMap,
            publishers);
        this.resourceMethodMap = Collections.unmodifiableMap(resourceMethodMap);
        this.publisherInvocationHandlers = Collections.unmodifiableMap(publishers);
        serviceExceptionTranslator = ServiceSkeletonOperationProcessor
            .buildServiceExceptionTranslateFunction(exceptionTranslators);
    }
    
    public Class<T> getSkeletonType() {
        return serviceSkeletonType;
    }
    
    public SkeletonResourceInfo getRootResourceInfo() {
        return rootResourceInfo;
    }
    
    public ProxyInvoker getPublisherInvocationHandler(final Class<? extends ServiceEventPublisher<?, ?, ?>> publisherType) {
        final ServiceEventPublisherProxy<T> handler = publisherInvocationHandlers.get(publisherType);
        if (handler == null) {
            throw new IllegalStateException("Handler for found for " + publisherType + " in " + serviceSkeletonType);
        }
        return handler;
    }
    
    @SuppressWarnings("unchecked")
    public <P extends ServiceEventPublisher<?, ?, ?>> P createPublisherProxy(final Class<P> publisherType) {
        return (P) Proxy.newProxyInstance(publisherType.getClassLoader(),
            new Class<?>[] { publisherType },
            getPublisherInvocationHandler(publisherType));
    }
    
    public ServiceSkeletonEventPublisher<T> getPublisherForServiceKey(final String serviceKey) {
        final String key = serviceKey != null ? serviceKey : NO_KEY;
        final ServiceSkeletonEventPublisher<T> processor = eventPublishers.get(key);
        if (processor != null) {
            return processor;
        } else {
            throw new IllegalStateException("No event processor for key [" + key + "]");
        }
    }
    
    public Set<ServicePathHolder> getPublisherPaths() {
        return publisherInvocationHandlers.values()
            .stream()
            .map(ServiceEventPublisherProxy::getPath)
            .collect(Collectors.toSet());
    }
    
    public synchronized ServiceOperationProcessor init(final T serviceSkeleton,
                                                       final String serviceKey,
                                                       final AsyncFilterNext<EventEnvelope, EventAckEnvelope> filterChain) {
        if (serviceSkeleton == null) {
            throw new IllegalArgumentException("serviceSkeleton is null");
        }
        if (!serviceSkeletonType.isAssignableFrom(serviceSkeleton.getClass())) {
            throw new IllegalStateException(String.format("serviceSkeleton [%s] is not of type [%s]", serviceSkeleton.getClass(), serviceSkeletonType));
        }
        if (serviceKey == null) {
            throw new IllegalArgumentException("serviceKey is null");
        }
        if (ServiceProviderSkeleton.class.isAssignableFrom(serviceSkeleton.getClass())) {
            if (serviceKey.equals(NO_KEY)) {
                throw new IllegalArgumentException("serviceKey is empty for " + serviceSkeletonType);
            }
        } else if (!serviceKey.equals(NO_KEY)) {
            throw new IllegalArgumentException("serviceKey should be empty for " + serviceSkeletonType);
        }
        
        final ServiceSkeletonOperationProcessor<T> processor = new ServiceSkeletonOperationProcessor<>(this, serviceSkeleton);
        operationProcessors = GuavaCollections.copyOfMapAdding(operationProcessors, serviceKey, processor);
        eventPublishers = GuavaCollections.copyOfMapAdding(eventPublishers,
            serviceKey,
            new ServiceSkeletonEventPublisher<>(filterChain, this, serviceKey));
        return processor;
    }
    
    public synchronized void destroy(final T serviceSkeleton, final String serviceKey) {
        if (serviceSkeleton == null) {
            throw new IllegalArgumentException("serviceSkeleton is null");
        }
        if (!serviceSkeletonType.isAssignableFrom(serviceSkeleton.getClass())) {
            throw new IllegalStateException(String.format("serviceSkeleton [%s] is not of type [%s]", serviceSkeleton.getClass(), serviceSkeletonType));
        }
        if (serviceKey == null) {
            throw new IllegalArgumentException("serviceKey is null");
        }
        if (ServiceProviderSkeleton.class.isAssignableFrom(serviceSkeleton.getClass())) {
            if (serviceKey.equals(NO_KEY)) {
                throw new IllegalArgumentException("serviceKey is empty for " + serviceSkeletonType);
            }
        } else if (!serviceKey.equals(NO_KEY)) {
            throw new IllegalArgumentException("serviceKey should be empty for " + serviceSkeletonType);
        }
        
        operationProcessors = GuavaCollections.copyOfMapRemoving(operationProcessors, serviceKey);
        eventPublishers = GuavaCollections.copyOfMapRemoving(eventPublishers, serviceKey);
    }
    
    public Async<ResponseEnvelope> process(final RequestEnvelope requestEnvelope) {
        return operationProcessors.get(requestEnvelope.getServiceKey()).process(requestEnvelope);
    }
    
    public void validateAndCheckSecurity(final SkeletonResourceMethodInfo<?, ?, ?> methodInfo,
                                         final ServiceOperationHeader<?> header,
                                         final MessageLite request) {
        final MessageValidation validation = (methodInfo.requestType != null)
            ? MessageValidator.validate(request) : null;
        
        if ((validation != null) && validation.getInvalid()) {
            throw new ClientInvalidRequestException(validation);
            
        } else if (!securityChecker.check(header, methodInfo.security, methodInfo.tags)) {
            throw new ClientUnauthorisedException();
        }
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + " for " + serviceSkeletonType;
    }
    
    @SuppressWarnings("unchecked")
    private SkeletonResourceInfo processSkeletonType(final Class<?> skeletonType,
                                                     final Method resourceMethod,
                                                     final int index,
                                                     final ResourcePathParam[] pathParams,
                                                     final ServicePathHolder eventPath,
                                                     final String parentSecurity,
                                                     final List<String> parentTags,
                                                     final Map<Class<?>, Map<String, SkeletonResourceMethodInfo<?, ?, ?>>> resourceMethodMap,
                                                     final Map<Class<? extends ServiceEventPublisher<?, ?, ?>>, ServiceEventPublisherProxy<T>> publishers) {
        final Class<?> resourceType = resourceMethod.getReturnType();
        if (!resourceType.isInterface()) {
            throw new IllegalArgumentException("[" + resourceType + "] is not an interface");
        }
        
        final Map<String, Class<?>> skeletonTypeDeclaredClasses = Arrays.stream(skeletonType.getDeclaredClasses())
            .collect(Collectors.toMap(this::operationClassToMethod, Function.identity()));
        final String resourceSecurity = Optional.ofNullable(resourceType.getAnnotation(ServiceSecurity.class))
            .map(ServiceSecurity::value)
            .orElse(parentSecurity);
        final List<String> resourceTags = Optional.ofNullable(resourceType.getAnnotation(ServiceTags.class))
            .map(a -> Arrays.asList(a.value()))
            .orElse(parentTags);
        PathParamType paramType = null;
        SkeletonResourceInfo paramInfo = null;
        final Map<String, SkeletonResourceInfo> resourceMap = new HashMap<>();
        final Map<String, SkeletonResourceMethodInfo<?, ?, ?>> methodMap = new HashMap<>();
        
        for (final Method method : resourceType.getMethods()) {
            if (isSubResource(method)) {
                // subresource, so there should be an equivalent method in handler
                final Class<?> subSkeleton = skeletonTypeDeclaredClasses.remove(method.getName() + "Skeleton");
                if (subSkeleton == null) {
                    throw new IllegalStateException("Skeleton for [" + method + "] not found in [" + skeletonType + "]");
                }
                final Class<?>[] parameterTypes = method.getParameterTypes();
                if (parameterTypes.length == 0) {
                    final String resourcePath = SnakeCaseHelper.camelToSnakeCase(method.getName());
                    final ServicePathHolder subEventPath = eventPath.push(resourcePath);
                    resourceMap.put(resourcePath,
                        processSkeletonType(subSkeleton,
                            method,
                            index + 1,
                            pathParams,
                            subEventPath,
                            resourceSecurity,
                            resourceTags,
                            resourceMethodMap,
                            publishers));
                } else if (parameterTypes.length == 1) {
                    if (paramType == null) {
                        paramType = PathParamType.match(parameterTypes[0]);
                        if (paramType == null) {
                            throw new IllegalStateException(String.format("Unsupported parameter type for [%s] in [%s]", method, resourceType));
                        }
                        final ResourcePathParam[] subPathParams = Arrays.copyOf(pathParams, pathParams.length + 1);
                        subPathParams[subPathParams.length - 1] = new ResourcePathParam(index, paramType);
                        final ServicePathHolder subEventPath = eventPath.push("");
                        paramInfo = processSkeletonType(subSkeleton,
                            method,
                            index + 1,
                            subPathParams,
                            subEventPath,
                            resourceSecurity,
                            resourceTags,
                            resourceMethodMap,
                            publishers);
                    } else {
                        throw new IllegalStateException(String.format("Multiple parametrised paths [%s] in [%s]", method, resourceType));
                    }
                } else {
                    throw new IllegalStateException(String.format("Too many parameters for path method [%s] in [%s]", method, resourceType));
                }
                
            } else if (isTransformedResourceMethod(method)) {
                // skip transformed operations
                
            } else if (isResourceMethod(method)) {
                // resource method
                final String methodSecurity = Optional.ofNullable(method.getAnnotation(ServiceSecurity.class))
                    .map(ServiceSecurity::value)
                    .orElse(resourceSecurity);
                final List<String> methodTags = Optional.ofNullable(method.getAnnotation(ServiceTags.class))
                    .map(a -> Arrays.asList(a.value()))
                    .orElse(resourceTags);
                final Type[] genericParameterTypes = method.getGenericParameterTypes();
                final Class<? extends MessageLite> contextType = (Class<? extends MessageLite>) ((ParameterizedType) genericParameterTypes[0]).getActualTypeArguments()[0];
                if (!this.contextType.equals(contextType)) {
                    throw new IllegalStateException("Mismatched context in operation method ["
                        + method
                        + "] in ["
                        + resourceType
                        + "]");
                }
                final Class<?>[] parameterTypes = method.getParameterTypes();
                final boolean hasRequest = parameterTypes.length == 2;
                if (hasRequest && !MessageLite.class.isAssignableFrom(parameterTypes[1])) {
                    throw new IllegalStateException("Invalid second parameter for operation method ["
                        + method
                        + "] in ["
                        + resourceType
                        + "]");
                }
                final Class<? extends MessageLite> requestType = hasRequest
                    ? (Class<? extends MessageLite>) parameterTypes[1] : null;
                
                final Type[] returnTypeArgs = ((ParameterizedType) method.getGenericReturnType())
                    .getActualTypeArguments();
                final Class<? extends MessageLite> responseType = returnTypeArgs[0].equals(Nil.class)
                    ? null : (Class<? extends MessageLite>) returnTypeArgs[0];
                
                final Class<?>[] exceptionTypes = method.getExceptionTypes();
                final Class<? extends MessageLite> conflictType;
                if (exceptionTypes.length == 0) {
                    conflictType = null;
                } else if (exceptionTypes.length == 1) {
                    if (ClientConflictException.class.isAssignableFrom(exceptionTypes[0])) {
                        final Class<? extends ClientConflictException> conflictException = (Class<? extends ClientConflictException>) exceptionTypes[0];
                        try {
                            conflictType = (Class<? extends MessageLite>) conflictException.getDeclaredMethod("getConflict").getReturnType();
                        } catch (NoSuchMethodException e) {
                            throw new IllegalStateException(e);
                        }
                    } else {
                        throw new IllegalStateException("Invalid operation method ["
                            + method
                            + "] should only throw a conflict exception");
                    }
                } else {
                    throw new IllegalStateException("Invalid operation method ["
                        + method
                        + "] should only throw a conflict exception");
                }
                
                final Class<?> opClass = skeletonTypeDeclaredClasses.remove(method.getName());
                if (opClass == null) {
                    throw new IllegalStateException("Operation class for ["
                        + method.getName()
                        + "] not found in ["
                        + skeletonType
                        + "]");
                }
                
                final Constructor<?>[] declaredConstructors = opClass.getDeclaredConstructors();
                if (declaredConstructors.length == 0) {
                    throw new IllegalStateException("No constructors found in operation class for ["
                        + method.getName()
                        + "] in ["
                        + resourceType
                        + "]");
                } else if (declaredConstructors.length > 1) {
                    throw new IllegalStateException("More than 1 constructor found in operation class for ["
                        + method.getName()
                        + "] in ["
                        + resourceType
                        + "]");
                }
                
                final Constructor<?> constructor = declaredConstructors[0];
                
                if (constructor.getParameterCount() != pathParams.length + (hasRequest ? 4 : 3)) {
                    throw new IllegalStateException("Wrong number of arguments for [" + constructor + "]");
                }
                final Class<?>[] constructorParameterTypes = constructor.getParameterTypes();
                final Type[] constructorGenericParameterTypes = constructor.getGenericParameterTypes();
                for (int i = 0; i < pathParams.length; i++) {
                    if (pathParams[i].pathParamType != PathParamType.match(constructorParameterTypes[i])) {
                        throw new IllegalStateException("Wrong parameter in constructor ["
                            + constructor
                            + "] at position ["
                            + i
                            + "]");
                    }
                }
                final Class<? extends MessageLite> constructorContextType = (Class<? extends MessageLite>) ((ParameterizedType) constructorGenericParameterTypes[pathParams.length])
                    .getActualTypeArguments()[0];
                if (!this.contextType.equals(constructorContextType)) {
                    throw new IllegalStateException("Mismatched context in constructor [" + constructor + "]");
                }
                if (hasRequest && !requestType.equals(constructorParameterTypes[pathParams.length + 1])) {
                    throw new IllegalStateException("Mismatched request in constructor [" + constructor + "]");
                }
                final Type[] wrapperArgs = ((ParameterizedType) constructorGenericParameterTypes[constructorGenericParameterTypes.length - 2])
                    .getActualTypeArguments();
                final Class<? extends MessageLite> constructorResponseType = wrapperArgs[0].equals(Nil.class)
                    ? null : (Class<? extends MessageLite>) wrapperArgs[0];
                final Class<? extends MessageLite> constructorConflictType = wrapperArgs[1].equals(Nil.class)
                    ? null : (Class<? extends MessageLite>) wrapperArgs[1];
                if (!Objects.equals(responseType, constructorResponseType)) {
                    throw new IllegalStateException("Mismatched response in constructor [" + constructor + "]");
                }
                if (!Objects.equals(conflictType, constructorConflictType)) {
                    throw new IllegalStateException("Mismatched conflict in constructor [" + constructor + "]");
                }
                if (!ServiceOperationAroundAsync.class.equals(constructorParameterTypes[constructorParameterTypes.length - 1])) {
                    throw new IllegalStateException("Wrong parameter in constructor ["
                        + constructor
                        + "] at position ["
                        + (constructorParameterTypes.length - 1)
                        + "]");
                }
                
                methodMap.put(SnakeCaseHelper.camelToSnakeCase(method.getName()),
                    new SkeletonResourceMethodInfo<>(pathParams,
                        constructor,
                        method,
                        methodSecurity,
                        methodTags,
                        requestType,
                        responseType,
                        conflictType));
                
            } else {
                throw new IllegalStateException("Neither path nor operation method ["
                    + method
                    + "] in ["
                    + resourceType
                    + "]");
            }
        }
        
        final Class<?> pubClass = skeletonTypeDeclaredClasses.remove(WATCH_METHOD_NAME);
        if (pubClass != null) {
            if (eventPath == null) {
                throw new IllegalStateException("Invalid publisher in parametrised path ["
                    + pubClass
                    + "] in ["
                    + skeletonType
                    + "]");
            }
            if (!ServiceEventPublisher.class.isAssignableFrom(pubClass)) {
                throw new IllegalStateException("Invalid publisher [" + pubClass + "] in [" + skeletonType + "]");
            }
            final List<String> methodTags = Optional.ofNullable(pubClass.getAnnotation(ServiceTags.class))
                .map(a -> Arrays.asList(a.value()))
                .orElse(resourceTags);
            final Map<String, Type> typeArguments = GenericsUtil.getGenericTypeArguments(pubClass,
                ServiceEventPublisher.class);
            final Class<? extends MessageLite> contextType = (Class<? extends MessageLite>) typeArguments.get("C");
            if (!this.contextType.equals(contextType)) {
                throw new IllegalStateException("Mismatched context in watch ["
                    + pubClass
                    + "] in ["
                    + skeletonType
                    + "]");
            }
            
            final Class<? extends MessageLite> eventType = (Class<? extends MessageLite>) typeArguments.get("E");
            
            methodMap.put(SnakeCaseHelper.camelToSnakeCase(WATCH_METHOD_NAME),
                new SkeletonResourceMethodInfo<>(pathParams, null, null, null, methodTags, null, eventType, null));
            
            publishers.put((Class<? extends ServiceEventPublisher<?, ?, ?>>) pubClass,
                new ServiceEventPublisherProxy<>(this, eventPath));
        }
        
        if (!skeletonTypeDeclaredClasses.isEmpty()) {
            throw new IllegalStateException("Unmatched classes "
                + skeletonTypeDeclaredClasses
                + " in ["
                + skeletonType
                + "]");
        }
        
        resourceMethodMap.put(resourceType, Collections.unmodifiableMap(methodMap));
        return new SkeletonResourceInfo(resourceMethod, methodMap, paramType, paramInfo, resourceMap);
    }
    
    private String operationClassToMethod(final Class<?> c) {
        return Character.toLowerCase(c.getSimpleName().charAt(0)) + c.getSimpleName().substring(1);
    }
    
    private boolean isSubResource(final Method method) {
        return method.getAnnotation(ServicePath.class) != null;
    }
    
    private boolean isWatch(final Method method) {
        return method.getName().equals(WATCH_METHOD_NAME);
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
