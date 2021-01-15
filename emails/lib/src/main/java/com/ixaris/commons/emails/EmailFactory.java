package com.ixaris.commons.emails;

import javax.mail.Session;
import javax.mail.Transport;

import com.ixaris.commons.emails.config.SMTPConfig;
import com.ixaris.commons.emails.lib.config.CryptoConfig;
import com.ixaris.commons.emails.security.EmailSecurity;
import com.ixaris.commons.misc.lib.object.Tuple2;

/**
 * @deprecated use {@link com.ixaris.commons.emails.lib.EmailFactory}
 */
@Deprecated
public class EmailFactory {
    
    public EmailFactory() {}
    
    public void send(final String senderAddress,
                     final String recipientAddress,
                     final String subject,
                     final String textContent,
                     final String htmlContent,
                     final EmailSecurity emailSecurity,
                     final SMTPConfig smtpConfig,
                     final CryptoConfig cryptoConfig) {
        
        com.ixaris.commons.emails.lib.EmailFactory.blockingSend(senderAddress,
            null,
            recipientAddress,
            null,
            subject,
            textContent,
            htmlContent,
            null,
            com.ixaris.commons.emails.lib.security.EmailSecurity.valueOf(emailSecurity.name()),
            smtpConfig,
            cryptoConfig);
    }
    
    public static Tuple2<Session, Transport> getSMTPSession(final SMTPConfig smtpConfig) throws javax.mail.NoSuchProviderException {
        return smtpConfig.getSMTPSession();
    }
    
}
