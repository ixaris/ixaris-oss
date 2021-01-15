/*
 * Copyright 2002, 2012 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.iso8583.lib;

import static com.ixaris.commons.iso8583.lib.FieldContentCoding.*;

import java.io.DataInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.charset.StandardCharsets;

import com.ixaris.commons.iso8583.lib.MasterCardIpmMsg.Field;

/**
 * Packager which takes care of assembling a message byte stream and disassembling a byte stream into a MasterCard IPM Clearing message object
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 * @author <a href="mailto:olivia-ann.grech@ixaris.com">Olivia Grech</a>
 */
public class MasterCardIpmPackager extends ISOPackager {
    
    public static final int PDS_TAGID_LENGTH = 4;
    public static final int PDS_TAGLEN_LENGTH = 3;
    
    private static enum TagReadStage {
        NONE,
        TAG_ID,
        TAG_LENGTH,
        TAG_DATA;
    }
    
    /*
     * The list of ISO-defined data elements used in IPM messages as specified in the IMP Clearing Format documentation, defined according to their coding format and
     * length type (fixed or variable) together with other attributes: data element length and name.
     */
    private static final MasterCardIpmPackager INSTANCE = new MasterCardIpmPackager();
    
    public static MasterCardIpmPackager getInstance() {
        return INSTANCE;
    }
    
