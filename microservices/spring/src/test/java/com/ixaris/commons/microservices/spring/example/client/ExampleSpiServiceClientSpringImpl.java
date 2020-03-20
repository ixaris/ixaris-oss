package com.ixaris.commons.microservices.spring.example.client;

import static com.ixaris.commons.async.lib.Async.all;
import static com.ixaris.commons.async.lib.Async.awaitExceptions;
import static com.ixaris.commons.async.lib.Async.result;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import org.awaitility.Awaitility;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.microservices.spring.example.Example.ExampleContext;
import com.ixaris.commons.microservices.spring.example.Example.ExampleRequest;
import com.ixaris.commons.microservices.spring.example.Example.ExampleResponse;
import com.ixaris.commons.microservices.spring.examplespi.client.ExampleSpiStub;
import com.ixaris.commons.microservices.spring.examplespi.resource.ExampleSpiResource.ExampleSpiErrorException;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;

@Component
public final class ExampleSpiServiceClientSpringImpl {
    
    private final ExampleSpiStub exampleSpi;
    
    private final Map<String, Integer> events = new HashMap<>();
    
    @Autowired
    ExampleSpiServiceClientSpringImpl(final ExampleSpiStub stub) {
        this.exampleSpi = stub;
        stub.watch((h, e) -> {
            synchronized (events) {
                events.compute(h.getServiceKey(), (k, v) -> v == null ? 1 : v + 1);
            }
            return result();
        });
    }
    
    public Async<List<ExampleResponse>> doSomethingOnAll(final int id,
                                                         final int timeout,
                                                         final Integer serverSleepDuration,
                                                         final boolean serviceThrowException) {
        
        final ExampleRequest.Builder requestBuilder = ExampleRequest.newBuilder().setId(id);
        if (serverSleepDuration != null) {
            requestBuilder.setSleepDuration(serverSleepDuration);
        }
        requestBuilder.setThrowException(serviceThrowException);
        
        final Set<String> keys = exampleSpi._keys();
        if (keys.isEmpty()) {
            throw new IllegalStateException("Was expecting at least one service SPI implementation but found none");
        }
        System.out.println("Available SPIs: " + keys);
        
        List<Async<ExampleResponse>> promises = new ArrayList<>(keys.size());
        for (final String key : keys) {
            System.out.println("Calling operation on SPI: " + key);
            final ServiceOperationHeader<ExampleContext> context = ServiceOperationHeader.newBuilder(System.currentTimeMillis(),
                MultiTenancy.SYSTEM_TENANT,
                ExampleContext.newBuilder().build())
                .withTimeout(timeout)
                .withTargetServiceKey(key)
                .build();
            promises.add(doSomething(context, requestBuilder.build()));
        }
        
        Awaitility
            .await()
            .until(() -> {
                synchronized (events) {
                    boolean ok = true;
                    for (final String key : keys) {
                        ok &= events.getOrDefault(key, 0) == 1;
                    }
                    return ok;
                }
            });
        
        return all(promises);
    }
    
    private Async<ExampleResponse> doSomething(final ServiceOperationHeader<ExampleContext> context, final ExampleRequest request) {
        try {
            return awaitExceptions(exampleSpi.op(context, request));
        } catch (final ExampleSpiErrorException conflict) {
            throw new IllegalStateException(conflict);
        }
    }
    
}
