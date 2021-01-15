package com.ixaris.commons.iso8583.lib;

public class ISOLLLLVariableFieldSpec extends ISOFieldSpec {
    
    public ISOLLLLVariableFieldSpec(final FieldContentCoding coding, final int minLength, final int maxLength, final String name) {
        
        super(FieldFormat.HLLLLVAR, coding, minLength, maxLength, name);
    }
    
    public ISOLLLLVariableFieldSpec(final FieldContentCoding coding, final int maxLength, final String name) {
        
        super(FieldFormat.HLLLLVAR, coding, 0, maxLength, name);
    }
    
}
