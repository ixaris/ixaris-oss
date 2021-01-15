/*
 * Copyright 2002 - 2011 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.ixaris.commons.iso8583.lib;

import java.io.Serializable;

/**
 * Wrapper class around ISOMsg for use with BASE24 systems
 *
 * @author Brian Vella
 */
public final class Base24Msg extends ISOMsg implements Serializable {
    
    private static final long serialVersionUID = 5747915244196410090L;
    
    public enum MessageType {
        ECHO,
        LOGON,
        LOGOFF,
        QUERY_BALANCE,
        DEBIT_BALANCE,
        CREDIT_BALANCE,
        
        ECHO_REPLY,
        LOGON_REPLY,
        LOGOFF_REPLY,
        QUERY_BALANCE_REPLY,
        DEBIT_BALANCE_REPLY,
        CREDIT_BALANCE_REPLY,
        
        REJECTED_ECHO,
        REJECTED_LOGON,
        REJECTED_LOGOFF,
        REJECTED_QUERY_BALANCE,
        REJECTED_DEBIT_BALANCE,
        REJECTED_CREDIT_BALANCE
    }
    
    public enum Field {
        SECONDARY_BITMAP(1),
        PRIMARY_ACCOUNT_NUMBER(2),
        PROCESSING_CODE(3),
        TRANSACTION_AMOUNT(4),
        TRANSMISSION_DATE_AND_TIME(7),
        SYSTEMS_TRACE_AUDIT_NUMBER(11),
        LOCAL_TRANSACTION_TIME(12),
        LOCAL_TRANSACTION_DATE(13),
        EXPIRATION_DATE(14),
        SETTLEMENT_DATE(15),
        CAPTURE_DATE(17),
        MERCHANT_TYPE(18),
        POINT_OF_SERVICE_ENTRY_MODE(22),
        CARD_SEQUENCE_NUMBER(23),
        POINT_OF_SERVICE_CONDITION_CODE(25),
        AUTHORIZATION_IDENTIFICATION_RESPONSE_LENGTH(27),
        TRANSACTION_FEE_AMOUNT(28),
        ACQUIRING_INSTITUTION_IDENTIFICATION_CODE(32),
        FORWARDING_INSTITUTION_IDENTIFICATION_CODE(33),
        TRACK_2_DATA(35),
        RETRIEVAL_REFERENCE_NUMBER(37),
        AUTHORIZATION_IDENTIFICATION_RESPONSE(38),
        RESPONSE_CODE(39),
        CARD_ACCEPTOR_TERMINAL_IDENTIFICATION(41),
        CARD_ACCEPTOR_IDENTIFICATION_CODE(42),
        CARD_ACCEPTOR_NAME_LOCATION(43),
        TRACK_1_DATA(45),
        RETAILER_DATA(48),
        TRANSACTION_CURRENCY_CODE(49),
        PERSONAL_IDENTIFICATION_NUMBER_DATA(52),
        DDITIONAL_AMOUNTS(54),
        TERMINAL_DATA(60),
        CARD_ISSUER_CATEGORY_RESPONSE_CODE_DATA(61),
        POSTAL_CODE(62),
        ADDITIONAL_DATA(63),
        PRIMARY_MESSAGE_AUTHENTICATION_CODE(64),
        NETWORK_MANAGEMENT_INFORMATION_CODE(70),
        ORIGINAL_DATA_ELEMENTS(90),
        REPLACEMENT_AMOUNTS(95),
        RECEIVING_INSTITUTION_IDENTIFICATION_CODE(100),
        ACCOUNT_IDENTIFICATION_1(102),
        TERMINAL_ADDRESS_BRANCH(120),
        AUTHORIZATION_INDICATORS(121),
        INVOICE_DATA(123),
        BATCH_AND_SHIFT_DATA(124),
        SETTLEMENT_DATA(125),
        PREAUTHORIZATION_AND_CHARGEBACK_DATA(126),
        USER_DATA(127),
        SECONDARY_MESSAGE_AUTHENTICATION_CODE(128);
        
        private final int index;
        
