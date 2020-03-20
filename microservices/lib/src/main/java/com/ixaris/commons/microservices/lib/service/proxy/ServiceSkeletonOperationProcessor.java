package com.ixaris.commons.microservices.lib.service.proxy;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.idempotency.Intent.INTENT;
import static com.ixaris.commons.async.lib.logging.AsyncLogging.ASYNC_MDC;
import static com.ixaris.commons.async.lib.logging.AsyncLogging.CORRELATION;
import static com.ixaris.commons.clustering.lib.common.ClusterShardResolver.getShardKeyFromString;
import static com.ixaris.commons.microservices.lib.common.ServiceHeader.extractCorrelation;
import static com.ixaris.commons.microservices.lib.common.ServiceLoggingHelper.KEY_SERVICE_KEY;
import static com.ixaris.commons.microservices.lib.common.ServiceLoggingHelper.KEY_SERVICE_NAME;
import static com.ixaris.commons.microservices.lib.service.ServiceSkeletonOperation.wrapError;
import static com.ixaris.commons.misc.lib.object.Tuple.tuple;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;

import java.lang.reflect.InvocationTargetException;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.common.collect.ImmutableMap;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.Message;
import com.google.protobuf.MessageLite;
import com.google.protobuf.ProtocolStringList;
import com.google.protobuf.util.JsonFormat;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.AsyncLocal;
import com.ixaris.commons.async.lib.idempotency.Intent;
import com.ixaris.commons.microservices.lib.common.Nil;
import com.ixaris.commons.microservices.lib.common.ServiceHeader;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.common.exception.ClientConflictException;
import com.ixaris.commons.microservices.lib.common.exception.ClientInvalidRequestException;
import com.ixaris.commons.microservices.lib.common.exception.ClientMethodNotAllowedException;
import com.ixaris.commons.microservices.lib.common.exception.ClientTooManyRequestsException;
import com.ixaris.commons.microservices.lib.common.exception.ServerErrorException;
import com.ixaris.commons.microservices.lib.common.exception.ServerNotImplementedException;
import com.ixaris.commons.microservices.lib.common.exception.ServerTimeoutException;
import com.ixaris.commons.microservices.lib.common.exception.ServerUnavailableException;
import com.ixaris.commons.microservices.lib.common.exception.ServiceException;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;
import com.ixaris.commons.microservices.lib.service.ServiceOperation;
import com.ixaris.commons.microservices.lib.service.ServiceOperation.BackpressureException;
import com.ixaris.commons.microservices.lib.service.ServiceOperation.ServiceOperationAroundAsync;
import com.ixaris.commons.microservices.lib.service.ServiceResponseWrapper;
import com.ixaris.commons.microservices.lib.service.ServiceSkeleton;
import com.ixaris.commons.microservices.lib.service.ServiceSkeletonOperation;
import com.ixaris.commons.microservices.lib.service.support.ServiceExceptionTranslator;
import com.ixaris.commons.microservices.lib.service.support.ServiceOperationProcessor;
import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.misc.lib.function.FunctionThrows;
import com.ixaris.commons.misc.lib.object.ClassMatcherFunctionBuilder;
import com.ixaris.commons.misc.lib.object.ClassUtil;
import com.ixaris.commons.misc.lib.object.GenericsUtil;
import com.ixaris.commons.misc.lib.object.Tuple2;
import com.ixaris.commons.misc.lib.object.Tuple4;
import com.ixaris.commons.multitenancy.lib.TenantInactiveException;
import com.ixaris.commons.protobuf.lib.MessageHelper;
import com.ixaris.commons.protobuf.lib.MessageValidator;

import valid.Valid.MessageValidation;

final class ServiceSkeletonOperationProcessor<T extends ServiceSkeleton> implements ServiceOperationProcessor {
    
    private static final Logger LOG = LoggerFactory.getLogger(ServiceSkeletonOperationProcessor.class);
    
