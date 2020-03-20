package com.ixaris.commons.microservices.web.swagger;

import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.microservices.web.AbstractHttpEndpoint.AUTHORIZATION_HEADER;
import static com.ixaris.commons.microservices.web.AbstractHttpEndpoint.CALL_REF_HEADER;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;

import java.util.Optional;
import java.util.regex.Pattern;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;
import com.ixaris.commons.microservices.lib.common.exception.ClientInvalidRequestException;
import com.ixaris.commons.microservices.web.HttpRequest;
import com.ixaris.commons.microservices.web.service1.Service1.ExampleContext;
import com.ixaris.commons.microservices.web.swagger.TestOperationResolver.ExtendedResolvedOperation;
import com.ixaris.commons.microservices.web.swagger.operations.ResolvedOperation;
import com.ixaris.commons.microservices.web.swagger.operations.SwaggerOperationResolver;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;

/**
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public class TestOperationResolver implements SwaggerOperationResolver<ExampleContext, ExtendedResolvedOperation> {
    
    public static class ExtendedResolvedOperation extends ResolvedOperation<ExampleContext> {
        
        public final String programmeKey;
        public final String additionalInfo;
        
        public ExtendedResolvedOperation(final String callRef,
                                         final boolean create,
                                         final String serviceName,
                                         final ServicePathHolder path,
                                         final ServicePathHolder params,
                                         final ServiceOperationHeader<ExampleContext> header,
                                         final String programmeKey,
                                         final String additionalInfo) {
            super(callRef, create, serviceName, path, params, header);
            this.programmeKey = programmeKey;
            this.additionalInfo = additionalInfo;
        }
        
        public ExtendedResolvedOperation(final ExtendedResolvedOperation operation, final String additionalInfo) {
            super(operation);
            this.programmeKey = operation.programmeKey;
            this.additionalInfo = additionalInfo;
        }
        
    }
    
    static final String PROGRAMME_KEY_HEADER = "X-ProgrammeKey";
    
    private static final Pattern PROGRAMME_KEY_PATTERN = Pattern.compile("\\|");
    
    private final MultiTenancy multiTenancy;
    
    public TestOperationResolver(final MultiTenancy multiTenancy) {
        this.multiTenancy = multiTenancy;
    }
    
    @Override
    public Async<ExtendedResolvedOperation> resolve(final HttpRequest<?> httpRequest,
                                                    final String serviceName,
                                                    final String serviceKey,
                                                    final ServicePathHolder path,
                                                    final ServicePathHolder params,
                                                    final String method,
                                                    final boolean create,
                                                    final MessageLite request) {
        try {
            final String authToken = Optional.ofNullable(httpRequest.getHeaders().get(AUTHORIZATION_HEADER))
                .filter(a -> a.startsWith("X-TOKEN "))
                .map(a -> a.substring(8))
                .orElse("");
            
            final String programmeKey = Optional.ofNullable(httpRequest.getHeaders().get(PROGRAMME_KEY_HEADER))
                .orElseThrow(() -> new IllegalArgumentException("Missing programme key header"));
            
            final String[] progKeySplit = PROGRAMME_KEY_PATTERN.split(programmeKey);
            if (progKeySplit.length != 2) {
                throw new IllegalArgumentException("Invalid programme key header: " + programmeKey);
            }
            
            final String tenantId = progKeySplit[0];
            validateTenantId(tenantId);
            
            final long programmeIdInKey = resolveProgrammeId(progKeySplit[2]);
            final ExampleContext context = ExampleContext.newBuilder()
                .setAuthToken(authToken)
                .setProgrammeId(programmeIdInKey)
                .build();
            
            // Typically, from call ref we resolve potential previous call and get intent id from there for idempotency
            return result(new ExtendedResolvedOperation(httpRequest.getHeaders().get(CALL_REF_HEADER),
                create,
                serviceName,
                path,
                create ? params.replaceLastSegment(Long.toString(UniqueIdGenerator.generate())) : params,
                TENANT.exec(tenantId, () -> ServiceOperationHeader.newBuilder(context).build()),
                programmeKey,
                null));
        } catch (final RuntimeException e) {
            throw new ClientInvalidRequestException(e);
        }
    }
    
    private void validateTenantId(final String tenantId) {
        if (!multiTenancy.isTenantActive(tenantId)) {
            throw new IllegalArgumentException(String.format("Invalid tenant [%s]", tenantId));
        }
        if (MultiTenancy.SYSTEM_TENANT.equals(tenantId)) {
            throw new IllegalArgumentException("System tenant not authorised for use");
        }
    }
    
    private static long resolveProgrammeId(final String programmeId) {
        try {
            return Long.parseLong(programmeId);
        } catch (final NumberFormatException e) {
            throw new IllegalArgumentException("Invalid programme id in programme key header: " + programmeId, e);
        }
    }
    
}
