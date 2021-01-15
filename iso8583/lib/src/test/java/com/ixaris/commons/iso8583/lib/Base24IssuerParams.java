/*
 * Copyright 2002 - 2011 Ixaris Systems Ltd. All rights reserved.
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */

package com.ixaris.commons.iso8583.lib;

/**
 * Groups issuer parameters defined for every system base currency as context properties.
 */
public final class Base24IssuerParams implements java.io.Serializable {
    
    private static final long serialVersionUID = 8310055071933786059L;
    
    public static final Base24IssuerParams eur() {
        return new Base24IssuerParams("0000000019970010", "0000000000284006990", "PRO1");
    }
    
    public static final Base24IssuerParams gbp() {
        return new Base24IssuerParams("0000000019970001", "0000000000284004082", "PRO1");
    }
    
    public static final Base24IssuerParams usd() {
        return new Base24IssuerParams("0000000019970010", "0000000000284006889", "PRO1");
    }
    
    private final String cardAcceptorTerminalIdentification;
    private final String retailerID;
    private final String systemLogicalNetwork;
    
    /**
     * Class constructor. Initialises the state of this class and perform state validation
     *
     * @param cardAcceptorTerminalIdentification
     * @param retailerID
     * @throws IllegalStateException if any of the parameters is not valid
     */
    public Base24IssuerParams(String cardAcceptorTerminalIdentification, String retailerID, String systemLogicalNetwork) {
        this.cardAcceptorTerminalIdentification = cardAcceptorTerminalIdentification;
        this.retailerID = retailerID;
        this.systemLogicalNetwork = systemLogicalNetwork;
        validateObject();
    }
    
    /**
     * Get method for cardAcceptorTerminalIdentification
     *
     * @return the cardAcceptorTerminalIdentification
     */
    public String getCardAcceptorTerminalIdentification() {
        return cardAcceptorTerminalIdentification;
    }
    
    /**
     * Get method for retailerID
     *
     * @return the retailerID
     */
    public String getRetailerID() {
        return retailerID;
    }
    
    /**
     * Get method for systemLogicalNetwork
     *
     * @return the systemLogicalNetwork
     */
    public String getSystemLogicalNetwork() {
        return systemLogicalNetwork;
    }
    
    private void validateObject() {
        // validate Card Acceptor Terminal Identification
        if (cardAcceptorTerminalIdentification == null) {
            throw new IllegalStateException("the Card Acceptor Terminal Identification is null");
        }
        // we validate only the length since it can contain special
        // characters and we don't know exactly what these can be
        if (cardAcceptorTerminalIdentification.length() != 16) {
            throw new IllegalStateException("the Card Acceptor Terminal Identification has invalid length. Length is "
                + cardAcceptorTerminalIdentification.length()
                + " and should be 16");
        }
        
        // validate retailerId
        if (retailerID == null) {
            throw new IllegalStateException("the Retailer Id is null");
        }
        // we validate only the length since it can contain special
        // characters and we don't know exactly what these can be
        if (retailerID.length() != 19) {
            throw new IllegalStateException("the Retailer Id has invalid length. Length is " + retailerID.length() + " and should be 19");
        }
        
        // validate system logical network
        if (systemLogicalNetwork == null) {
            throw new IllegalStateException("the System Logical Network is null");
        }
        // we validate only the length since it can contain special
        // characters and we don't know exactly what these can be
        if (systemLogicalNetwork.length() != 4) {
            throw new IllegalStateException("the System Logical Network has invalid length. Length is "
                + systemLogicalNetwork.length()
                + " and should be 4");
        }
    }
}
