package com.ixaris.commons.microservices.web;

import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.IOException;

import javax.servlet.AsyncContext;
import javax.servlet.WriteListener;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;

import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.DefaultError;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;
import com.ixaris.commons.protobuf.lib.MessageHelper;

/**
 * A {@link WriteListener} that will asynchronously write response data when it is received from the services Retrieves the necessary data from
 * the {@link com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope}
 *
 * @author benjie.gatt
 */
final class ServiceResponseWriteListener implements WriteListener {
    
    private static final Logger LOG = LoggerFactory.getLogger(ServiceResponseWriteListener.class);
    private static final String CALLREF_HEADER = "X-callref";
    private static final String CODE = "code";
    private static final String REASON = "reason";
    private static final String EXTRA_INFO = "extraInfo";
    
    private final HttpResponse<?> operationResponse;
    private final HttpServletRequest request;
    private final HttpServletResponse response;
    private final AsyncContext asyncContext;
    
    ServiceResponseWriteListener(final HttpResponse<?> operationResponse,
                                 final HttpServletResponse response,
                                 final HttpServletRequest request,
                                 final AsyncContext asyncContext) {
        this.operationResponse = operationResponse;
        this.response = response;
        this.request = request;
        this.asyncContext = asyncContext;
    }
    
    @Override
    public void onWritePossible() {
        try {
            LOG.info("Writing [{}] response", operationResponse.getStatus().getCode());
            
            response.setContentType("application/json");
            response.setCharacterEncoding("UTF-8");
            
            final String callRefHeader = request.getHeader(CALLREF_HEADER);
            if (callRefHeader != null) {
                response.setHeader(CALLREF_HEADER, callRefHeader);
            } else {
                // TODO If callref was not present, ideally we retrieve the intent id that was generated and return it
                // as the CALLREF_HEADER
            }
            
            response.setStatus(operationResponse.getStatus().getCode());
            
            switch (operationResponse.getStatus()) {
                case OK:
                case CONFLICT:
                    response.getOutputStream().write(operationResponse.getBody().getBytes(UTF_8));
                    break;
                case BAD_REQUEST:
                    final String messageBody = buildClientInvalidRequestResponse(operationResponse);
                    response.getOutputStream().write(messageBody.getBytes(UTF_8));
                    break;
                case INTERNAL_SERVER_ERROR:
                    writeErrorCodeAndOptionalReasonWithCallRef(callRefHeader);
                    break;
                default:
                    writeErrorCodeWithoutReason();
                    break;
            }
        } catch (final IOException e) {
            LOG.error("Error in onWritePossible", e);
        } finally {
            asyncContext.complete();
        }
    }
    
    private void writeErrorCodeAndOptionalReasonWithCallRef(final String callRefHeader) throws IOException {
        final DefaultError.Builder defaultErrorModel = DefaultError.newBuilder().setCode(operationResponse.getStatus().name());
        
        if (callRefHeader != null) {
            defaultErrorModel.setMessage(String.format("Something went wrong. For more information contact support and quote reference %s",
                callRefHeader));
        } else {
            defaultErrorModel.setMessage("Something went wrong. For more information contact support.");
        }
        
        response.getOutputStream().write(MessageHelper.json(defaultErrorModel.build()).getBytes(UTF_8));
    }
    
    private void writeErrorCodeWithoutReason() throws IOException {
        final JsonObject defaultErrorModel = new JsonObject();
        defaultErrorModel.addProperty("code", operationResponse.getStatus().name());
        response.getOutputStream().write(defaultErrorModel.toString().getBytes(UTF_8));
    }
    
    @Override
    public void onError(final Throwable t) {
        LOG.error("Error in writeListener", t);
        asyncContext.complete();
    }
    
    private static String buildClientInvalidRequestResponse(final HttpResponse<?> responseEnvelope) {
        final JsonObject errorModel = new JsonObject();
        errorModel.addProperty(CODE, ResponseStatusCode.CLIENT_INVALID_REQUEST.name());
        
        errorModel.addProperty(REASON, "JSON Body syntax was malformed.");
        final JsonObject o = new JsonParser().parse(responseEnvelope.getBody()).getAsJsonObject();
        errorModel.add(EXTRA_INFO, o);
        
        return errorModel.toString();
    }
    
}
