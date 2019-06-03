package com.ixaris.commons.misc.lib.sensitivedata;

import static org.junit.jupiter.api.Assertions.assertEquals;

import org.junit.jupiter.api.Test;

public class ConfEncryptTest {
    
    private static final String DECRYPTED_PASSWORD = "Password123";
    private static final String ENCRYPTED_PASSWORD = "XyS3oVensWR6Nzg1EdEseA==";
    
    @Test
    public void testConfEncrypt_encryptValue() {
        assertEquals(
            ENCRYPTED_PASSWORD,
            ConfEncrypt.encryptValueUsingInsecureKey(DECRYPTED_PASSWORD),
            "Encrypted value not expected"
        );
    }
    
    @Test
    public void testConfEncrypt_decryptValue() {
        assertEquals(
            DECRYPTED_PASSWORD,
            ConfEncrypt.decryptValueUsingInsecureKey(ENCRYPTED_PASSWORD),
            "Decrypted value not expected"
        );
    }
    
}
