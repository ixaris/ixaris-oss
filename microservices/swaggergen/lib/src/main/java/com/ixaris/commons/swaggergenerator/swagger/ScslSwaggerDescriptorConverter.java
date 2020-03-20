package com.ixaris.commons.swaggergenerator.swagger;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.Arrays;
import java.util.Collections;
import java.util.LinkedList;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Optional;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.Nil;
import com.ixaris.commons.microservices.lib.common.ServiceConstants;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.microservices.lib.common.annotations.ServicePath;
import com.ixaris.commons.microservices.lib.common.annotations.ServiceSecurity;
import com.ixaris.commons.microservices.lib.common.annotations.ServiceTags;
import com.ixaris.commons.microservices.lib.common.exception.ClientConflictException;
import com.ixaris.commons.microservices.scslparser.model.ScslDefinition;
import com.ixaris.commons.microservices.scslparser.model.ScslMethod;
import com.ixaris.commons.microservices.scslparser.model.ScslNode;
import com.ixaris.commons.microservices.scslparser.model.ScslParam;
import com.ixaris.commons.microservices.scslparser.model.ScslResource;
import com.ixaris.commons.microservices.web.swagger.exposed.ScslCreateMethodFilter;
import com.ixaris.commons.microservices.web.swagger.exposed.ScslMethodFilter;
import com.ixaris.commons.misc.lib.conversion.SnakeCaseHelper;
import com.ixaris.commons.protobuf.validationcodegen.CodegenHelper;

import io.swagger.models.HttpMethod;
import io.swagger.models.parameters.PathParameter;
import io.swagger.models.properties.BaseIntegerProperty;
import io.swagger.models.properties.StringProperty;

