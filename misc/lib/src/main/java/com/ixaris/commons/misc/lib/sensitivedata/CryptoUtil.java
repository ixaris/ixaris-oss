package com.ixaris.commons.misc.lib.sensitivedata;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;
import java.security.InvalidAlgorithmParameterException;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.security.SecureRandom;

import javax.crypto.BadPaddingException;
import javax.crypto.Cipher;
import javax.crypto.CipherInputStream;
import javax.crypto.CipherOutputStream;
import javax.crypto.IllegalBlockSizeException;
import javax.crypto.NoSuchPaddingException;
import javax.crypto.spec.GCMParameterSpec;
import javax.crypto.spec.IvParameterSpec;
import javax.crypto.spec.SecretKeySpec;

import com.ixaris.commons.misc.lib.conversion.Base64Util;

public class CryptoUtil {
    
    private static final int GCM_AUTHENTICATION_TAG_LENGTH = 128;
    private static final int GCM_IV_LENGTH = 12;
    
    private static final SecureRandom SECURE_RANDOM = new SecureRandom();
    
    public static Cipher createCipher(final String algorithm, final String modeAndPadding) {
        try {
            // get an INSTANCE of cipher
            return Cipher.getInstance(algorithm + modeAndPadding);
        } catch (final NoSuchAlgorithmException | NoSuchPaddingException e) {
            throw new UnsupportedOperationException(e);
        }
    }
    
    /**
     * @return the iv if required or null
     */
    public static byte[] initCipherForEncryptionAndGenerateIvIfRequired(final Cipher cipher, final SecretKeySpec key) {
        try {
            // initialise using the key
            if (cipher.getAlgorithm().contains("/GCM/")) {
                final byte[] iv = generateIv(GCM_IV_LENGTH);
                cipher.init(Cipher.ENCRYPT_MODE, key, new GCMParameterSpec(GCM_AUTHENTICATION_TAG_LENGTH, iv));
                return iv;
            } else if (cipher.getAlgorithm().contains("/CBC/")) {
                final byte[] iv = generateIv(cipher.getBlockSize());
                cipher.init(Cipher.ENCRYPT_MODE, key, new IvParameterSpec(iv));
                return iv;
            } else {
                cipher.init(Cipher.ENCRYPT_MODE, key);
                if (cipher.getIV() != null) {
                    throw new IllegalStateException(String.format("Expecting algorithm %s to not generate an IV", cipher.getAlgorithm()));
                }
                return null;
            }
        } catch (final InvalidKeyException e) {
            throw new IllegalStateException(e);
        } catch (final InvalidAlgorithmParameterException e) {
            throw new UnsupportedOperationException(e);
        }
    }
    
    /**
     * @return the offset of the data without the prepended iv if iv is required, or 0
     */
    public static int initCipherForDecryptionAndGetOffset(final Cipher cipher, final SecretKeySpec key, final byte[] data) {
        try {
            // initialise using the key
            if (cipher.getAlgorithm().contains("/GCM/")) {
                final byte[] iv = extractIv(data);
                cipher.init(Cipher.DECRYPT_MODE, key, new GCMParameterSpec(GCM_AUTHENTICATION_TAG_LENGTH, iv));
                return iv.length + 1;
            } else if (cipher.getAlgorithm().contains("/CBC/")) {
                final byte[] iv = extractIv(data);
                cipher.init(Cipher.DECRYPT_MODE, key, new IvParameterSpec(iv));
                return iv.length + 1;
            } else {
                cipher.init(Cipher.DECRYPT_MODE, key);
                return 0;
            }
        } catch (final InvalidKeyException e) {
            throw new IllegalStateException(e);
        } catch (final InvalidAlgorithmParameterException e) {
            throw new UnsupportedOperationException(e);
        }
    }
    
    /**
     * Decrypt service
     *
     * @param data the data (byte array) to be decrypted
     * @param key key to decryption
     * @param algorithm decryption algorithm for transformation
     * @param modeAndPadding mode and padding for transformation
     * @return the decrypted string
     */
    public static byte[] decrypt(final byte[] data, final byte[] key, final String algorithm, final String modeAndPadding) {
        if (data == null) {
            return new byte[] {};
        }
        try {
            final Cipher cipher = createCipher(algorithm, modeAndPadding);
            final SecretKeySpec keySpec = new SecretKeySpec(key, algorithm);
            final int offset = initCipherForDecryptionAndGetOffset(cipher, keySpec, data);
            return cipher.doFinal(data, offset, data.length - offset);
        } catch (final IllegalBlockSizeException | BadPaddingException e) {
            throw new IllegalStateException(e);
        }
    }
    
    /**
     * Decrypt service
     *
     * @param data the string data to be decrypted
     * @return the decrypted string
     */
    public static String decrypt(final String data, final byte[] key, final String algorithm, final String modeAndPadding) {
        return decryptToString(Base64Util.decode(data), key, algorithm, modeAndPadding);
    }
    
