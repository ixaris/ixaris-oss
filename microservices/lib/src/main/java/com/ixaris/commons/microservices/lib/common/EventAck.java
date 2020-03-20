package com.ixaris.commons.microservices.lib.common;

public final class EventAck {
    
    private static final EventAck INSTANCE = new EventAck();
    
    public static EventAck getInstance() {
        return INSTANCE;
    }
    
    private EventAck() {}
    
}
