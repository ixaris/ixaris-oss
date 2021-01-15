package com.ixaris.commons.zeromq.microservices;

import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;

import java.security.SecureRandom;
import java.util.Collections;
import java.util.HashSet;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.atomic.AtomicReference;

import org.assertj.core.api.Assertions;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.AfterAll;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.zeromq.ZMQ;
import org.zeromq.ZMQ.Socket;

import com.google.protobuf.InvalidProtocolBufferException;

import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.microservices.lib.local.LocalService;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;
import com.ixaris.commons.microservices.lib.service.ServiceSkeleton;
import com.ixaris.commons.microservices.lib.service.proxy.ServiceSkeletonProxy;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.test.TestTenants;
import com.ixaris.commons.zeromq.microservices.example.Example.ExampleContext;
import com.ixaris.commons.zeromq.microservices.example.Example.ExampleRequest;
import com.ixaris.commons.zeromq.microservices.example.Example.ExampleResponse;
import com.ixaris.commons.zeromq.microservices.example.client.ExampleServiceClientImpl;
import com.ixaris.commons.zeromq.microservices.example.client.ExampleStub;
import com.ixaris.commons.zeromq.microservices.example.service.Alt2ExampleZmqSpiResourceImpl;
import com.ixaris.commons.zeromq.microservices.example.service.Alt3ExampleZmqSpiResourceImpl;
import com.ixaris.commons.zeromq.microservices.example.service.AltExampleZmqSpiResourceImpl;
import com.ixaris.commons.zeromq.microservices.example.service.ExampleResourceImpl;
import com.ixaris.commons.zeromq.microservices.example.service.ExampleSkeleton;
import com.ixaris.commons.zeromq.microservices.example.service.ExampleSupportSkeletonImpl;
import com.ixaris.commons.zeromq.microservices.examplesupport.client.ExampleSupportStub;
import com.ixaris.commons.zeromq.microservices.proxy.ZMQOperationProxy;
import com.ixaris.commons.zeromq.microservices.stack.TestServiceSecurityChecker;
import com.ixaris.commons.zeromq.microservices.stack.TestZMQServiceChannel;
import com.ixaris.commons.zeromq.microservices.stack.TestZMQServiceClientSupport;
import com.ixaris.commons.zeromq.microservices.stack.TestZMQServiceSupport;

public class ZMQProxyTest {
    
    private static final Logger LOG = LoggerFactory.getLogger(ZMQProxyTest.class);
    private static final int START_PORT = 39000;
    
    private static MultiTenancy multiTenancy;
    private static TestZMQServiceChannel channel;
    private static ExampleServiceClientImpl client;
    private static final SecureRandom RANDOM = new SecureRandom();
    private static final Set<ServiceSkeleton> INSTANCES = new HashSet<>();
    private static final AtomicReference<Map<String, String>> NODE_ATTRIBUTES = new AtomicReference<>();
    
    private static Socket proxyDealerSocket;
    
