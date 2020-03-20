package com.ixaris.commons.microservices.web.swagger;

import static com.ixaris.commons.async.lib.Async.awaitExceptions;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.logging.AsyncLogging.CORRELATION;
import static com.ixaris.commons.microservices.lib.common.ServiceConstants.WATCH_METHOD_NAME;
import static com.ixaris.commons.microservices.lib.common.ServiceHeader.extractCorrelation;
import static com.ixaris.commons.microservices.web.swagger.ScslHelpers.extractSkeletonInterfaceFromScslDefinition;
import static com.ixaris.commons.microservices.web.swagger.ScslHelpers.extractStubInterfaceFromScslDefinition;
import static com.ixaris.commons.misc.lib.object.Tuple.tuple;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Set;
import java.util.function.Function;
import java.util.function.Supplier;

import com.google.common.collect.LinkedHashMultimap;
import com.google.common.collect.Multimap;
import com.google.protobuf.ByteString;
import com.google.protobuf.InvalidProtocolBufferException;
import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.AsyncLocal;
import com.ixaris.commons.async.lib.filter.AsyncFilterChain;
import com.ixaris.commons.microservices.lib.client.ServiceEventListener;
import com.ixaris.commons.microservices.lib.client.ServiceEventSubscription;
import com.ixaris.commons.microservices.lib.client.ServiceStub;
import com.ixaris.commons.microservices.lib.client.proxy.ServiceStubProxy;
import com.ixaris.commons.microservices.lib.client.proxy.StubResourceInfo;
import com.ixaris.commons.microservices.lib.client.proxy.StubResourceMethodInfo;
import com.ixaris.commons.microservices.lib.client.support.ServiceClientSupport;
import com.ixaris.commons.microservices.lib.common.Nil;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.common.exception.ClientConflictException;
import com.ixaris.commons.microservices.lib.common.exception.ClientInvalidRequestException;
import com.ixaris.commons.microservices.lib.common.exception.ClientMethodNotAllowedException;
import com.ixaris.commons.microservices.lib.common.exception.ClientNotFoundException;
import com.ixaris.commons.microservices.lib.common.exception.ServiceException;
import com.ixaris.commons.microservices.lib.service.ServiceSkeleton;
import com.ixaris.commons.microservices.lib.service.proxy.PathParamType;
import com.ixaris.commons.microservices.lib.service.proxy.ServiceSkeletonProxy;
import com.ixaris.commons.microservices.lib.service.proxy.SkeletonResourceInfo;
import com.ixaris.commons.microservices.lib.service.proxy.SkeletonResourceMethodInfo;
import com.ixaris.commons.microservices.lib.service.support.ServiceSupport;
import com.ixaris.commons.microservices.scslparser.model.ScslDefinition;
import com.ixaris.commons.microservices.web.HttpRequest;
import com.ixaris.commons.microservices.web.swagger.events.SwaggerEvent;
import com.ixaris.commons.microservices.web.swagger.events.SwaggerEventAck;
import com.ixaris.commons.microservices.web.swagger.events.SwaggerEventAck.Status;
import com.ixaris.commons.microservices.web.swagger.events.SwaggerEventFilter;
import com.ixaris.commons.microservices.web.swagger.events.SwaggerEventPublisher;
import com.ixaris.commons.microservices.web.swagger.events.SwaggerEventPublisherParamResolver;
import com.ixaris.commons.microservices.web.swagger.exposed.ExposedServicesSpec;
import com.ixaris.commons.microservices.web.swagger.exposed.ScslCreateMethodFilter;
import com.ixaris.commons.microservices.web.swagger.exposed.ScslMethodFilter;
import com.ixaris.commons.microservices.web.swagger.http.SwaggerDecodedRequestParts;
import com.ixaris.commons.microservices.web.swagger.http.SwaggerDecodedRequestParts.SpiChecker;
import com.ixaris.commons.microservices.web.swagger.operations.SwaggerOperationFilter;
import com.ixaris.commons.microservices.web.swagger.operations.SwaggerOperationResolver;
import com.ixaris.commons.microservices.web.swagger.operations.SwaggerRequest;
import com.ixaris.commons.microservices.web.swagger.operations.SwaggerResponse;
import com.ixaris.commons.misc.lib.conversion.SnakeCaseHelper;
import com.ixaris.commons.misc.lib.object.Ordered;
import com.ixaris.commons.misc.lib.object.Tuple2;
import com.ixaris.commons.misc.lib.object.Tuple3;
import com.ixaris.commons.protobuf.lib.MessageHelper;

