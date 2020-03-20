package com.ixaris.commons.microservices.defaults.context;

import static org.assertj.core.api.Assertions.assertThat;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.junit.Before;
import org.junit.ClassRule;
import org.junit.Ignore;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.PropertySource;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

import com.ixaris.commons.microservices.defaults.context.CommonsMicroservicesDefaultsContext.Context;
import com.ixaris.commons.microservices.defaults.context.CommonsMicroservicesDefaultsContext.Subject;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;
import com.ixaris.commons.multitenancy.test.TestTenants;

/**
 * Created by tiago.cucki on 23/06/2017.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(
                      loader = AnnotationConfigContextLoader.class,
                      classes = { ContextServiceSecurityCheckerTest.Config.class })
public class ContextServiceSecurityCheckerTest {
    
    public static final String PRIVATE_KEY_CERTIFICATE_FILE = "/dev-test-app-private-key.cer";
    
    @ClassRule
    public static TemporaryFolder temporaryFolder = new TemporaryFolder();
    
    @PropertySource("classpath:/security.properties")
    @ConfigurationProperties
    public static class Config {
        
        @Bean
        public static ContextServiceSecurityChecker liveServiceSecurityChecker() {
            return new ContextServiceSecurityChecker();
        }
        
    }
    
    @Before
    public void setup() {
        copyCertificateFile();
    }
    
    @Autowired
    private ContextServiceSecurityChecker contextServiceSecurityChecker;
    
    @Test
    public void checkSecurity_unsecuredResource_successfulCheck() {
        final ServiceOperationHeader<Context> header = getUnauthorisedHeader();
        assertThat(contextServiceSecurityChecker.check(header, ContextServiceSecurityChecker.UNSECURED_SECURITY_TAG, null)).isTrue();
    }
    
    @Test
    public void checkSecurity_tenantsDontMatch_failingCheck() {
        final ServiceOperationHeader<Context> header = buildServiceOperationHeader(Subject.newBuilder().setTenantId(TestTenants.LEFT).build());
        assertThat(contextServiceSecurityChecker.check(header, null, null)).isFalse();
    }
    
    @Test
    public void checkSecurity_noSubject_failingCheck() {
        final Context context = Context.getDefaultInstance();
        final ServiceOperationHeader<Context> header = new ServiceOperationHeader<>(1L, TestTenants.DEFAULT, context);
        assertThat(contextServiceSecurityChecker.check(header, null, null)).isFalse();
    }
    
    @Test
    public void checkSecurity_subjectNoSessionId_failingCheck() {
        final ServiceOperationHeader<Context> header = buildServiceOperationHeader(Subject.newBuilder().setTenantId(TestTenants.DEFAULT).build());
        assertThat(contextServiceSecurityChecker.check(header, null, null)).isFalse();
    }
    
    @Test
    public void checkSecurity_subjectIsSuperUser_successfulCheck() {
        final ServiceOperationHeader<Context> header = getAuthorisedHeaderForDefaultSuperUser();
        assertThat(contextServiceSecurityChecker.check(header, null, null)).isTrue();
    }
    
    @Ignore("Signature checker has been removed, to be added back in AL-1444")
    @Test
    public void checkSecurity_subjectNoSignature_failingCheck() {
        final ServiceOperationHeader<Context> header = getHeaderForIdentityIdInDefaultTenant(1L);
        assertThat(contextServiceSecurityChecker.check(header, null, null)).isFalse();
    }
    
    // Opposite of checkSecurity_subjectNoSignature_failingCheck, to be removed after AL-1444
    @Test
    public void checkSecurity_subjectNoSignature_passingCheck() {
        final ServiceOperationHeader<Context> header = getHeaderForIdentityIdInDefaultTenant(1L);
        assertThat(contextServiceSecurityChecker.check(header, null, null)).isTrue();
    }
    
    @Ignore("Signature checker has been removed, to be added back in AL-1444")
    @Test
    public void checkSecurity_subjectInvalidSignature_illegalStateException() {
        final Subject subject = Subject.newBuilder().setTenantId(TestTenants.DEFAULT).setSessionId(1L).setSignature("error").build();
        final Context context = Context.newBuilder().setSubject(subject).build();
        final ServiceOperationHeader<Context> header = new ServiceOperationHeader<>(1L, TestTenants.DEFAULT, context);
        assertThat(contextServiceSecurityChecker.check(header, null, null)).isFalse();
    }
    
    @Test
    public void checkSecurity_signedSubject_successfulCheck() {
        final Subject subject = getCompleteSubject(TestTenants.DEFAULT);
        final Context context = Context.newBuilder().setSubject(subject).build();
        final ServiceOperationHeader<Context> header = new ServiceOperationHeader<>(1L, TestTenants.DEFAULT, context);
        assertThat(contextServiceSecurityChecker.check(header, null, null)).isTrue();
    }
    
    @Ignore("Signature checker has been removed, to be added back in AL-1444")
    @Test
    public void checkSecurity_tamperedSubject_failingCheck() {
        // authorised subject has correct signature
        final Subject authorisedSubject = getCompleteSubject(TestTenants.DEFAULT);
        
        // tampered subject is the subject which signature doesn't correspond to its values
        final Subject tamperedSubject = authorisedSubject.toBuilder().setCredentialId(100L).build();
        
        final Context context = Context.newBuilder().setSubject(tamperedSubject).build();
        final ServiceOperationHeader<Context> header = new ServiceOperationHeader<>(1L, TestTenants.DEFAULT, context);
        assertThat(contextServiceSecurityChecker.check(header, null, null)).isFalse();
    }
    
    private Subject getCompleteSubject(final String tenantId) {
        final Subject unsignedSubject = getUnsignedSubject(tenantId);
        return createSignedSubject(unsignedSubject);
    }
    
    private Subject createSignedSubject(final Subject subject) {
        //        final Long fingerprint = MessageHelper.fingerprint(subject);
        //        final String signature = getSignature(fingerprint);
        //        return subject.toBuilder().setSignature(signature).build();
        return subject;
    }
    
    private Subject getUnsignedSubject(final String tenantId) {
        return Subject.newBuilder().setSessionId(1L).setTenantId(tenantId).setIsSuperUser(false).setCredentialId(1L).build();
    }
    
    //    private String getSignature(final Long fingerprint) {
    //        try {
    //            final PrivateKey privateKey = certificateLoader.getPrivateKey();
    //            final Signature rsa = Signature.getInstance(liveServiceSecurityCheckerConfig.getSignatureAlgorithm());
    //            rsa.initSign(privateKey);
    //            rsa.update(Long.toString(fingerprint).getBytes(StandardCharsets.UTF_8));
    //            final byte[] signature = rsa.sign();
    //            return Base64Util.encode(signature);
    //        } catch (Exception e) {
    //            throw new IllegalStateException("Error on signing a subject", e);
    //        }
    //    }
    
    private void copyCertificateFile() {
        final Path dest = Paths.get(temporaryFolder.getRoot() + PRIVATE_KEY_CERTIFICATE_FILE);
        if (dest.toFile().exists()) {
            return;
        }
        try (final InputStream sourceStream = getClass().getResourceAsStream("/subjectsignaturecertificate" + PRIVATE_KEY_CERTIFICATE_FILE)) {
            Files.copy(sourceStream, dest);
        } catch (IOException e) {
            throw new RuntimeException("Error copying test certificate file to temp folder", e);
        }
    }
    
    private ServiceOperationHeader<Context> getHeaderForIdentityIdInDefaultTenant(final long identityId) {
        final Subject subject = Subject.newBuilder()
            .setSessionId(UniqueIdGenerator.generate())
            .setIdentityId(identityId)
            .setTenantId(TestTenants.DEFAULT)
            .build();
        return buildServiceOperationHeader(subject);
    }
    
    private ServiceOperationHeader<Context> getAuthorisedHeaderForDefaultSuperUser() {
        final Subject subject = Subject.newBuilder()
            .setSessionId(UniqueIdGenerator.generate())
            .setIdentityId(UniqueIdGenerator.generate())
            .setIsSuperUser(true)
            .setTenantId(TestTenants.DEFAULT)
            .build();
        return buildServiceOperationHeader(subject);
    }
    
    private ServiceOperationHeader<Context> getUnauthorisedHeader() {
        return buildServiceOperationHeader(Subject.newBuilder().setTenantId(TestTenants.DEFAULT).build());
    }
    
    private ServiceOperationHeader<Context> buildServiceOperationHeader(final Subject subject) {
        return new ServiceOperationHeader<>(UniqueIdGenerator.generate(), TestTenants.DEFAULT, Context.newBuilder().setSubject(subject).build());
    }
}