        Field(final int index) {
            this.index = index;
        }
        
        public final int index() {
            return index;
        }
    }
    
    public enum ResponseCode {
        APPROVED_00("00"),
        DENIED_05("05"),
        ERROR_06("06"),
        INVALID_TRANSACTION_12("12"),
        INVALID_CARD_NUMBER_14("14"),
        EXPIRED_CARD_901_33("33"),
        RESTRICTED_CARD_36("36"),
        INSUFFICIENT_FUNDS_51("51"),
        EXPIRED_CARD_051_54("54"),
        CAF_NOT_FOUND_56("56"),
        DPC_DOWN_91("91"),
        DUPLICATE_TRANSACTION_94("94");
        
        private final String code;
        
        ResponseCode(final String code) {
            this.code = code;
        }
        
        public final String code() {
            return code;
        }
        
        @Override
        public String toString() {
            return name() + "(" + code + ")";
        }
        
    }
    
    public enum NetworkManagementInformationCode {
        LOGON("001"),
        LOGOFF("002"),
        ECHO("301");
        
        private final String code;
        
        NetworkManagementInformationCode(final String code) {
            this.code = code;
        }
        
        public final String code() {
            return code;
        }
    }
    
    public enum ProcessingCode {
        CREDIT("223000"), // positive adjustment
        DEBIT("023000"), // negative adjustment
        QUERY("303000"); // balance enquiry
        
        private final String code;
        
        ProcessingCode(final String code) {
            this.code = code;
        }
        
        public final String code() {
            return code;
        }
    }
    
    private static final Field[] MANDATORY_FIELDS_0800 = {
                                                           Field.TRANSMISSION_DATE_AND_TIME, Field.SYSTEMS_TRACE_AUDIT_NUMBER, Field.NETWORK_MANAGEMENT_INFORMATION_CODE
    };
    
    private static final Field[] MANDATORY_FIELDS_0810 = {
                                                           Field.TRANSMISSION_DATE_AND_TIME, Field.SYSTEMS_TRACE_AUDIT_NUMBER, Field.RESPONSE_CODE,
                                                           Field.NETWORK_MANAGEMENT_INFORMATION_CODE
    };
    
    private static final Field[] MANDATORY_FIELDS_0200 = {
                                                           Field.PROCESSING_CODE,
                                                           Field.TRANSACTION_AMOUNT,
                                                           Field.TRANSMISSION_DATE_AND_TIME,
                                                           Field.SYSTEMS_TRACE_AUDIT_NUMBER,
                                                           Field.LOCAL_TRANSACTION_TIME,
                                                           Field.LOCAL_TRANSACTION_DATE,
                                                           Field.CAPTURE_DATE,
                                                           Field.ACQUIRING_INSTITUTION_IDENTIFICATION_CODE,
                                                           Field.TRACK_2_DATA,
                                                           Field.RETRIEVAL_REFERENCE_NUMBER,
                                                           Field.CARD_ACCEPTOR_TERMINAL_IDENTIFICATION,
                                                           Field.CARD_ACCEPTOR_NAME_LOCATION, // Request only
                                                           Field.RETAILER_DATA,
                                                           Field.TRANSACTION_CURRENCY_CODE,
                                                           Field.TERMINAL_DATA,
                                                           Field.CARD_ISSUER_CATEGORY_RESPONSE_CODE_DATA,
                                                           Field.RECEIVING_INSTITUTION_IDENTIFICATION_CODE,
                                                           Field.AUTHORIZATION_INDICATORS,
                                                           Field.BATCH_AND_SHIFT_DATA,
                                                           Field.SETTLEMENT_DATA,
                                                           Field.PREAUTHORIZATION_AND_CHARGEBACK_DATA // Request only
    };
    