    @BeforeAll
    public static void setupClass() {
        ZMQGlobal.start();
        
        final TestServiceSecurityChecker testServiceSecurityChecker = new TestServiceSecurityChecker();
        LocalService localService = new LocalService(Collections.emptySet(), Collections.emptySet());
        
        multiTenancy = new MultiTenancy();
        multiTenancy.start();
        
        channel = new TestZMQServiceChannel(
            new TestZMQServiceSupport(multiTenancy, START_PORT, localService, testServiceSecurityChecker, true),
            new TestZMQServiceClientSupport(multiTenancy, localService, true));
        
        multiTenancy.addTenant(TestTenants.DEFAULT);
        
        final ZMQOperationProxy zmqOperationProxy = new ZMQOperationProxy(channel.serviceSupport.getUrlHelper(),
            channel.serviceClientSupport,
            NODE_ATTRIBUTES::set);
        
        Awaitility.await().until(() -> zmqOperationProxy.getOperationUrl() != null);
        String proxyUrl = zmqOperationProxy.getOperationUrl();
        
        proxyDealerSocket = ZMQGlobal.getContext().socket(ZMQ.DEALER);
        proxyDealerSocket.setImmediate(false);
        final boolean connect = proxyDealerSocket.connect(proxyUrl);
        Assertions.assertThat(connect).isTrue();
        
        // service 1
        final ExampleSupportSkeletonImpl exampleSupportSkeletonImpl = new ExampleSupportSkeletonImpl();
        channel.serviceSupport.init(exampleSupportSkeletonImpl);
        INSTANCES.add(exampleSupportSkeletonImpl);
        
        // service 1
        final ServiceSkeletonProxy<ExampleSkeleton> proxy = channel.serviceSupport.getOrCreate(ExampleSkeleton.class);
        final ExampleSkeleton.Watch exampleEventPublisher = proxy.createPublisherProxy(ExampleSkeleton.Watch.class);
        final ExampleSupportStub exampleSupportResource = channel.serviceClientSupport
            .getOrCreate(ExampleSupportStub.class)
            .createProxy();
        final ExampleResourceImpl exampleResourceImpl = new ExampleResourceImpl(
            exampleEventPublisher, exampleSupportResource);
        channel.serviceSupport.init(exampleResourceImpl);
        INSTANCES.add(exampleResourceImpl);
        
        // service ALT
        final AltExampleZmqSpiResourceImpl exampleSpiImpl = new AltExampleZmqSpiResourceImpl();
        channel.serviceSupport.init(exampleSpiImpl);
        INSTANCES.add(exampleSpiImpl);
        
        // service ALT2
        final Alt2ExampleZmqSpiResourceImpl exampleSpiImpl2 = new Alt2ExampleZmqSpiResourceImpl();
        channel.serviceSupport.init(exampleSpiImpl2);
        INSTANCES.add(exampleSpiImpl2);
        
        // service ALT3
        final Alt3ExampleZmqSpiResourceImpl exampleSpiImpl3 = new Alt3ExampleZmqSpiResourceImpl();
        channel.serviceSupport.init(exampleSpiImpl3);
        INSTANCES.add(exampleSpiImpl3);
        
        // client
        final ExampleStub exampleResource = channel.serviceClientSupport.getOrCreate(ExampleStub.class).createProxy();
        client = new ExampleServiceClientImpl(exampleResource);
    }
    
    @AfterAll
    public static void teardownClass() {
        proxyDealerSocket.close();
        
        NODE_ATTRIBUTES.set(null);
        INSTANCES.forEach(i -> channel.serviceSupport.destroy(i));
        INSTANCES.clear();
        
        multiTenancy.stop();
        
        ZMQGlobal.shutdown();
    }
    
    @BeforeEach
    public void setup() {
        ExampleServiceClientImpl.log = true;
        ExampleResourceImpl.log = true;
    }
    
    @Test
    public void zmqOperationProxy_advertisedUrl_shouldRegisterInServiceRegistry() {
        Assertions.assertThat(NODE_ATTRIBUTES.get()).containsKey(ZMQGlobal.OPERATION_URL_KEY);
    }
    
    @Test
    public void test_single_call_with_future() throws Exception {
        
        final int id = RANDOM.nextInt();
        
        final ServiceOperationHeader<ExampleContext> header = TENANT.exec(TestTenants.DEFAULT, () -> ServiceOperationHeader.newBuilder(ExampleContext.newBuilder().build()).build());
        
        final ExampleRequest exampleRequest = client.buildExampleRequest(id, 0, false);
        
        // transform header and request in envelope
        final RequestEnvelope requestEnvelope = RequestEnvelope.newBuilder()
            .setCorrelationId(header.getCorrelationId())
            .setCallRef(UniqueIdGenerator.generate())
            .setParentRef(header.getCallRef())
            .setMethod("op")
            .setIntentId(header.getIntentId())
            .setServiceName("example")
            .setTenantId(header.getTenantId())
            .setContext(header.getContext().toByteString())
            .setPayload(exampleRequest.toByteString())
            .build();
        
        final ResponseEnvelope responseEnvelope = sendToProxyAndGetResponse(requestEnvelope);
        
        Assertions.assertThat(responseEnvelope.getStatusCode()).isEqualTo(ResponseStatusCode.OK);
        LOG.info("response envelope: {}", responseEnvelope);
        Assertions.assertThat(responseEnvelope.getCorrelationId()).isEqualTo(requestEnvelope.getCorrelationId());
        
        final ExampleResponse exampleResponse = ExampleResponse.parseFrom(responseEnvelope.getPayload());
        Assertions.assertThat(exampleResponse.getId()).isEqualTo(id);
    }
    
