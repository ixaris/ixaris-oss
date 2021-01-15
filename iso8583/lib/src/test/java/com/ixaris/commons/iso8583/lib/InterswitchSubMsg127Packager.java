package com.ixaris.commons.iso8583.lib;

import static com.ixaris.commons.iso8583.lib.FieldContentCoding.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Created by lokesh.bharani on 21/15/2015.
 *
 * <p>This Packager contains the Field description of ISO8583 subMessage format as used by Interswitch. It mentions field length, content type
 * and description. This is used to create an ISO8583 subMessage from Interswitch real time Auth message. Here ISO8583 subMessage is the 127th
 * field of the ISO8583 message: Postilion Private Data
 */
public final class InterswitchSubMsg127Packager extends ISOPackager {
    
    public static final InterswitchSubMsg127Packager INSTANCE = new InterswitchSubMsg127Packager();
    
    public static InterswitchSubMsg127Packager getInstance() {
        return INSTANCE;
    }
    
    private InterswitchSubMsg127Packager() {
        
        super(MtiType.NONE,
            VarFieldLengthType.ASCII,
            StandardCharsets.UTF_8,
            /* 000 */ new ISOFixedFieldSpec(BIN, 8, "Primary Bit map"),
            /* 001 */ new ISOUnusedFieldSpec(),
            /* 002 */ new ISOLLVariableFieldSpec(ASCII, 32, "Switch Key"),
            /* 003 */ new ISOFixedFieldSpec(BIN, 48, "Routing Information"),
            /* 004 */ new ISOFixedFieldSpec(BIN, 22, "POS Data"),
            /* 005 */ new ISOFixedFieldSpec(BIN, 73, "Service station Data"),
            /* 006 */ new ISOFixedFieldSpec(BIN, 2, "Authorization File"),
            /* 007 */ new ISOLLVariableFieldSpec(BIN, 70, "Check Data"),
            /* 008 */ new ISOLLLVariableFieldSpec(BIN, 999, "Retention Data"),
            /* 009 */ new ISOLLLVariableFieldSpec(BIN, 255, "Additonal Node data"),
            /* 010 */ new ISOFixedFieldSpec(BIN, 3, "CVV2"),
            /* 011 */ new ISOLLVariableFieldSpec(BIN, 32, "Original Key"),
            /* 012 */ new ISOLLVariableFieldSpec(BIN, 25, "Terminal Owner"),
            /* 013 */ new ISOFixedFieldSpec(BIN, 17, "POS Geographic Data"),
            /* 014 */ new ISOFixedFieldSpec(BIN, 8, "Sponsor Bank"),
            /* 015 */ new ISOLLVariableFieldSpec(BIN, 29, "Address Verification Data"),
            /* 016 */ new ISOFixedFieldSpec(BIN, 1, "Address Verification Result"),
            /* 017 */ new ISOLLVariableFieldSpec(BIN, 50, "Card Holder Information"),
            /* 018 */ new ISOLLVariableFieldSpec(BIN, 50, "Validation Data"),
            /* 019 */ new ISOFixedFieldSpec(BIN, 31, "Bank Details"),
            /* 020 */ new ISOFixedFieldSpec(BIN, 8, "Originator/Authorizer date Settlementn"),
            /* 021 */ new ISOLLVariableFieldSpec(BIN, 12, "Record Identification"),
            /* 022 */ new ISOLLLLLVariableFieldSpec(BIN, 99999, "Structured Data"),
            /* 023 */ new ISOFixedFieldSpec(BIN, 253, "payee name and address"),
            /* 024 */ new ISOLLVariableFieldSpec(BIN, 28, "Payee reference"),
            /* 025 */ new ISOLLLLVariableFieldSpec(BIN, 9999, "Integrated Circuit Card (ICC) data"),
            /* 026 */ new ISOLLVariableFieldSpec(BIN, 20, "Original Node"),
            /* 027 */ new ISOFixedFieldSpec(BIN, 1, "Card Verification Result"),
            /* 028 */ new ISOFixedFieldSpec(BIN, 4, "American Express Card Identifier(CID)"),
            /* 029 */ new ISOFixedFieldSpec(BIN, 40, "3-D Secure Data"),
            /* 030 */ new ISOFixedFieldSpec(BIN, 1, "3-D Secure Result"),
            /* 031 */ new ISOLLVariableFieldSpec(BIN, 11, "Issuer Network Id"),
            /* 032 */ new ISOLLVariableFieldSpec(BIN, 33, "UCAF Data"),
            /* 033 */ new ISOFixedFieldSpec(BIN, 4, "Extended Transaction Type"),
            /* 034 */ new ISOFixedFieldSpec(BIN, 2, "Account Type Qualifiers"),
            /* 035 */ new ISOLLVariableFieldSpec(BIN, 11, "Acquirer Network ID"),
            /* 036 */ new ISOLLVariableFieldSpec(BIN, 25, "Customer ID"),
            /* 037 */ new ISOFixedFieldSpec(BIN, 4, "Extended Response Code"),
            /* 038 */ new ISOLLVariableFieldSpec(BIN, 99, "Additional POS Data Code"),
            /* 039 */ new ISOFixedFieldSpec(BIN, 2, "Original Response Code"),
            /* 040 */ new ISOLLLVariableFieldSpec(BIN, 512, "Transaction Reference"), //
            /* 041 */ new ISOLLVariableFieldSpec(BIN, 99, "Originating Remote Address"),
            /* 042 */ new ISOLLVariableFieldSpec(BIN, 25, "Customer ID"),
            /* 043 */ new ISOLLVariableFieldSpec(BIN, 10, "Transaction Number"),
            /* 044 */ new ISOUnusedFieldSpec(),
            /* 045 */ new ISOUnusedFieldSpec(),
            /* 046 */ new ISOUnusedFieldSpec(),
            /* 047 */ new ISOUnusedFieldSpec(),
            /* 048 */ new ISOUnusedFieldSpec(),
            /* 049 */ new ISOUnusedFieldSpec(),
            /* 050 */ new ISOUnusedFieldSpec(),
            /* 051 */ new ISOUnusedFieldSpec(),
            /* 052 */ new ISOUnusedFieldSpec(),
            /* 053 */ new ISOUnusedFieldSpec(),
            /* 054 */ new ISOUnusedFieldSpec(),
            /* 055 */ new ISOUnusedFieldSpec(),
            /* 056 */ new ISOUnusedFieldSpec(),
            /* 057 */ new ISOUnusedFieldSpec(),
            /* 058 */ new ISOUnusedFieldSpec(),
            /* 059 */ new ISOUnusedFieldSpec(),
            /* 060 */ new ISOUnusedFieldSpec(),
            /* 061 */ new ISOUnusedFieldSpec(),
            /* 062 */ new ISOUnusedFieldSpec(),
            /* 063 */ new ISOUnusedFieldSpec(),
            /* 064 */ new ISOUnusedFieldSpec());
    }
    
    /**
     * Constructs a message object from a byte array
     *
     * @param b byte array containing the raw read message
     * @return ISO8583Msg object containing the data held in the message
     * @throws IOException
     */
    public final InterswitchSubMsg127 disassemble(final InputStream is) throws IOException {
        
        // Create a new message object
        final InterswitchSubMsg127 m = new InterswitchSubMsg127();
        
        // unpack the rest of the message, here mti index is set as -333, just to flag that ISO8583 submessage doesn't
        // contain mti
        unpack(m, is);
        
        return m;
    }
    
    /**
     * Constructs a byte array from a message object <i> Note that the length of the message includes the trailer byte </i>
     *
     * @param m ISO8583SubMsg object to be converted to a byte array
     * @return byte[] array containing the data held in the message ready to be transmitted
     * @throws IOException
     */
    public final void assemble(final InterswitchSubMsg127 m, final OutputStream os) throws IOException {
        
        // invoke pack method to construct byte array from message object
        pack(m, os);
    }
}
