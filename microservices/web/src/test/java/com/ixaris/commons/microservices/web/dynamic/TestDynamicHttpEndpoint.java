package com.ixaris.commons.microservices.web.dynamic;

import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;

import com.ixaris.commons.microservices.lib.client.dynamic.DynamicServiceFactory;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.microservices.lib.common.exception.ClientInvalidRequestException;
import com.ixaris.commons.microservices.web.service1.Service1.ExampleContext;
import com.ixaris.commons.multitenancy.test.TestTenants;

public class TestDynamicHttpEndpoint extends AbstractDynamicHttpEndpoint<ExampleContext, ResolvedOperation<ExampleContext>> {
    
    public TestDynamicHttpEndpoint(final DynamicServiceFactory dynamicServiceFactory) {
        super(
            (request, decodedUrl, methodAndPayload) -> {
                try {
                    return result(
                        new ResolvedOperation<>(
                            "externalCallRef",
                            decodedUrl.serviceName,
                            decodedUrl.path,
                            methodAndPayload.method,
                            TENANT.exec(TestTenants.DEFAULT, () -> ServiceOperationHeader.newBuilder(
                                ExampleContext.newBuilder().setAuthToken("TEST").build())
                                .build()),
                            methodAndPayload.payload));
                } catch (final Exception e) {
                    throw new ClientInvalidRequestException(e);
                }
            },
            dynamicServiceFactory,
            "/dynamic");
    }
    
}
