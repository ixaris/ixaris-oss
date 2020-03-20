package com.ixaris.commons.microservices.defaults.test;

import com.ixaris.commons.microservices.defaults.context.CommonsMicroservicesDefaultsContext.Context;
import com.ixaris.commons.microservices.defaults.context.CommonsMicroservicesDefaultsContext.Subject;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.misc.lib.defaults.Defaults;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;
import com.ixaris.commons.multitenancy.lib.MultiTenancy;
import com.ixaris.commons.multitenancy.test.TestTenants;

/**
 * Factory class to generate {@link ServiceOperationHeader}s for test purposes.
 *
 * @author trevor.farrugia
 */
public final class ServiceOperationHeaderTestFactory {
    
    private ServiceOperationHeaderTestFactory() {}
    
    /**
     * @return {@link ServiceOperationHeader} pre-initialised with a System tenant.
     */
    public static ServiceOperationHeader<Context> getAuthorisedHeaderForSystemTenant() {
        final Subject subject = Subject.newBuilder().setSessionId(UniqueIdGenerator.generate()).build();
        return buildServiceOperationHeader(subject, MultiTenancy.SYSTEM_TENANT, UniqueIdGenerator.generate());
    }
    
    public static ServiceOperationHeader<Context> getUnauthenticatedHeaderForTenant(final String tenantId) {
        return ServiceOperationHeader.newBuilder(UniqueIdGenerator.generate(), tenantId, Context.newBuilder().build()).build();
    }
    
    public static ServiceOperationHeader<Context> getAuthorisedHeaderForSystemSuperUser() {
        final Subject subject = Subject.newBuilder()
            .setSessionId(UniqueIdGenerator.generate())
            .setIdentityId(UniqueIdGenerator.generate())
            .setIsSuperUser(true)
            .setTenantId(MultiTenancy.SYSTEM_TENANT)
            .build();
        return buildServiceOperationHeader(subject, MultiTenancy.SYSTEM_TENANT, UniqueIdGenerator.generate());
    }
    
    public static ServiceOperationHeader<Context> getAuthorisedHeaderForDefaultSuperUser() {
        final Subject subject = Subject.newBuilder()
            .setSessionId(UniqueIdGenerator.generate())
            .setIdentityId(UniqueIdGenerator.generate())
            .setIsSuperUser(true)
            .setTenantId(TestTenants.DEFAULT)
            .build();
        return buildServiceOperationHeader(subject, TestTenants.DEFAULT, UniqueIdGenerator.generate());
    }
    
    public static ServiceOperationHeader<Context> getHeaderForIdentityIdInDefaultTenant(final long identityId) {
        final Subject subject = Subject.newBuilder()
            .setSessionId(UniqueIdGenerator.generate())
            .setIdentityId(identityId)
            .setTenantId(TestTenants.DEFAULT)
            .build();
        return buildServiceOperationHeader(subject, TestTenants.DEFAULT, UniqueIdGenerator.generate());
    }
    
    public static ServiceOperationHeader<Context> getHeaderForIdentityIdInSystemTenant(final long identityId) {
        final Subject subject = Subject.newBuilder()
            .setSessionId(UniqueIdGenerator.generate())
            .setIdentityId(identityId)
            .setTenantId(MultiTenancy.SYSTEM_TENANT)
            .build();
        return buildServiceOperationHeader(subject, MultiTenancy.SYSTEM_TENANT, UniqueIdGenerator.generate());
    }
    
    public static ServiceOperationHeader<Context> getHeaderForIdentityIdAndTypeInTenant(final long identityId,
                                                                                        final String identityType,
                                                                                        final String tenant) {
        final Subject subject = Subject.newBuilder()
            .setSessionId(UniqueIdGenerator.generate())
            .setIdentityId(identityId)
            .setIdentityType(identityType)
            .setTenantId(tenant)
            .build();
        return buildServiceOperationHeader(subject, tenant, UniqueIdGenerator.generate());
    }
    
    public static ServiceOperationHeader<Context> getUnauthorisedHeaderForTenant(final String tenantId) {
        return getUnauthorisedHeaderForTenant(tenantId, UniqueIdGenerator.generate());
    }
    
    public static ServiceOperationHeader<Context> getUnauthorisedHeaderForTenant(final String tenantId, final Long intentId) {
        return buildServiceOperationHeader(Subject.newBuilder().setTenantId(tenantId).build(), tenantId, intentId);
    }
    
    public static ServiceOperationHeader<Context> getAuthorisedHeaderForTenantAndIdentityType(final String tenantId, final String identityType) {
        final Subject subject = Subject.newBuilder()
            .setSessionId(UniqueIdGenerator.generate())
            .setTenantId(tenantId)
            .setIdentityId(UniqueIdGenerator.generate())
            .setIdentityType(identityType)
            .setIsSuperUser(true)
            .build();
        return buildServiceOperationHeader(subject, tenantId, UniqueIdGenerator.generate());
    }
    
