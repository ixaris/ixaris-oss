/*
 * Copyright 2002 - 2011 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.ixaris.commons.iso8583.lib;

import java.security.SecureRandom;
import java.util.Calendar;

import com.ixaris.commons.iso8583.lib.Base24Msg.Field;
import com.ixaris.commons.iso8583.lib.Base24Msg.ResponseCode;

/**
 * Class containing helper functions for the BASE24 resource adaptor. This must be in the same package as the adaptor, since it must be able to
 * stand alone.
 *
 * <p>It is acceptable for other packages in the server to use items from here, but not for the adaptor classes to use anything from the rest of
 * the server.
 *
 * @author Brian Vella
 */
public final class Base24Utils {
    
    public static final int DEFAULT_EXPIRY_MONTH_SHIFT = 3;
    
    /**
     * Maximum length of credit-card number allowed (in digits) for BASE-24/ISO.
     */
    public static final int BASE24_CARDNO_MAX_DIGITS = 19;
    
    /**
     * Max absolute value of balance adjustment; strictly positive. This is constrained by the number of digits in the ISO message.
     *
     * <p>This is in terms of the units passed, eg pennies for GBP.
     *
     * <p>Given a 12-digit limit this is 1E12 - 1.
     */
    public static final long BASE24_AMOUNT_ABS_LIMIT = 999999999999L;
    
    public static final String CURRENCY_EUR = "978";
    public static final String CURRENCY_GBP = "826";
    public static final String CURRENCY_USD = "840";
    
    private static final int AMOUNT_FIELD_WIDTH = 12;
    
    /**
     * Template messages, filled with static data. Upon generation of a new message, a copy is taken, and dynamic fields filled.
     */
    public static final Base24Packager BASE24_PACKAGER;
    
    private static final Base24Msg BASE24_LOGON_TEMPLATE;
    private static final Base24Msg BASE24_LOGOFF_TEMPLATE;
    private static final Base24Msg BASE24_ECHO_TEMPLATE;
    private static final Base24Msg BASE24_QUERY_BALANCE_TEMPLATE;
    private static final Base24Msg BASE24_CREDIT_BALANCE_TEMPLATE;
    private static final Base24Msg BASE24_DEBIT_BALANCE_TEMPLATE;
    
