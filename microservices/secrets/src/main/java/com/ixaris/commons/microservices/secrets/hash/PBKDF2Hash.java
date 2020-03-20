package com.ixaris.commons.microservices.secrets.hash;

import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;
import java.security.spec.InvalidKeySpecException;
import java.security.spec.KeySpec;

import javax.crypto.SecretKeyFactory;
import javax.crypto.spec.PBEKeySpec;

import com.ixaris.commons.misc.lib.conversion.HexUtil;

/**
 * PBKDF2 (Password-Based Key Derivation Function 2) is a salted secret hashing algorithm, currently classified as one of the industry best
 * security mechanisms, aimed to reduce the vulnerability of encrypted keys to brute force attacks.
 *
 * @author <a href="mailto:bernice.zerafa@ixaris.com">bernice.zerafa</a>
 */
public final class PBKDF2Hash {
    
    private static final String PBKDF2_ALGORITHM = "PBKDF2WithHmacSHA1";
    
    // The following constants may be changed without breaking existing hashes.
    private static final int SALT_BYTES = 24;
    private static final int HASH_BYTES = 24;
    private static final int PBKDF2_ITERATIONS = 1000;
    
    /**
     * @param secret the secret to hash
     * @return a salted PBKDF2 hash of the secret
     */
    public static String createHash(final String secret) throws NoSuchAlgorithmException, InvalidKeySpecException {
        return createHash(secret.toCharArray());
    }
    
    /**
     * @param secret the secret to hash
     * @return a salted PBKDF2 hash of the secret
     */
    private static String createHash(final char[] secret) throws NoSuchAlgorithmException, InvalidKeySpecException {
        final SecureRandom random = new SecureRandom();
        final byte[] salt = new byte[SALT_BYTES];
        random.nextBytes(salt);
        
        final byte[] hash = pbkdf2(secret, salt, PBKDF2_ITERATIONS, HASH_BYTES);
        return String.format("%d:%s:%s", PBKDF2_ITERATIONS, HexUtil.encode(salt), HexUtil.encode(hash));
    }
    
    /**
     * @param secret the secret to hash.
     * @param salt the salt
     * @param iterations the iteration count (slowness factor)
     * @param bytes the length of the hash to compute in bytes
     * @return the PBDKF2 hash of the secret
     */
    private static byte[] pbkdf2(final char[] secret, final byte[] salt, final int iterations, final int bytes) throws NoSuchAlgorithmException, InvalidKeySpecException {
        final KeySpec spec = new PBEKeySpec(secret, salt, iterations, bytes * 8);
        final SecretKeyFactory skf = SecretKeyFactory.getInstance(PBKDF2_ALGORITHM);
        return skf.generateSecret(spec).getEncoded();
    }
    
    private PBKDF2Hash() {}
    
}
