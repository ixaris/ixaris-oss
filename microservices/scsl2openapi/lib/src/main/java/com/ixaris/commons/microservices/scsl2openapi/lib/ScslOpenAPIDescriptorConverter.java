package com.ixaris.commons.microservices.scsl2openapi.lib;

import java.lang.reflect.Method;
import java.lang.reflect.ParameterizedType;
import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
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

import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.media.IntegerSchema;
import io.swagger.v3.oas.models.media.Schema;
import io.swagger.v3.oas.models.media.StringSchema;
import io.swagger.v3.oas.models.parameters.Parameter.StyleEnum;
import io.swagger.v3.oas.models.parameters.PathParameter;

/**
 * Conversion helpers that can translate from SCSL definition to a more Swagger/HTTP oriented definition
 *
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public final class ScslOpenAPIDescriptorConverter {
    
    private static final Logger LOG = LoggerFactory.getLogger(ScslOpenAPIDescriptorConverter.class);
    
    private ScslOpenAPIDescriptorConverter() {}
    
    /**
     * Given a parsed SCSL Definition, extract all HTTP methods described as {@link OpenAPIOperationDescriptor} as a pre-processing stage to
     * generate a Swagger definition.
     *
     * @param scslDefinition The SCSL definition to use to generate swagger
     * @return A list of Swagger method descriptors that will define the full swagger spec
     */
    public static OperationAndEventDescriptors generateSwaggerDescriptorsFromScsl(final String serviceName,
                                                                                  final ScslDefinition scslDefinition,
                                                                                  final ScslMethodFilter methodFilter,
                                                                                  final ScslCreateMethodFilter createMethodFilter) {
        final Class<?> stubType = extractStubTypeFromScslNode(scslDefinition);
        return processSkeletonType(stubType, serviceName, scslDefinition, methodFilter, createMethodFilter);
    }
    
    @SuppressWarnings("squid:S2658")
    private static Class<?> extractStubTypeFromScslNode(final ScslDefinition scslDefinition) {
        final String className = scslDefinition.getBasePackage()
            + ".client."
            + CodegenHelper.convertNameToType(scslDefinition.getName())
            + "Stub";
        final Class<?> type;
        try {
            type = Class.forName(className, false, Thread.currentThread().getContextClassLoader());
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException("Unable to resolve resource class", e);
        }
        return type;
    }
    
    private static OperationAndEventDescriptors processSkeletonType(final Class<?> stubType,
                                                                    final String serviceName,
                                                                    final ScslDefinition scslDefinition,
                                                                    final ScslMethodFilter methodFilter,
                                                                    final ScslCreateMethodFilter createMethodFilter) {
        return processSkeletonType(stubType,
            serviceName,
            scslDefinition.isSpi(),
            scslDefinition,
            0,
            null,
            Collections.emptyList(),
            methodFilter,
            createMethodFilter);
    }
    
    @SuppressWarnings("unchecked")
    private static OperationAndEventDescriptors processSkeletonType(final Class<?> stubType, // NOSONAR We can only fix this if we use the visitor pattern
                                                                    final String serviceName,
                                                                    final boolean spi,
                                                                    final ScslNode<?> scslNode,
                                                                    final int index,
                                                                    final String parentSecurity,
                                                                    final List<String> parentTags,
                                                                    final ScslMethodFilter methodFilter,
                                                                    final ScslCreateMethodFilter createMethodFilter) {
        
        if (!stubType.isInterface()) {
            throw new IllegalArgumentException("[" + stubType + "] is not an interface");
        }
        
        final String resourceSecurity = Optional.ofNullable(stubType.getAnnotation(ServiceSecurity.class))
            .map(ServiceSecurity::value)
            .orElse(parentSecurity);
        final List<String> resourceTags = Optional.ofNullable(stubType.getAnnotation(ServiceTags.class))
            .map(a -> Arrays.asList(a.value()))
            .orElse(parentTags);
        
        final Map<String, ScslMethod> scslMethods = scslNode.getMethods()
            .stream()
            .collect(Collectors.toMap(ScslMethod::getName, m -> m));
        final OperationAndEventDescriptors descriptorsToRender = new OperationAndEventDescriptors();
        
        for (final Method method : stubType.getMethods()) {
            final Class<?>[] parameterTypes = method.getParameterTypes();
            
            if (method.isDefault()) {
                // skip
            } else if (isSubResourceMethod(method)) {
                // subresource, so there should be an equivalent method in handler
                // Sub resource may be parameterised or not .. if it is, this is distinguished by the number/type of
                // parameters
                final ScslNode<?> subResourceScslNode;
                if (parameterTypes.length == 0) {
                    subResourceScslNode = resolveScslResource(scslNode, method);
                } else if (parameterTypes.length == 1) {
                    subResourceScslNode = scslNode.getParam();
                } else {
                    throw new IllegalStateException(String.format("Too many parameters for path method [%s] in [%s]", method, stubType));
                }
                
                descriptorsToRender.addAll(processSkeletonType(method.getReturnType(),
                    serviceName,
                    spi,
                    subResourceScslNode,
                    index + 1,
                    resourceSecurity,
                    resourceTags,
                    methodFilter,
                    createMethodFilter));
                
            } else if (isWatchMethod(method)) {
                final List<String> methodTags = Optional.ofNullable(method.getAnnotation(ServiceTags.class))
                    .map(a -> Arrays.asList(a.value()))
                    .orElse(resourceTags);
                
                if (!methodFilter.shouldProcess(methodTags, resourceSecurity)) {
                    continue;
                }
                
                // Resolve Scsl Method
                final ScslMethod scslMethod = scslMethods.get(SnakeCaseHelper.camelToSnakeCase(method.getName()));
                
                if (scslMethod == null) {
                    throw new IllegalStateException(String.format("Unable to match SCSL method with method name: %s for scslNode %s", method.getName(), scslNode));
                }
                
                final ParameterizedType genericListenerType = (ParameterizedType) method.getGenericParameterTypes()[0];
                final Class<? extends MessageLite> eventType = (Class<? extends MessageLite>) genericListenerType.getActualTypeArguments()[1];
                
                descriptorsToRender.addEventDescriptor(generateEventDescriptorFromMethod(serviceName, scslMethod, eventType));
                
            } else if (isOperationMethod(method, parameterTypes)) {
                final String methodSecurity = Optional.ofNullable(method.getAnnotation(ServiceSecurity.class))
                    .map(ServiceSecurity::value)
                    .orElse(resourceSecurity);
                final List<String> methodTags = Optional.ofNullable(method.getAnnotation(ServiceTags.class))
                    .map(a -> Arrays.asList(a.value()))
                    .orElse(resourceTags);
                
                if (!methodFilter.shouldProcess(methodTags, methodSecurity)) {
                    continue;
                }
                
                final boolean secured = !"UNSECURED".equals(methodSecurity);
                
                // Resolve Scsl Method
                final ScslMethod scslMethod = scslMethods.get(SnakeCaseHelper.camelToSnakeCase(method.getName()));
                if (scslMethod == null) {
                    throw new IllegalStateException(String.format("Unable to match SCSL method with method name: %s for scslNode %s", method.getName(), scslNode));
                }
                
                final boolean hasRequest = parameterTypes.length == 2;
                if (hasRequest && !MessageLite.class.isAssignableFrom(parameterTypes[1])) {
                    throw new IllegalStateException(String.format("Invalid second parameter for operation method [%s] in [%s]", method, stubType));
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
                
                descriptorsToRender.addOperationDescriptor(generateOperationDescriptorFromMethod(serviceName,
                    spi,
                    scslMethod,
                    methodTags,
                    createMethodFilter,
                    secured,
                    requestType,
                    responseType,
                    conflictType));
            }
        }
        
        return descriptorsToRender;
    }
    
    private static ScslResource resolveScslResource(final ScslNode<?> scslNode, final Method method) {
        final String resourcePath = SnakeCaseHelper.camelToSnakeCase(method.getName());
        final Optional<ScslResource> maybeScslResource = scslNode.getSubResources()
            .stream()
            .filter(subResourceName -> Objects.equals(subResourceName.getName(), resourcePath))
            .findFirst();
        if (!maybeScslResource.isPresent()) {
            throw new IllegalStateException(String.format("Unable to match scsl resource [%s] from generated interfaces", resourcePath));
        }
        return maybeScslResource.get();
    }
    
    private static OpenAPIOperationDescriptor generateOperationDescriptorFromMethod(final String serviceName,
                                                                                    final boolean spi,
                                                                                    final ScslMethod scslMethod,
                                                                                    final List<String> methodTags,
                                                                                    final ScslCreateMethodFilter createMethodFilter,
                                                                                    final boolean secured,
                                                                                    final Class<? extends MessageLite> requestType,
                                                                                    final Class<? extends MessageLite> responseType,
                                                                                    final Class<? extends MessageLite> conflictType) {
        final String methodDescription = scslMethod.getDescription() == null ? "" : scslMethod.getDescription();
        final String scslPath = scslMethod.getPath();
        final String baseUrlPath = '/' + serviceName.toLowerCase() + (spi ? "/{spi_key}" : "") + scslPath;
        final String operationId = serviceName.toLowerCase()
            + scslPath.replaceAll("\\{", "").replaceAll("}", "").replaceAll("/", "_")
            + '_'
            + scslMethod.getName().toLowerCase();
        
        // An operation is a create operation if it is called "create" and performed on a parameterised resource
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
        
        final Map<String, ScslParam> scslPathParams = scslMethod.getPathParams();
        final List<PathParameter> swaggerPathParams;
        if (spi) {
            swaggerPathParams = new ArrayList<>(scslMethod.getPathParams().size() + 1);
            final PathParameter pathParameter = new PathParameter();
            pathParameter.name("spi_key").style(StyleEnum.SIMPLE).explode(false).schema(new StringSchema());
            swaggerPathParams.add(pathParameter);
        } else {
            swaggerPathParams = new ArrayList<>(scslMethod.getPathParams().size());
        }
        for (final Entry<String, ScslParam> scslParamEntry : scslPathParams.entrySet()) {
            
            // In the case of create methods, the parameter is replaced by an "_" to handle ID generation at the gateway
            if (isCreate && Objects.equals(scslParamEntry.getKey(), resourceCreateParam)) {
                continue;
            }
            
            final PathParameter pathParameter = resolvePathParameter(scslParamEntry);
            swaggerPathParams.add(pathParameter);
        }
        
        return new OpenAPIOperationDescriptor(methodDescription,
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
    
    private static OpenAPIEventDescriptor generateEventDescriptorFromMethod(final String serviceName,
                                                                            final ScslMethod scslMethod,
                                                                            final Class<? extends MessageLite> eventType) {
        
        final String methodDescription = scslMethod.getDescription() == null ? "" : scslMethod.getDescription();
        final String scslPath = scslMethod.getPath();
        final String baseUrlPath = '/' + serviceName.toLowerCase() + scslPath;
        final String eventId = serviceName.toLowerCase()
            + scslPath.replaceAll("\\{", "").replaceAll("}", "").replaceAll("/", "_")
            + "_watch";
        
        final String methodPath = baseUrlPath + '/' + scslMethod.getName();
        return new OpenAPIEventDescriptor(methodDescription, eventId, methodPath, eventType);
    }
    
    private static PathParameter resolvePathParameter(final Entry<String, ScslParam> scslParamEntry) {
        final ScslParam scslParam = scslParamEntry.getValue();
        final PathParameter pathParameter = new PathParameter();
        pathParameter
            .name(scslParamEntry.getKey())
            .description(scslParam.getDescription())
            .style(StyleEnum.SIMPLE)
            .explode(false);
        
        final Schema schema;
        if (Objects.equals(scslParam.getType(), "int64")
            || Objects.equals(scslParam.getType(), "uint64")
            || Objects.equals(scslParam.getType(), "sint64")) {
            schema = new IntegerSchema();
            schema.setFormat("int64");
        } else if (Objects.equals(scslParam.getType(), "int32")
            || Objects.equals(scslParam.getType(), "uint32")
            || Objects.equals(scslParam.getType(), "sint32")) {
            schema = new IntegerSchema();
        } else {
            if (!Objects.equals(scslParam.getType(), "string")) {
                LOG.warn("Unsupported path parameter type [{}]. Defaulting to String.", scslParam.getType());
            }
            schema = new StringSchema();
        }
        pathParameter.setSchema(schema);
        
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