    private MasterCardIpmPackager() {
        super(MtiType.ASCII,
            VarFieldLengthType.ASCII,
            StandardCharsets.ISO_8859_1,
            /* 000 */ new ISOFixedFieldSpec(BIN, 8, "Primary Bit Map"),
            /* 001 */ new ISOFixedFieldSpec(BIN, 8, "Secondary Bit Map"),
            /* 002 */ new ISOLLVariableFieldSpec(ASCII, 6, 19, "Primary Account Number (PAN)"),
            /* 003 */ new ISOFixedFieldSpec(ASCII, 6, "Processing Code"),
            /* 004 */ new ISOFixedFieldSpec(ASCII, 12, "Transaction Amount"),
            /* 005 */ new ISOFixedFieldSpec(ASCII, 12, "Reconciliation Amount"),
            /* 006 */ new ISOFixedFieldSpec(ASCII, 12, "Cardholder Billing Amount"),
            /* 007 */ new ISOUnusedFieldSpec(),
            /* 008 */ new ISOUnusedFieldSpec(),
            /* 009 */ new ISOFixedFieldSpec(ASCII, 8, "Reconciliation Conversion Rate"),
            /* 010 */ new ISOFixedFieldSpec(ASCII, 8, "Cardholder Billing Conversion Rate"),
            /* 011 */ new ISOUnusedFieldSpec(),
            /* 012 */ new ISOFixedFieldSpec(ASCII, 12, "Local Transaction Date and Time"),
            /* 013 */ new ISOUnusedFieldSpec(),
            /* 014 */ new ISOFixedFieldSpec(ASCII, 4, "Expiration Date"),
            /* 015 */ new ISOUnusedFieldSpec(),
            /* 016 */ new ISOUnusedFieldSpec(),
            /* 017 */ new ISOUnusedFieldSpec(),
            /* 018 */ new ISOUnusedFieldSpec(),
            /* 019 */ new ISOUnusedFieldSpec(),
            /* 020 */ new ISOUnusedFieldSpec(),
            /* 021 */ new ISOUnusedFieldSpec(),
            /* 022 */ new ISOFixedFieldSpec(ASCII, 12, "Point of Service Data Code"),
            /* 023 */ new ISOFixedFieldSpec(ASCII, 3, "Card Sequence Number"),
            /* 024 */ new ISOFixedFieldSpec(ASCII, 3, "Function Code"),
            /* 025 */ new ISOFixedFieldSpec(ASCII, 4, "Message Reason Code"),
            /* 026 */ new ISOFixedFieldSpec(ASCII, 4, "Card Acceptor Business Code (MCC)"),
            /* 027 */ new ISOUnusedFieldSpec(),
            /* 028 */ new ISOUnusedFieldSpec(),
            /* 029 */ new ISOUnusedFieldSpec(),
            /* 030 */ new ISOFixedFieldSpec(ASCII, 24, "Original Amounts"),
            /* 031 */ new ISOLLVariableFieldSpec(ASCII, 23, "Acquirer Reference Data"),
            /* 032 */ new ISOLLVariableFieldSpec(ASCII, 11, "Acquiring Institution ID Code"),
            /* 033 */ new ISOLLVariableFieldSpec(ASCII, 6, 11, "Forwarding Institution ID Code"),
            /* 034 */ new ISOUnusedFieldSpec(),
            /* 035 */ new ISOUnusedFieldSpec(),
            /* 036 */ new ISOUnusedFieldSpec(),
            /* 037 */ new ISOFixedFieldSpec(ASCII, 12, "Retrieval Reference Number"),
            /* 038 */ new ISOFixedFieldSpec(ASCII, 6, "Approval Code"),
            /* 039 */ new ISOUnusedFieldSpec(),
            /* 040 */ new ISOFixedFieldSpec(ASCII, 3, "Service Code"),
            /* 041 */ new ISOFixedFieldSpec(ASCII, 8, "Card Acceptor Terminal ID"),
            /* 042 */ new ISOFixedFieldSpec(ASCII, 15, "Card Acceptor ID Code"),
            /* 043 */ new ISOLLVariableFieldSpec(ASCII, 20, 99, "Card Acceptor Name/Location"),
            /* 044 */ new ISOUnusedFieldSpec(),
            /* 045 */ new ISOUnusedFieldSpec(),
            /* 046 */ new ISOUnusedFieldSpec(),
            /* 047 */ new ISOUnusedFieldSpec(),
            /* 048 */ new ISOLLLVariableFieldSpec(ASCII, 999, "Additional Data 2"),
            /* 049 */ new ISOFixedFieldSpec(ASCII, 3, "Transaction Currency Code"),
            /* 050 */ new ISOFixedFieldSpec(ASCII, 3, "Reconciliation Currency Code"),
            /* 051 */ new ISOFixedFieldSpec(ASCII, 3, "Cardholder Billing Currency Code"),
            /* 052 */ new ISOUnusedFieldSpec(),
            /* 053 */ new ISOUnusedFieldSpec(),
            /* 054 */ new ISOLLLVariableFieldSpec(ASCII, 20, 120, "Additional Amounts"),
            /* 055 */ new ISOLLLVariableFieldSpec(BIN, 255, "Integrated Circuit Card (ICC) System-Related Data"),
            /* 056 */ new ISOUnusedFieldSpec(),
            /* 057 */ new ISOUnusedFieldSpec(),
            /* 058 */ new ISOUnusedFieldSpec(),
            /* 059 */ new ISOUnusedFieldSpec(),
            /* 060 */ new ISOUnusedFieldSpec(),
            /* 061 */ new ISOUnusedFieldSpec(),
            /* 062 */ new ISOLLLVariableFieldSpec(ASCII, 999, "Additional Data 2"),
            /* 063 */ new ISOLLLVariableFieldSpec(ASCII, 16, "Transaction Life Cycle ID"),
            /* 064 */ new ISOUnusedFieldSpec(),
            /* 065 */ new ISOUnusedFieldSpec(),
            /* 066 */ new ISOUnusedFieldSpec(),
            /* 067 */ new ISOUnusedFieldSpec(),
            /* 068 */ new ISOUnusedFieldSpec(),
            /* 069 */ new ISOUnusedFieldSpec(),
            /* 070 */ new ISOUnusedFieldSpec(),
            /* 071 */ new ISOFixedFieldSpec(ASCII, 8, "Message Number"),
            /* 072 */ new ISOLLLVariableFieldSpec(ASCII, 999, "Data Record"),
            /* 073 */ new ISOFixedFieldSpec(ASCII, 6, "Action Date"),
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
            /* 093 */ new ISOLLVariableFieldSpec(ASCII, 6, 11, "Transaction Destination Institution ID Code"),
            /* 094 */ new ISOLLVariableFieldSpec(ASCII, 6, 11, "Transaction Originator Institution ID Code"),
            /* 095 */ new ISOLLVariableFieldSpec(ASCII, 10, "Card Issuer Reference Data"),
            /* 096 */ new ISOUnusedFieldSpec(),
            /* 097 */ new ISOUnusedFieldSpec(),
            /* 098 */ new ISOUnusedFieldSpec(),
            /* 099 */ new ISOUnusedFieldSpec(),
            /* 100 */ new ISOLLVariableFieldSpec(ASCII, 6, 11, "Receiving Institution ID Code"),
            /* 101 */ new ISOUnusedFieldSpec(),
            /* 102 */ new ISOUnusedFieldSpec(),
            /* 103 */ new ISOUnusedFieldSpec(),
            /* 104 */ new ISOUnusedFieldSpec(),
            /* 105 */ new ISOUnusedFieldSpec(),
            /* 106 */ new ISOUnusedFieldSpec(),
            /* 107 */ new ISOUnusedFieldSpec(),
            /* 108 */ new ISOUnusedFieldSpec(),
            /* 109 */ new ISOUnusedFieldSpec(),
            /* 110 */ new ISOUnusedFieldSpec(),
            /* 111 */ new ISOLLLVariableFieldSpec(ASCII, 12, "Currency Conversion Assessment Amounts"),
            /* 112 */ new ISOLLVariableFieldSpec(BIN, 999, "Proprietary Field 112"),
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
            /* 123 */ new ISOLLLVariableFieldSpec(ASCII, 999, "Additional Data 3"),
            /* 124 */ new ISOLLLVariableFieldSpec(ASCII, 999, "Additional Data 4"),
            /* 125 */ new ISOLLLVariableFieldSpec(ASCII, 999, "Additional Data 5"),
            /* 126 */ new ISOUnusedFieldSpec(),
            /* 127 */ new ISOLLLVariableFieldSpec(ASCII, 8, 9, "Network Data"),
            /* 128 */ new ISOUnusedFieldSpec());
    }
    
