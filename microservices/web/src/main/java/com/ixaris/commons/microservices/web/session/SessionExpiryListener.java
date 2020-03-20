package com.ixaris.commons.microservices.web.session;

@FunctionalInterface
public interface SessionExpiryListener<SESSION extends SessionInfo> {
    
    void onSessionExpired(SESSION session, SessionTerminationType type);
    
}
