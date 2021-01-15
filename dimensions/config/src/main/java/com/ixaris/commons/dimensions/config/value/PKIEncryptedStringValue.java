package com.ixaris.commons.dimensions.config.value;

import java.util.Objects;

import com.ixaris.commons.microservices.secrets.PKICryptoService;
import com.ixaris.commons.misc.lib.object.EqualsUtil;

/**
 * A {@link String} value that is encrypted before being persisted. The encryption/decryption is handled by the {@link PKICryptoService}.
 *
 * @author <a href="mailto:jacob.falzon@ixaris.com">jacob.falzon</a>.
 */
public final class PKIEncryptedStringValue extends Value {
    
    public static Builder newBuilder() {
        return new Builder();
    }
    
    public static class Builder implements Value.Builder<PKIEncryptedStringValue> {
        
        @Override
        public final PKIEncryptedStringValue buildFromPersisted(final PersistedValue persistedValue) {
            
            if (persistedValue == null) {
                throw new IllegalArgumentException("persistedValue is null");
            }
            
            if (persistedValue.getStringValue() == null) {
                throw new IllegalArgumentException("String part is null");
            }
            
            return new PKIEncryptedStringValue(persistedValue.getStringValue());
        }
        
        @Override
        public final PKIEncryptedStringValue buildFromStringParts(final String... parts) {
            
            if (parts == null) {
                throw new IllegalArgumentException("parts is null");
            }
            
            if (parts.length != 1) {
                throw new IllegalArgumentException("Should have 1 part. Found " + parts.length);
            }
            
            if (parts[0] == null || parts[0].isEmpty()) {
                throw new IllegalArgumentException("part 0 is null or empty");
            }
            
            return new PKIEncryptedStringValue(PKICryptoService.getInstance().encrypt(parts[0]));
        }
    }
    
    private final String encryptedValue;
    
    public PKIEncryptedStringValue(final String encryptedValue) {
        this.encryptedValue = encryptedValue;
    }
    
    public String getValue() {
        return PKICryptoService.getInstance().decrypt(encryptedValue);
    }
    
    @Override
    public String[] getStringParts() {
        return new String[] { getValue() };
    }
    
    @Override
    public PersistedValue getPersistedValue() {
        return new PersistedValue(null, encryptedValue);
    }
    
    @Override
    public boolean equals(final Object o) {
        return EqualsUtil.equals(this, o, other -> Objects.equals(encryptedValue, other.encryptedValue));
    }
    
    @Override
    public int hashCode() {
        return encryptedValue.hashCode();
    }
    
    @Override
    public String toString() {
        return encryptedValue;
    }
}
