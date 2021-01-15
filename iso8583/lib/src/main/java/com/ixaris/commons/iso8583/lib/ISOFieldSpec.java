package com.ixaris.commons.iso8583.lib;

public abstract class ISOFieldSpec {
    
    private final FieldFormat format;
    private final int minLength;
    private final int maxLength;
    private final FieldContentCoding coding;
    private final String name;
    
    public ISOFieldSpec(final FieldFormat format, final FieldContentCoding coding, final int minLength, final int maxLength, final String name) {
        
        if (format == null) {
            throw new IllegalArgumentException("format cannot be null");
        }
        if ((minLength < 0) || (minLength > 99999)) {
            throw new IllegalArgumentException("invalid minLength [" + minLength + "]");
        }
        if ((maxLength < 1) || (maxLength < minLength) || (maxLength > 999999)) {
            throw new IllegalArgumentException("invalid maxLength [" + maxLength + "]");
        }
        if (coding == null) {
            throw new IllegalArgumentException("coding cannot be null");
        }
        if (name == null) {
            throw new IllegalArgumentException("name cannot be null");
        }
        
        this.format = format;
        this.minLength = minLength;
        this.maxLength = maxLength;
        this.coding = coding;
        this.name = name;
    }
    
    public FieldFormat getFormat() {
        return format;
    }
    
    public int getMinLength() {
        return minLength;
    }
    
    public int getMaxLength() {
        return maxLength;
    }
    
    public FieldContentCoding getCoding() {
        return coding;
    }
    
    public String getName() {
        return name;
    }
    
}
