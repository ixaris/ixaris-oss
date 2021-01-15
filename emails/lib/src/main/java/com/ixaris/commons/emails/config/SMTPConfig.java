package com.ixaris.commons.emails.config;

import com.ixaris.commons.emails.lib.config.SmtpConfig;

/**
 * @deprecated use {@link SmtpConfig}
 */
@Deprecated
public class SMTPConfig extends SmtpConfig {
    
    public SMTPConfig(final String host,
                      final Integer port,
                      final Integer securePort,
                      final boolean secure,
                      final String username,
                      final String password) {
        super(secure, host, secure ? securePort : port, username, password);
    }
    
}