    public static ServiceOperationHeader<Context> getAuthorisedHeaderForTenantAndCredentialType(final String tenantId,
                                                                                                final String credentialType) {
        final Subject subject = Subject.newBuilder()
            .setSessionId(UniqueIdGenerator.generate())
            .setTenantId(tenantId)
            .setCredentialId(UniqueIdGenerator.generate())
            .setCredentialType(credentialType)
            .setIsSuperUser(true)
            .build();
        return buildServiceOperationHeader(subject, tenantId, UniqueIdGenerator.generate());
    }
    
    public static ServiceOperationHeader<Context> getAuthorisedHeaderForTenant(final String tenantId) {
        final Subject subject = Subject.newBuilder()
            .setSessionId(UniqueIdGenerator.generate())
            .setTenantId(tenantId)
            .setIsSuperUser(true)
            .build();
        return buildServiceOperationHeader(subject, tenantId, UniqueIdGenerator.generate());
    }
    
    public static ServiceOperationHeader<Context> getAuthorisedHeaderForDefaultTenant() {
        return getAuthorisedHeaderForTenant(TestTenants.DEFAULT);
    }
    
    public static ServiceOperationHeader<Context> getDefaultTenantHeaderForIdentityType(final String identityType) {
        return getAuthorisedHeaderForTenantAndIdentityType(TestTenants.DEFAULT, identityType);
    }
    
    public static ServiceOperationHeader<Context> getAuthorisedHeaderForTenantWithIdentityId(final String tenantId) {
        final Subject subject = Subject.newBuilder()
            .setSessionId(UniqueIdGenerator.generate())
            .setIdentityId(1L)
            .setTenantId(tenantId)
            .setIsSuperUser(true)
            .build();
        return buildServiceOperationHeader(subject, tenantId, UniqueIdGenerator.generate());
    }
    
    public static ServiceOperationHeader<Context> getHeaderForServiceKeyAndTenant(final String serviceKey, final String tenantId) {
        final Subject subject = Subject.newBuilder().setSessionId(UniqueIdGenerator.generate()).build();
        return new ServiceOperationHeader<>(UniqueIdGenerator.generate(), serviceKey, tenantId, Context.newBuilder().setSubject(subject).build());
    }
    
    public static ServiceOperationHeader<Context> getAuthorisedSuperUserHeaderForServiceKeyAndTenant(final String serviceKey,
                                                                                                     final String tenantId) {
        final Subject subject = Subject.newBuilder().setSessionId(UniqueIdGenerator.generate()).setIsSuperUser(true).build();
        return new ServiceOperationHeader<>(UniqueIdGenerator.generate(), serviceKey, tenantId, Context.newBuilder().setSubject(subject).build());
    }
    
    public static ServiceOperationHeader<Context> getAuthorisedHeaderForUniqueTenant() {
        final Subject subject = Subject.newBuilder().setSessionId(UniqueIdGenerator.generate()).setIsSuperUser(true).build();
        return new ServiceOperationHeader<>(Defaults.DEFAULT_TIMEOUT,
            UniqueIdGenerator.generate(),
            String.format("Tenant-%s", UniqueIdGenerator.generate()),
            Context.newBuilder().setSubject(subject).build());
    }
    
    public static ServiceOperationHeader<Context> getHeaderForTenantAndSubject(final String tenantId, final Subject subject) {
        return buildServiceOperationHeader(subject, tenantId, UniqueIdGenerator.generate());
    }
    
    public static ServiceOperationHeader<Context> getAuthenticatedHeader(final Subject subject, final String tenantId, final long intentId) {
        return buildServiceOperationHeader(subject, tenantId, intentId);
    }
    
    public static ServiceOperationHeader<Context> generateNewHeaderIntent(final ServiceOperationHeader<Context> header) {
        return new ServiceOperationHeader<>(UniqueIdGenerator.generate(), header.getServiceKey(), header.getTenantId(), header.getContext());
    }
    
    public static ServiceOperationHeader<Context> getHeader(final String tenantId, final long programmeId, final long identityId) {
        final Subject subject = Subject.newBuilder()
            .setSessionId(UniqueIdGenerator.generate())
            .setIdentityId(identityId)
            .setProgrammeId(programmeId)
            .build();
        return buildServiceOperationHeader(subject, tenantId, UniqueIdGenerator.generate());
    }
    
    private static ServiceOperationHeader<Context> buildServiceOperationHeader(final Subject subject, final String tenantId, final long intentId) {
        return new ServiceOperationHeader<>(intentId, tenantId, Context.newBuilder().setSubject(subject).build());
    }
    
    public static ServiceOperationHeader<Context> getHeaderForServiceKeyAndSubject(final String serviceKey, final Subject subject) {
        return new ServiceOperationHeader<>(UniqueIdGenerator.generate(),
            serviceKey,
            TestTenants.DEFAULT,
            Context.newBuilder().setSubject(subject).build());
    }
    
    public static ServiceOperationHeader<Context> getHeaderForServiceKeyAndTenantAndIdentityId(final String serviceKey,
                                                                                               final String tenantId,
                                                                                               final long identityId) {
        final Subject subject = Subject.newBuilder().setSessionId(UniqueIdGenerator.generate()).setIdentityId(identityId).build();
        return new ServiceOperationHeader<>(UniqueIdGenerator.generate(), serviceKey, tenantId, Context.newBuilder().setSubject(subject).build());
    }
}