    private static final Field[] MANDATORY_FIELDS_0210 = {
                                                           Field.PROCESSING_CODE,
                                                           Field.TRANSACTION_AMOUNT,
                                                           Field.TRANSMISSION_DATE_AND_TIME,
                                                           Field.SYSTEMS_TRACE_AUDIT_NUMBER,
                                                           Field.LOCAL_TRANSACTION_TIME,
                                                           Field.LOCAL_TRANSACTION_DATE,
                                                           Field.CAPTURE_DATE,
                                                           Field.ACQUIRING_INSTITUTION_IDENTIFICATION_CODE,
                                                           Field.TRACK_2_DATA,
                                                           Field.RETRIEVAL_REFERENCE_NUMBER,
                                                           Field.AUTHORIZATION_IDENTIFICATION_RESPONSE, // Response only
                                                           Field.RESPONSE_CODE, // Response only
                                                           Field.CARD_ACCEPTOR_TERMINAL_IDENTIFICATION,
                                                           Field.RETAILER_DATA,
                                                           Field.TRANSACTION_CURRENCY_CODE,
                                                           Field.TERMINAL_DATA,
                                                           Field.CARD_ISSUER_CATEGORY_RESPONSE_CODE_DATA,
                                                           Field.RECEIVING_INSTITUTION_IDENTIFICATION_CODE,
                                                           Field.AUTHORIZATION_INDICATORS,
                                                           Field.BATCH_AND_SHIFT_DATA,
                                                           Field.SETTLEMENT_DATA
    };
    
    private byte[] header;
    
    public Base24Msg() {}
    
    public Base24Msg(final String mti) {
        setMTI(mti.getBytes());
    }
    
    public Base24Msg(final Base24Msg other) {
        super(other);
        if (other.header != null) {
            header = new byte[other.header.length];
            System.arraycopy(other.header, 0, header, 0, other.header.length);
        }
    }
    
    public Base24Msg(final Base24Msg other, final String mti) {
        this(other);
        setMTI(mti.getBytes());
    }
    
    public byte[] getHeader() {
        return header;
    }
    
    public void setHeader(byte[] header) {
        this.header = header;
    }
    
    /**
     * Gets the message type from the message.
     *
     * @return the message type
     */
    public final MessageType getMessageType() {
        try {
            String mti = new String(getMTI());
            
            if (mti.equals("0800")) {
                
                String networkCode = (String) getValue(Field.NETWORK_MANAGEMENT_INFORMATION_CODE);
                if (networkCode.equals(NetworkManagementInformationCode.LOGON.code())) {
                    return MessageType.LOGON;
                } else if (networkCode.equals(NetworkManagementInformationCode.LOGOFF.code())) {
                    return MessageType.LOGOFF;
                } else if (networkCode.equals(NetworkManagementInformationCode.ECHO.code())) {
                    return MessageType.ECHO;
                } else {
                    throw new IllegalStateException("Unknown Network Management Information Code [" + networkCode + "]");
                }
                
            } else if (mti.equals("0810")) {
                
                String networkCode = (String) getValue(Field.NETWORK_MANAGEMENT_INFORMATION_CODE);
                
                if (networkCode.equals(NetworkManagementInformationCode.LOGON.code())) {
                    return MessageType.LOGON_REPLY;
                } else if (networkCode.equals(NetworkManagementInformationCode.LOGOFF.code())) {
                    return MessageType.LOGOFF_REPLY;
                } else if (networkCode.equals(NetworkManagementInformationCode.ECHO.code())) {
                    return MessageType.ECHO_REPLY;
                } else {
                    throw new IllegalStateException("Unknown Network Management Information Code [" + networkCode + "]");
                }
                
            } else if (mti.equals("9800")) {
                
                String networkCode = (String) getValue(Field.NETWORK_MANAGEMENT_INFORMATION_CODE);
                if (networkCode.equals(NetworkManagementInformationCode.LOGON.code())) {
                    return MessageType.REJECTED_LOGON;
                } else if (networkCode.equals(NetworkManagementInformationCode.LOGOFF.code())) {
                    return MessageType.REJECTED_LOGOFF;
                } else if (networkCode.equals(NetworkManagementInformationCode.ECHO.code())) {
                    return MessageType.REJECTED_ECHO;
                } else {
                    throw new IllegalStateException("Unknown Network Management Information Code [" + networkCode + "]");
                }
            } else if (mti.equals("0200")) {
                
                String processingCode = (String) getValue(Field.PROCESSING_CODE);
                
                if (processingCode.equals(ProcessingCode.CREDIT.code())) {
                    return MessageType.CREDIT_BALANCE;
                } else if (processingCode.equals(ProcessingCode.DEBIT.code())) {
                    return MessageType.DEBIT_BALANCE;
                } else if (processingCode.equals(ProcessingCode.QUERY.code())) {
                    return MessageType.QUERY_BALANCE;
                } else {
                    throw new IllegalStateException("Unknown Processing Code [" + processingCode + "]");
                }
                
            } else if (mti.equals("0210")) {
                
                String processingCode = (String) getValue(Field.PROCESSING_CODE);
                
                if (processingCode.equals(ProcessingCode.CREDIT.code())) {
                    return MessageType.CREDIT_BALANCE_REPLY;
                } else if (processingCode.equals(ProcessingCode.DEBIT.code())) {
                    return MessageType.DEBIT_BALANCE_REPLY;
                } else if (processingCode.equals(ProcessingCode.QUERY.code())) {
                    return MessageType.QUERY_BALANCE_REPLY;
                } else {
                    throw new IllegalStateException("Unknown Processing Code [" + processingCode + "]");
                }
                
            } else if (mti.equals("9200")) {
                
                String processingCode = (String) getValue(Field.PROCESSING_CODE);
                
                if (processingCode.equals(ProcessingCode.CREDIT.code())) {
                    return MessageType.REJECTED_CREDIT_BALANCE;
                } else if (processingCode.equals(ProcessingCode.DEBIT.code())) {
                    return MessageType.REJECTED_DEBIT_BALANCE;
                } else if (processingCode.equals(ProcessingCode.QUERY.code())) {
                    return MessageType.REJECTED_QUERY_BALANCE;
                } else {
                    throw new IllegalStateException("Unknown Processing Code [" + processingCode + "]");
                }
            } else
                throw new IllegalStateException("Unknown Message Type Indicator " + mti);
        } catch (Exception e) {
            throw new IllegalStateException("Unparsable message : " + e.getMessage());
        }
    }
    
