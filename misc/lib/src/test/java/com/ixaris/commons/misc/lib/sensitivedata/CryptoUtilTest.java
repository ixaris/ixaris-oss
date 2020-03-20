package com.ixaris.commons.misc.lib.sensitivedata;

import static org.assertj.core.api.Assertions.assertThat;

import java.nio.charset.StandardCharsets;
import java.security.SecureRandom;

import javax.crypto.spec.SecretKeySpec;

import org.junit.Test;

public class CryptoUtilTest {
    
    private SecureRandom secureRandom = new SecureRandom();
    
    @Test
    public void testEncrypt_AES_GCM_NOPADDING() {
        // When using SSM, this would be retrieved from SSM
        final SecretKeySpec key = generateKey();
        
        final String toEncrypt = "somerandomlongemail@somelongerdomain.com";
        final byte[] toEncryptBytes = toEncrypt.getBytes(StandardCharsets.UTF_8);
        
        final byte[] encryptedData = CryptoUtil.encrypt(toEncryptBytes, key.getEncoded(), "AES", "/GCM/NoPadding");
        final byte[] decryptedData = CryptoUtil.decrypt(encryptedData, key.getEncoded(), "AES", "/GCM/NoPadding");
        
        assertThat(toEncryptBytes).isEqualTo(decryptedData);
    }
    
    @Test
    public void testEncrypt_AES_CBC_PKCS5PADDING() {
        // When using SSM, this would be retrieved from SSM
        final SecretKeySpec key = generateKey();
        
        final String toEncrypt = "somerandomlongemail@somelongerdomain.com";
        final byte[] toEncryptBytes = toEncrypt.getBytes(StandardCharsets.UTF_8);
        
        final byte[] encryptedData = CryptoUtil.encrypt(toEncryptBytes, key.getEncoded(), "AES", "/CBC/PKCS5Padding");
        final byte[] decryptedData = CryptoUtil.decrypt(encryptedData, key.getEncoded(), "AES", "/CBC/PKCS5Padding");
        
        assertThat(toEncryptBytes).isEqualTo(decryptedData);
    }
    
    private SecretKeySpec generateKey() {
        final byte[] key = new byte[16]; // 16 bytes = 128 bit
        secureRandom.nextBytes(key);
        return new SecretKeySpec(key, "AES");
    }
    
}