    @Test
    public void test_duplicate_call() throws Exception {
        final int id = RANDOM.nextInt();
        
        final ServiceOperationHeader<ExampleContext> header = TENANT.exec(TestTenants.DEFAULT, () -> ServiceOperationHeader.newBuilder(ExampleContext.newBuilder().build()).build());
        
        final ExampleRequest exampleRequest = client.buildExampleRequest(id, 0, false);
        
        // transform header and request in envelope
        final RequestEnvelope requestEnvelope = RequestEnvelope.newBuilder()
            .setCorrelationId(header.getCorrelationId())
            .setCallRef(UniqueIdGenerator.generate())
            .setParentRef(header.getCallRef())
            .setMethod("op")
            .setIntentId(header.getIntentId())
            .setServiceName("example")
            .setTenantId(header.getTenantId())
            .setContext(header.getContext().toByteString())
            .setPayload(exampleRequest.toByteString())
            .build();
        
        final boolean send1 = proxyDealerSocket.send(requestEnvelope.toByteArray());
        Assertions.assertThat(send1).isTrue();
        final boolean send2 = proxyDealerSocket.send(requestEnvelope.toByteArray());
        Assertions.assertThat(send2).isTrue();
        
        final ResponseEnvelope responseEnvelope1 = ResponseEnvelope.parseFrom(proxyDealerSocket.recv());
        Assertions.assertThat(responseEnvelope1.getStatusCode()).isEqualTo(ResponseStatusCode.SERVER_TIMEOUT);
        Assertions.assertThat(responseEnvelope1.getStatusMessage()).startsWith("Duplicate concurrent request");
        
        final ResponseEnvelope responseEnvelope2 = ResponseEnvelope.parseFrom(proxyDealerSocket.recv());
        
        Assertions.assertThat(responseEnvelope2.getStatusCode()).isEqualTo(ResponseStatusCode.OK);
        LOG.info("response envelope: {}", responseEnvelope2);
        Assertions.assertThat(responseEnvelope2.getCorrelationId()).isEqualTo(requestEnvelope.getCorrelationId());
        
        final ExampleResponse exampleResponse = ExampleResponse.parseFrom(responseEnvelope2.getPayload());
        Assertions.assertThat(exampleResponse.getId()).isEqualTo(id);
    }
    
