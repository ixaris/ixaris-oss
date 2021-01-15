package com.ixaris.commons.zeromq.microservices.example.service;

import com.ixaris.commons.microservices.lib.service.annotations.ServiceKey;

/**
 * Another example ALT2 com.ixaris.core.authsessions.validatesecret.spi implementation, just to have 2 implementation of the SPI
 */
@ServiceKey(Alt3ExampleZmqSpiResourceImpl.KEY)
public final class Alt3ExampleZmqSpiResourceImpl extends AltExampleZmqSpiResourceImpl {
    
    public static final String KEY = "ALT3";
    
}