    // new ISOPDSTagSpec[] { new ISOPDSTagSpec("0105", "File ID"), new ISOPDSTagSpec("0122", "Processing Mode") }),
    
    /**
     * Constructs a byte array from a message object <i> Note that the length of the message includes the trailer byte </i>
     *
     * @param m ISOMsg object to be converted to a byte array
     * @return byte[] array containing the data held in the message ready to be transmitted
     * @throws IERR_UNPARSEABLE_MESSAGE if the message could not be parsed
     */
    public final void assemble(final MasterCardIpmMsg m, final OutputStream os) throws IOException {
        
        int len = m.getLength();
        
        os.write((byte) (len >>> 8));
        os.write((byte) len);
        
        pack(m, os);
    }
    
    /**
     * Constructs a message object from a byte array
     *
     * @param b byte array containing the raw read message
     * @return BASE24Msg object containing the data held in the message
     * @throws IOException
     * @throws IERR_UNPARSEABLE_MESSAGE if the message could not be parsed
     */
    public final MasterCardIpmMsg disassemble(final InputStream is) throws IOException {
        
        final DataInputStream dis = new DataInputStream(is);
        
        // Check message length from first 2 bytes & check EOM byte at the end
        final int len = dis.readShort();
        
        // return unpack(new MasterCardISOMsg(), b);
        final MasterCardIpmMsg m = new MasterCardIpmMsg();
        unpack(m, is);
        
        if (len != m.getLength()) {
            throw new IllegalStateException("Mismatched header length [" + len + "] and message length [" + m.getLength() + "]");
        }
        
        unpackTags(m);
        return m;
    }
    
