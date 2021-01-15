package com.ixaris.commons.iso8583.lib;

import java.io.Serializable;

/**
 * Created by lokesh.bharani on 12/16/2015.
 */
public final class InterswitchMsg extends ISOMsg implements Serializable {
    
    private static final long serialVersionUID = 4547915246796410090L;
    
    public InterswitchMsg() {}
    
    public InterswitchMsg(final String mti) {
        setMTI(mti.getBytes());
    }
    
    public enum MessageType {
        
        TRANSACTION_NOTIFICATION,
        TRANSACTION_NOTIFICATION_RESPONSE,
        REVERSE_AUTHENTICATION;
    }
    
    public enum Field {
        PRIMARY_ACCOUNT_NUMBER(2, "Pan"),
        PROCESSING_CODE(3, "DerivePikEntryTransactionType"),
        TRANSACTION_AMOUNT(4, "TransactionAmount"),
        SETTLEMENT_AMOUNT(5, "BillingAmount"),
        TRANSMISSION_DATE_TIME(7, "ProviderProcessingTimestamp"),
        CONVERSION_RATE_SETTLEMENT(9, "BillingConversionRate"),
        SYSTEM_TRACE_AUDIT_NUMBER(11, "Stan"),
        LOCAL_TRANSACTION_TIME(12, "LocalTransactionTime"),
        LOCAL_TRANSACTION_DATE(13, "LocalTransactionDate"),
        EXPIRATION_DATE(14, "ExpirationDate"),
        SETTLEMENT_DATE(15, "SettlementDate"),
        MERCHANT_TYPE(18, "MerchantCategoryCode"),
        POS_ENTRY_MODE(22, "PosEntryMode"),
        CARD_SEQUENCE_NUMBER(23, "CardSequenceNumber"),
        POS_CONDITION_CODE(25, "CardHolderPresent"),
        TRANSACTION_FEE_AMOUNT(28, "TransactionFeeAmount"),
        SETTLEMENT_FEE_AMOUNT(29, "SettlementFeeAmount"),
        TRANSACTION_PROCESSING_FEE(30, "TransactionProcessingFee"),
        SETTLE_PROCESSING_FEE_AMT(31, "CommissionAmount"),
        ACQUIRING_INSTITUTION_ID_CODE(32, "AcquirerReferenceNumber"),
        FORWARDING_INSTITUTION_ID(33, "ForwardingInstitutionId"),
        TRACK2_DATA(35, "Track2Data"),
        RETRIEVAL_REFERENCE_NUMBER(37, "RetrievalReferenceNumber"),
        AUTHORIZATION_ID_RESPONSE(38, "AuthorizationIdResponse"),
        AUTH_RESPONSE_CODE(39, "AuthCode"),
        SERVICE_RESTRICTION_CODE(40, "ServiceRestrictionCode"),
        CARD_ACCEPTOR_TERMINAL_ID(41, "CardAcceptorTerminalId"),
        CARD_ACCEPTOR_ID_CODE(42, "MerchantId"),
        CARD_ACCEPTOR_NAME_LOCATION(43, "MerchantName"),
        TRANSACTION_CURRENCY_CODE(49, "TransactionCurrencyCode"),
        SETTLEMENT_CURRENCY_CODE(50, "SettlementCurrencyCode"),
        ADDITIONAL_AMOUNTS(54, "AdditionalAmounts"),
        MESSAGE_REASON_CODE(56, "MessageReasonCode"),
        AUTHORIZATION_AGENT_INSTITUTION(58, "AuthorizationAgentInstitution"),
        ECHO_CODE(59, "EchoCode"),
        SETTLEMENT_CODE(66, "SettlementCode"),
        REPLACEMENT_AMOUNTS(95, "ReplacementAmounts"),
        // FILE_NAME(101, ""),
        ACCOUNT_IDENTIFICATION_1(102, "AccountIdentification1"),
        ACCOUNT_IDENTIFICATION_2(103, "AccountIdentification2"),
        POS_DATA_CODE(123, "PosDataCode"),
        POSTILION_PRIVATE_DATA(127, "PostilionPrivateData");
        
        private final int index;
        private final String name;
        
        Field(final int index, final String name) {
            
            if (name == null) {
                throw new IllegalArgumentException();
            }
            this.index = index;
            this.name = name;
        }
        
        public final int index() {
            return index;
        }
        
        public final String getName() {
            return name;
        }
        
    }
    
    public final MessageType getMessageType() {
        
        String mti = new String(getMTI());
        
        try {
            if (mti.equals("9220")) {
                return MessageType.TRANSACTION_NOTIFICATION;
            } else if (mti.equals("9420")) {
                return MessageType.REVERSE_AUTHENTICATION;
            } else if (mti.equals("9230")) {
                return MessageType.TRANSACTION_NOTIFICATION_RESPONSE;
            } else {
                throw new IllegalStateException("Unknown MTI : " + mti);
            }
        } catch (Exception e) {
            throw new IllegalStateException("Unknown MTI : " + mti);
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
        EXPIRED_CARD_54("54"),
        CARD_NOT_FOUND_56("56"),
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
    
    /**
     * Gets the value of the given Field <i> This method will not work correctly with composite fields.</i>
     *
     * @return String representation of field
     */
    public final String getValue(final Field f) {
        String value = getStringValue(f.index());
        if (value == null) {
            // throw new ISO8583ParsingException("Unable to retrieve field [" + f.name() + "] from ISO8583Msg");
            return null;
        } else {
            return value.toString();
        }
    }
    
    public final void set(final Field f, final String s) {
        set(f.index(), s);
    }
    
    public final void set(final Field f, final byte[] b) {
        set(f.index(), new ISOField(b.length, b));
    }
    
    public final void unset(final Field f) {
        unset(f.index());
    }
    
    @Override
    public ISOPackager getPackager() {
        return InterswitchPackager.getInstance();
    }
    
}