    static FunctionThrows<Throwable, ServiceException, RuntimeException> buildServiceExceptionTranslateFunction(final Set<? extends ServiceExceptionTranslator<?>> exceptionTranslators) {
        final ClassMatcherFunctionBuilder<Throwable, ServiceException, RuntimeException> functionBuilder = ClassMatcherFunctionBuilder.newBuilder();
        functionBuilder.when(ServiceException.class).apply(FunctionThrows.identity());
        functionBuilder.when(UnsupportedOperationException.class).apply(ServerNotImplementedException::new);
        for (final ServiceExceptionTranslator<?> exceptionTranslator : exceptionTranslators) {
            addExceptionTranslator(functionBuilder, exceptionTranslator);
        }
        return functionBuilder.otherwise(e -> {
            LOG.error("Unexpected error", e);
            return new ServerErrorException(e);
        });
    }
    
    @SuppressWarnings("unchecked")
    private static <E extends Exception> void addExceptionTranslator(final ClassMatcherFunctionBuilder<Throwable, ServiceException, RuntimeException> functionBuilder,
                                                                     final ServiceExceptionTranslator<E> exceptionTranslator) {
        final Class<? extends E> exceptionType = (Class<? extends E>) GenericsUtil
            .resolveGenericTypeArguments(exceptionTranslator.getClass(), ServiceExceptionTranslator.class)
            .get("T");
        functionBuilder.when(exceptionType).apply(exceptionTranslator::translate);
    }
    
    private static ResponseEnvelope.Builder newResponseEnvelopeBuilder(final RequestEnvelope requestEnvelope) {
        final ResponseEnvelope.Builder builder = ResponseEnvelope.newBuilder()
            .setCorrelationId(requestEnvelope.getCorrelationId())
            .setCallRef(requestEnvelope.getCallRef());
        
        if (requestEnvelope.getJsonPayload()) {
            builder.setJsonPayload(true);
        }
        
        return builder;
    }
    
    private static String getConflictStatusMessage(final MessageLite conflict) {
        final Message conflictMessage = (Message) conflict;
        try {
            return JsonFormat.printer().print(conflictMessage);
        } catch (final InvalidProtocolBufferException e) {
            LOG.warn(String.format("Unable to extract details for conflict [%s]", conflict.getClass().getName()), e);
            return "";
        }
    }
    
    private final ServiceSkeletonProxy<T> proxy;
    private final T serviceSkeleton;
    
    ServiceSkeletonOperationProcessor(final ServiceSkeletonProxy<T> proxy, final T serviceSkeleton) {
        if (proxy == null) {
            throw new IllegalArgumentException("proxy is null");
        }
        if (serviceSkeleton == null) {
            throw new IllegalArgumentException("serviceSkeleton is null");
        }
        
        this.proxy = proxy;
        this.serviceSkeleton = serviceSkeleton;
    }
    
    @Override
    public Async<ResponseEnvelope> process(final RequestEnvelope requestEnvelope) {
        if (requestEnvelope == null) {
            throw new IllegalArgumentException("requestEnvelope is null");
        }
        
        LOG.debug("Received request [{}] for [{}]", requestEnvelope.getCorrelationId(), proxy.serviceSkeletonType);
        
        ResponseEnvelope responseEnvelope;
        try {
            proxy.multiTenancy.verifyTenantIsActive(requestEnvelope.getTenantId());
            responseEnvelope = await(serviceSkeleton.handle(getServiceSkeletonOperation(requestEnvelope)));
        } catch (final TenantInactiveException e) {
            LOG.error("Tenant inactive while handling request", e);
            responseEnvelope = wrapError(requestEnvelope, new ServerUnavailableException(e));
        } catch (final ServiceException e) {
            responseEnvelope = wrapError(requestEnvelope, e);
        } catch (final Throwable e) { // NOSONAR framework code
            LOG.error("Unexpected error while handling request", e);
            responseEnvelope = wrapError(requestEnvelope, new ServerErrorException(e));
        }
        
        LOG.debug("Sending response [{}] [{}:{}] for [{}",
            responseEnvelope.getStatusCode(),
            requestEnvelope.getCorrelationId(),
            requestEnvelope.getCallRef(),
            proxy.serviceSkeletonType);
        
        return result(responseEnvelope);
    }
    
