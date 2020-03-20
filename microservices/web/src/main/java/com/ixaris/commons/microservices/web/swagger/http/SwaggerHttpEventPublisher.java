//package com.ixaris.commons.microservices.web.swagger.http;
//
//import com.ixaris.commons.async.lib.Async;
//import com.ixaris.commons.async.lib.FutureAsync;
//import com.ixaris.commons.async.reactive.AbstractUnboundedSubscriber;
//import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
//import com.ixaris.commons.microservices.lib.common.exception.ServiceException;
//import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;
//import com.ixaris.commons.microservices.web.swagger.events.SwaggerEvent;
//import com.ixaris.commons.microservices.web.swagger.events.SwaggerEventPublisher;
//import com.ixaris.commons.protobuf.lib.MessageHelper;
//import io.micronaut.core.type.Argument;
//import io.micronaut.http.HttpRequest;
//import io.micronaut.http.MutableHttpRequest;
//import io.micronaut.http.client.DefaultHttpClient;
//import io.micronaut.http.client.HttpClient;
//import java.net.MalformedURLException;
//import java.net.URL;
//import java.util.function.Consumer;
//import org.reactivestreams.Publisher;
//
//public final class SwaggerHttpEventPublisher implements SwaggerEventPublisher {
//    
//    private final String name;
//    private final String baseUrl;
//    private final Consumer<MutableHttpRequest<?>> requestConsumer;
//    private final HttpClient httpClient;
//    
//    public SwaggerHttpEventPublisher(
//        final String name, final String baseUrl, final Consumer<MutableHttpRequest<?>> requestConsumer
//    ) {
//        this.name = name;
//        this.baseUrl = baseUrl;
//        this.requestConsumer = requestConsumer;
//        try {
//            this.httpClient = new DefaultHttpClient(new URL(baseUrl));
//        } catch (final MalformedURLException e) {
//            throw new IllegalStateException(e);
//        }
//    }
//    
//    @Override
//    public String getName() {
//        return name;
//    }
//    
//    @Override
//    public Async<Void> publishEvent(final SwaggerEvent swaggerEvent) {
//        final String path = ServicePathHolder.of(swaggerEvent.getEventEnvelope().getPathList()).toString();
//        final MutableHttpRequest<String> request = HttpRequest.POST(
//            baseUrl + "/" + swaggerEvent.getEventEnvelope().getServiceName() + (path.isEmpty() ? "" : "/" + path),
//            MessageHelper.json(swaggerEvent.getEvent())
//        )
//            .contentEncoding("UTF-8")
//            .contentType("application/json");
//        requestConsumer.accept(request);
//        
//        return fromPublisher(httpClient.exchange(request, Argument.of(String.class))).map(r -> {
//            final ResponseStatusCode statusCode;
//            if (r.code() == 204) {
//                statusCode = ResponseStatusCode.OK;
//            } else {
//                final ResponseStatusCode tmp = ResponseStatusCode.forNumber(r.code());
//                statusCode = tmp != null ? tmp : ResponseStatusCode.SERVER_ERROR;
//            }
//            
//            if (statusCode == ResponseStatusCode.OK) {
//                return null;
//            } else {
//                throw ServiceException.from(statusCode, "Event webhook returned " + r.code(), null, false);
//            }
//        });
//    }
//    
//    private static <T> Async<T> fromPublisher(final Publisher<T> publisher) {
//        final FutureAsync<T> future = new FutureAsync<>();
//        publisher.subscribe(new AbstractUnboundedSubscriber<T>() {
//            
//            @Override
//            public void onNext(final T value) {
//                future.complete(value);
//            }
//            
//            @Override
//            public void onError(final Throwable throwable) {
//                future.completeExceptionally(throwable);
//            }
//            
//        });
//        return future;
//    }
//    
//}
