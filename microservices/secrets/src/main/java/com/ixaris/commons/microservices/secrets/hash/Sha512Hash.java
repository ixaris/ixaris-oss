package com.ixaris.commons.microservices.secrets.hash;

import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.util.Base64;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:bernice.zerafa@ixaris.com">bernice.zerafa</a>
 */
public final class Sha512Hash {
    
    private static final Logger LOG = LoggerFactory.getLogger(Sha512Hash.class);
    
    public static String generate(final String data) {
        try {
            final MessageDigest digest = MessageDigest.getInstance("SHA-512");
            digest.reset();
            digest.update(data.getBytes());
            return Base64.getEncoder().encodeToString(digest.digest());
        } catch (final NoSuchAlgorithmException e) {
            LOG.error("Hashing failed due to wrong algorithm being used in MessageDigest ", e);
            return String.valueOf(data.hashCode());
        }
    }
    
    private Sha512Hash() {}
}
