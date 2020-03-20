package com.ixaris.commons.microservices.lib.example.client;

import static com.ixaris.commons.async.lib.CompletionStageUtil.block;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;

import com.ixaris.commons.async.lib.AsyncExecutor;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.microservices.lib.example.Example.ExampleContext;
import com.ixaris.commons.microservices.lib.example.Example.ExampleRequest;
import com.ixaris.commons.microservices.lib.example.service.ExampleSkeleton;
import com.ixaris.commons.microservices.lib.example.service.ExampleSkeletonImpl;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.lib.service.proxy.ServiceSkeletonProxy;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;
import com.ixaris.commons.multitenancy.test.TestTenants;

public final class ExampleServiceClientNoEventsImpl {
    
    private final ExampleStub exampleService;
    private final ServiceSkeletonProxy<ExampleSkeleton> serviceSkeletonProxy;
    private final ExampleSkeletonImpl exampleServiceImpl;
    
    public ExampleServiceClientNoEventsImpl(final ExampleStub exampleService,
                                            final ServiceSkeletonProxy<ExampleSkeleton> serviceSkeletonProxy,
                                            final ExampleSkeletonImpl exampleServiceImpl) {
        this.exampleService = exampleService;
        this.serviceSkeletonProxy = serviceSkeletonProxy;
        this.exampleServiceImpl = exampleServiceImpl;
    }
    
    public void benchmarkDirect(final int count) {
        TENANT.exec(TestTenants.DEFAULT, () -> {
            long start = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                final ServiceOperationHeader<ExampleContext> header = ServiceOperationHeader.newBuilder(
                    ExampleContext.newBuilder().build())
                    .withTimeout(5000)
                    .build();
                try {
                    block(AsyncExecutor.exec(() -> exampleServiceImpl.exampleOperationNoLogs(header,
                        ExampleRequest.newBuilder().build())));
                } catch (final Throwable e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Warm up took " + ((System.nanoTime() - start) / 1000000) + " ms");
            
            start = System.nanoTime();
            for (int i = 0; i < count; i++) {
                final ServiceOperationHeader<ExampleContext> header = ServiceOperationHeader.newBuilder(
                    ExampleContext.newBuilder().build())
                    .withTimeout(5000)
                    .build();
                try {
                    block(AsyncExecutor.exec(() -> exampleServiceImpl.exampleOperationNoLogs(header,
                        ExampleRequest.newBuilder().build())));
                } catch (final Throwable e) {
                    e.printStackTrace();
                }
                if (i % 10000 == 0) {
                    printProgress(start, i + 1);
                }
            }
            long nanoTime = System.nanoTime() - start;
            System.out.println("Direct Average " + (nanoTime / count) + " ns per call");
            System.out.println("Direct Average " + (nanoTime / (count * 1000000.0)) + " ms per call");
            System.out.println("Direct Benchmark took " + (nanoTime / 1000000) + " ms");
        });
    }
    
    public void benchmarkInvoke(final int count) {
        TENANT.exec(TestTenants.DEFAULT, () -> {
            long start = System.nanoTime();
            for (int i = 0; i < 1000; i++) {
                final ServiceOperationHeader<ExampleContext> header = ServiceOperationHeader.newBuilder(
                    ExampleContext.newBuilder().build())
                    .withTimeout(5000)
                    .build();
                try {
                    block(AsyncExecutor.exec(() -> serviceSkeletonProxy
                        .process(
                            RequestEnvelope.newBuilder()
                                .setCorrelationId(UniqueIdGenerator.generate())
                                .setCallRef(UniqueIdGenerator.generate())
                                .setParentRef(header.getCallRef())
                                .setMethod("example_operation_no_logs")
                                .setIntentId(header.getIntentId())
                                .setContext(header.getContext().toByteString())
                                .setServiceName("example")
                                .setTenantId(TestTenants.DEFAULT)
                                .setTimeout(5000)
                                .build())));
                } catch (final Throwable e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Warm up took " + ((System.nanoTime() - start) / 1000000) + " ms");
            
            start = System.nanoTime();
            for (int i = 0; i < count; i++) {
                final ServiceOperationHeader<ExampleContext> header = ServiceOperationHeader.newBuilder(
                    ExampleContext.newBuilder().build())
                    .withTimeout(5000)
                    .build();
                try {
                    block(AsyncExecutor.exec(() -> serviceSkeletonProxy
                        .process(
                            RequestEnvelope.newBuilder()
                                .setCorrelationId(UniqueIdGenerator.generate())
                                .setCallRef(UniqueIdGenerator.generate())
                                .setParentRef(header.getCallRef())
                                .setMethod("example_operation_no_logs")
                                .setIntentId(header.getIntentId())
                                .setContext(header.getContext().toByteString())
                                .setServiceName("example")
                                .setTenantId(TestTenants.DEFAULT)
                                .setTimeout(5000)
                                .build())));
                } catch (final Throwable e) {
                    e.printStackTrace();
                }
                if (i % 10000 == 0) {
                    printProgress(start, i + 1);
                }
            }
            long nanoTime = System.nanoTime() - start;
            System.out.println("Direct Average " + (nanoTime / count) + " ns per call");
            System.out.println("Direct Average " + (nanoTime / (count * 1000000.0)) + " ms per call");
            System.out.println("Direct Benchmark took " + (nanoTime / 1000000) + " ms");
        });
    }
    
    public void benchmarkMessages(final int count) {
        TENANT.exec(TestTenants.DEFAULT, () -> {
            long start = System.nanoTime();
            for (int i = 0; i < 2000; i++) {
                final ServiceOperationHeader<ExampleContext> header = ServiceOperationHeader.newBuilder(
                    ExampleContext.newBuilder().build())
                    .withTimeout(5000)
                    .build();
                try {
                    block(exampleService.exampleOperationNoLogs(header, ExampleRequest.newBuilder().build()));
                } catch (Throwable e) {
                    e.printStackTrace();
                }
            }
            System.out.println("Warm up took " + ((System.nanoTime() - start) / 1000000) + " ms");
            
            start = System.nanoTime();
            for (int i = 0; i < count; i++) {
                final ServiceOperationHeader<ExampleContext> header = ServiceOperationHeader.newBuilder(
                    ExampleContext.newBuilder().build())
                    .withTimeout(5000)
                    .build();
                try {
                    block(exampleService.exampleOperationNoLogs(header, ExampleRequest.newBuilder().build()));
                } catch (Throwable e) {
                    e.printStackTrace();
                }
                if (i % 10000 == 0) {
                    printProgress(start, i + 1);
                }
            }
            long nanoTime = System.nanoTime() - start;
            System.out.println("Messages Average " + (nanoTime / count) + " ns per call");
            System.out.println("Messages Average " + (nanoTime / (count * 1000000.0)) + " ms per call");
            System.out.println("Messages Benchmark took " + (nanoTime / 1000000) + " ms");
        });
    }
    
    private void printProgress(final long start, final int index) {
        long avg = (System.nanoTime() - start) / index;
        System.out.println("Average so far " + avg + " ns per call");
    }
    
}
