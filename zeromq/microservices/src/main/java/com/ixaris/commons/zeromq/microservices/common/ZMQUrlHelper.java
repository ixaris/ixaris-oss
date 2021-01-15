package com.ixaris.commons.zeromq.microservices.common;

import java.util.concurrent.atomic.AtomicInteger;

public final class ZMQUrlHelper {
    
    private final AtomicInteger portNumber;
    private final String hostname;
    
    public ZMQUrlHelper(final String hostname, final int startPort) {
        this.portNumber = new AtomicInteger(startPort);
        this.hostname = hostname;
    }
    
    public String generateServiceOperationUrl() {
        return String.format("tcp://%s:%d", hostname, portNumber.getAndIncrement());
    }
    
}