    @Test
    public void testCallWithProxy_MultipleServices() throws Exception {
        final int id = RANDOM.nextInt();
        
        final ServiceOperationHeader<ExampleContext> header = TENANT.exec(TestTenants.DEFAULT, () -> ServiceOperationHeader.newBuilder(ExampleContext.newBuilder().build()).build());
        
        final ExampleRequest exampleRequest = client.buildExampleRequest(id, 100, false);
        
        // transform header and request in envelope
        final RequestEnvelope requestEnvelope = RequestEnvelope.newBuilder()
            .setCorrelationId(header.getCorrelationId())
            .setCallRef(UniqueIdGenerator.generate())
            .setParentRef(header.getCallRef())
            .setMethod("op")
            .setIntentId(header.getIntentId())
            .setServiceName("example")
            .setTenantId(header.getTenantId())
            .setContext(header.getContext().toByteString())
            .setPayload(exampleRequest.toByteString())
            .build();
        
        final ResponseEnvelope responseEnvelope = sendToProxyAndGetResponse(requestEnvelope);
        
        Assertions.assertThat(responseEnvelope.getStatusCode()).isEqualTo(ResponseStatusCode.OK);
        LOG.info("response envelope: {}", responseEnvelope);
        Assertions.assertThat(responseEnvelope.getCorrelationId()).isEqualTo(requestEnvelope.getCorrelationId());
        
        final ExampleResponse exampleResponse = ExampleResponse.parseFrom(responseEnvelope.getPayload());
        Assertions.assertThat(exampleResponse.getId()).isEqualTo(id);
        
        long supportCorrelationId = UniqueIdGenerator.generate();
        final RequestEnvelope supportRequest = requestEnvelope.toBuilder()
            .setCorrelationId(supportCorrelationId)
            .setServiceName("example_support")
            .setPayload(exampleRequest.toBuilder().setId(id + 1).build().toByteString())
            .build();
        
        final ResponseEnvelope supportResponseEnvelope = sendToProxyAndGetResponse(supportRequest);
        LOG.info("Received response");
        
        LOG.info("support response envelope: {}", supportResponseEnvelope);
        Assertions.assertThat(supportResponseEnvelope.getCorrelationId()).isEqualTo(supportRequest.getCorrelationId());
        
        final ExampleResponse supportResponse = ExampleResponse.parseFrom(supportResponseEnvelope.getPayload());
        Assertions.assertThat(supportResponse.getId()).isEqualTo(id + 1);
        
        final ResponseEnvelope supportResponseEnvelope2 = sendToProxyAndGetResponse(supportRequest);
        Assertions.assertThat(supportResponseEnvelope2).isNotNull();
        Assertions.assertThat(supportResponseEnvelope2.getCorrelationId()).isEqualTo(supportRequest.getCorrelationId());
    }
    
    @Test
    public void testCallOnSPIWithProxy() throws Exception {
        final String[] spiKeys = { "ALT", "ALT2", "ALT3" };
        for (final String spiKey : spiKeys) {
            final int id = RANDOM.nextInt();
            
            final ServiceOperationHeader<ExampleContext> header = TENANT.exec(TestTenants.DEFAULT, () -> ServiceOperationHeader.newBuilder(ExampleContext.newBuilder().build()).build());
            
            final ExampleRequest exampleRequest = client.buildExampleRequest(id, 0, false);
            
            // transform header and request in envelope
            final RequestEnvelope requestEnvelope = RequestEnvelope.newBuilder()
                .setCorrelationId(header.getCorrelationId())
                .setCallRef(UniqueIdGenerator.generate())
                .setParentRef(header.getCallRef())
                .setMethod("op")
                .setIntentId(header.getIntentId())
                .setServiceName("example_spi")
                .setServiceKey(spiKey)
                .setTenantId(header.getTenantId())
                .setContext(header.getContext().toByteString())
                .setPayload(exampleRequest.toByteString())
                .build();
            
            final ResponseEnvelope responseEnvelope = sendToProxyAndGetResponse(requestEnvelope);
            
            Assertions.assertThat(responseEnvelope.getStatusCode()).isEqualTo(ResponseStatusCode.OK);
            LOG.info("response envelope: {}", responseEnvelope);
            Assertions.assertThat(responseEnvelope.getCorrelationId()).isEqualTo(requestEnvelope.getCorrelationId());
            
            final ExampleResponse exampleResponse = ExampleResponse.parseFrom(responseEnvelope.getPayload());
            Assertions.assertThat(exampleResponse.getId()).isEqualTo(id);
        }
    }
    
    private ResponseEnvelope sendToProxyAndGetResponse(final RequestEnvelope requestEnvelope) throws InvalidProtocolBufferException {
        final boolean send = proxyDealerSocket.send(requestEnvelope.toByteArray());
        Assertions.assertThat(send).isTrue();
        
        final byte[] recvEnvelope = proxyDealerSocket.recv();
        return ResponseEnvelope.parseFrom(recvEnvelope);
    }
    
}