    /**
     * Static block that initialises message templates
     */
    static {
        BASE24_PACKAGER = Base24Packager.getInstance();
        BASE24_LOGON_TEMPLATE = new Base24Msg("0800");
        BASE24_LOGOFF_TEMPLATE = new Base24Msg("0800");
        BASE24_ECHO_TEMPLATE = new Base24Msg("0800");
        BASE24_QUERY_BALANCE_TEMPLATE = new Base24Msg("0200");
        BASE24_CREDIT_BALANCE_TEMPLATE = new Base24Msg("0200");
        BASE24_DEBIT_BALANCE_TEMPLATE = new Base24Msg("0200");
        
        final byte[] managementHeader = { '0', '0', '6', '0', '0', '0', '0', '5', '0' };
        
        BASE24_LOGON_TEMPLATE.setHeader(managementHeader);
        BASE24_LOGON_TEMPLATE.set(Field.NETWORK_MANAGEMENT_INFORMATION_CODE, Base24Msg.NetworkManagementInformationCode.LOGON.code());
        
        BASE24_LOGOFF_TEMPLATE.setHeader(managementHeader);
        BASE24_LOGOFF_TEMPLATE.set(Field.NETWORK_MANAGEMENT_INFORMATION_CODE, Base24Msg.NetworkManagementInformationCode.LOGOFF.code());
        
        BASE24_ECHO_TEMPLATE.setHeader(managementHeader);
        BASE24_ECHO_TEMPLATE.set(Field.NETWORK_MANAGEMENT_INFORMATION_CODE, Base24Msg.NetworkManagementInformationCode.ECHO.code());
        
        final byte[] financialHeader = { '0', '2', '6', '0', '0', '0', '0', '5', '0' };
        BASE24_QUERY_BALANCE_TEMPLATE.setHeader(financialHeader);
        BASE24_QUERY_BALANCE_TEMPLATE.set(Field.PROCESSING_CODE, Base24Msg.ProcessingCode.QUERY.code());
        BASE24_QUERY_BALANCE_TEMPLATE.set(Field.TRANSACTION_AMOUNT, "000000000000");
        BASE24_QUERY_BALANCE_TEMPLATE.set(Field.POINT_OF_SERVICE_ENTRY_MODE, "012");
        BASE24_QUERY_BALANCE_TEMPLATE.set(Field.ACQUIRING_INSTITUTION_IDENTIFICATION_CODE, "3");
        BASE24_QUERY_BALANCE_TEMPLATE.set(Field.CARD_ACCEPTOR_NAME_LOCATION, "HTTPS://WWW.IXARIS.COMINTA_XBIEXMLT000ML");
        BASE24_QUERY_BALANCE_TEMPLATE.set(Field.RECEIVING_INSTITUTION_IDENTIFICATION_CODE, "1");
        BASE24_QUERY_BALANCE_TEMPLATE.set(Field.AUTHORIZATION_INDICATORS, "000000000000000000P0");
        BASE24_QUERY_BALANCE_TEMPLATE.set(Field.BATCH_AND_SHIFT_DATA, "001001000");
        BASE24_QUERY_BALANCE_TEMPLATE.set(Field.SETTLEMENT_DATA, "000000000000");
        BASE24_QUERY_BALANCE_TEMPLATE.set(Field.PREAUTHORIZATION_AND_CHARGEBACK_DATA, "11100000000000000000000000000000000000");
        
        BASE24_CREDIT_BALANCE_TEMPLATE.setHeader(financialHeader);
        BASE24_CREDIT_BALANCE_TEMPLATE.set(Field.PROCESSING_CODE, Base24Msg.ProcessingCode.CREDIT.code());
        BASE24_CREDIT_BALANCE_TEMPLATE.set(Field.POINT_OF_SERVICE_ENTRY_MODE, "012");
        BASE24_CREDIT_BALANCE_TEMPLATE.set(Field.ACQUIRING_INSTITUTION_IDENTIFICATION_CODE, "3");
        BASE24_CREDIT_BALANCE_TEMPLATE.set(Field.CARD_ACCEPTOR_NAME_LOCATION, "HTTPS://WWW.IXARIS.COMINTA_XBIEXMLT000ML");
        BASE24_CREDIT_BALANCE_TEMPLATE.set(Field.RECEIVING_INSTITUTION_IDENTIFICATION_CODE, "1");
        BASE24_CREDIT_BALANCE_TEMPLATE.set(Field.AUTHORIZATION_INDICATORS, "000000000000000000P0");
        BASE24_CREDIT_BALANCE_TEMPLATE.set(Field.BATCH_AND_SHIFT_DATA, "001001000");
        BASE24_CREDIT_BALANCE_TEMPLATE.set(Field.SETTLEMENT_DATA, "000000000000");
        BASE24_CREDIT_BALANCE_TEMPLATE.set(Field.PREAUTHORIZATION_AND_CHARGEBACK_DATA, "11100000000000000000000000000000000000");
        
        BASE24_DEBIT_BALANCE_TEMPLATE.setHeader(financialHeader);
        BASE24_DEBIT_BALANCE_TEMPLATE.set(Field.PROCESSING_CODE, Base24Msg.ProcessingCode.DEBIT.code());
        BASE24_DEBIT_BALANCE_TEMPLATE.set(Field.POINT_OF_SERVICE_ENTRY_MODE, "012");
        BASE24_DEBIT_BALANCE_TEMPLATE.set(Field.ACQUIRING_INSTITUTION_IDENTIFICATION_CODE, "3");
        BASE24_DEBIT_BALANCE_TEMPLATE.set(Field.CARD_ACCEPTOR_NAME_LOCATION, "HTTPS://WWW.IXARIS.COMINTA_XBIEXMLT000ML");
        BASE24_DEBIT_BALANCE_TEMPLATE.set(Field.RECEIVING_INSTITUTION_IDENTIFICATION_CODE, "1");
        BASE24_DEBIT_BALANCE_TEMPLATE.set(Field.AUTHORIZATION_INDICATORS, "000000000000000000P0");
        BASE24_DEBIT_BALANCE_TEMPLATE.set(Field.BATCH_AND_SHIFT_DATA, "001001000");
        BASE24_DEBIT_BALANCE_TEMPLATE.set(Field.SETTLEMENT_DATA, "000000000000");
        BASE24_DEBIT_BALANCE_TEMPLATE.set(Field.PREAUTHORIZATION_AND_CHARGEBACK_DATA, "11100000000000000000000000000000000000");
    }
    
