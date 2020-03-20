package com.ixaris.commons.microservices.web.swagger.http;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.protobuf.lib.MessageValidator.fieldSnakeToCamelCasePreservingMapKey;
import static java.nio.charset.StandardCharsets.UTF_8;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.exception.ClientInvalidRequestException;
import com.ixaris.commons.microservices.lib.common.exception.ServiceException;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ClientInvalidRequest;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.web.AbstractHttpEndpoint;
import com.ixaris.commons.microservices.web.HttpRequest;
import com.ixaris.commons.microservices.web.HttpResponse;
import com.ixaris.commons.microservices.web.HttpStatus;
import com.ixaris.commons.microservices.web.MediaType;
import com.ixaris.commons.microservices.web.swagger.ScslSwaggerRouter;
import com.ixaris.commons.microservices.web.swagger.operations.SwaggerRequest;
import com.ixaris.commons.microservices.web.swagger.operations.SwaggerResponse;
import com.ixaris.commons.misc.lib.exception.ExceptionUtil;
import com.ixaris.commons.protobuf.lib.MessageHelper;

import valid.Valid.FieldValidationErrors;
import valid.Valid.MessageValidation;

/**
 * HTTP Servlet that offers all the endpoints explained in Swagger and depends on {@link ScslSwaggerRouter} to interpret
 * which APIs are available and to build the respective {@link RequestEnvelope}
 */
public abstract class AbstractSwaggerApiEndpoint extends AbstractHttpEndpoint {
    
    public static final String CREATE_ID_HEADER = "create-id";
    
    private final ScslSwaggerRouter scslSwaggerRouter;
    private final String prefix;
    private final String wwwAuthenticate;
    
    public AbstractSwaggerApiEndpoint(final ScslSwaggerRouter scslSwaggerRouter,
                                      final String prefix,
                                      final String wwwAuthenticate) {
        this.scslSwaggerRouter = scslSwaggerRouter;
        this.prefix = prefix;
        this.wwwAuthenticate = wwwAuthenticate;
    }
    
    @Override
    protected final Async<HttpResponse<String>> invoke(final HttpRequest<String> request) {
        HttpResponse<String> response;
        SwaggerRequest swaggerRequest = null;
        try {
            swaggerRequest = await(scslSwaggerRouter.buildRequest(request, prefix));
            final SwaggerResponse swaggerResponse = await(scslSwaggerRouter.invoke(swaggerRequest));
            if (swaggerResponse.getThrowable() != null) {
                throw ExceptionUtil.sneakyThrow(swaggerResponse.getThrowable());
            }
            
            if (swaggerResponse.getConflictException() != null) {
                response = HttpResponse
                    .<String>status(HttpStatus.CONFLICT)
                    .characterEncoding(UTF_8)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(MessageHelper.json(swaggerResponse.getConflictException().getConflict()));
            } else {
                response = (swaggerResponse.getResponse() != null)
                    ? HttpResponse
                        .<String>ok()
                        .characterEncoding(UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(MessageHelper.json(swaggerResponse.getResponse()))
                    : HttpResponse.noContent(); // In case response is empty, set 204 status code
            }
            if (swaggerRequest.getOperation().create) {
                response.getHeaders().add(CREATE_ID_HEADER, swaggerRequest.getOperation().params.getLast());
            }
        } catch (final ServiceException e) {
            response = HttpResponse.status(HttpStatus.valueOf(e.getStatusCode().getNumber()));
            switch (e.getStatusCode()) {
                case CLIENT_INVALID_REQUEST:
                    response
                        .characterEncoding(UTF_8)
                        .contentType(MediaType.APPLICATION_JSON)
                        .body(buildClientInvalidRequestResponse((ClientInvalidRequestException) e));
                    break;
                case CLIENT_UNAUTHORISED:
                    if (wwwAuthenticate != null) {
                        response.getHeaders().add("www-authenticate", wwwAuthenticate);
                    }
                    break;
            }
        }
        
        if (swaggerRequest != null) {
            // If call ref was not present, we return the intent id that was generated as the CALL_REF_HEADER
            response.getHeaders().add(CALL_REF_HEADER, swaggerRequest.getOperation().callRef);
        } else {
            final String callRef = request.getHeaders().get(CALL_REF_HEADER);
            if (callRef != null) {
                response.getHeaders().add(CALL_REF_HEADER, callRef);
            }
        }
        return result(response);
    }
    
    private static String buildClientInvalidRequestResponse(final ClientInvalidRequestException invalidRequest) {
        final ClientInvalidRequest.Builder clientInvalidRequest = ClientInvalidRequest.newBuilder();
        final MessageValidation messageValidation = invalidRequest.getMessageValidation();
        if (messageValidation == null || !messageValidation.getInvalid()) {
            clientInvalidRequest.setMessage(invalidRequest.getMessage());
        } else {
            // convert snake_case fields to camelCase for json response
            final MessageValidation.Builder jsonMessageValidation = MessageValidation.newBuilder();
            jsonMessageValidation.setInvalid(messageValidation.getInvalid());
            for (final FieldValidationErrors f : messageValidation.getFieldsList()) {
                jsonMessageValidation.addFields(
                    FieldValidationErrors.newBuilder()
                        .setName(fieldSnakeToCamelCasePreservingMapKey(f.getName()))
                        .addAllErrors(f.getErrorsList()));
            }
            
            clientInvalidRequest
                .setMessage("JSON request violated validation rules")
                .setValidation(jsonMessageValidation);
        }
        
        return MessageHelper.json(clientInvalidRequest);
    }
    
}
