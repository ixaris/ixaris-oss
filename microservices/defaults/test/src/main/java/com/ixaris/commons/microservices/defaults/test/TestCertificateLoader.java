package com.ixaris.commons.microservices.defaults.test;

import java.io.BufferedReader;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;
import java.util.Arrays;
import java.util.stream.Collectors;

import com.ixaris.commons.microservices.secrets.CertificateLoader;
import com.ixaris.commons.misc.lib.conversion.Base64Util;

/**
 * CertificateLoader for testing purposes Created by tiago.cucki on 14/07/2017.
 */
public class TestCertificateLoader implements CertificateLoader {
    
    private static final String DEFAULT_CLIENT_KEY_FILE_NAME = "secrets/test-microservices-client-key.cer";
    
    private static final String DEFAULT_PUBLIC_KEY_FILE_NAME = "secrets/test-microservices-public-key.cer";
    
    private static final String DEFAULT_PRIVATE_KEY_FILE_NAME = "secrets/test-microservices-private-key.cer";
    
    private static final CertificateFactory CERTIFICATE_FACTORY;
    
    private Certificate publicCertificate;
    
    private PrivateKey privateKey;
    
    private byte[] encryptionKey;
    
    private String clientKeyFileName;
    private String publicKeyFileName;
    private String privateKeyFileName;
    
    public TestCertificateLoader() {
        this.clientKeyFileName = DEFAULT_CLIENT_KEY_FILE_NAME;
        this.publicKeyFileName = DEFAULT_PUBLIC_KEY_FILE_NAME;
        this.privateKeyFileName = DEFAULT_PRIVATE_KEY_FILE_NAME;
    }
    
    public TestCertificateLoader(final String clientKeyFileName, final String publicKeyFileName, final String privateKeyFileName) {
        this.clientKeyFileName = clientKeyFileName;
        this.publicKeyFileName = publicKeyFileName;
        this.privateKeyFileName = privateKeyFileName;
    }
    
    static {
        try {
            CERTIFICATE_FACTORY = CertificateFactory.getInstance("X.509");
        } catch (final CertificateException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private static Certificate getCertificate(final byte[] certificateData) {
        try (final InputStream stream = new ByteArrayInputStream(certificateData)) {
            return CERTIFICATE_FACTORY.generateCertificate(stream);
        } catch (final IOException | CertificateException e) {
            throw new IllegalStateException(e);
        }
    }
    
    /**
     * Independently of the input parameter certificateData, returns the pre-configured test public key
     *
     * @param certificateData
     * @return
     */
    @Override
    public Certificate getCertificate(final String certificateData) {
        if (publicCertificate == null) {
            loadPublicCertificate();
        }
        return publicCertificate;
    }
    
    @Override
    public PrivateKey getPrivateKey() {
        loadPrivateKeyAndDeriveEncryptionKey();
        return privateKey;
    }
    
    @Override
    public byte[] getEncryptionKey() {
        loadPrivateKeyAndDeriveEncryptionKey();
        return Arrays.copyOf(encryptionKey, encryptionKey.length);
    }
    
    private static final int KEY_SIZE = 32;
    
    private void loadPrivateKeyAndDeriveEncryptionKey() {
        if (privateKey == null) {
            synchronized (this) {
                if (privateKey == null) {
                    final byte[] certificateData = getDecodedCertificate(privateKeyFileName);
                    // derive encryption key from private key
                    encryptionKey = new byte[KEY_SIZE];
                    System.arraycopy(certificateData, 0, encryptionKey, 0, KEY_SIZE);
                    for (int i = KEY_SIZE, index = 0; i < certificateData.length; i++, index++) {
                        if (index == KEY_SIZE) {
                            index = 0;
                        }
                        
                        encryptionKey[index] = (byte) (encryptionKey[index] ^ certificateData[i]);
                    }
                    
                    final PKCS8EncodedKeySpec spec = new PKCS8EncodedKeySpec(certificateData);
                    try {
                        privateKey = KeyFactory.getInstance("RSA").generatePrivate(spec);
                    } catch (NoSuchAlgorithmException | InvalidKeySpecException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
        }
    }
    
    private void loadPublicCertificate() {
        publicCertificate = getCertificate(loadCertificateFile(publicKeyFileName).getBytes());
    }
    
    private byte[] getDecodedCertificate(final String certificateFileName) {
        final String cert = new String(getCertificateAsByteArray(certificateFileName)).replaceAll("----.*----\r?\n", "");
        return Base64Util.decode(cert);
    }
    
    private byte[] getCertificateAsByteArray(final String certificateFileName) {
        try (final InputStream is = getClass().getClassLoader().getResourceAsStream(certificateFileName)) { // NOSONAR keep same as commons
            final ByteArrayOutputStream bos = new ByteArrayOutputStream();
            final byte[] buffer = new byte[1024];
            int readBytes;
            while ((readBytes = is.read(buffer)) > 0) {
                bos.write(buffer, 0, readBytes);
            }
            return bos.toByteArray();
        } catch (IOException ioe) {
            throw new IllegalStateException(ioe);
        }
    }
    
    private String loadCertificateFile(final String certificateFileName) {
        try (
            final InputStream inputStream = getClass().getClassLoader().getResourceAsStream(certificateFileName); // NOSONAR keep same as commons
            final BufferedReader input = new BufferedReader(new InputStreamReader(inputStream, StandardCharsets.UTF_8))) {
            return input.lines().collect(Collectors.joining("\n"));
        } catch (final IOException e) {
            throw new IllegalStateException(String.format("Error reading test certificate file %s from class path", certificateFileName), e);
        }
    }
    
    public String getClientKeyFileName() {
        return clientKeyFileName;
    }
    
    protected void setClientKeyFileName(final String clientKeyFileName) {
        this.clientKeyFileName = clientKeyFileName;
    }
    
    public String getPublicKeyFileName() {
        return publicKeyFileName;
    }
    
    protected void setPublicKeyFileName(final String publicKeyFileName) {
        this.publicKeyFileName = publicKeyFileName;
    }
    
    public String getPrivateKeyFileName() {
        return privateKeyFileName;
    }
    
    protected void setPrivateKeyFileName(final String privateKeyFileName) {
        this.privateKeyFileName = privateKeyFileName;
    }
}