    /**
     * Sets the given Field with the given value
     */
    public final void set(final Field f, final String s) {
        set(f.index(), s);
    }
    
    /**
     * Unsets the given Field
     */
    public final void unset(final Field f) {
        unset(f.index());
    }
    
    /**
     * Gets the value of the given Field <i> This method will not work correctly with composite fields.</i>
     *
     * @return String representation of field
     */
    public final String getValue(final Field f) {
        String value = getStringValue(f.index());
        if (value == null) {
            throw new IllegalStateException("Unable to retrieve field [" + f.name() + "] from Base24Msg");
        } else {
            return value.toString();
        }
    }
    
    /**
     * Checks whether the specified mandatory fields exist in the given message. <i> This method only checks the existence of the designated
     * fields and does not perform any kind of semantic checks. </i>
     *
     * @return true if all the required fields are present in the message, false otherwise.
     */
    public boolean containsMandatoryFields() {
        final Field[] requiredFields;
        String mti = new String(getMTI());
        
        if (mti.equals("0800")) {
            requiredFields = MANDATORY_FIELDS_0800;
        } else if (mti.equals("0810")) {
            requiredFields = MANDATORY_FIELDS_0810;
        } else if (mti.equals("0200")) {
            requiredFields = MANDATORY_FIELDS_0200;
        } else if (mti.equals("0210")) {
            requiredFields = MANDATORY_FIELDS_0210;
        } else {
            throw new IllegalStateException("Unknown Message Type Indicator " + mti);
        }
        
        // Make sure all the required fields are there
        for (int i = 0; i < requiredFields.length; i++) {
            if (!hasField(requiredFields[i].index())) {
                // one of the fields is missing, return false
                return false;
            }
        }
        
        return true;
    }
    
    @Override
    protected ISOPackager getPackager() {
        return Base24Packager.getInstance();
    }
    
}