    /**
     * Field 7: the TimeStamp in the format: MMDDhhmmss.
     *
     * @return the timestamp
     */
    public static final String getTimeStamp() {
        final Calendar cal = Calendar.getInstance();
        // Set to GMT
        // i.e 0 Offset
        cal.set(Calendar.ZONE_OFFSET, 0);
        // and 0 Daylight savings
        cal.set(Calendar.DST_OFFSET, 0);
        return ((cal.get(Calendar.MONTH) + 1 < 10
            ? ("0" + Integer.toString(cal.get(Calendar.MONTH) + 1)) : Integer.toString(cal.get(Calendar.MONTH) + 1))
            + (cal.get(Calendar.DAY_OF_MONTH) < 10
                ? ("0" + Integer.toString(cal.get(Calendar.DAY_OF_MONTH))) : Integer.toString(cal.get(Calendar.DAY_OF_MONTH)))
            + (cal.get(Calendar.HOUR_OF_DAY) < 10
                ? ("0" + Integer.toString(cal.get(Calendar.HOUR_OF_DAY))) : (Integer.toString(cal.get(Calendar.HOUR_OF_DAY))))
            + (cal.get(Calendar.MINUTE) < 10 ? ("0" + Integer.toString(cal.get(Calendar.MINUTE))) : (Integer.toString(cal.get(Calendar.MINUTE))))
            + (cal.get(Calendar.SECOND) < 10
                ? ("0" + Integer.toString(cal.get(Calendar.SECOND))) : (Integer.toString(cal.get(Calendar.SECOND)))));
    }
    
    /**
     * Field 11: the system trace audit number with format: HHMMSS.
     *
     * @return the system trace audit number
     */
    public static final String getSystemTraceAuditNumber() {
        final Calendar cal = Calendar.getInstance();
        return ((cal.get(Calendar.HOUR_OF_DAY) < 10
            ? ("0" + Integer.toString(cal.get(Calendar.HOUR_OF_DAY))) : (Integer.toString(cal.get(Calendar.HOUR_OF_DAY))))
            + (cal.get(Calendar.MINUTE) < 10 ? ("0" + Integer.toString(cal.get(Calendar.MINUTE))) : (Integer.toString(cal.get(Calendar.MINUTE))))
            + (cal.get(Calendar.SECOND) < 10
                ? ("0" + Integer.toString(cal.get(Calendar.SECOND))) : (Integer.toString(cal.get(Calendar.SECOND)))));
    }
    
    /**
     * Field 12: The local transaction time. Currently returns the time with format: HHMMSS
     *
     * @return the local transaction time
     */
    public static final String getLocalTransactionTime() {
        String date;
        Calendar cal = Calendar.getInstance();
        
        date = (cal.get(Calendar.HOUR_OF_DAY) < 10
            ? ("0" + Integer.toString(cal.get(Calendar.HOUR_OF_DAY))) : (Integer.toString(cal.get(Calendar.HOUR_OF_DAY))))
            + (cal.get(Calendar.MINUTE) < 10 ? ("0" + Integer.toString(cal.get(Calendar.MINUTE))) : (Integer.toString(cal.get(Calendar.MINUTE))))
            + (cal.get(Calendar.SECOND) < 10
                ? ("0" + Integer.toString(cal.get(Calendar.SECOND))) : (Integer.toString(cal.get(Calendar.SECOND))));
        return date;
    }
    
