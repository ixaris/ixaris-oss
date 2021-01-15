package com.ixaris.commons.iso8583.lib;

public class ISOLLLLLLVariableFieldSpec extends ISOFieldSpec {
    
    public ISOLLLLLLVariableFieldSpec(final FieldContentCoding coding, final int minLength, final int maxLength, final String name) {
        
        super(FieldFormat.HLLLLLLVAR, coding, minLength, maxLength, name);
    }
    
    public ISOLLLLLLVariableFieldSpec(final FieldContentCoding coding, final int maxLength, final String name) {
        
        super(FieldFormat.HLLLLLLVAR, coding, 0, maxLength, name);
    }
    
}
