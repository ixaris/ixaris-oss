package com.ixaris.commons.iso8583.lib;

public class ISOLLLLLVariableFieldSpec extends ISOFieldSpec {
    
    public ISOLLLLLVariableFieldSpec(final FieldContentCoding coding, final int minLength, final int maxLength, final String name) {
        
        super(FieldFormat.HLLLLLVAR, coding, minLength, maxLength, name);
    }
    
    public ISOLLLLLVariableFieldSpec(final FieldContentCoding coding, final int maxLength, final String name) {
        
        super(FieldFormat.HLLLLLVAR, coding, 0, maxLength, name);
    }
    
}
