package com.ixaris.commons.microservices.secrets;

import com.ixaris.commons.misc.lib.sensitivedata.CryptoUtil;

/**
 * A utility class that is used for encryption/decryption by using an encryption key that is generated from the private key of a particular
 * service.
 *
 * @author <a href="mailto:jacob.falzon@ixaris.com">jacob.falzon</a>.
 */
public final class PKICryptoService {
    
    public static final String DEFAULT_ALGORITHM = "AES";
    public static final String DEFAULT_MODE_PADDING = "/ECB/PKCS5Padding";
    
    private static PKICryptoService instance;
    
    private final CertificateLoader certificateLoader;
    
    public PKICryptoService(final CertificateLoader certificateLoader) {
        this.certificateLoader = certificateLoader;
        instance = this;
    }
    
    public static PKICryptoService getInstance() {
        return instance;
    }
    
    /**
     * @param plainText the {@link String} to be encrypted.
     * @return the encrypted {@link String}.
     */
    public String encrypt(final String plainText) {
        return CryptoUtil.encryptToString(plainText, certificateLoader.getEncryptionKey(), DEFAULT_ALGORITHM, DEFAULT_MODE_PADDING);
    }
    
    /**
     * @param cipherText the {@link String} to be decrypted.
     * @return the decrypted {@link String}.
     */
    public String decrypt(final String cipherText) {
        return CryptoUtil.decrypt(cipherText, certificateLoader.getEncryptionKey(), DEFAULT_ALGORITHM, DEFAULT_MODE_PADDING);
    }
}
