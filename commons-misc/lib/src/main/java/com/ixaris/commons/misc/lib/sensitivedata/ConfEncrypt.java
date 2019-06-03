package com.ixaris.commons.misc.lib.sensitivedata;

import java.nio.charset.Charset;

/**
 * DO NOT USE FOR SENSITIVE INFORMATION. Currently used for reading/writing encrypted file values (or things like
 * password to the SSM, which obviously cannot be encrypted by the SSM). Created by tiago.cucki on 24/11/2016.
 */
public class ConfEncrypt {
    
    private static final String ALGORITHM = "AES";
    private static final String MODE_PADDING = "/ECB/PKCS5Padding";
    
    private static final byte[] KEY = "AZAZB3NzaC1kc3MAAACBAKv0stHlfMsx".getBytes(Charset.forName("UTF-8"));
    
    public static void main(final String... args) {
        if (args.length == 1) {
            println(encryptValueUsingInsecureKey(args[0]));
        } else if (args.length == 2 && args[0].equals("-e")) {
            println(encryptValueUsingInsecureKey(args[1]));
        } else if (args.length == 2 && args[0].equals("-d")) {
            println(encryptValueUsingInsecureKey(args[1]));
        } else {
            println("Incorrect number of parameters.");
            println("Usage: ");
            println("  To encrypt: confencrypt.bat <STRING_TO_ENCRYPT> or confencrypt.bat -e <STRING_TO_ENCRYPT>");
            println("  To decrypt: confencrypt.bat -d <STRING_TO_DECRYPT>");
        }
    }
    
    private static void println(final String message) {
        System.out.println(message); // NOPMD
    }
    
    /**
     * DO NOT USE FOR SENSITIVE INFORMATION. Currently used for reading/writing encrypted file values (or things like
     * password to the SSM, which obviously cannot be encrypted by the SSM).
     *
     * @param value the string to encrypt
     * @return an INSECURELY encrypted string
     */
    public static String encryptValueUsingInsecureKey(final String value) {
        return CryptoUtil.encryptToString(value, KEY, ALGORITHM, MODE_PADDING);
    }
    
    public static String decryptValueUsingInsecureKey(final String value) {
        return CryptoUtil.decrypt(value, KEY, ALGORITHM, MODE_PADDING);
    }
    
    private ConfEncrypt() {}
    
}
