package com.ixaris.commons.microservices.secrets;

import java.security.NoSuchAlgorithmException;

import javax.crypto.KeyGenerator;

import org.assertj.core.api.Assertions;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.MockitoJUnitRunner;

/**
 * A test suite for {@link PKICryptoService}.
 *
 * @author <a href="mailto:jacob.falzon@ixaris.com">jacob.falzon</a>.
 */
@RunWith(MockitoJUnitRunner.class)
public class PKICryptoServiceTest {
    
    @InjectMocks
    private PKICryptoService cryptoService;
    
    @Mock
    private CertificateLoader certificateLoader;
    
    private static final String TEXT = "PLAIN_TEXT";
    
    @Before
    public void setup() throws NoSuchAlgorithmException {
        Mockito.when(certificateLoader.getEncryptionKey()).thenReturn(generateEncryptionKey());
    }
    
    @Test
    public void encrypt_decrypt_decryptedTextMatchesOriginalText() {
        
        final String cipherText = cryptoService.encrypt(TEXT);
        
        Assertions.assertThat(cipherText).isNotNull();
        Assertions.assertThat(cipherText).isNotEqualTo(TEXT);
        
        final String decryptedText = cryptoService.decrypt(cipherText);
        
        Assertions.assertThat(decryptedText).isNotNull();
        Assertions.assertThat(decryptedText).isEqualTo(TEXT);
    }
    
    private static byte[] generateEncryptionKey() throws NoSuchAlgorithmException {
        return KeyGenerator.getInstance("AES").generateKey().getEncoded();
    }
    
}
