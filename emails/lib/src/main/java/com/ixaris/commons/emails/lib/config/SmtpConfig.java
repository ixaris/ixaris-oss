package com.ixaris.commons.emails.lib.config;

import static com.ixaris.commons.misc.lib.object.Tuple.tuple;

import java.util.Properties;

import javax.mail.NoSuchProviderException;
import javax.mail.Session;
import javax.mail.Transport;

import com.ixaris.commons.emails.lib.security.GenericAuthenticator;
import com.ixaris.commons.misc.lib.object.Tuple2;

/**
 * Class representing configuration data of SMTP server used when sending emails.
 *
 * @author <a href="mailto:lazar.agatonovic@ixaris.com">lazar.agatonovic</a>
 */
public class SmtpConfig {
    
    private static final int CONNECTION_TIMEOUT = 30_000;
    
    private final boolean secure;
    private final String host;
    private final int port;
    private final String username;
    private final String password;
    
    public SmtpConfig(final boolean secure,
                      final String host,
                      final int port,
                      final String username,
                      final String password) {
        if (host == null) {
            throw new IllegalArgumentException("host is null");
        }
        if (username == null) {
            throw new IllegalArgumentException("username is null");
        }
        if (password == null) {
            throw new IllegalArgumentException("passwordPassword is null");
        }
        
        this.secure = secure;
        this.host = host;
        this.port = port;
        this.username = username;
        this.password = password;
    }
    
    public boolean isSecure() {
        return secure;
    }
    
    public String getHost() {
        return host;
    }
    
    public Integer getPort() {
        return port;
    }
    
    public String getUsername() {
        return username;
    }
    
    public String getPassword() {
        return password;
    }
    
    public Tuple2<Session, Transport> getSMTPSession() throws NoSuchProviderException {
        final Properties props = new Properties();
        if (secure) {
            props.put("mail.transport.protocol", "smtps");
            props.put("mail.smtps.host", host);
            props.put("mail.smtps.auth", "true");
            props.put("mail.smtps.port", Integer.toString(port));
            props.put("mail.smtps.connectiontimeout", CONNECTION_TIMEOUT);
            props.put("mail.smtps.timeout", CONNECTION_TIMEOUT);
        } else {
            props.put("mail.smtp.host", host);
            props.put("mail.smtp.auth", "true");
            props.put("mail.smtp.port", Integer.toString(port));
            props.put("mail.smtp.connectiontimeout", CONNECTION_TIMEOUT);
            props.put("mail.smtp.timeout", CONNECTION_TIMEOUT);
            props.put("mail.smtp.starttls.enable", "true");
        }
        final GenericAuthenticator auth = new GenericAuthenticator(username, password);
        
        final Session session = Session.getInstance(props, auth);
        session.setDebug(false);
        
        final Transport transport;
        if (secure) {
            transport = session.getTransport("smtps");
        } else {
            transport = session.getTransport("smtp");
        }
        
        return tuple(session, transport);
    }
    
}
