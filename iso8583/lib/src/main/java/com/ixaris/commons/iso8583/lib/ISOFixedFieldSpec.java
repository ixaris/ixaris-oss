package com.ixaris.commons.iso8583.lib;

public class ISOFixedFieldSpec extends ISOFieldSpec {
    
    public ISOFixedFieldSpec(final FieldContentCoding coding, final int length, final String name) {
        
        super(FieldFormat.FIXED, coding, length, length, name);
    }
    
}
