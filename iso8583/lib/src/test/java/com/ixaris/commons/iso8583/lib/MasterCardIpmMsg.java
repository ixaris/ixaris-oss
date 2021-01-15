/*
 * Copyright 2002, 2012 Ixaris Systems Ltd. All rights reserved. IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to
 * license terms.
 */
package com.ixaris.commons.iso8583.lib;

import java.io.Serializable;
import java.util.SortedMap;
import java.util.TreeMap;

/**
 * Wrapper class around ISOMsg for use with MasterCard clearing system
 *
 * @author <a href="mailto:olivia-ann.grech@ixaris.com">Olivia Grech</a>
 */
public final class MasterCardIpmMsg extends ISOMsg implements Serializable {
    
    private static final long serialVersionUID = 5747915244196410090L;
    
    public static enum MessageType {
        // the different types of messages which can be interchanged
        PRESENTMENT("1240"),
        CHARGEBACK("1442"),
        ADMINISTRATIVE("1644"),
        FEE_COLLECTION("1740");
        
        private final String code;
        
        MessageType(String code) {
            this.code = code;
        }
        
        public String getCode() {
            return this.code;
        }
        
        public static MessageType match(String value) {
            for (MessageType messageType : MessageType.values()) {
                if (messageType.getCode().equals(value)) {
                    return messageType;
                }
            }
            return null;
        }
    }
    
    // list of Data Elements for a MasterCard ISOMsg
    public static enum Field {
        SECONDARY_BITMAP(1),
        PRIMARY_ACCOUNT_NUMBER(2),
        PROCESSING_CODE(3),
        TRANSACTION_AMOUNT(4),
        RECONCILIATION_AMOUNT(5),
        CARDHOLDER_BILLING_AMOUNT(6),
        RECONCILIATION_CONVERSION_RATE(9),
        CARDHOLDER_BILLING_CONVERSION_RATE(10),
        LOCAL_TRANSACTION_DATE_AND_TIME(12),
        EXPIRATION_DATE(14),
        POINT_OF_SERVICE_DATA_CODE(22),
        CARD_SEQUENCE_NUMBER(23),
        FUNCTION_CODE(24),
        MESSAGE_REASON_CODE(25),
        CARD_ACCEPTOR_BUSINESS_CODE_MCC(26),
        ORIGINAL_AMOUNTS(30),
        ACQUIRER_REFERENCE_DATA(31),
        ACQUIRING_INSTITUTION_ID_CODE(32),
        FORWARDING_INSTITUTION_ID_CODE(33),
        RETRIEVAL_REFERENCE_NUMBER(37),
        APPROVAL_CODE(38),
        SERVICE_CODE(40),
        CARD_ACCEPTOR_TERMINAL_ID(41),
        CARD_ACCEPTOR_ID_CODE(42),
        CARD_ACCEPTOR_NAME_LOCATION(43),
        ADDITIONAL_DATA(48),
        TRANSACTION_CURRENCY_CODE(49),
        RECONCILIATION_CURRENCY_CODE(50),
        CARDHOLDER_BILLING_CURRENCY_CODE(51),
        ADDITIONAL_AMOUNTS(54),
        INTEGRATED_CIRCUIT_CARD_SYSTEM_RELATED_DATA(55),
        ADDITIONAL_DATA_2(62),
        TRANSACTION_LIFE_CYCLE_ID(63),
        MESSAGE_NUMBER(71),
        DATA_RECORD(72),
        ACTION_DATE(73),
        TRANSACTION_DESTINATION_INSTITUTION_ID_CODE(93),
        TRANSACTION_ORIGINATOR_INSTITUTION_ID_CODE(94),
        CARD_ISSUER_REFERENCE_DATA(95),
        RECEIVING_INSTITUTION_ID_CODE(100),
        CURRENCY_CONVERSION_ASSESSMENT_AMOUNT(111),
        ADDITIONAL_DATA_3(123),
        ADDITIONAL_DATA_4(124),
        ADDITIONAL_DATA_5(125),
        NETWORK_DATA(127);
        
        private final int index;
        
        Field(final int index) {
            this.index = index;
        }
        
        public final int index() {
            return index;
        }
        
        public static Field getField(int index) {
            for (Field field : Field.values()) {
                if (field.index() == index) {
                    return field;
                }
            }
            return null;
        }
    }
    
    /*
     * list of PDS tags in MasterCardISOMsg
     * Note: PDS tags defined are not all PDS Tags which can be present in a MasterCardISOMsg
     * PDSTags defined below are only required by Ixaris for settlement files processing.
     * There might be need to add more later on, if more data is required...
     */
    public static enum PDSTag {
        
        TERMINAL_TYPE("0023"),
        CURRENCY_EXPONENTS("0148"),
        ORIGINAL_CURRENCY_CODES("0149"),
        BUSINESS_ACTIVITY("0158"),
        SETTLEMENT_INDICATOR("0165"),
        DOCUMENTATION_INDICATOR("0262"),
        FILE_ID("0105"),
        PROCESSING_MODE("0122"),
        AMOUNT_CHECKSUM("0301"),
        MESSAGE_COUNT("0306"),
        TRANSACTION_FEE("0146"),
        SETTLEMENT_DATA("0159"),
        FEE_COLLECTION_CONTROL_NUMBER("0137");
        
        private final String pdsTag;
        
        PDSTag(final String pdsTag) {
            this.pdsTag = pdsTag;
        }
        
        public final String tag() {
            return pdsTag;
        }
    }
    
    /*
     * List of Clearing SYstem Transaction Functions used by MasterCard Settlement messages (defined by the Message Type and Function Code)
     */
    public static enum ClearingSystemTransactionFunction {
        FIRST_PRESENTMENT("200", MessageType.PRESENTMENT),
        SECOND_PRESENTMENT_FULL("205", MessageType.PRESENTMENT),
        SECOND_PRESENTMENT_PARTIAL("282", MessageType.PRESENTMENT),
        
