package com.ixaris.commons.microservices.scsl2openapi.lib;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import com.ixaris.commons.misc.lib.object.ToStringUtil;

import io.swagger.v3.oas.models.PathItem.HttpMethod;
import io.swagger.v3.oas.models.parameters.PathParameter;

/**
 * Descriptor on which methods are to be exposed in Swagger. This is the result of the pre-processing stage when interpreting SCSL methods.
 *
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public final class OpenAPIOperationDescriptor {
    
    private final String description;
    private final String operationId;
    private final String methodName;
    private final String httpPath;
    private final HttpMethod httpMethod;
    private final boolean create;
    private final boolean secured;
    
    private final Class<?> requestType;
    private final Class<?> responseType;
    private final Class<?> conflictType;
    
    private final List<PathParameter> pathParams;
    
    OpenAPIOperationDescriptor(final String description,
                               final String operationId,
                               final String methodName,
                               final String httpPath,
                               final HttpMethod httpMethod,
                               final boolean create,
                               final Class<?> requestType,
                               final Class<?> responseType,
                               final Class<?> conflictType,
                               final List<PathParameter> pathParams,
                               final boolean secured) {
        
        this.description = description;
        this.operationId = operationId;
        this.methodName = methodName;
        this.httpPath = httpPath;
        this.httpMethod = httpMethod;
        this.create = create;
        this.requestType = requestType;
        this.responseType = responseType;
        this.conflictType = conflictType;
        this.pathParams = new ArrayList<>(pathParams);
        this.secured = secured;
    }
    
    public String getDescription() {
        return description;
    }
    
    public String getOperationId() {
        return operationId;
    }
    
    public String getMethodName() {
        return methodName;
    }
    
    public String getHttpPath() {
        return httpPath;
    }
    
    public HttpMethod getHttpMethod() {
        return httpMethod;
    }
    
    public boolean isCreate() {
        return create;
    }
    
    public Class<?> getRequestType() {
        return requestType;
    }
    
    public Class<?> getResponseType() {
        return responseType;
    }
    
    public Class<?> getConflictType() {
        return conflictType;
    }
    
    public List<PathParameter> getPathParams() {
        return Collections.unmodifiableList(pathParams);
    }
    
    @Override
    public String toString() {
        return ToStringUtil.of(this)
            .with("operationId", operationId)
            .with("httpPath", httpPath)
            .with("httpMethod", httpMethod)
            .toString();
    }
    
    public boolean isSecured() {
        return secured;
    }
}