    private ServiceSkeletonOperation getServiceSkeletonOperation(final RequestEnvelope requestEnvelope) {
        return new ServiceSkeletonOperation() {
            
            private Tuple2<Class<?>, List<Object>> resourceTypeAndParams;
            
            @Override
            public Async<ResponseEnvelope> invokeOnResourceProxy() {
                return ServiceSkeletonOperationProcessor.this.invokeOnResourceProxy(requestEnvelope);
            }
            
            @Override
            public ServiceOperation<?, ?, ?, ?> getResourceOperationObject() throws BackpressureException {
                return ServiceSkeletonOperationProcessor.this.getResourceOperationObject(getResourceType(), requestEnvelope);
            }
            
            @Override
            public ResponseEnvelope wrapError(final ServiceException e) {
                return ServiceSkeletonOperation.wrapError(requestEnvelope, e);
            }
            
            @Override
            public Class<?> getResourceType() {
                return getResourceTypeAndParams().get1();
            }
            
            @Override
            public List<Object> getParams() {
                return getResourceTypeAndParams().get2();
            }
            
            @Override
            public Long getShardKey() {
                final List<Object> params = getParams();
                if (params.isEmpty()) {
                    return null;
                } else {
                    final Object param = params.get(0);
                    if (param instanceof Long) {
                        return (Long) param;
                    } else if (param instanceof Integer) {
                        return ((Integer) param).longValue();
                    } else {
                        return getShardKeyFromString((String) param);
                    }
                }
            }
            
            @Override
            public RequestEnvelope getRequestEnvelope() {
                return requestEnvelope;
            }
            
            @Override
            public MessageLite getRequest() {
                return Optional.ofNullable(
                    Optional.ofNullable(proxy.resourceMethodMap.get(getResourceType()))
                        .map(methodMap -> methodMap.get(requestEnvelope.getMethod()))
                        .orElseThrow(() -> new ClientMethodNotAllowedException(String.format(
                            "Method [%s] not allowed for pathParams %s",
                            requestEnvelope.getMethod(),
                            ServicePathHolder.of(requestEnvelope.getPathList())))))
                    .map(methodInfo -> methodInfo.requestType)
                    .map(requestType -> {
                        try {
                            return MessageHelper.parse(requestType, requestEnvelope.getPayload());
                        } catch (final InvalidProtocolBufferException e) {
                            throw new ClientInvalidRequestException(e);
                        }
                    })
                    .orElse(null);
            }
            
            @Deprecated
            @Override
            public ServiceSkeletonProxy<?> getProxy() {
                return proxy;
            }
            
            @SuppressWarnings("squid:S134")
            private Tuple2<Class<?>, List<Object>> getResourceTypeAndParams() {
                if (resourceTypeAndParams == null) {
                    resourceTypeAndParams = determineResourceTypeAndParams(requestEnvelope);
                }
                return resourceTypeAndParams;
            }
            
        };
    }
    
    @Override
    public String toString() {
        return getClass().getSimpleName() + " for " + proxy.serviceSkeletonType;
    }
    
