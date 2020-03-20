package com.ixaris.commons.microservices.secrets.certificate;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

import com.ixaris.commons.microservices.secrets.CertificateLoader;
import com.ixaris.commons.microservices.secrets.CertificateLoaderImpl;
import com.ixaris.commons.microservices.secrets.PKICryptoService;
import com.ixaris.commons.misc.lib.sensitivedata.CryptoUtil;

/**
 * Created by tiago.cucki on 06/06/2017.
 */
public class CertificateLoaderTest {
    
    private CertificateLoader certificateManager;
    
    @Rule
    public TemporaryFolder temporaryFolder = new TemporaryFolder();
    
    private final String validServerPublicKey = "-----BEGIN CERTIFICATE-----\n"
        + "MIIDOTCCAiGgAwIBAgIEWRxY4DANBgkqhkiG9w0BAQ0FADBNMQswCQYDVQQGEwJN\n"
        + "VDETMBEGA1UEChMKSXhhcmlzIEx0ZDEPMA0GA1UECxMGSXhhcmlzMRgwFgYDVQQD\n"
        + "Ew9JeGFyaXNTU01TZXJ2ZXIwHhcNMTcwNTE3MTQwNjI0WhcNMjcwNTE1MTQwNjI0\n"
        + "WjBNMQswCQYDVQQGEwJNVDETMBEGA1UEChMKSXhhcmlzIEx0ZDEPMA0GA1UECxMG\n"
        + "SXhhcmlzMRgwFgYDVQQDEw9JeGFyaXNTU01TZXJ2ZXIwggEiMA0GCSqGSIb3DQEB\n"
        + "AQUAA4IBDwAwggEKAoIBAQCIpaO0Ts7CRAVkmBgWAuaAHX3TG60pMRXcbcSmpsem\n"
        + "hxxWXKHp2gTDsDzSKg78wMHPCE87JNEGfwQwM27Ggo/doDL8RXxlRt1GX6Mbr75e\n"
        + "bxidTtE2lAd8K+kzYQQdDp5ervgm4IcdT93LxrxWX4mnSIuezn5B3Y1tz4vLKjbp\n"
        + "YVQz8FnMQp3ATcsZIbWMB7k3sLTq0ypdI0SaWyaTAje28C1Uf0B23Q+ZJYKW0FXR\n"
        + "0fUcvD4GkOUcpYvVzjOpylU7MC+Ps6yncokFdZYl1/87qG/enrjsgPttJNaU+1bu\n"
        + "3YKquQPXcIx7r2FM9G9EDaTgLYDlsNJliUI5A9O6tzcTAgMBAAGjITAfMB0GA1Ud\n"
        + "DgQWBBQUvpvp/MI1xQcVoEY4KxW68FBlITANBgkqhkiG9w0BAQ0FAAOCAQEAfAWb\n"
        + "xixxUca7DME305qsrEimoQOPMzg7ScptO+f+0tBiQq8Mfd4Vx+8aMgpCGXcRZg0Q\n"
        + "BoB0CkUyca7Z8avMgIxXBwvYE6iodo7HvPoCIRhSY5EG7V6wuxU22PrSJ3M506XU\n"
        + "XXMvW+lVtsNODkd7fMVS4mfpgqTDlZVBRSRL8oSGKqpexh9pWAp0+zJrDqcf02Pp\n"
        + "aXtrkYhHCSA+SVCugdcRl7oZV/KIBM4rSarXs2k9Dmf2BcNDBgT2qLkW6aAQHXQS\n"
        + "ZKu5/GPO+YL8y3hxYFrmKW9IQvhss5CLjJQfLiqpsksUE8kaV2OGQbv6rRFDNwDi\n"
        + "Nwj+tEurxRdwIvHICg==\n"
        + "-----END CERTIFICATE-----\n";
    
    @Before
    public void setup() {
        certificateManager = new CertificateLoaderImpl("test", "secrets", temporaryFolder.getRoot().getPath());
    }
    
    @Test
    public void readCertificate_validCertificate_returnCertificate() {
        final Certificate certificate = certificateManager.getCertificate(validServerPublicKey);
        Assertions.assertThat(certificate).as("Valid certificate").isNotNull();
    }
    
    @Test(expected = IllegalStateException.class)
    public void readCertificate_invalidCertificate_returnException() {
        certificateManager.getCertificate("invalid value");
    }
    
    @Test
    public void readPrivateKey_validCertificate_returnCertificate() {
        copyFile("test-secrets-private-key.cer", "test-secrets-private-key.cer");
        final PrivateKey privateKey = certificateManager.getPrivateKey();
        Assertions.assertThat(privateKey).as("Valid private key").isNotNull();
    }
    
    @Test
    public void readPrivateKey_validCertificate_encryptAndDecryptUsingDerivedKey() {
        copyFile("test-secrets-private-key.cer", "test-secrets-private-key.cer");
        final byte[] key = certificateManager.getEncryptionKey();
        final byte[] encrypt = CryptoUtil.encrypt("TEST".getBytes(StandardCharsets.UTF_8), key, PKICryptoService.DEFAULT_ALGORITHM, PKICryptoService.DEFAULT_MODE_PADDING);
        final String result = new String(CryptoUtil.decrypt(encrypt, key, PKICryptoService.DEFAULT_ALGORITHM, PKICryptoService.DEFAULT_MODE_PADDING), StandardCharsets.UTF_8);
        Assertions.assertThat(result).isEqualTo("TEST");
    }
    
    @Test(expected = IllegalStateException.class)
    public void readPrivateKey_invalidCertificate_returnException() {
        copyFile("invalid.cer", "test-secrets-private-key.cer");
        certificateManager.getPrivateKey();
    }
    
    @Test(expected = IllegalStateException.class)
    public void readPrivateKey_absentCertificate_returnException() {
        certificateManager.getPrivateKey();
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