    /**
     * Field 13: The local transaction date. Currently returns the date with format: MMDD
     *
     * @return the local transaction date
     */
    public static final String getLocalTransactionDate() {
        String date;
        Calendar cal = Calendar.getInstance();
        
        date = (cal.get(Calendar.MONTH) + 1 < 10
            ? ("0" + Integer.toString(cal.get(Calendar.MONTH) + 1)) : Integer.toString(cal.get(Calendar.MONTH) + 1))
            + (cal.get(Calendar.DAY_OF_MONTH) < 10
                ? ("0" + Integer.toString(cal.get(Calendar.DAY_OF_MONTH))) : Integer.toString(cal.get(Calendar.DAY_OF_MONTH)));
        return date;
    }
    
    /**
     * Field 17: The capture date. Currently returns the date of tomorrow with format: MMDD
     *
     * @return the capture date
     */
    public static final String getCaptureDate() {
        String date;
        Calendar cal = Calendar.getInstance();
        cal.add(Calendar.DAY_OF_MONTH, 1);
        
        date = (cal.get(Calendar.MONTH) + 1 < 10
            ? ("0" + Integer.toString(cal.get(Calendar.MONTH) + 1)) : Integer.toString(cal.get(Calendar.MONTH) + 1))
            + (cal.get(Calendar.DAY_OF_MONTH) < 10
                ? ("0" + Integer.toString(cal.get(Calendar.DAY_OF_MONTH))) : Integer.toString(cal.get(Calendar.DAY_OF_MONTH)));
        return date;
    }
    
    /**
     * Field 35: Part of Track 2 Data. Returns the default expiry date for a card
     *
     * @return the default expiry date in the form of YYMM
     */
    public static final String getDefaultExpiryDate() {
        final Calendar cal = Calendar.getInstance();
        cal.add(Calendar.MONTH, DEFAULT_EXPIRY_MONTH_SHIFT);
        
        final String year = Integer.toString(cal.get(Calendar.YEAR));
        final String month = Integer.toString(cal.get(Calendar.MONTH) + 1);
        
        // get last 2 digits for year
        String date = year.substring(year.length() - 2);
        // add ) if month < 10
        if (month.length() == 1) {
            date += "0";
        }
        date += month;
        
        return date;
    }
    
    private static String lastRetrievalReferenceNumber = "";
    private static final Object retrievalReferenceNumberLock = new Object();
    
    private static final SecureRandom r = new SecureRandom();
    
    private static String generateReferenceNumber() {
        // return "RF" + getTimeStamp();
        // we get the current timestamp and convert it into hex
        String refNo = Long.toHexString(r.nextLong());
        
        if (refNo.length() > 12) {
            // we truncate it if the length exceeds 12
            refNo = refNo.substring(refNo.length() - 12);
        } else if (refNo.length() < 12) {
            // we pad the string with 0s if the length is less that 12
            StringBuilder sb = new StringBuilder(12);
            for (int i = refNo.length(); i < 12; i++) {
                sb.append("0");
            }
            sb.append(refNo);
            
            refNo = sb.toString();
        }
        
        return refNo;
    }
    
    /**
     * Field 37: The retrieval reference number. Currently returns with format RF + Timestamp
     *
     * @return the retrieval reference number, 12 alphanumeric
     */
    public static String getRetrievalReferenceNumber() {
        
        boolean retry;
        String refNo;
        
        do {
            refNo = generateReferenceNumber();
            
            synchronized (retrievalReferenceNumberLock) {
                if (refNo.equals(lastRetrievalReferenceNumber)) {
                    retry = true;
                } else {
                    lastRetrievalReferenceNumber = refNo;
                    retry = false;
                }
            }
        } while (retry);
        
        return refNo;
    }
    