    @SuppressWarnings({ "squid:S134", "checkstyle:com.puppycrawl.tools.checkstyle.checks.metrics.NPathComplexityCheck" })
    Async<ResponseEnvelope> invokeOnResourceProxy(final RequestEnvelope requestEnvelope) {
        final List<String> path = requestEnvelope.getPathList();
        int paramIndex = 0;
        SkeletonResourceInfo resourceInfo = proxy.getRootResourceInfo();
        Object resolvedResource = serviceSkeleton.resource();
        for (final String pathPart : path) {
            try {
                final SkeletonResourceInfo subResourceInfo = resourceInfo.resourceMap.get(pathPart);
                if (subResourceInfo != null) {
                    resourceInfo = subResourceInfo;
                    resolvedResource = resourceInfo.resourceMethod.invoke(resolvedResource);
                } else if (resourceInfo.paramInfo != null) {
                    final Object param = resourceInfo.paramType.fromString("_".equals(pathPart) ? requestEnvelope.getParams(paramIndex) : pathPart);
                    paramIndex++;
                    resourceInfo = resourceInfo.paramInfo;
                    resolvedResource = resourceInfo.resourceMethod.invoke(resolvedResource, param);
                } else {
                    return result(wrapError(requestEnvelope,
                        new ClientMethodNotAllowedException(String
                            .format("Method [%s] not allowed for pathParams %s",
                                requestEnvelope.getMethod(),
                                ServicePathHolder.of(requestEnvelope.getPathList())))));
                }
            } catch (final IllegalAccessException | InvocationTargetException | RuntimeException e) {
                LOG.error("Unexpected error while handling request", e);
                final ServiceException ex = new ServerErrorException(String.format("Error matching [%s] in [%s]", pathPart, path), e);
                return result(wrapError(requestEnvelope, ex));
            }
        }
        
        final Object resource = resolvedResource;
        final SkeletonResourceMethodInfo<?, ?, ?> methodInfo;
        final ServiceOperationHeader<?> header;
        final MessageLite request;
        final Intent intent;
        try {
            final Tuple4<SkeletonResourceMethodInfo<?, ?, ?>, ServiceOperationHeader<?>, MessageLite, Intent> extracted = validateAndExtractFromEnvelope(resourceInfo, requestEnvelope);
            methodInfo = extracted.get1();
            header = extracted.get2();
            request = extracted.get3();
            intent = extracted.get4();
        } catch (final ServiceException e) {
            LOG.error("Unexpected error while handling request", e);
            return result(wrapError(requestEnvelope, e));
        } catch (final RuntimeException e) {
            LOG.error("Unexpected error while handling request", e);
            return result(wrapError(requestEnvelope, new ServerErrorException(e)));
        }
        
        if (!proxy.handlerStrategy.getRequestStrategy().startMessage()) {
            return result(wrapError(requestEnvelope, new ClientTooManyRequestsException()));
        }
        
        Async<?> future;
        try {
            if (request != null) {
                future = aroundAsync(header,
                    intent,
                    () -> (Async<?>) methodInfo.method.invoke(resource, header, request));
            } else {
                future = aroundAsync(header, intent, () -> (Async<?>) methodInfo.method.invoke(resource, header));
            }
        } catch (final InvocationTargetException e) {
            future = Async.rejected(e.getCause());
        } catch (final Throwable e) {
            future = Async.rejected(e);
        }
        
        final ServiceResponseWrapperImpl<?, ?> responseWrapper = new ServiceResponseWrapperImpl<>(requestEnvelope,
            methodInfo.responseType,
            methodInfo.conflictType);
        if (future != null) {
            try {
                return result(responseWrapper.result(await(future)));
            } catch (final Throwable t) {
                if (t instanceof ClientConflictException) {
                    return result(responseWrapper.conflict(((ClientConflictException) t).getConflict()));
                } else {
                    LOG.error("Unexpected error while handling request", t);
                    return result(responseWrapper.error(t));
                }
            }
        } else {
            return result(responseWrapper.error(new ServerTimeoutException()));
        }
    }
    