/**
 * Conversion helpers that can translate from SCSL definition to a more Swagger/HTTP oriented definition
 *
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public final class ScslSwaggerDescriptorConverter {
    
    private static final Logger LOG = LoggerFactory.getLogger(ScslSwaggerDescriptorConverter.class);
    
    private ScslSwaggerDescriptorConverter() {}
    
    /**
     * Given a parsed SCSL Definition, extract all HTTP methods described as {@link SwaggerOperationDescriptor} as a pre-processing stage to
     * generate a Swagger definition.
     *
     * @param scslDefinition The SCSL definition to use to generate swagger
     * @return A list of Swagger method descriptors that will define the full swagger spec
     */
    public static OperationAndEventDescriptors generateSwaggerDescriptorsFromScsl(final String serviceName,
                                                                                  final ScslDefinition scslDefinition,
                                                                                  final ScslMethodFilter methodFilter,
                                                                                  final ScslCreateMethodFilter createMethodFilter) {
        
        final Class<?> rootResourceClass = extractResourceInterfaceFromScslNode(scslDefinition);
        return processSkeletonType(rootResourceClass, serviceName, scslDefinition, methodFilter, createMethodFilter);
    }
    
    @SuppressWarnings("squid:S2658")
    private static Class<?> extractResourceInterfaceFromScslNode(final ScslDefinition scslDefinition) {
        final String resourceClassName = scslDefinition.getBasePackage()
            + ".resource."
            + CodegenHelper.convertNameToType(scslDefinition.getName())
            + "Resource";
        final Class<?> resourceType;
        try {
            resourceType = Class.forName(resourceClassName, false, Thread.currentThread().getContextClassLoader());
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException("Unable to resolve resource class", e);
        }
        return resourceType;
    }
    
    private static OperationAndEventDescriptors processSkeletonType(final Class<?> resourceType,
                                                                    final String serviceName,
                                                                    final ScslDefinition scslDefinition,
                                                                    final ScslMethodFilter methodFilter,
                                                                    final ScslCreateMethodFilter createMethodFilter) {
        return processSkeletonType(resourceType, serviceName, scslDefinition, 0, null, Collections.emptyList(), methodFilter, createMethodFilter);
    }
    
    @SuppressWarnings("unchecked")
    private static OperationAndEventDescriptors processSkeletonType(final Class<?> resourceType, // NOSONAR We can only fix this if we use the visitor pattern
                                                                    final String serviceName,
                                                                    final ScslNode<?> scslNode,
                                                                    final int index,
                                                                    final String parentSecurity,
                                                                    final List<String> parentTags,
                                                                    final ScslMethodFilter methodFilter,
                                                                    final ScslCreateMethodFilter createMethodFilter) {
        
        if (!resourceType.isInterface()) {
            throw new IllegalArgumentException("[" + resourceType + "] is not an interface");
        }
        
        final String resourceSecurity = Optional.ofNullable(resourceType.getAnnotation(ServiceSecurity.class))
            .map(ServiceSecurity::value)
            .orElse(parentSecurity);
        final List<String> resourceTags = Optional.ofNullable(resourceType.getAnnotation(ServiceTags.class))
            .map(a -> Collections.unmodifiableList(Arrays.asList(a.value())))
            .orElse(parentTags);
        
        final Map<String, ScslMethod> scslMethods = scslNode.getMethods().stream().collect(Collectors.toMap(ScslMethod::getName, m -> m));
        final OperationAndEventDescriptors operationAndEventDescriptorsToRender = new OperationAndEventDescriptors();
        
        for (final Method method : resourceType.getMethods()) {
            final Class<?>[] parameterTypes = method.getParameterTypes();
            
            if (isSubResourceMethod(method)) {
                // subresource, so there should be an equivalent method in handler
                // Sub resource may be parameterised or not .. if it is, this is distinguished by the number/type of
                // parameters
                final ScslNode<?> subResourceScslNode;
                if (parameterTypes.length == 0) {
                    subResourceScslNode = resolveScslResource(scslNode, method);
                } else if (parameterTypes.length == 1) {
                    subResourceScslNode = scslNode.getParam();
                } else {
                    throw new IllegalStateException("Too many parameters for path method [" + method + "] in [" + resourceType + ']');
                }
                
                operationAndEventDescriptorsToRender.addAll(processSkeletonType(method.getReturnType(),
                    serviceName,
                    subResourceScslNode,
                    index + 1,
                    resourceSecurity,
                    resourceTags,
                    methodFilter,
                    createMethodFilter));
                
            } else if (isWatchMethod(method)) {
                final List<String> methodTags = Optional.ofNullable(scslMethods.get(method.getName()).getTags()).orElse(resourceTags);
                
                if (!methodFilter.shouldProcess(methodTags, null)) {
                    continue;
                }
                
                // Resolve Scsl Method
                final ScslMethod scslMethod = scslMethods.get(SnakeCaseHelper.camelToSnakeCase(method.getName()));
                if (scslMethod == null) {
                    throw new IllegalStateException("Unable to match SCSL method with method name: "
                        + method.getName()
                        + " for scslNode "
                        + scslNode);
                }
                
                final ParameterizedType genericListenerType = (ParameterizedType) method.getGenericParameterTypes()[0];
                final Class<? extends MessageLite> eventType = (Class<? extends MessageLite>) genericListenerType.getActualTypeArguments()[1];
                
                operationAndEventDescriptorsToRender.addEventDescriptor(generateEventDescriptorFromMethod(serviceName, scslMethod, eventType));
                
            } else if (isOperationMethod(method, parameterTypes)) {
                final String methodSecurity = Optional.ofNullable(method.getAnnotation(ServiceSecurity.class))
                    .map(ServiceSecurity::value)
                    .orElse(resourceSecurity);
                final List<String> methodTags = Optional.ofNullable(method.getAnnotation(ServiceTags.class))
                    .map(a -> Collections.unmodifiableList(Arrays.asList(a.value())))
                    .orElse(resourceTags);
                
                if (!methodFilter.shouldProcess(methodTags, methodSecurity)) {
                    continue;
                }
                
                final boolean secured = !"UNSECURED".equals(methodSecurity);
                
                // Resolve Scsl Method
                final ScslMethod scslMethod = scslMethods.get(SnakeCaseHelper.camelToSnakeCase(method.getName()));
                if (scslMethod == null) {
                    throw new IllegalStateException("Unable to match SCSL method with method name: "
                        + method.getName()
                        + " for scslNode "
                        + scslNode);
                }
                
                final boolean hasRequest = parameterTypes.length == 2;
                if (hasRequest && !MessageLite.class.isAssignableFrom(parameterTypes[1])) {
                    throw new IllegalStateException("Invalid second parameter for operation method [" + method + "] in [" + resourceType + ']');
                }
                final Class<? extends MessageLite> requestType = hasRequest ? (Class<? extends MessageLite>) parameterTypes[1] : null;
                
                final Type[] returnTypeArgs = ((ParameterizedType) method.getGenericReturnType()).getActualTypeArguments();
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
                        throw new IllegalStateException("Invalid operation method [" + method + "] should only throw a conflict exception");
                    }
                } else {
                    throw new IllegalStateException("Invalid operation method [" + method + "] should only throw a conflict exception");
                }
                
                operationAndEventDescriptorsToRender.addOperationDescriptor(generateOperationDescriptorFromMethod(serviceName,
                    scslMethod,
                    methodTags,
                    createMethodFilter,
                    secured,
                    requestType,
                    responseType,
                    conflictType));
            }
        }
        
        return operationAndEventDescriptorsToRender;
    }
    
    private static ScslResource resolveScslResource(final ScslNode<?> scslNode, final Method method) {
        final String resourcePath = SnakeCaseHelper.camelToSnakeCase(method.getName());
        final Optional<ScslResource> maybeScslResource = scslNode
            .getSubResources()
            .stream()
            .filter(subResourceName -> Objects.equals(subResourceName.getName(), resourcePath))
            .findFirst();
        if (!maybeScslResource.isPresent()) {
            throw new IllegalStateException("Unable to match scsl resource [" + resourcePath + "] from generated interfaces");
        }
        return maybeScslResource.get();
    }
    
    private static SwaggerOperationDescriptor generateOperationDescriptorFromMethod(final String serviceName,
                                                                                    final ScslMethod scslMethod,
                                                                                    final List<String> methodTags,
                                                                                    final ScslCreateMethodFilter createMethodFilter,
                                                                                    final boolean secured,
                                                                                    final Class<? extends MessageLite> requestType,
                                                                                    final Class<? extends MessageLite> responseType,
                                                                                    final Class<? extends MessageLite> conflictType) {
        
        final String methodDescription = scslMethod.getDescription() == null ? "" : scslMethod.getDescription();
        final String scslPath = scslMethod.getPath();
        final String baseUrlPath = '/' + serviceName.toLowerCase() + scslPath;
        final String operationId = serviceName.toLowerCase()
            + scslPath.replaceAll("\\{", "").replaceAll("\\}", "").replaceAll("/", "_")
            + '_'
            + scslMethod.getName().toLowerCase();
        
        // An operation is considered to be a create operation if it is called "create" and performed on a parameterised
        // resource
        final boolean isCreate = createMethodFilter.isCreate(scslMethod.getName(), methodTags)
            && scslMethod.getParent() instanceof ScslParam;
        
        final String methodPath;
        final HttpMethod httpMethod;
        String resourceCreateParam = null;
        // The following is commented since HTTP GET do not support body so all operations are mapped to GET ending with
        // the verb
        // if (scslMethod.getName().equalsIgnoreCase("get")) {
        // httpMethod = HttpMethod.GET;
        // methodPath = baseUrlPath;
        // } else
        if (isCreate) {
            httpMethod = HttpMethod.POST;
            // Create needs to be POST on /managed_cards/_/method instead of /managed_cards/{id}
            methodPath = String.format("%s/_/%s", baseUrlPath.substring(0, baseUrlPath.lastIndexOf("/{")), scslMethod.getName());
            resourceCreateParam = ((ScslNode<ScslParam>) scslMethod.getParent()).getName();
        } else {
            httpMethod = HttpMethod.POST;
            methodPath = baseUrlPath + '/' + scslMethod.getName();
        }
        
        final List<PathParameter> swaggerPathParams = new LinkedList<>();
        final Map<String, ScslParam> scslPathParams = scslMethod.getPathParams();
        for (final Entry<String, ScslParam> scslParamEntry : scslPathParams.entrySet()) {
            
            // In the case of create methods, the parameter is replaced by an "_" to handle ID generation at the gateway
            if (isCreate && Objects.equals(scslParamEntry.getKey(), resourceCreateParam)) {
                continue;
            }
            
            final PathParameter pathParameter = resolvePathParameter(scslParamEntry);
            swaggerPathParams.add(pathParameter);
        }
        
        return new SwaggerOperationDescriptor(methodDescription,
            operationId,
            scslMethod.getName(),
            methodPath,
            httpMethod,
            isCreate,
            requestType,
            responseType,
            conflictType,
            swaggerPathParams,
            secured);
    }
    
    private static SwaggerEventDescriptor generateEventDescriptorFromMethod(final String serviceName,
                                                                            final ScslMethod scslMethod,
                                                                            final Class<? extends MessageLite> eventType) {
        
        final String methodDescription = scslMethod.getDescription() == null ? "" : scslMethod.getDescription();
        final String scslPath = scslMethod.getPath();
        final String baseUrlPath = '/' + serviceName.toLowerCase() + scslPath;
        final String eventId = serviceName.toLowerCase() + scslPath.replaceAll("\\{", "").replaceAll("\\}", "").replaceAll("/", "_") + "_watch";
        
        final String methodPath = baseUrlPath + '/' + scslMethod.getName();
        return new SwaggerEventDescriptor(methodDescription, eventId, methodPath, eventType);
    }
    
    private static PathParameter resolvePathParameter(final Entry<String, ScslParam> scslParamEntry) {
        final PathParameter pathParameter = new PathParameter();
        final ScslParam scslParam = scslParamEntry.getValue();
        pathParameter.setName(scslParamEntry.getKey());
        pathParameter.description(scslParam.getDescription());
        
        if (Objects.equals(scslParam.getType(), "string")) {
            pathParameter.setType(StringProperty.TYPE);
        } else if (Objects.equals(scslParam.getType(), "int64")
            || Objects.equals(scslParam.getType(), "uint64")
            || Objects.equals(scslParam.getType(), "sint64")) {
            pathParameter.setType(BaseIntegerProperty.TYPE);
            pathParameter.format("int64");
        } else if (Objects.equals(scslParam.getType(), "int32")
            || Objects.equals(scslParam.getType(), "uint32")
            || Objects.equals(scslParam.getType(), "sint32")) {
            pathParameter.setType(BaseIntegerProperty.TYPE);
            pathParameter.format("int32");
        } else {
            pathParameter.setType(StringProperty.TYPE);
            LOG.warn("Unsupported path parameter type [{}]. Defaulting to String.", scslParam.getType());
        }
        return pathParameter;
    }
    
    private static boolean isSubResourceMethod(final Method method) {
        return method.getAnnotation(ServicePath.class) != null;
    }
    
    private static boolean isWatchMethod(final Method method) {
        return method.getName().equals(ServiceConstants.WATCH_METHOD_NAME);
    }
    
    private static boolean isOperationMethod(final Method method, final Class<?>[] parameterTypes) {
        return Async.class.equals(method.getReturnType())
            && (parameterTypes.length >= 1)
            && ServiceOperationHeader.class.equals(parameterTypes[0])
            && (parameterTypes.length <= 2);
    }
    
}
