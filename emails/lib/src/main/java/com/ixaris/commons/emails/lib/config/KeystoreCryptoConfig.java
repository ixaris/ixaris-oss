package com.ixaris.commons.emails.lib.config;

import static com.ixaris.commons.misc.lib.object.Tuple.tuple;

import java.io.IOException;
import java.io.InputStream;
import java.security.GeneralSecurityException;
import java.security.Key;
import java.security.KeyStore;
import java.security.PrivateKey;
import java.security.cert.Certificate;

import com.ixaris.commons.misc.lib.object.Tuple2;

public final class KeystoreCryptoConfig implements CryptoConfig {
    
    private final String keystorePassword;
    private final KeyStore keyStore;
    
    @SuppressWarnings("squid:S1166 ")
    public KeystoreCryptoConfig(final InputStream keystoreStream, final String keystorePassword) {
        this.keystorePassword = keystorePassword;
        try {
            keyStore = KeyStore.getInstance("pkcs12");
            keyStore.load(keystoreStream, keystorePassword.toCharArray());
        } catch (final GeneralSecurityException | IOException e) {
            throw new IllegalStateException(e);
        }
    }
    
    @Override
    public Tuple2<PrivateKey, Certificate[]> getSenderPrivateKeyAndCertificateChain(final String senderAlias) {
        try {
            final Key senderKey = keyStore.getKey(senderAlias, keystorePassword.toCharArray());
            if (!(senderKey instanceof PrivateKey)) {
                throw new IllegalStateException("Sender private key not found for sender [" + senderAlias + "]");
            }
            
            Certificate[] senderChain = keyStore.getCertificateChain(senderAlias);
            if (senderChain == null) {
                final Certificate senderCertificate = keyStore.getCertificate(senderAlias);
                if (senderCertificate == null) {
                    throw new IllegalStateException("Sender certificate not found for sender [" + senderAlias + "]");
                }
                senderChain = new Certificate[] { senderCertificate };
            }
            
            return tuple((PrivateKey) senderKey, senderChain);
        } catch (final GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
    
    @Override
    public Certificate[] getRecipientCertificateChain(final String recipientAlias) {
        try {
            Certificate[] recipientChain = keyStore.getCertificateChain(recipientAlias);
            if (recipientChain == null) {
                final Certificate recipientCertificate = keyStore.getCertificate(recipientAlias);
                if (recipientCertificate == null) {
                    throw new IllegalStateException("Recipient certificate not found for recipient [" + recipientAlias + "]");
                }
                recipientChain = new Certificate[] { recipientCertificate };
            }
            
            return recipientChain;
        } catch (final GeneralSecurityException e) {
            throw new IllegalStateException(e);
        }
    }
    
}
