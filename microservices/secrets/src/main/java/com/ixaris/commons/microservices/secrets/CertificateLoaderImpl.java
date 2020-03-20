package com.ixaris.commons.microservices.secrets;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.security.KeyFactory;
import java.security.NoSuchAlgorithmException;
import java.security.PrivateKey;
import java.security.cert.Certificate;
import java.security.cert.CertificateException;
import java.security.cert.CertificateFactory;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.PKCS8EncodedKeySpec;

import com.ixaris.commons.misc.lib.conversion.Base64Util;

/**
 * Default implementation for retrieving client, server and private certificates for the current service and environment Created by tiago.cucki
 * on 14/07/2017.
 */
public final class CertificateLoaderImpl implements CertificateLoader {
    
    private static final CertificateFactory CERTIFICATE_FACTORY;
    
    private final String privateKeyFileName;
    
    private volatile PrivateKey privateKey;
    private byte[] encryptionKey;
    
    static {
        try {
            CERTIFICATE_FACTORY = CertificateFactory.getInstance("X.509");
        } catch (final CertificateException e) {
            throw new IllegalStateException(e);
        }
    }
    
    @Deprecated
    public CertificateLoaderImpl(final CertificateConfig certificateConfig) {
        this(certificateConfig.getEnvironment(), certificateConfig.getServiceName(), certificateConfig.getCertificateRootPath());
    }
    
    public CertificateLoaderImpl(final String environment, final String serviceName, final String certificateRootPath) {
        final String rootFileName = certificateRootPath + "/" + environment + "-" + serviceName;
        privateKeyFileName = rootFileName + "-private-key.cer";
    }
    
    /**
     * @param certificateData
     * @return Certificate object from a certificate string
     */
    @Override
    public Certificate getCertificate(final String certificateData) {
        if (certificateData == null) {
            throw new IllegalArgumentException("certificateData is null");
        }
        return getCertificate(certificateData.getBytes());
    }
    
    private Certificate getCertificate(final byte[] certificateData) {
        try (final InputStream stream = new ByteArrayInputStream(certificateData)) {
            return CERTIFICATE_FACTORY.generateCertificate(stream);
        } catch (final IOException | CertificateException e) {
            throw new IllegalStateException(e);
        }
    }
    
    /**
     * @return PrivateKey object from current environment and service, looking for the certificate file on secrets folder
     */
    @Override
    public PrivateKey getPrivateKey() {
        loadPrivateKeyAndDeriveEncryptionKey();
        return privateKey;
    }
    
    @Override
    public byte[] getEncryptionKey() {
        loadPrivateKeyAndDeriveEncryptionKey();
        return encryptionKey;
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
    
    private byte[] getDecodedCertificate(final String certificateFileName) {
        final String cert = new String(getCertificateAsByteArray(certificateFileName)).replaceAll("----.*----\r?\n", "");
        return Base64Util.decode(cert);
    }
    
    private byte[] getCertificateAsByteArray(final String certificateFileName) {
        try (final InputStream is = new FileInputStream(certificateFileName)) {
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
}