    /**
     * @param responseCode
     */
    public static String getFailureReasonForResponseCode(final ResponseCode responseCode) {
        switch (responseCode) {
            case DENIED_05:
                return "Operation was denied by the server";
            case INSUFFICIENT_FUNDS_51:
                return "Insufficient balance for operation";
            case EXPIRED_CARD_051_54:
            case EXPIRED_CARD_901_33:
                return "The card is expired";
            case ERROR_06:
            case DPC_DOWN_91:
                return "Server reported error [" + responseCode + "]";
            case CAF_NOT_FOUND_56:
            case INVALID_CARD_NUMBER_14:
            case RESTRICTED_CARD_36:
                return "Card problem on BASE24 side [" + responseCode + "]";
            case INVALID_TRANSACTION_12:
            case DUPLICATE_TRANSACTION_94:
                return "Error during processing of transaction [" + responseCode + "]";
            default:
                throw new UnsupportedOperationException("Unhandled response Code [" + responseCode + "]");
        }
    }
    
    /**
     * Create a BASE-24 ISO-port logon message. We should be welcomed with one of these immediately upon opening a connection to BASE-24 (over
     * TCP/IP).
     *
     * @return entire message
     */
    public static Base24Msg base24LogonMsg() {
        // make a copy of the template message
        Base24Msg msg = new Base24Msg(BASE24_LOGON_TEMPLATE);
        
        // set dynamic fields
        msg.set(Field.TRANSMISSION_DATE_AND_TIME, getTimeStamp());
        msg.set(Field.SYSTEMS_TRACE_AUDIT_NUMBER, getSystemTraceAuditNumber());
        
        return msg;
    }
    
    /**
     * Creates a BASE24 ISO-port logoff message. This operation should be used just before we close the socket connection to the BASE24.
     *
     * @return logoff message
     */
    public static Base24Msg base24LogoffMsg() {
        // make a copy of the template message
        Base24Msg msg = new Base24Msg(BASE24_LOGOFF_TEMPLATE);
        
        // set dynamic fields
        msg.set(Field.TRANSMISSION_DATE_AND_TIME, getTimeStamp());
        msg.set(Field.SYSTEMS_TRACE_AUDIT_NUMBER, getSystemTraceAuditNumber());
        
        return msg;
    }
    
    /**
     * Creates a BASE24 ISO-port echo message.
     *
     * @return the echo message
     */
    public static Base24Msg base24EchoMsg() {
        // make a copy of the template message
        Base24Msg msg = new Base24Msg(BASE24_ECHO_TEMPLATE);
        
        // set dynamic fields
        msg.set(Field.TRANSMISSION_DATE_AND_TIME, getTimeStamp());
        msg.set(Field.SYSTEMS_TRACE_AUDIT_NUMBER, getSystemTraceAuditNumber());
        
        return msg;
    }
    