    private ServiceOperation<?, ?, ?, ?> getResourceOperationObject(final Class<?> resourceType,
                                                                    final RequestEnvelope requestEnvelope) throws BackpressureException {
        final SkeletonResourceMethodInfo<?, ?, ?> methodInfo;
        final ServiceOperationHeader<?> header;
        final MessageLite request;
        final Intent intent;
        try {
            final Tuple4<SkeletonResourceMethodInfo<?, ?, ?>, ServiceOperationHeader<?>, MessageLite, Intent> extracted = validateAndExtractFromEnvelope(resourceType, requestEnvelope);
            methodInfo = extracted.get1();
            header = extracted.get2();
            request = extracted.get3();
            intent = extracted.get4();
        } catch (final ServiceException e) {
            throw e;
        } catch (final RuntimeException e) {
            throw new ServerErrorException(e);
        }
        
        if (!proxy.handlerStrategy.getRequestStrategy().startMessage()) {
            throw new BackpressureException(wrapError(requestEnvelope, new ClientTooManyRequestsException()));
        }
        
        final ServiceResponseWrapperImpl<?, ?> responseWrapper = new ServiceResponseWrapperImpl<>(requestEnvelope,
            methodInfo.responseType,
            methodInfo.conflictType);
        try {
            final Object[] params;
            if (request == null) {
                params = new Object[3 + methodInfo.pathParams.length];
                params[params.length - 3] = header;
            } else {
                params = new Object[4 + methodInfo.pathParams.length];
                params[params.length - 4] = header;
                params[params.length - 3] = request;
            }
            params[params.length - 2] = responseWrapper;
            params[params.length - 1] =
                new ServiceOperationAroundAsync() {
                    
                    @Override
                    public <C extends MessageLite, X, E extends Exception> X aroundAsync(final ServiceOperationHeader<C> header,
                                                                                         final CallableThrows<X, E> callable) throws E {
                        return AsyncLocal
                            .with(CORRELATION, extractCorrelation(header))
                            .with(TENANT, header.getTenantId())
                            .with(ASYNC_MDC,
                                ImmutableMap.of(
                                    KEY_SERVICE_NAME,
                                    header.getServiceName(),
                                    KEY_SERVICE_KEY,
                                    header.getServiceKey()))
                            .exec(() -> ServiceSkeletonOperationProcessor.this.aroundAsync(header, intent, callable));
                    }
                    
                };
            
            for (int i = 0; i < methodInfo.pathParams.length; i++) {
                final ResourcePathParam pathParam = methodInfo.pathParams[i];
                final String pathPart = requestEnvelope.getPath(pathParam.index);
                params[i] = pathParam.pathParamType.fromString("_".equals(pathPart) ? requestEnvelope.getParams(i) : pathPart);
            }
            
            return (ServiceOperation<?, ?, ?, ?>) methodInfo.constructor.newInstance(params);
            
        } catch (final ServiceException e) {
            proxy.handlerStrategy.getRequestStrategy().finishMessage();
            throw e;
            
        } catch (final RuntimeException | InstantiationException | IllegalAccessException e) {
            proxy.handlerStrategy.getRequestStrategy().finishMessage();
            throw new ServerErrorException(e);
            
        } catch (final InvocationTargetException e) {
            proxy.handlerStrategy.getRequestStrategy().finishMessage();
            if (e.getCause() instanceof ServiceException) {
                throw (ServiceException) e.getCause();
            } else {
                throw new ServerErrorException(e.getCause());
            }
        }
    }
    
    private Tuple2<Class<?>, List<Object>> determineResourceTypeAndParams(final RequestEnvelope requestEnvelope) {
        final ProtocolStringList path = requestEnvelope.getPathList();
        final List<Object> params = new ArrayList<>(requestEnvelope.getParamsCount());
        int paramIndex = 0;
        SkeletonResourceInfo resourceInfo = proxy.getRootResourceInfo();
        for (final String pathPart : path) {
            try {
                final SkeletonResourceInfo subResourceInfo = resourceInfo.resourceMap.get(pathPart);
                if (subResourceInfo != null) {
                    resourceInfo = subResourceInfo;
                } else if (resourceInfo.paramInfo != null) {
                    final Object param = resourceInfo.paramType.fromString("_".equals(pathPart) ? requestEnvelope.getParams(paramIndex) : pathPart);
                    paramIndex++;
                    resourceInfo = resourceInfo.paramInfo;
                    params.add(param);
                } else {
                    throw new ClientMethodNotAllowedException(String
                        .format("Method [%s] not allowed for pathParams %s",
                            requestEnvelope.getMethod(),
                            ServicePathHolder.of(requestEnvelope.getPathList())));
                }
            } catch (final RuntimeException e) {
                throw new ServerErrorException(String.format("Error matching [%s] in [%s]", pathPart, path), e);
            }
        }
        
        final Class<?> resourceType = resourceInfo.resourceMethod.getReturnType();
        return tuple(resourceType, params);
    }
    
    private Tuple4<SkeletonResourceMethodInfo<?, ?, ?>, ServiceOperationHeader<?>, MessageLite, Intent> validateAndExtractFromEnvelope(final Class<?> resourceType,
                                                                                                                                       final RequestEnvelope requestEnvelope) {
        return validateAndExtractFromEnvelope(proxy.resourceMethodMap.get(resourceType), requestEnvelope);
    }
    
