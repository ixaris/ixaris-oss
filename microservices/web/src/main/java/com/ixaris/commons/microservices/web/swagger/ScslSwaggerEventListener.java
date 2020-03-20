package com.ixaris.commons.microservices.web.swagger;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.misc.lib.exception.ExceptionUtil.sneakyThrow;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.client.ServiceEvent;
import com.ixaris.commons.microservices.lib.client.ServiceEventListener;
import com.ixaris.commons.microservices.lib.client.ServiceStubEvent;
import com.ixaris.commons.microservices.lib.common.ServiceEventHeader;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.common.exception.ClientTooManyRequestsException;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventAckEnvelope;
import com.ixaris.commons.microservices.web.swagger.events.SwaggerEvent;
import com.ixaris.commons.microservices.web.swagger.events.SwaggerEventAck;
import com.ixaris.commons.microservices.web.swagger.events.SwaggerEventPublisher;
import com.ixaris.commons.microservices.web.swagger.events.SwaggerEventPublisherParamResolver;

/**
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public final class ScslSwaggerEventListener<T> implements ServiceEventListener<MessageLite, MessageLite> {
    
    private final ServicePathHolder path;
    private final ScslSwaggerRouter scslSwaggerRouter;
    private final SwaggerEventPublisher<T> publisher;
    private final SwaggerEventPublisherParamResolver<T> publisherParamResolver;
    
    public ScslSwaggerEventListener(final ServicePathHolder path,
                                    final ScslSwaggerRouter scslSwaggerRouter,
                                    final SwaggerEventPublisher<T> publisher,
                                    final SwaggerEventPublisherParamResolver<T> publisherParamResolver) {
        this.path = path;
        this.scslSwaggerRouter = scslSwaggerRouter;
        this.publisher = publisher;
        this.publisherParamResolver = publisherParamResolver;
    }
    
    @Override
    public String getName() {
        return publisher.getName() + "/" + path.toString();
    }
    
    @Override
    public Async<Void> onEvent(final ServiceEventHeader<MessageLite> header, final MessageLite event) {
        final SwaggerEvent swaggerEvent = new SwaggerEvent(path, header, event);
        final SwaggerEventAck swaggerEventAck = await(scslSwaggerRouter.handle(swaggerEvent, publisher, publisherParamResolver));
        if (swaggerEventAck.getThrowable() == null) {
            return result();
        } else {
            throw sneakyThrow(swaggerEventAck.getThrowable());
        }
    }
    
}
