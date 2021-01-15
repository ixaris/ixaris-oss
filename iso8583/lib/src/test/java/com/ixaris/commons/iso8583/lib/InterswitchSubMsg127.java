package com.ixaris.commons.iso8583.lib;

import java.io.Serializable;

/**
 * Created by lokesh.bharani on 12/22/2015.
 */
public class InterswitchSubMsg127 extends ISOMsg implements Serializable {
    
    private static final long serialVersionUID = 4547915247796410090L;
    
    public InterswitchSubMsg127() {
        super();
    }
    
    public enum Field {
        
        SWITCH_KEY(2, "SwitchKey"),
        ROUTINGG_INFORMATION(3, "RoutingInformation"),
        POS_DATA(4, "PosData"),
        SERVICE_STATION_DATA(5, "ServiceStationData"),
        AUTHORIZATION_PROFILE(6, "AuthorizationProfile"),
        CHECK_DATA(7, "CheckData"),
        RETENTION_DATA(8, "RetentionData"),
        ADDITIONAL_NODE_DATA(9, "AdditionalNodeData"),
        CVV2(10, "Cvv2"),
        ORIGINAL_KEY(11, "OriginalKey"),
        TERMINAL_OWNER(12, "TerminalOwner"),
        POS_GEOGRAPHIC_DATA(13, "PosGeographicData"),
        SPONSOR_BANK(14, "SponsorBank"),
        ADDRESS_VERIFICATION_DATA(15, "AddressVerficationData"),
        ADDRESS_VERIFICATION_RESULT(16, "AddressVerficationResult"),
        CARDHOLDER_INFORMATION(17, "CardHolderInformation"),
        VALIDATION_DATA(18, "ValidationData"),
        BANK_DETAILS(19, "BankDetails"),
        ORIGINATOR_AUTHORIZER_SETTLEMENT_DATE(20, "AuthorizerSettlementDate"),
        RECORD_IDENTIFICATION(21, "RecordIdentification"),
        STRUCTURED_DATA(22, "StructuredData"),
        PAYEE_NAME_ADDRESS(23, "PayeeNameAddress"),
        PAYEE_REFERENCE(24, "PayeeReference"),
        ICC_DATA(25, "IccData"),
        ORIGINAL_NODE(26, "OrginalNode"),
        CARD_VERIFICATION_RESULT(27, "CardVerificationResult");
        
        /*
         * AMEX_CARD_IDENTIFIER(28),
         * THREED_SECURE_DATA(29),
         * THREED_SECURE_RESULT(30),
         * ISSUER_NETWORK_ID(31),
         * UCAF_DATA(32),
         * EXTENDED_TRANSACTION_TYPE(33),
         * ACCOUNT_TYPE_QUALIFIERS(34),
         * ACQUIRER_NETWORK_ID(35),
         * CUSTOMER_ID(36),
         * EXTENDED_RESPONSE_CODE(37),
         * ADDITIONAL_POS_DATA_CODE(38),
         * ORIGINAL_RESPONSE_CODE(39),
         * TRANSACTION_REFERENCE(40),
         * ORIGINATING_REMOTE_ADDRESS(41),
         * TRANSACTION_NUMBER(42);
         */
        
        private final int index;
        private final String name;
        
        Field(final int index, final String setterMethodName) {
            this.index = index;
            this.name = setterMethodName;
        }
        
        public final int index() {
            return index;
        }
        
        public final String getName() {
            return name;
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
            // throw new ISO8583ParsingException("Unable to retrieve field [" + f.name() + "] from ISO8583SubMsg");
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
        return InterswitchSubMsg127Packager.getInstance();
    }
    
}