    /**
     * Composes a BASE-24 ISO message to adjust the balance of a specified card in the PBF. The adjustment can be positive or negative.
     *
     * <p>The (pure-numeric, usually 16-digit) full credit-card number is passed.
     *
     * <p>This reasonably carefully vets its parameters for syntax, but we cannot validate the currency code or amount for good sense.
     *
     * @param issParams fixed parameters for a particular issuer, not null
     * @param cardNumber full credit-card number, not more than 19 digits, and not null
     * @param currencyCodeBASE24 3-digit currency code (i.e. range 100-999)
     * @param amountInLSUnits positive or negative amount to add to available spend balance in smallest units for the currency, e.g. pennies for
     *     GBP
     * @return the adjust balance message
     */
    public static Base24Msg base24AdjustCardBalanceMsg(final Base24IssuerParams issParams,
                                                       final String cardNumber,
                                                       final String currencyCodeBASE24,
                                                       final long amountInLSUnits,
                                                       final String localTransactionDate,
                                                       final String localTransactionTime,
                                                       final String retrievalReferenceNumber) {
        
        // Validate syntax of all parameters.
        
        // We must have a set of issuer parameters.
        if (issParams == null) {
            throw new IllegalArgumentException("issParams");
        }
        
        // Check the cardNumber is all numeric and not too long.
        if ((cardNumber == null) || (cardNumber.length() > BASE24_CARDNO_MAX_DIGITS)) {
            throw new IllegalArgumentException("Invalid card number: must not be null or more than "
                + BASE24_CARDNO_MAX_DIGITS
                + " digits ["
                + cardNumber
                + "].");
        }
        for (int i = cardNumber.length(); --i >= 0;) {
            final char c = cardNumber.charAt(i);
            if ((c < '0') || (c > '9')) {
                throw new IllegalArgumentException("Invalid card number: must only contain ASCII digits 0 to 9 [" + cardNumber + "].");
            }
        }
        
        // Check the payment is within the message limits.
        if ((amountInLSUnits < -BASE24_AMOUNT_ABS_LIMIT) || (amountInLSUnits > BASE24_AMOUNT_ABS_LIMIT)) {
            throw new IllegalArgumentException("Invalid payment adjustment, limit +/- "
                + BASE24_AMOUNT_ABS_LIMIT
                + ". ["
                + amountInLSUnits
                + "].");
        }
        
        // make a copy of the template message
        Base24Msg msg;
        
        if (amountInLSUnits >= 0) {
            msg = new Base24Msg(BASE24_CREDIT_BALANCE_TEMPLATE);
        } else {
            msg = new Base24Msg(BASE24_DEBIT_BALANCE_TEMPLATE);
        }
        
        // set dynamic fields
        
        // We build a left-padded strictly-positive value in positiveAmount.
        final StringBuffer positiveAmount = new StringBuffer(AMOUNT_FIELD_WIDTH);
        positiveAmount.append(Math.abs(amountInLSUnits));
        while (positiveAmount.length() < AMOUNT_FIELD_WIDTH) {
            positiveAmount.insert(0, '0');
        }
        // System.out.println("amountInLSUnits == " + amountInLSUnits);
        // System.out.println("positiveAmount == " + positiveAmount);
        
        msg.set(Field.TRANSACTION_AMOUNT, positiveAmount.toString());
        msg.set(Field.TRANSMISSION_DATE_AND_TIME, getTimeStamp());
        msg.set(Field.SYSTEMS_TRACE_AUDIT_NUMBER, getSystemTraceAuditNumber());
        msg.set(Field.LOCAL_TRANSACTION_TIME, localTransactionTime);
        msg.set(Field.LOCAL_TRANSACTION_DATE, localTransactionDate);
        msg.set(Field.CAPTURE_DATE, getCaptureDate());
        msg.set(Field.TRACK_2_DATA, cardNumber + "=" + getDefaultExpiryDate());
        msg.set(Field.RETRIEVAL_REFERENCE_NUMBER, retrievalReferenceNumber);
        msg.set(Field.CARD_ACCEPTOR_TERMINAL_IDENTIFICATION, issParams.getCardAcceptorTerminalIdentification());
        msg.set(Field.RETAILER_DATA, issParams.getRetailerID() + "00010001");
        msg.set(Field.TRANSACTION_CURRENCY_CODE, currencyCodeBASE24);
        msg.set(Field.TERMINAL_DATA, "IXA " + issParams.getSystemLogicalNetwork() + "00000000");
        msg.set(Field.CARD_ISSUER_CATEGORY_RESPONSE_CODE_DATA, "IXA " + issParams.getSystemLogicalNetwork() + "03100000000");
        
        return msg;
    }
    
