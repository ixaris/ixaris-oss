package com.ixaris.commons.iso8583.lib;

public class ISOLLVariableFieldSpec extends ISOFieldSpec {
    
    public ISOLLVariableFieldSpec(final FieldContentCoding coding, final int minLength, final int maxLength, final String name) {
        
        super(FieldFormat.HLLVAR, coding, minLength, maxLength, name);
    }
    
    public ISOLLVariableFieldSpec(final FieldContentCoding coding, final int maxLength, final String name) {
        
        super(FieldFormat.HLLVAR, coding, 0, maxLength, name);
    }
    
}
