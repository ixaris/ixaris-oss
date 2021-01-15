package com.ixaris.commons.zeromq.microservices.stack;

public class TestZMQServiceChannel {
    
    public final TestZMQServiceSupport serviceSupport;
    public final TestZMQServiceClientSupport serviceClientSupport;
    
    public TestZMQServiceChannel(final TestZMQServiceSupport serviceSupport, final TestZMQServiceClientSupport serviceClientSupport) {
        this.serviceSupport = serviceSupport;
        this.serviceClientSupport = serviceClientSupport;
    }
    
}
