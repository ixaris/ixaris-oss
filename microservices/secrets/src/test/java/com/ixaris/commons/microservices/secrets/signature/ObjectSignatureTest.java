package com.ixaris.commons.microservices.secrets.signature;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.ixaris.commons.microservices.secrets.CertificateLoader;
import com.ixaris.commons.microservices.secrets.CertificateLoaderImpl;
import com.ixaris.commons.misc.lib.id.UniqueIdGenerator;
import com.ixaris.commons.protobuf.lib.CommonsProtobufLib.SensitiveString;

/**
 * Created by tiago.cucki on 07/07/2017.
 */
public class ObjectSignatureTest {
    
    private static final String SIGNATURE_ALGORITHM = "SHA256withRSA";
    
    private static final String PUBLIC_CERTIFICATE = "-----BEGIN CERTIFICATE-----\n"
        + "MIIDnTCCAoWgAwIBAgIJAIsv7v1QMagFMA0GCSqGSIb3DQEBCwUAMGUxCzAJBgNV\n"
        + "BAYTAk1UMRMwEQYDVQQIDApTb21lLVN0YXRlMRIwEAYDVQQHDAlTYW4gR3dhbm4x\n"
        + "DzANBgNVBAoMBklYQVJJUzENMAsGA1UECwwESVBTMjENMAsGA1UEAwwESVBTMjAe\n"
        + "Fw0xNzA3MDcxMzAwMzhaFw0xODA3MDcxMzAwMzhaMGUxCzAJBgNVBAYTAk1UMRMw\n"
        + "EQYDVQQIDApTb21lLVN0YXRlMRIwEAYDVQQHDAlTYW4gR3dhbm4xDzANBgNVBAoM\n"
        + "BklYQVJJUzENMAsGA1UECwwESVBTMjENMAsGA1UEAwwESVBTMjCCASIwDQYJKoZI\n"
        + "hvcNAQEBBQADggEPADCCAQoCggEBAK5Yr9CVJzmdJV6/GcjC6XfkmPKC5PqGAbaU\n"
        + "FxZNKw9H9y0ZUhLiKoenG4tpuan7om2kBGAl9+Bj9z5uYThlf6IbW1JjFrg60wgn\n"
        + "IUeJQ541o37SDmR4UL1PlwHnoYZkOrn0sDUlP4mk21RLbusbe0hnDr+mJkcieZDk\n"
        + "jg8mMVo3lYaYXVLfW3x7M1V8VFO3NIjiNTw+aKgFwVKx9gvdEIzD23VYYRkRbOog\n"
        + "FjzZSeT58kdHacN/CirQNyLkQY8yc7EeHj97+AJrqX7sEG5vG92Y0Ulf9GZRcGWt\n"
        + "6JtvWNGth6VLoHnk0mDgIjJpawTDojl1E0NoNGkWuhZLdX0t8EsCAwEAAaNQME4w\n"
        + "HQYDVR0OBBYEFAQINUtvJ3Jg1nlhJCpEktvO3YkaMB8GA1UdIwQYMBaAFAQINUtv\n"
        + "J3Jg1nlhJCpEktvO3YkaMAwGA1UdEwQFMAMBAf8wDQYJKoZIhvcNAQELBQADggEB\n"
        + "AGziGa8SSG+cBxiIC0KiOne+ZCJ9SHiPHh7vUCU3Uuc3kPO/s+G3BlFwJ8ACJA6S\n"
        + "mXkFRYa439jjsboRVtuWHSK1XJ/rbV4K047Unc8kxF1HGxSktb+dx4Fc1Ot+UQIP\n"
        + "r87x8V+j+HdTgJm9aWuAuYRnn32BYl2BSzVsVPOF0aS8QrSbV6rStJJDAos8nBXS\n"
        + "Wl3qKRO5YOyKyPWt4sCMESuCY0BXGaOjbp8+VvIT70lqewT7JHnb1BPJx1B/6hVN\n"
        + "KFCAJ5w9LRjHRq4wHDZJj8zqdpFCtZ6/YYpXV9+KyrrPzUMzzEWW+Y6Jaq7WUuyW\n"
        + "c3Lkor7btEaBs5yEL9dARTM=\n"
        + "-----END CERTIFICATE-----\n";
    
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    
    private ObjectSigner objectSigner;
    
    private SignatureChecker signatureChecker;
    
    private CertificateLoader certificateManager;
    
    @Before
    public void setup() {
        certificateManager = new CertificateLoaderImpl("test", "signature", temporaryFolder.getRoot().getPath());
        final SignatureConfig signatureConfig = new SignatureConfig(SIGNATURE_ALGORITHM, PUBLIC_CERTIFICATE);
        copyFile("test-signature-private-key.cer", "test-signature-private-key.cer");
        objectSigner = new ObjectSigner(certificateManager, signatureConfig);
        signatureChecker = new SignatureChecker(certificateManager, signatureConfig);
    }
    
    @Test
    public void verifyObjectSignature_validSignature_shouldPassVerification() {
        final SensitiveString secret = SensitiveString.newBuilder().setValue("secret" + UniqueIdGenerator.generate()).build();
        final String signature = objectSigner.getObjectSignature(secret);
        Assertions.assertThat(signatureChecker.checkSignature(secret, signature)).as("Valid signature for given secret").isTrue();
    }
    
    @Test
    public void verifyTamperedObjectSignature_validSignature_shouldFailVerification() {
        final SensitiveString secret = SensitiveString.newBuilder().setValue("secret" + UniqueIdGenerator.generate()).build();
        final String signature = objectSigner.getObjectSignature(secret);
        final SensitiveString tampered = SensitiveString.newBuilder().setValue(secret + "1").build();
        Assertions.assertThat(signatureChecker.checkSignature(tampered, signature)).as("Valid signature for given secret").isFalse();
    }
    
    private void copyFile(final String sourceFileName, final String destFileName) {
        final Path dest = Paths.get(temporaryFolder.getRoot() + "/" + destFileName);
        try (final InputStream sourceStream = getClass().getResourceAsStream("/secrets/" + sourceFileName)) {
            Files.copy(sourceStream, dest);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
