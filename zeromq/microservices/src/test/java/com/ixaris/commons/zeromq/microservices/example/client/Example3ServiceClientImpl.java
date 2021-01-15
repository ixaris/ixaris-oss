package com.ixaris.commons.zeromq.microservices.example.client;

import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;

import java.util.concurrent.CompletionStage;

import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.multitenancy.test.TestTenants;
import com.ixaris.commons.zeromq.microservices.example.Example.ExampleContext;
import com.ixaris.commons.zeromq.microservices.example.Example.ExampleRequest;
import com.ixaris.commons.zeromq.microservices.example.Example.ExampleResponse;
import com.ixaris.commons.zeromq.microservices.example3.client.Example3Stub;

public final class Example3ServiceClientImpl {
    
    private final Example3Stub example;
    
    public Example3ServiceClientImpl(final Example3Stub example) {
        
        this.example = example;
    }
    
    public void benchmark(final int count) {
        
        long start = System.nanoTime();
        for (int i = 0; i < 10000; i++) {
            final ServiceOperationHeader<ExampleContext> header = TENANT.exec(TestTenants.DEFAULT, () -> ServiceOperationHeader.newBuilder(ExampleContext.newBuilder().build()).build());
            final CompletionStage<ExampleResponse> p = example.op(header, ExampleRequest.newBuilder().build());
            try {
                p.toCompletableFuture().join();
            } catch (Throwable e) {
                e.printStackTrace();
            }
        }
        System.out.println("Warm up took " + ((System.nanoTime() - start) / 1000000) + " ms");
        
        long minTime = Long.MAX_VALUE;
        long nanoTime = 0;
        for (int i = 0; i < count; i++) {
            start = System.nanoTime();
            final ServiceOperationHeader<ExampleContext> context =
                TENANT.exec(TestTenants.DEFAULT, () -> ServiceOperationHeader.newBuilder(ExampleContext.newBuilder().build()).build());
            final CompletionStage<ExampleResponse> p = example.op(context, ExampleRequest.newBuilder().build());
            try {
                p.toCompletableFuture().join();
            } catch (Throwable e) {
                e.printStackTrace();
            }
            long thisTime = System.nanoTime() - start;
            if (thisTime < minTime) {
                minTime = thisTime;
            }
            nanoTime += thisTime;
            if (i % 2500 == 0) {
                printProgress(nanoTime, i + 1);
            }
        }
        System.out.println("Minimum " + minTime + " ns");
        System.out.println("Average " + (nanoTime / count) + " ns per call");
        System.out.println("Average " + (nanoTime / (count * 1000000.0)) + " ms per call");
        System.out.println("Benchmark took " + (nanoTime / 1000000) + " ms");
    }
    
    private void printProgress(final long time, final int index) {
        long avg = time / index;
        System.out.println("Average so far " + avg + " ns per call");
    }
    
}