    // determine the PDStagIDs and values contained in the DE with the given pdsFieldIndex
    public void unpackTags(final MasterCardIpmMsg m) {
        
        // get the value (data) for the DE with the given pdsFieldValue
        // the data contains all the PDS present in the given DE
        Field curPdsField = Field.ADDITIONAL_DATA;
        String curPdsFieldValue = m.getValue(curPdsField);
        
        int pos = 0;
        int toRead = 0;
        int tagDataLength = 0;
        TagReadStage tagReadStage = TagReadStage.NONE;
        String tagId = "";
        String tagLength = "";
        String tagData = "";
        
        // we will now parse the DE value to get all the PDSs:
        // each PDS format is: <Tag ID><Tag DataLength><TagData>
        do {
            switch (tagReadStage) {
                case NONE:
                    tagReadStage = TagReadStage.TAG_ID;
                    toRead = Math.min(PDS_TAGID_LENGTH, curPdsFieldValue.length() - pos);
                    tagId = curPdsFieldValue.substring(pos, pos + toRead);
                    pos += toRead;
                    if (tagId.length() < PDS_TAGID_LENGTH) {
                        break;
                    }
                    // fall through
                case TAG_ID:
                    if ((pos == 0) && (tagId.length() < PDS_TAGID_LENGTH)) {
                        final int remaining = PDS_TAGID_LENGTH - tagId.length();
                        toRead = Math.min(remaining, curPdsFieldValue.length());
                        tagId = tagId + curPdsFieldValue.substring(0, toRead);
                        pos = toRead;
                    }
                    tagReadStage = TagReadStage.TAG_LENGTH;
                    toRead = Math.min(PDS_TAGLEN_LENGTH, curPdsFieldValue.length() - pos);
                    tagLength = curPdsFieldValue.substring(pos, pos + toRead);
                    pos += toRead;
                    if (tagLength.length() < PDS_TAGLEN_LENGTH) {
                        break;
                    }
                    // fall through
                case TAG_LENGTH:
                    if ((pos == 0) && (tagLength.length() < PDS_TAGLEN_LENGTH)) {
                        final int remaining = PDS_TAGLEN_LENGTH - tagLength.length();
                        toRead = Math.min(remaining, curPdsFieldValue.length());
                        tagLength = tagLength + curPdsFieldValue.substring(0, toRead);
                        pos = toRead;
                    }
                    tagReadStage = TagReadStage.TAG_DATA;
                    tagDataLength = Integer.parseInt(tagLength);
                    toRead = Math.min(tagDataLength, curPdsFieldValue.length() - pos);
                    tagData = curPdsFieldValue.substring(pos, pos + toRead);
                    pos += toRead;
                    if (tagData.length() < tagDataLength) {
                        break;
                    }
                    // fall through
                case TAG_DATA:
                    if ((pos == 0) && (tagData.length() < tagDataLength)) {
                        final int remaining = tagDataLength - tagLength.length();
                        toRead = Math.min(remaining, curPdsFieldValue.length());
                        tagData = tagData + curPdsFieldValue.substring(0, toRead);
                        pos = toRead;
                    }
                    
                    if (tagData.length() == tagDataLength) {
                        m.setTag(tagId, tagData);
                        tagReadStage = TagReadStage.NONE;
                    }
                    break;
            }
            
            if (pos == curPdsFieldValue.length()) {
                // we are at exacly the end of this field, so check if there is a next field
                curPdsField = getNextPDSFieldIndex(curPdsField);
                if (curPdsField != null) {
                    final String nextValue = m.getValue(curPdsField);
                    // next value might be null if the message did not use all of the additional data fields
                    if (nextValue != null) {
                        curPdsFieldValue = nextValue;
                        pos = 0;
                    }
                }
            }
            
        } while (pos < curPdsFieldValue.length());
        
        if (!TagReadStage.NONE.equals(tagReadStage)) {
            throw new IllegalStateException("Error when parsing PDS Fields");
        }
    }
    
    public Field getNextPDSFieldIndex(final Field currentPDSField) {
        switch (currentPDSField) {
            case ADDITIONAL_DATA:
                return Field.ADDITIONAL_DATA_2;
            case ADDITIONAL_DATA_2:
                return Field.ADDITIONAL_DATA_3;
            case ADDITIONAL_DATA_3:
                return Field.ADDITIONAL_DATA_4;
            case ADDITIONAL_DATA_4:
                return Field.ADDITIONAL_DATA_5;
            case ADDITIONAL_DATA_5:
            default:
                return null; // no matching pds fields
        }
    }
    
}