    private Tuple4<SkeletonResourceMethodInfo<?, ?, ?>, ServiceOperationHeader<?>, MessageLite, Intent> validateAndExtractFromEnvelope(final SkeletonResourceInfo resourceInfo,
                                                                                                                                       final RequestEnvelope requestEnvelope) {
        return validateAndExtractFromEnvelope(resourceInfo.methodMap, requestEnvelope);
    }
    
    private Tuple4<SkeletonResourceMethodInfo<?, ?, ?>, ServiceOperationHeader<?>, MessageLite, Intent> validateAndExtractFromEnvelope(final Map<String, SkeletonResourceMethodInfo<?, ?, ?>> methodMap,
                                                                                                                                       final RequestEnvelope requestEnvelope) {
        try {
            final SkeletonResourceMethodInfo<?, ?, ?> methodInfo = methodMap != null
                ? methodMap.get(requestEnvelope.getMethod()) : null;
            if (methodInfo == null) {
                throw new ClientMethodNotAllowedException(String.format("Method [%s] not allowed for pathParams %s",
                    requestEnvelope.getMethod(),
                    ServicePathHolder.of(requestEnvelope.getPathList())));
            }
            
            final MessageLite parsedContext = MessageHelper.parse(proxy.contextType, requestEnvelope.getContext());
            final ServiceOperationHeader<?> header = ServiceOperationHeader.from(requestEnvelope, parsedContext);
            final MessageLite request = (methodInfo.requestType == null)
                ? null
                : MessageHelper.parse(methodInfo.requestType,
                    requestEnvelope.getPayload(),
                    requestEnvelope.getJsonPayload());
            final ServicePathHolder path = ServicePathHolder.of(requestEnvelope.getPathList(),
                requestEnvelope.getParamsList());
            final Intent intent = new Intent(requestEnvelope.getIntentId(),
                requestEnvelope.getMethod() + " " + path,
                request == null ? 0L : MessageHelper.fingerprint(request));
            
            proxy.validateAndCheckSecurity(methodInfo, header, request);
            
            return tuple(methodInfo, header, request, intent);
            
        } catch (final InvalidProtocolBufferException e) {
            throw new ClientInvalidRequestException(e);
        }
    }
    
    public <C extends MessageLite, X, E extends Exception> X aroundAsync(final ServiceHeader<C> header,
                                                                         final Intent intent,
                                                                         final CallableThrows<X, E> callable) throws E {
        return INTENT.exec(intent, () -> proxy.asyncInterceptor.aroundAsync(header, () -> proxy.handlerStrategy.aroundAsync(() -> serviceSkeleton.aroundAsync(callable))));
    }
    
    private final class ServiceResponseWrapperImpl<R, C> implements ServiceResponseWrapper<R, C> {
        
        private final RequestEnvelope requestEnvelope;
        private final Class<R> responseType;
        private final Class<C> conflictType;
        private final AtomicBoolean used = new AtomicBoolean(false);
        
        /**
         * @param responseType the response class or null if the type is Nil
         * @param conflictType the conflict class or null if the type is Nil
         */
        private ServiceResponseWrapperImpl(final RequestEnvelope requestEnvelope,
                                           final Class<R> responseType,
                                           final Class<C> conflictType) {
            
            this.requestEnvelope = requestEnvelope;
            this.responseType = responseType;
            this.conflictType = conflictType;
        }
        