/**
 * Router that is able to interpret URLs and request body and build the necessary RequestEnvelope. This class is
 * responsible of interpreting requests in the same way it is described in the Swagger contract by using the same
 * EXPOSED_SERVICES_FILENAME. A request should only be accepted if it is exposed based on the exposed services and the
 * necessary tags.
 *
 * <p>This class is also responsible for translating "_" in URL for create methods to a unique ID
 *
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public class ScslSwaggerRouter implements SpiChecker {
    
    private static final String EXPOSED_SERVICES_FILENAME = "exposed_services.txt";
    
    private final AsyncFilterChain<SwaggerRequest, SwaggerResponse> operationFilterChain;
    private final AsyncFilterChain<SwaggerEvent, SwaggerEventAck> eventFilterChain;
    private final SwaggerOperationResolver<?, ?> operationResolver;
    private final ScslMethodFilter exposedMethodFilter;
    private final ScslCreateMethodFilter createMethodFilter;
    
    private final Multimap<String, String> serviceNameMapping = LinkedHashMultimap.create();
    private final Map<String, ServiceSkeletonProxy<?>> skeletonByServiceName = new HashMap<>(); // NOSONAR
    private final Map<String, ServiceStubProxy<?>> stubByServiceName = new HashMap<>(); // NOSONAR
    private final Set<ServiceEventSubscription> subscriptions = new HashSet<>();
    
    public ScslSwaggerRouter(final SwaggerOperationResolver<?, ?> operationResolver,
                             final ServiceSupport serviceSupport,
                             final ServiceClientSupport serviceClientSupport,
                             final Set<? extends SwaggerOperationFilter> operationFilters,
                             final Set<? extends SwaggerEventFilter> eventFilters,
                             final String name) {
        this(operationResolver,
            serviceSupport,
            serviceClientSupport,
            operationFilters,
            eventFilters,
            () -> Thread
                .currentThread()
                .getContextClassLoader()
                .getResourceAsStream(name + "_" + EXPOSED_SERVICES_FILENAME));
    }
    
    public ScslSwaggerRouter(final SwaggerOperationResolver<?, ?> operationResolver,
                             final ServiceSupport serviceSupport,
                             final ServiceClientSupport serviceClientSupport,
                             final Set<? extends SwaggerOperationFilter> operationFilters,
                             final Set<? extends SwaggerEventFilter> eventFilters,
                             final Supplier<InputStream> exposedServicesSupplier) {
        this.operationResolver = operationResolver;
        final List<SwaggerOperationFilter> sortedOperationFilters = new ArrayList<>(operationFilters);
        sortedOperationFilters.sort(Ordered.COMPARATOR);
        operationFilterChain = new AsyncFilterChain<>(sortedOperationFilters);
        final List<SwaggerEventFilter> sortedEventFilters = new ArrayList<>(eventFilters);
        sortedEventFilters.sort(Ordered.COMPARATOR);
        eventFilterChain = new AsyncFilterChain<>(sortedEventFilters);
        
        final ExposedServicesSpec exposedServicesSpec;
        try (final InputStream is = exposedServicesSupplier.get()) {
            exposedServicesSpec = ExposedServicesSpec.fromInputStream(is);
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
        
        exposedMethodFilter = exposedServicesSpec.getExposedMethodFilter();
        createMethodFilter = exposedServicesSpec.getCreateMethodFilter();
        
        for (final Tuple2<ScslDefinition, String> scslToExposed : exposedServicesSpec.getExposedScslFiles()) {
            serviceNameMapping.put(scslToExposed.get2(), scslToExposed.get1().getName());
            
            final Class<? extends ServiceSkeleton> skeletonType = extractSkeletonInterfaceFromScslDefinition(scslToExposed.get1());
            final ServiceSkeletonProxy<?> skeleton = serviceSupport.getOrCreate(skeletonType);
            skeletonByServiceName.put(scslToExposed.get1().getName(), skeleton);
            
            final Class<? extends ServiceStub> stubType = extractStubInterfaceFromScslDefinition(scslToExposed.get1());
            final ServiceStubProxy<?> stub = serviceClientSupport.getOrCreate(stubType);
            stubByServiceName.put(scslToExposed.get1().getName(), stub);
        }
    }
    
    public synchronized <T> void subscribe(final SwaggerEventPublisher<T> publisher,
                                           final SwaggerEventPublisherParamResolver<T> paramResolver,
                                           final Function<ServiceEventListener<?, ?>, ServiceEventListener<?, ?>> listenerWrapperFunction) {
        for (final Entry<String, String> mapping : serviceNameMapping.entries()) {
            final String name = mapping.getValue();
            final ServiceSkeletonProxy<?> skeleton = skeletonByServiceName.get(name);
            final ServiceStubProxy<?> stub = stubByServiceName.get(name);
            final Set<StubResourceInfo> resources = resolveEventResources(skeleton, stub, exposedMethodFilter);
            for (final StubResourceInfo resource : resources) {
                subscriptions.add(stub.subscribe(listenerWrapperFunction.apply(new ScslSwaggerEventListener<>(resource.getPath(), this, publisher, paramResolver)), resource));
            }
        }
    }
    
    public synchronized void shutdown() {
        subscriptions.forEach(ServiceEventSubscription::cancel);
        subscriptions.clear();
    }
    
    @Override
    public boolean isSpi(final String serviceName) {
        final ServiceStubProxy<?> proxy = stubByServiceName.get(serviceName);
        return proxy != null && proxy.isSpi();
    }
    
    public Async<SwaggerRequest> buildRequest(final HttpRequest<?> httpRequest, final String prefix) {
        final SwaggerDecodedRequestParts parts = new SwaggerDecodedRequestParts(httpRequest, prefix, this);
        final Tuple3<String, ServiceSkeletonProxy<?>, MethodInfo> nameProxyAndMethodInfo = getNameAndProxyAndMethodInfo(parts.getServiceName(), parts.getPath(), parts.getMethod());
        final MethodInfo methodInfo = nameProxyAndMethodInfo.get3();
        final MessageLite request;
        if (methodInfo.stub.getRequestType() != null) {
            if (parts.getPayload() == null) {
                throw new ClientInvalidRequestException("Empty request body");
            }
            try {
                request = MessageHelper.parse(methodInfo.stub.getRequestType(), parts.getPayload());
            } catch (final InvalidProtocolBufferException e) {
                throw new ClientInvalidRequestException(
                    "The request body does not match the schema or is not valid JSON.", e);
            }
        } else {
            request = null;
        }
        
        final String serviceName = nameProxyAndMethodInfo.get1();
        
        return operationResolver
            .resolve(httpRequest, serviceName, parts.getServiceKey(), methodInfo.path, methodInfo.params, methodInfo.stub.getName(), methodInfo.create, request)
            .map(operation -> {
                nameProxyAndMethodInfo.get2().validateAndCheckSecurity(methodInfo.skeleton, operation.header, request);
                return new SwaggerRequest(operation, methodInfo.stub, request);
            });
    }
    
    public Async<SwaggerResponse> invoke(final SwaggerRequest request) {
        return AsyncLocal
            .with(CORRELATION, extractCorrelation(request.getOperation().header))
            .with(TENANT, request.getOperation().header.getTenantId())
            .exec(() -> operationFilterChain.exec(
                request,
                rq -> {
                    try {
                        return awaitExceptions(
                            stubByServiceName.get(rq.getOperation().serviceName)
                                .invoke(
                                    rq.getOperation().path,
                                    rq.getOperation().params,
                                    rq.getMethodInfo(),
                                    rq.getOperation().header,
                                    rq.getRequest())
                                .map(r -> new SwaggerResponse(r instanceof Nil ? null : (MessageLite) r, null, null)));
                    } catch (final ClientConflictException e) {
                        return result(new SwaggerResponse(null, e, null));
                    }
                },
                (rq, t) -> result(new SwaggerResponse(null, null, t))));
    }
    
    @SuppressWarnings("squid:S1452")
    public Async<?> invoke(final String serviceName,
                           final ServicePathHolder path,
                           final ServicePathHolder params,
                           final String method,
                           final ServiceOperationHeader<?> header,
                           final ByteString payload) throws ClientConflictException {
        final ServiceStubProxy<?> stub = stubByServiceName.get(serviceName);
        final StubResourceMethodInfo<?, ?, ?> methodInfo = resolveStubMethodInfo(stub, path, method);
        if (methodInfo == null) {
            throw new IllegalStateException(String.format("Method not found while retrying for %s %s %s", serviceName, path, method));
        }
        final MessageLite request;
        try {
            request = payload != null ? MessageHelper.parse(methodInfo.getRequestType(), payload) : null;
        } catch (final InvalidProtocolBufferException e) {
            throw new IllegalStateException(e);
        }
        return stub.invoke(path, params, methodInfo, header, request);
    }
    
    public <T> Async<SwaggerEventAck> handle(final SwaggerEvent swaggerEvent,
                                             final SwaggerEventPublisher<T> publisher,
                                             final SwaggerEventPublisherParamResolver<T> publisherParamResolver) {
        return eventFilterChain.exec(
            swaggerEvent,
            ev -> publisher.publishEvent(ev, publisherParamResolver).map(status -> new SwaggerEventAck(ev, status, null)),
            (ev, t) -> result(new SwaggerEventAck(ev, new Status(true, true), t)));
    }
    
    private Tuple3<String, ServiceSkeletonProxy<?>, MethodInfo> getNameAndProxyAndMethodInfo(final String requestServiceName,
                                                                                             final ServicePathHolder path,
                                                                                             final String method) throws ServiceException {
        final Collection<String> serviceNameToScslName = serviceNameMapping.get(requestServiceName);
        if (serviceNameToScslName.isEmpty()) {
            throw new ClientInvalidRequestException("Unknown Service [" + requestServiceName + "]");
        }
        for (final String serviceName : serviceNameToScslName) {
            final ServiceSkeletonProxy<?> skeleton = skeletonByServiceName.get(serviceName);
            final ServiceStubProxy<?> stub = stubByServiceName.get(serviceName);
            final MethodInfo methodInfo = resolveMethodInfo(
                skeleton, stub, path, method, exposedMethodFilter, createMethodFilter);
            if (methodInfo != null) {
                return tuple(serviceName, skeleton, methodInfo);
            }
        }
        
        // operation is not exposed or unmapped
        throw new ClientMethodNotAllowedException("Unmatched path " + path + " for service: " + requestServiceName);
    }
    
    private static boolean isMethodExposed(final ScslMethodFilter scslMethodFilter, final SkeletonResourceMethodInfo<?, ?, ?> resourceMethodInfo) {
        return scslMethodFilter.shouldProcess(resourceMethodInfo.getTags(), resourceMethodInfo.getSecurity());
    }
    
    private static final class MethodInfo {
        
        private final boolean create;
        private final ServicePathHolder path;
        private final ServicePathHolder params;
        private final SkeletonResourceMethodInfo<?, ?, ?> skeleton;
        private final StubResourceMethodInfo<?, ?, ?> stub;
        
        private MethodInfo(final boolean create,
                           final ServicePathHolder path,
                           final ServicePathHolder params,
                           final SkeletonResourceMethodInfo<?, ?, ?> skeleton,
                           final StubResourceMethodInfo<?, ?, ?> stub) {
            this.create = create;
            this.path = path;
            this.params = params;
            this.skeleton = skeleton;
            this.stub = stub;
        }
        
    }
    
    @SuppressWarnings("squid:MethodCyclomaticComplexity")
    private static MethodInfo resolveMethodInfo(final ServiceSkeletonProxy<?> skeleton,
                                                final ServiceStubProxy<?> stub,
                                                final ServicePathHolder path,
                                                final String method,
                                                final ScslMethodFilter scslMethodFilter,
                                                final ScslCreateMethodFilter createMethodFilter) {
        SkeletonResourceInfo skeletonResource = skeleton.getRootResourceInfo();
        StubResourceInfo stubResource = stub.getRootResourceInfo();
        ServicePathHolder params = ServicePathHolder.empty();
        try {
            for (int i = 0; i < path.size(); i++) {
                final String pathSegment = path.get(i);
                final SkeletonResourceInfo skeletonSubResource = skeletonResource.getResourceMap().get(pathSegment);
                final StubResourceInfo stubSubResource = stubResource
                    .getResourceMap()
                    .get(SnakeCaseHelper.snakeToCamelCase(pathSegment));
                if (skeletonSubResource == null) {
                    if (skeletonResource.getParamInfo() != null) {
                        // Skip Validation if last parameter is _ and it is of type long
                        if ((i < path.size() - 1)
                            || !pathSegment.equals("_")
                            || (skeletonResource.getParamType() != PathParamType.LONG)) {
                            validateParamType(path, skeletonResource.getParamType(), i);
                        }
                        
                        skeletonResource = skeletonResource.getParamInfo();
                        stubResource = stubResource.getParamInfo();
                        params = params.push(pathSegment);
                    } else {
                        skeletonResource = null;
                        stubResource = null;
                        break;
                    }
                } else {
                    skeletonResource = skeletonSubResource;
                    stubResource = stubSubResource;
                }
            }
        } catch (final RuntimeException e) {
            return null;
        }
        
        // Method names in skeletonResourceInfo are in snake-case (SCSL convention). The urls in Swagger are also
        // snake-case hence no need to convert to snake case check that method is exposed
        if (skeletonResource != null) {
            final SkeletonResourceMethodInfo<?, ?, ?> skeletonMethod = skeletonResource.getMethodMap().get(method);
            if ((skeletonMethod != null) && isMethodExposed(scslMethodFilter, skeletonMethod)) {
                final StubResourceMethodInfo<?, ?, ?> stubMethod = stubResource
                    .getMethodMap()
                    .get(SnakeCaseHelper.snakeToCamelCase(method));
                final boolean create = createMethodFilter.isCreate(stubMethod.getName(), skeletonMethod.getTags());
                if (create && (params.isEmpty() || !"_".equals(params.getLast()))) {
                    // Fail if create and last part of path is not "_"
                    throw new ClientMethodNotAllowedException("Method [" + method + "] can only be called with '_' as the parameter");
                }
                return new MethodInfo(create, stubResource.getPath(), params, skeletonMethod, stubMethod);
            }
        }
        return null;
    }
    
    /**
     * To be used when retrying an operation, where all the path validation has already been done
     */
    @SuppressWarnings("squid:S1452")
    public static StubResourceMethodInfo<?, ?, ?> resolveStubMethodInfo(final ServiceStubProxy<?> stub, final ServicePathHolder path, final String method) {
        StubResourceInfo stubResource = stub.getRootResourceInfo();
        try {
            for (int i = 0; i < path.size(); i++) {
                final String pathSegment = path.get(i);
                final StubResourceInfo stubSubResource = stubResource
                    .getResourceMap()
                    .get(SnakeCaseHelper.snakeToCamelCase(pathSegment));
                if (stubSubResource == null) {
                    if (stubResource.getParamInfo() != null) {
                        stubResource = stubResource.getParamInfo();
                    } else {
                        stubResource = null;
                        break;
                    }
                } else {
                    stubResource = stubSubResource;
                }
            }
        } catch (final RuntimeException e) {
            return null;
        }
        
        if (stubResource != null) {
            return stubResource.getMethodMap().get(SnakeCaseHelper.snakeToCamelCase(method));
        }
        return null;
    }
    
    private static void validateParamType(
                                          final ServicePathHolder path, final PathParamType type, final int i) throws ServiceException {
        try {
            switch (type) {
                case LONG:
                    Long.parseLong(path.get(i));
                    break;
                case INTEGER:
                    Integer.parseInt(path.get(i));
                    break;
                default:
            }
        } catch (final RuntimeException e) {
            throw new ClientInvalidRequestException(
                "Invalid parameter for path " + path + " at index [" + i + "]: " + e.getMessage(), e);
        }
    }
    
    private static Set<StubResourceInfo> resolveEventResources(
                                                               final ServiceSkeletonProxy<?> skeleton, final ServiceStubProxy<?> stub, final ScslMethodFilter scslMethodFilter) {
        return resolveEventResources(skeleton.getRootResourceInfo(), stub.getRootResourceInfo(), scslMethodFilter);
    }
    
    private static Set<StubResourceInfo> resolveEventResources(
                                                               final SkeletonResourceInfo skeletonResource,
                                                               final StubResourceInfo stubResource,
                                                               final ScslMethodFilter scslMethodFilter) {
        final Set<StubResourceInfo> eventResources = new HashSet<>();
        final SkeletonResourceMethodInfo<?, ?, ?> watchInfo = skeletonResource.getMethodMap().get(WATCH_METHOD_NAME);
        if ((watchInfo != null) && isMethodExposed(scslMethodFilter, watchInfo)) {
            eventResources.add(stubResource);
        }
        for (final Entry<String, SkeletonResourceInfo> subResourceEntry : skeletonResource.getResourceMap().entrySet()) {
            final SkeletonResourceInfo skeletonSubResource = subResourceEntry.getValue();
            if (skeletonSubResource.getParamInfo() == null) { // stop recursion if we find a parameter
                final StubResourceInfo stubSubResource = stubResource
                    .getResourceMap()
                    .get(SnakeCaseHelper.snakeToCamelCase(subResourceEntry.getKey()));
                eventResources.addAll(resolveEventResources(skeletonSubResource, stubSubResource, scslMethodFilter));
            }
        }
        return eventResources;
    }
    
}
