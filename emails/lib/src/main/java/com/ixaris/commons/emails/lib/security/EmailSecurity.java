package com.ixaris.commons.emails.lib.security;

/**
 * Enum representing basic types of email security.
 *
 * @author <a href="mailto:lazar.agatonovic@ixaris.com">lazar.agatonovic</a>
 */
public enum EmailSecurity {
    
    NONE('N', false, false),
    SIGN('S', true, false),
    ENCRYPT('E', false, true),
    SIGN_ENCRYPT('A', true, true);
    
    private final boolean sign;
    private final boolean encrypt;
    private final char code;
    
    EmailSecurity(final char code, final boolean sign, final boolean encrypt) {
        this.code = code;
        this.sign = sign;
        this.encrypt = encrypt;
    }
    
    public boolean isSign() {
        return sign;
    }
    
    public boolean isEncrypt() {
        return encrypt;
    }
    
    public char getCode() {
        return code;
    }
    
}