    /**
     * @param in data to decrypt prepended by IV length and IV
     * @param out Saves the data stored in {@param in} but in decrypted form
     * @throws IllegalArgumentException whenever the ivLength is not prepended in the input stream. This might indicate
     *     that the file was not encrypted using GCM
     * @throws IOException whenever there is a IO problem with reading the file or writing to the output stream
     */
    public static void decrypt(final InputStream in,
                               final OutputStream out,
                               final byte[] key,
                               final String algorithm,
                               final String modeAndPadding) throws IOException {
        final Cipher cipher = createCipher(algorithm, modeAndPadding);
        final SecretKeySpec keySpec = new SecretKeySpec(key, algorithm);
        if (requiresIv(cipher)) {
            final byte ivLength = (byte) in.read();
            if (ivLength <= 0) {
                throw new IllegalStateException("Invalid ivLength prepended to file");
            }
            final byte[] iv = new byte[ivLength];
            final int ivRead = in.read(iv);
            if (ivRead != ivLength) {
                throw new IllegalStateException("Expected " + ivLength + " bytes but got " + ivRead);
            }
            initCipherForDecryptionAndGetOffset(cipher, keySpec, iv);
        } else {
            initCipherForDecryptionAndGetOffset(cipher, keySpec, null);
        }
        
        final CipherInputStream cis = new CipherInputStream(in, cipher);
        final byte[] data = new byte[4096];
        int read = cis.read(data);
        while (read != -1) {
            out.write(data, 0, read);
            read = cis.read(data);
        }
    }
    
    public static String decryptToString(final byte[] data, final byte[] key, final String algorithm, final String modeAndPadding) {
        return new String(decrypt(data, key, algorithm, modeAndPadding), StandardCharsets.UTF_8);
    }
    
    /**
     * Encrypt service
     *
     * @param data the data (utf-8 string) to be encrypted
     * @param modeAndPadding either empty string or string starting with /
     * @return the encrypted data as a btye array
     */
    public static byte[] encrypt(final byte[] data, final byte[] key, final String algorithm, final String modeAndPadding) {
        if (data == null) {
            return new byte[] {};
        }
        try {
            final Cipher cipher = createCipher(algorithm, modeAndPadding);
            final SecretKeySpec keySpec = new SecretKeySpec(key, algorithm);
            final byte[] iv = initCipherForEncryptionAndGenerateIvIfRequired(cipher, keySpec);
            final byte[] encrypted = cipher.doFinal(data);
            if (iv != null) {
                final byte[] out = new byte[1 + iv.length + encrypted.length];
                out[0] = (byte) iv.length;
                System.arraycopy(iv, 0, out, 1, iv.length);
                System.arraycopy(encrypted, 0, out, iv.length + 1, encrypted.length);
                return out;
            } else {
                return encrypted;
            }
        } catch (final IllegalBlockSizeException | BadPaddingException e) {
            throw new IllegalStateException(e);
        }
    }
    
    public static byte[] encrypt(final String data, final byte[] key, final String algorithm, final String modeAndPadding) {
        return encrypt(data.getBytes(StandardCharsets.UTF_8), key, algorithm, modeAndPadding);
    }
    
    /**
     * @param in data to encrypt
     * @param out Saves contents of {@param in} in encrypted form prepended by IV length and IV
     */
    public static void encrypt(final InputStream in,
                               final OutputStream out,
                               final byte[] key,
                               final String algorithm,
                               final String modeAndPadding) throws IOException {
        final Cipher cipher = createCipher(algorithm, modeAndPadding);
        final SecretKeySpec keySpec = new SecretKeySpec(key, algorithm);
        final byte[] iv = initCipherForEncryptionAndGenerateIvIfRequired(cipher, keySpec);
        
        if (iv != null) {
            out.write((byte) iv.length);
            out.write(iv);
            out.flush();
        }
        
        try (final CipherOutputStream cipherOutputStream = new CipherOutputStream(out, cipher)) {
            final byte[] b = new byte[4096];
            int read;
            while ((read = in.read(b)) != -1) {
                cipherOutputStream.write(b, 0, read);
            }
        }
    }
    
    /**
     * Encrypt service
     *
     * @param data the data (utf-8 string) to be encrypted
     * @return the encrypted data as a string
     */
    public static String encryptToString(final String data, final byte[] key, final String algorithm, final String modeAndPadding) {
        return Base64Util.encode(encrypt(data, key, algorithm, modeAndPadding));
    }
    
    private static boolean requiresIv(final Cipher cipher) {
        return (cipher.getAlgorithm().contains("/GCM/") || cipher.getAlgorithm().contains("/CBC/"));
    }
    
    private static byte[] generateIv(final int length) {
        final byte[] iv = new byte[length]; // For GCM, use a 12 byte (not 16) random byte-array, as recommended by NIST
        SECURE_RANDOM.nextBytes(iv); // NEVER REUSE THIS IV WITH SAME KEY
        return iv;
    }
    
    private static byte[] extractIv(byte[] data) {
        final int ivLength = data[0] & 0xff;
        final byte[] iv = new byte[ivLength];
        System.arraycopy(data, 1, iv, 0, iv.length);
        return iv;
    }
    
    private CryptoUtil() {}
    
}
