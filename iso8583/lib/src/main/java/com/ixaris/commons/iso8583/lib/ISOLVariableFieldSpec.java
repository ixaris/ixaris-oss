package com.ixaris.commons.iso8583.lib;

public class ISOLVariableFieldSpec extends ISOFieldSpec {
    
    public ISOLVariableFieldSpec(final FieldContentCoding coding, final int minLength, final int maxLength, final String name) {
        
        super(FieldFormat.HLVAR, coding, minLength, maxLength, name);
    }
    
    public ISOLVariableFieldSpec(final FieldContentCoding coding, final int maxLength, final String name) {
        
        super(FieldFormat.HLVAR, coding, 0, maxLength, name);
    }
    
}
