package com.ixaris.commons.microservices.web.dynamic;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;
import static java.nio.charset.StandardCharsets.UTF_8;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.ByteString;
import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.client.dynamic.DynamicServiceFactory;
import com.ixaris.commons.microservices.lib.client.proxy.UntypedOperationInvoker;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.common.exception.ServiceException;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.DefaultError;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;
import com.ixaris.commons.microservices.web.AbstractHttpEndpoint;
import com.ixaris.commons.microservices.web.HttpRequest;
import com.ixaris.commons.microservices.web.HttpResponse;
import com.ixaris.commons.microservices.web.HttpStatus;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;
import com.ixaris.commons.protobuf.lib.MessageHelper;

/**
 * HTTP Servlet that acts as an HTTP proxy to other microservices. In this way, microservices which otherwise do not
 * offer an HTTP endpoint can still be queried via HTTP.
 */
public abstract class AbstractDynamicHttpEndpoint<C extends MessageLite, R extends ResolvedOperation<C>> extends AbstractHttpEndpoint {
    
    private static final Logger LOG = LoggerFactory.getLogger(AbstractDynamicHttpEndpoint.class);
    
    private final OperationResolver<C, R> operationResolver;
    private final DynamicServiceFactory dynamicServiceFactory;
    private final String prefix;
    private final boolean withServiceKey;
    
    public AbstractDynamicHttpEndpoint(final OperationResolver<C, R> operationResolver,
                                       final DynamicServiceFactory dynamicServiceFactory,
                                       final String prefix) {
        this(operationResolver, dynamicServiceFactory, prefix, false);
    }
    
    public AbstractDynamicHttpEndpoint(final OperationResolver<C, R> operationResolver,
                                       final DynamicServiceFactory dynamicServiceFactory,
                                       final String prefix,
                                       final boolean withServiceKey) {
        this.operationResolver = operationResolver;
        this.dynamicServiceFactory = dynamicServiceFactory;
        this.prefix = prefix;
        this.withServiceKey = withServiceKey;
    }
    
    @Override
    protected final Async<HttpResponse<String>> invoke(final HttpRequest<String> request) {
        try {
            final DecodedUrl decodedUrl = new DecodedUrl(request.getPath().substring(prefix.length()), withServiceKey);
            final MethodAndPayload methodAndPayload = new MethodAndPayload(request);
            final ResolvedOperation<C> operation = await(operationResolver.resolve(
                request, decodedUrl, methodAndPayload));
            ServicePathHolder path = ServicePathHolder.empty();
            ServicePathHolder params = ServicePathHolder.empty();
            for (final String pathPart : operation.path) {
                if (pathPart.startsWith("_")) {
                    path = path.push("_");
                    // Underscore is an illegal character in SCSL files. A segment starting with _ indicates a
                    // parameter. Thus we need to copy the parameter value. For segments that are just an underscore, we
                    // generate a unique identifier (typically used for create operations to avoid generating a unique
                    // id on the client side
                    if (pathPart.length() > 1) {
                        params = params.push(pathPart.substring(1));
                    } else {
                        params = params.push(String.valueOf(UniqueIdGenerator.generate()));
                    }
                } else {
                    path = path.push(pathPart);
                }
            }
            
            final RequestEnvelope.Builder requestEnvelope = RequestEnvelope.newBuilder()
                .setCorrelationId(UniqueIdGenerator.generate())
                .setCallRef(UniqueIdGenerator.generate())
                .setServiceName(operation.serviceName)
                .addAllPath(path)
                .addAllParams(params)
                .setMethod(operation.method)
                .setIntentId(operation.header.getIntentId())
                .setContext(operation.header.getContext().toByteString())
                .setTimeout(
                    (operation.header.getTimeout() > 0L)
                        ? operation.header.getTimeout() : dynamicServiceFactory.getDefaultTimeout())
                .setJsonPayload(true);
            
            if (operation.header.getTargetServiceKey() != null) {
                requestEnvelope.setServiceKey(operation.header.getTargetServiceKey());
            }
            if (operation.header.getTenantId() != null) {
                requestEnvelope.setTenantId(operation.header.getTenantId());
            }
            if (operation.payload != null) {
                requestEnvelope.setPayload(ByteString.copyFromUtf8(operation.payload));
            }
            
            LOG.debug("Invoking [{}] on path [{}]", operation.serviceName, path);
            final UntypedOperationInvoker processor = await(dynamicServiceFactory.getProxy(operation.serviceName));
            final ResponseEnvelope responseEnvelope = await(processor.invoke(requestEnvelope.build()));
            
            final HttpResponse<String> response = HttpResponse
                .<String>status(HttpStatus.valueOf(responseEnvelope.getStatusCode().getNumber()))
                .characterEncoding(UTF_8)
                .contentType("application/json");
            
            final String callRef = request.getHeaders().get(CALL_REF_HEADER);
            if (callRef != null) {
                response.getHeaders().add(CALL_REF_HEADER, callRef);
            }
            
            switch (responseEnvelope.getStatusCode()) {
                case OK:
                case CLIENT_CONFLICT:
                case CLIENT_INVALID_REQUEST:
                    response.body(responseEnvelope.getPayload().toStringUtf8());
                    break;
                case SERVER_ERROR:
                    final DefaultError.Builder defaultError = DefaultError.newBuilder();
                    if (callRef != null) {
                        defaultError.setMessage(String.format(
                            "Something went wrong. For more information contact support and quote reference %s", callRef));
                    } else {
                        defaultError.setMessage("Something went wrong. For more information contact support.");
                    }
                    response.body(MessageHelper.json(defaultError));
                    break;
                default:
                    break;
            }
            
            return result(response);
        } catch (final ServiceException e) {
            return result(HttpResponse.status(HttpStatus.valueOf(e.getStatusCode().getNumber())));
        }
    }
    
}