        FIRST_CHARGEBACK_FULL("450", MessageType.CHARGEBACK),
        ARBITRATION_CHARGEBACK_FULL("451", MessageType.CHARGEBACK),
        FIRST_CHARGEBACK_PARTIAL("453", MessageType.CHARGEBACK),
        ARBITRATION_CHARGEBACK_PARTIAL("454", MessageType.CHARGEBACK),
        
        RETRIEVAL_REQUEST("603", MessageType.ADMINISTRATIVE),
        RETRIEVAL_REQUEST_ACKNOWLEDGEMENT("605", MessageType.ADMINISTRATIVE),
        CURRENCY_UPDATE("640", MessageType.ADMINISTRATIVE),
        FILE_CURRENCY_SUMMARY("680", MessageType.ADMINISTRATIVE),
        FINANCIAL_POSITION_DETAIL("685", MessageType.ADMINISTRATIVE),
        SETTLEMENT_POSITION_DETAIL("688", MessageType.ADMINISTRATIVE),
        MESSAGE_EXCEPTION("691", MessageType.ADMINISTRATIVE),
        TEXT_MESSAGE("693", MessageType.ADMINISTRATIVE),
        FILE_TRAILER("695", MessageType.ADMINISTRATIVE),
        FINANCIAL_DETAIL_ADDENDUM("696", MessageType.ADMINISTRATIVE),
        FILE_HEADER("697", MessageType.ADMINISTRATIVE),
        FILE_REJECT("699", MessageType.ADMINISTRATIVE),
        
        MEMBER_GENERATED_FEE_COLLECTION("700", MessageType.FEE_COLLECTION),
        FEE_COLLECTION_RETURN("780", MessageType.FEE_COLLECTION),
        FEE_COLLECTION_RESUBMISSION("781", MessageType.FEE_COLLECTION),
        FEE_COLLECTION_ARBITRATION_RETURN("782", MessageType.FEE_COLLECTION),
        CLEARING_SYSTEM_GENERATED_FEE_COLLECTION("783", MessageType.FEE_COLLECTION),
        FEE_COLLECTION_FUNDS_TRANSFER("790", MessageType.FEE_COLLECTION),
        FEE_COLLECTION_FUNDS_TRANSFER_BACKOUT("791", MessageType.FEE_COLLECTION);
        
        private final String functionCode;
        private final MessageType messageType;
        
        ClearingSystemTransactionFunction(final String functionCode, final MessageType messageType) {
            this.functionCode = functionCode;
            this.messageType = messageType;
        }
        
        public final String getFunctionCode() {
            return this.functionCode;
        }
        
        public final MessageType getMessageType() {
            return this.messageType;
        }
        
        public static ClearingSystemTransactionFunction match(String messageType, String functionCode) {
            for (ClearingSystemTransactionFunction clearingSystemTransactionFunction : ClearingSystemTransactionFunction.values()) {
                if (clearingSystemTransactionFunction.getFunctionCode().equals(functionCode)
                    && clearingSystemTransactionFunction.getMessageType().getCode().equals(messageType)) {
                    return clearingSystemTransactionFunction;
                }
            }
            return null;
        }
    }
    
    // Stores a map of <PDS Tag id, value> pairs for PDS fields (if any) contained in the ISOMsg
    private final SortedMap<String, String> tags = new TreeMap<>();
    
    public MasterCardIpmMsg() {}
    
    public MasterCardIpmMsg(final String mti) {
        super();
        setMTI(mti.getBytes());
    }
    
    @Override
    public ISOPackager getPackager() {
        return MasterCardIpmPackager.getInstance();
    }
    
    /**
     * gets the message type from the message
     *
     * @return MessageType the message type
     * @throws IERR_UNKNOWN_FIELD_VALUE if a field value is not known
     */
    public final MessageType getMessageType() {
        String mtid = new String(getMTI());
        
        MessageType messageType = MessageType.match(mtid);
        
        if (messageType == null) {
            throw new IllegalStateException("Unhandled MTID [" + mtid + "]");
        } else {
            return messageType;
        }
    }
    
    /**
     * gets the Clearing System Transaction Function from the message
     *
     * @return ResponseCode the message response code
     * @throws IERR_UNKNOWN_FIELD_VALUE if an field value is not known
     */
    public final ClearingSystemTransactionFunction getTransactionFunction() {
        String functionCode = getValue(Field.FUNCTION_CODE);
        
        ClearingSystemTransactionFunction clearingSystemTransactionFunction = ClearingSystemTransactionFunction.match(new String(getMTI()),
            functionCode);
        
        if (clearingSystemTransactionFunction == null) {
            throw new IllegalStateException("Unhandled Function Code [" + functionCode + "]");
        } else {
            return clearingSystemTransactionFunction;
        }
    }
    
    /**
     * sets the given Field with the given value
     */
    public final void set(final Field f, final String s) {
        set(f.index(), s);
    }
    
    public final void setTag(final String tagId, final String tagValue) {
        if (tagId.length() != MasterCardIpmPackager.PDS_TAGID_LENGTH) {
            throw new IllegalArgumentException("Given tag id [" + tagId + "] should be 4 characters");
        }
        tags.put(tagId, tagValue);
    }
    
    /**
     * Gets the value of the given Field <i> This method will not work correctly with composite fields </i>
     *
     * @return String representation of field
     */
    public final String getValue(final Field f) {
        return getStringValue(f.index());
    }
    
    public final String getPdsTagValue(final PDSTag p) {
        return tags.get(p.tag());
    }
    
}
