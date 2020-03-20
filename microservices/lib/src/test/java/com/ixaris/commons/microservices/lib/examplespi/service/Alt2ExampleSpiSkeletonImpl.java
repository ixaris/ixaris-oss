package com.ixaris.commons.microservices.lib.examplespi.service;

import com.ixaris.commons.microservices.lib.service.annotations.ServiceKey;

/**
 * Another example ALT2 com.ixaris.core.authsessions.validatesecret.spi implementation, just to have 2 implementation of the SPI
 */
@ServiceKey({ "ALT2", "ALT3" })
public final class Alt2ExampleSpiSkeletonImpl extends AltExampleSpiSkeletonImpl {}
