package com.ixaris.commons.iso8583.lib;

public class ISOLLLVariableFieldSpec extends ISOFieldSpec {
    
    public ISOLLLVariableFieldSpec(final FieldContentCoding coding, final int minLength, final int maxLength, final String name) {
        
        super(FieldFormat.HLLLVAR, coding, minLength, maxLength, name);
    }
    
    public ISOLLLVariableFieldSpec(final FieldContentCoding coding, final int maxLength, final String name) {
        
        super(FieldFormat.HLLLVAR, coding, 0, maxLength, name);
    }
    
}
