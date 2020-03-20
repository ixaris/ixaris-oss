package com.ixaris.commons.misc.lib.sensitivedata;

import static junit.framework.TestCase.*;

import org.junit.Test;

/**
 * Created by tiago.cucki on 24/11/2016.
 */
public class ConfEncryptTest {
    
    private static final String DECRYPTED_PASSWORD = "Password123";
    private static final String ENCRYPTED_PASSWORD = "XyS3oVensWR6Nzg1EdEseA==";
    
    @Test
    public void testConfEncrypt_encryptValue() {
        assertEquals("Encrypted value not expected", ENCRYPTED_PASSWORD, ConfEncrypt.encryptValueUsingInsecureKey(DECRYPTED_PASSWORD));
    }
    
    @Test
    public void testConfEncrypt_decryptValue() {
        assertEquals("Decrypted value not expected", DECRYPTED_PASSWORD, ConfEncrypt.decryptValueUsingInsecureKey(ENCRYPTED_PASSWORD));
    }
}
