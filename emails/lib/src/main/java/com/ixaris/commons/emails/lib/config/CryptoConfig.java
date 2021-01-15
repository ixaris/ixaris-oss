package com.ixaris.commons.emails.lib.config;

import java.security.PrivateKey;
import java.security.cert.Certificate;

import com.ixaris.commons.misc.lib.object.Tuple2;

/**
 * Class representing configuration data needed for email content encryption. Used in case when content of actual email
 * to be sent needs to be encrypted, rather than encrypting it before storing it in the database.
 *
 * @author <a href="mailto:lazar.agatonovic@ixaris.com">lazar.agatonovic</a>
 */
public interface CryptoConfig {
    
    Tuple2<PrivateKey, Certificate[]> getSenderPrivateKeyAndCertificateChain(String senderAlias);
    
    Certificate[] getRecipientCertificateChain(String recipientAlias);
    
}
