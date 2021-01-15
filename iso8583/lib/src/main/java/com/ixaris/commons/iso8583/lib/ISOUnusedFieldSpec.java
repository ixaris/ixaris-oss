package com.ixaris.commons.iso8583.lib;

public class ISOUnusedFieldSpec extends ISOFieldSpec {
    
    public ISOUnusedFieldSpec() {
        super(FieldFormat.UNUSED, FieldContentCoding.ASCII, 1, 1, "");
    }
    
}
