package com.ixaris.commons.microservices.defaults.live;

import javax.annotation.PreDestroy;

import com.ixaris.commons.zeromq.microservices.ZMQGlobal;

public class ZMQBean {
    
    public ZMQBean() {
        ZMQGlobal.start();
    }
    
    @PreDestroy
    // Method needs to be called shutdown or close to be automatically invoked by spring
    public void shutdown() {
        ZMQGlobal.shutdown();
    }
    
}
