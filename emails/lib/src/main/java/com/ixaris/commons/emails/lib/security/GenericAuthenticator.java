package com.ixaris.commons.emails.lib.security;

import javax.mail.Authenticator;
import javax.mail.PasswordAuthentication;

/**
 * Class implementing {@link Authenticator Authenticator} used when initiating the session with mail server.
 *
 * @author <a href="mailto:lazar.agatonovic@ixaris.com">lazar.agatonovic</a>
 */
public final class GenericAuthenticator extends Authenticator {
    
    private final String username;
    private final String password;
    
    public GenericAuthenticator(final String username, final String password) {
        this.username = username;
        this.password = password;
    }
    
    @Override
    public PasswordAuthentication getPasswordAuthentication() {
        return new PasswordAuthentication(this.username, this.password);
    }
    
}