    /**
     * Composes a BASE-24 ISO message to query the balance of a card.
     *
     * @param issParams fixed parameters for a particular issuer, not null
     * @param cardNumber full credit-card number, not more than 19 digits, and not null
     * @param currencyCodeBASE24 3-digit currency code (i.e. range 100-999)
     * @return the query balance message
     */
    public static Base24Msg base24QueryCardBalanceMsg(final Base24IssuerParams issParams,
                                                      final String cardNumber,
                                                      final String currencyCodeBASE24,
                                                      final String localTransactionDate,
                                                      final String localTransactionTime,
                                                      final String retrievalReferenceNumber) {
        
        // We must have a set of issuer parameters.
        if (issParams == null) {
            throw new IllegalArgumentException("issParams");
        }
        // Check the cardNumber is all numeric and not too long.
        if ((cardNumber == null) || (cardNumber.length() > BASE24_CARDNO_MAX_DIGITS)) {
            throw new IllegalArgumentException("Invalid card number: must not be null or more than "
                + BASE24_CARDNO_MAX_DIGITS
                + " digits ["
                + cardNumber
                + "].");
        }
        for (int i = cardNumber.length(); --i >= 0;) {
            final char c = cardNumber.charAt(i);
            if ((c < '0') || (c > '9')) {
                throw new IllegalArgumentException("Invalid card number: must only contain ASCII digits 0 to 9 [" + cardNumber + "].");
            }
        }
        
        // make a copy of the template message
        Base24Msg msg = new Base24Msg(BASE24_QUERY_BALANCE_TEMPLATE);
        
        // set dynamic fields
        msg.set(Field.TRANSMISSION_DATE_AND_TIME, getTimeStamp());
        msg.set(Field.SYSTEMS_TRACE_AUDIT_NUMBER, getSystemTraceAuditNumber());
        msg.set(Field.LOCAL_TRANSACTION_TIME, localTransactionTime);
        msg.set(Field.LOCAL_TRANSACTION_DATE, localTransactionDate);
        msg.set(Field.CAPTURE_DATE, getCaptureDate());
        msg.set(Field.TRACK_2_DATA, cardNumber + "=" + getDefaultExpiryDate());
        msg.set(Field.RETRIEVAL_REFERENCE_NUMBER, retrievalReferenceNumber);
        msg.set(Field.CARD_ACCEPTOR_TERMINAL_IDENTIFICATION, issParams.getCardAcceptorTerminalIdentification());
        msg.set(Field.RETAILER_DATA, issParams.getRetailerID() + "00010001");
        msg.set(Field.TRANSACTION_CURRENCY_CODE, currencyCodeBASE24);
        msg.set(Field.TERMINAL_DATA, "IXA " + issParams.getSystemLogicalNetwork() + "00000000");
        msg.set(Field.CARD_ISSUER_CATEGORY_RESPONSE_CODE_DATA, "IXA " + issParams.getSystemLogicalNetwork() + "03100000000");
        
        return msg;
    }
    
    /**
     * This method constructs the response to the given base24 ECHO-request message.
     *
     * @param msg the echo request message
     * @return the echo response to the echo request message
     */
    public static Base24Msg base24EchoResponse(final Base24Msg msg) {
        // If the message is null then throw an exception
        if (msg == null) {
            throw new IllegalArgumentException("msg");
        }
        
        // If message does not represent a valid ECHO message then throw exception
        if (!msg.getMessageType().equals(Base24Msg.MessageType.ECHO)) {
            throw new IllegalArgumentException("Msg is not a valid echo response message [" + msg + "].");
        }
        
        // make a copy of the received message
        Base24Msg newMsg = new Base24Msg(msg, "0810");
        
        // set fields that indicate successful reply
        newMsg.setHeader(new byte[] { '0', '0', '6', '0', '0', '0', '0', '0', '0' });
        newMsg.set(Field.RESPONSE_CODE, ResponseCode.APPROVED_00.code());
        
        return newMsg;
    }
    
    private Base24Utils() {}
    
}