        @Override
        public ResponseEnvelope result(final Object result) {
            if (!used.compareAndSet(false, true)) {
                throw new IllegalStateException("result(), conflict() or error() already called");
            }
            
            proxy.handlerStrategy.getRequestStrategy().finishMessage();
            if (responseType != null) {
                try {
                    if (result == null) {
                        return wrapError(requestEnvelope,
                            new ServerErrorException("Expecting non-null result but was null"));
                    }
                    if (!ClassUtil.isInstanceOf(result, responseType)) {
                        return wrapError(requestEnvelope,
                            new ServerErrorException("Unexpected result ["
                                + result
                                + "] when expecting ["
                                + responseType
                                + "]"));
                    }
                    final MessageLite resultMessage = (MessageLite) result;
                    final MessageValidation validation = MessageValidator.validate(resultMessage);
                    if (!validation.getInvalid()) {
                        final ByteString payload = !requestEnvelope.getJsonPayload()
                            ? resultMessage.toByteString() : ByteString.copyFromUtf8(MessageHelper.json(resultMessage));
                        return newResponseEnvelopeBuilder(requestEnvelope)
                            .setStatusCode(ResponseStatusCode.OK)
                            .setPayload(payload)
                            .build();
                    } else {
                        return wrapError(requestEnvelope, new ServerErrorException(validation));
                    }
                } catch (final RuntimeException e) {
                    LOG.error("Unexpected exception when processing result", e);
                    return wrapError(requestEnvelope, new ServerErrorException(e));
                }
            } else {
                if ((result != null) && !ClassUtil.isInstanceOf(result, Nil.class)) {
                    return wrapError(requestEnvelope,
                        new ServerErrorException("Unexpected result [" + result + "] when expecting [Nil]"));
                }
                return newResponseEnvelopeBuilder(requestEnvelope).setStatusCode(ResponseStatusCode.OK).build();
            }
        }
        
        @Override
        public ResponseEnvelope conflict(final Object conflict) {
            if (!used.compareAndSet(false, true)) {
                throw new IllegalStateException("result(), conflict() or error() already called");
            }
            
            proxy.handlerStrategy.getRequestStrategy().finishMessage();
            if (conflictType != null) {
                try {
                    if (conflict == null) {
                        return wrapError(requestEnvelope,
                            new ServerErrorException("Expecting non-null conflict but was null"));
                    }
                    if (!ClassUtil.isInstanceOf(conflict, conflictType)) {
                        return wrapError(requestEnvelope,
                            new ServerErrorException("Unexpected conflict ["
                                + conflict
                                + "] when expecting ["
                                + conflictType
                                + "]"));
                    }
                    final MessageLite conflictMessage = (MessageLite) conflict;
                    final MessageValidation validation = MessageValidator.validate(conflictMessage);
                    if (!validation.getInvalid()) {
                        final ByteString payload = !requestEnvelope.getJsonPayload()
                            ? conflictMessage.toByteString()
                            : ByteString.copyFromUtf8(MessageHelper.json(conflictMessage));
                        
                        return newResponseEnvelopeBuilder(requestEnvelope)
                            .setStatusCode(ResponseStatusCode.CLIENT_CONFLICT)
                            .setStatusMessage(getConflictStatusMessage(conflictMessage))
                            .setPayload(payload)
                            .build();
                    } else {
                        return wrapError(requestEnvelope, new ServerErrorException(MessageHelper.json(validation)));
                    }
                } catch (final RuntimeException e) {
                    LOG.error("Unexpected exception when processing conflict", e);
                    return wrapError(requestEnvelope, new ServerErrorException(e));
                }
            } else {
                if ((conflict != null) && !ClassUtil.isInstanceOf(conflict, Nil.class)) {
                    return wrapError(requestEnvelope,
                        new ServerErrorException("Unexpected conflict   [" + conflict + "] when expecting [Nil]"));
                }
                return newResponseEnvelopeBuilder(requestEnvelope)
                    .setStatusCode(ResponseStatusCode.CLIENT_CONFLICT)
                    .build();
            }
        }
        
        @Override
        public ResponseEnvelope error(final Throwable t) {
            if (used.compareAndSet(false, true)) {
                proxy.handlerStrategy.getRequestStrategy().finishMessage();
                ServiceException ex;
                try {
                    ex = proxy.serviceExceptionTranslator.apply(t);
                } catch (final ServiceException e) {
                    ex = e;
                } catch (final RuntimeException e) {
                    ex = new ServerErrorException(t);
                }
                return wrapError(requestEnvelope, ex);
            } else {
                throw new IllegalStateException("result(), conflict() or error() already called");
            }
        }
        
        @Override
        protected void finalize() throws Throwable {
            // just in case
            if (used.compareAndSet(false, true)) {
                proxy.handlerStrategy.getRequestStrategy().finishMessage();
                LOG.warn("ServiceResponseWrapperImpl was not consumed for [{}:{}]",
                    requestEnvelope.getCorrelationId(),
                    requestEnvelope.getCallRef());
            }
            super.finalize();
        }
        
    }
    
}
