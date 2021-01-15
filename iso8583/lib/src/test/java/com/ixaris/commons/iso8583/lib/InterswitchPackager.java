package com.ixaris.commons.iso8583.lib;

import static com.ixaris.commons.iso8583.lib.FieldContentCoding.*;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

/**
 * Created by lokesh.bharani on 12/15/2015.
 *
 * <p>This Packager contains the Field description of ISO8583 format as used by Interswitch. It mentions field length, content type and
 * description. This is used to create an ISO8583 message from Interswitch real time Auth message
 */
public final class InterswitchPackager extends ISOPackager {
    
    private static final InterswitchPackager INSTANCE = new InterswitchPackager();
    
    public static InterswitchPackager getInstance() {
        return INSTANCE;
    }
    
    private InterswitchPackager() {
        super(MtiType.ASCII,
            VarFieldLengthType.ASCII,
            StandardCharsets.UTF_8,
            /* 000 */ new ISOFixedFieldSpec(BIN, 8, "Primary Bit map"),
            /* 001 */ new ISOFixedFieldSpec(BIN, 8, "Secondary Bit map"),
            /* 002 */ new ISOLLVariableFieldSpec(ASCII, 19, "Primary Account Number"),
            /* 003 */ new ISOFixedFieldSpec(ASCII, 6, "Processing Code"),
            /* 004 */ new ISOFixedFieldSpec(ASCII, 12, "Transaction Amount"),
            /* 005 */ new ISOFixedFieldSpec(ASCII, 12, "Settlement Amount"),
            /* 006 */ new ISOUnusedFieldSpec(),
            /* 007 */ new ISOFixedFieldSpec(ASCII, 10, "Transmission Date and Time"),
            /* 008 */ new ISOUnusedFieldSpec(),
            /* 009 */ new ISOUnusedFieldSpec(),
            /* 010 */ new ISOUnusedFieldSpec(),
            /* 011 */ new ISOFixedFieldSpec(ASCII, 6, "Systems Trace Audit Number"),
            /* 012 */ new ISOFixedFieldSpec(ASCII, 6, "Local Transaction Time"),
            /* 013 */ new ISOFixedFieldSpec(ASCII, 4, "Local Transaction Date"),
            /* 014 */ new ISOFixedFieldSpec(ASCII, 4, "Expiration Date"),
            /* 015 */ new ISOFixedFieldSpec(ASCII, 4, "Settlement Date"),
            /* 016 */ new ISOUnusedFieldSpec(),
            /* 017 */ new ISOUnusedFieldSpec(),
            /* 018 */ new ISOFixedFieldSpec(ASCII, 4, "Merchant Type"),
            /* 019 */ new ISOUnusedFieldSpec(),
            /* 020 */ new ISOUnusedFieldSpec(),
            /* 021 */ new ISOUnusedFieldSpec(),
            /* 022 */ new ISOFixedFieldSpec(ASCII, 3, "POS Entry Mode"),
            /* 023 */ new ISOFixedFieldSpec(ASCII, 3, "Card Sequence Number"),
            /* 024 */ new ISOUnusedFieldSpec(),
            /* 025 */ new ISOFixedFieldSpec(ASCII, 2, "Point of Service Condition Code"),
            /* 026 */ new ISOFixedFieldSpec(ASCII, 2, "Point of Service Capture Code"),
            /* 027 */ new ISOFixedFieldSpec(ASCII, 1, "Authorization Identification Response Length"),
            /* 028 */ new ISOFixedFieldSpec(ASCII, 9, "Transaction Fee Amount"), // ??
            /* 029 */ new ISOFixedFieldSpec(ASCII, 9, "Settlement Fee Amount"), // ??
            /* 030 */ new ISOFixedFieldSpec(ASCII, 9, "Transaction Processing Fee Amount"),
            /* 031 */ new ISOFixedFieldSpec(ASCII, 9, "Settlement Processing Fee Amount"),
            /* 032 */ new ISOLLVariableFieldSpec(ASCII, 11, "Acquiring Institution Identification Code"),
            /* 033 */ new ISOLLVariableFieldSpec(ASCII, 11, "Forwarding Institution Identification Code"),
            /* 034 */ new ISOUnusedFieldSpec(),
            /* 035 */ new ISOLLVariableFieldSpec(ASCII, 37, "Track 2 data"),
            /* 036 */ new ISOUnusedFieldSpec(),
            /* 037 */ new ISOFixedFieldSpec(ASCII, 12, "Retrieval Reference Number"),
            /* 038 */ new ISOFixedFieldSpec(ASCII, 6, "Authorization Identification Response"),
            /* 039 */ new ISOFixedFieldSpec(ASCII, 2, "Response Code"),
            /* 040 */ new ISOFixedFieldSpec(ASCII, 3, "Service Restriction Code"),
            /* 041 */ new ISOFixedFieldSpec(ASCII, 8, "Card Acceptor Terminal Identification"),
            /* 042 */ new ISOFixedFieldSpec(ASCII, 15, "Card Acceptor Identification Code"),
            /* 043 */ new ISOFixedFieldSpec(ASCII, 40, "Card Acceptor Name/Location"),
            /* 044 */ new ISOLLVariableFieldSpec(ASCII, 25, "Additional Response Data"),
            /* 045 */ new ISOLLVariableFieldSpec(ASCII, 76, "Track 1 data"),
            /* 046 */ new ISOUnusedFieldSpec(),
            /* 047 */ new ISOUnusedFieldSpec(),
            /* 048 */ new ISOLLLVariableFieldSpec(ASCII, 999, "Additional Data"), // ??
            /* 049 */ new ISOFixedFieldSpec(ASCII, 3, "Transaction Currency Code"),
            /* 050 */ new ISOFixedFieldSpec(ASCII, 3, "Settlement Currency Code"),
            /* 051 */ new ISOUnusedFieldSpec(),
            /* 052 */ new ISOFixedFieldSpec(ASCII, 8, "Personal Identification Number (PIN) Data"),
            /* 053 */ new ISOUnusedFieldSpec(),
            /* 054 */ new ISOLLLVariableFieldSpec(ASCII, 120, "Additional Amounts"),
            /* 055 */ new ISOUnusedFieldSpec(),
            /* 056 */ new ISOLLLVariableFieldSpec(ASCII, 4, "Message Reason Code"),
            /* 057 */ new ISOLLLVariableFieldSpec(ASCII, 3, "Authorization Life-cycle code"),
            /* 058 */ new ISOLLLVariableFieldSpec(ASCII, 11, "Authorizing Agent Institution"),
            /* 059 */ new ISOLLLVariableFieldSpec(ASCII, 500, "Echo Data"),
            /* 060 */ new ISOUnusedFieldSpec(),
            /* 061 */ new ISOUnusedFieldSpec(),
            /* 062 */ new ISOUnusedFieldSpec(),
            /* 063 */ new ISOUnusedFieldSpec(),
            /* 064 */ new ISOUnusedFieldSpec(),
            /* 065 */ new ISOUnusedFieldSpec(),
            /* 066 */ new ISOUnusedFieldSpec(),
            /* 067 */ new ISOUnusedFieldSpec(),
            /* 068 */ new ISOUnusedFieldSpec(),
            /* 069 */ new ISOUnusedFieldSpec(),
            /* 070 */ new ISOUnusedFieldSpec(),
            /* 071 */ new ISOUnusedFieldSpec(),
            /* 072 */ new ISOUnusedFieldSpec(),
            /* 073 */ new ISOUnusedFieldSpec(),
            /* 074 */ new ISOUnusedFieldSpec(),
            /* 075 */ new ISOUnusedFieldSpec(),
            /* 076 */ new ISOUnusedFieldSpec(),
            /* 077 */ new ISOUnusedFieldSpec(),
            /* 078 */ new ISOUnusedFieldSpec(),
            /* 079 */ new ISOUnusedFieldSpec(),
            /* 080 */ new ISOUnusedFieldSpec(),
            /* 081 */ new ISOUnusedFieldSpec(),
            /* 082 */ new ISOUnusedFieldSpec(),
            /* 083 */ new ISOUnusedFieldSpec(),
            /* 084 */ new ISOUnusedFieldSpec(),
            /* 085 */ new ISOUnusedFieldSpec(),
            /* 086 */ new ISOUnusedFieldSpec(),
            /* 087 */ new ISOUnusedFieldSpec(),
            /* 088 */ new ISOUnusedFieldSpec(),
            /* 089 */ new ISOUnusedFieldSpec(),
            /* 090 */ new ISOUnusedFieldSpec(),
            /* 091 */ new ISOUnusedFieldSpec(),
            /* 092 */ new ISOUnusedFieldSpec(),
            /* 093 */ new ISOUnusedFieldSpec(),
            /* 094 */ new ISOUnusedFieldSpec(),
            /* 095 */ new ISOFixedFieldSpec(ASCII, 42, "Replacement Amount"),
            /* 096 */ new ISOUnusedFieldSpec(),
            /* 097 */ new ISOUnusedFieldSpec(),
            /* 098 */ new ISOUnusedFieldSpec(),
            /* 099 */ new ISOUnusedFieldSpec(),
            /* 100 */ new ISOUnusedFieldSpec(),
            /* 101 */ new ISOUnusedFieldSpec(),
            /* 102 */ new ISOLLVariableFieldSpec(ASCII, 28, "Account Identification 1"),
            /* 103 */ new ISOLLVariableFieldSpec(ASCII, 28, "Account Identification 2"),
            /* 104 */ new ISOUnusedFieldSpec(),
            /* 105 */ new ISOUnusedFieldSpec(),
            /* 106 */ new ISOUnusedFieldSpec(),
            /* 107 */ new ISOUnusedFieldSpec(),
            /* 108 */ new ISOUnusedFieldSpec(),
            /* 109 */ new ISOUnusedFieldSpec(),
            /* 110 */ new ISOUnusedFieldSpec(),
            /* 111 */ new ISOUnusedFieldSpec(),
            /* 112 */ new ISOUnusedFieldSpec(),
            /* 113 */ new ISOUnusedFieldSpec(),
            /* 114 */ new ISOUnusedFieldSpec(),
            /* 115 */ new ISOUnusedFieldSpec(),
            /* 116 */ new ISOUnusedFieldSpec(),
            /* 117 */ new ISOUnusedFieldSpec(),
            /* 118 */ new ISOUnusedFieldSpec(),
            /* 119 */ new ISOUnusedFieldSpec(),
            /* 120 */ new ISOUnusedFieldSpec(),
            /* 121 */ new ISOUnusedFieldSpec(),
            /* 122 */ new ISOUnusedFieldSpec(),
            /* 123 */ new ISOLLLVariableFieldSpec(ASCII, 15, "POS Data Code"),
            /* 124 */ new ISOUnusedFieldSpec(),
            /* 125 */ new ISOUnusedFieldSpec(),
            /* 126 */ new ISOUnusedFieldSpec(),
            /* 127 */ new ISOLLLLLLVariableFieldSpec(BIN, 999999, "Postilion Private Data"),
            /* 128 */ new ISOUnusedFieldSpec());
    }
    
    /**
     * Constructs a message object from a byte array
     *
     * @param b byte array containing the raw read message
     * @return ISO8583Msg object containing the data held in the message
     * @throws IOException
     */
    public final InterswitchMsg disassemble(final InputStream is) throws IOException {
        
        // Create a new message object
        final InterswitchMsg m = new InterswitchMsg();
        
        // unpack the rest of the message
        unpack(m, is);
        
        return m;
    }
    
    /**
     * Constructs a byte array from a message object <i> Note that the length of the message includes the trailer byte </i>
     *
     * @param m ISOMsg object to be converted to a byte array
     * @return byte[] array containing the data held in the message ready to be transmitted
     * @throws IOException
     */
    public final void assemble(final InterswitchMsg m, final OutputStream os) throws IOException {
        
        // invoke pack method to construct byte array from message object
        pack(m, os);
    }
    
}
