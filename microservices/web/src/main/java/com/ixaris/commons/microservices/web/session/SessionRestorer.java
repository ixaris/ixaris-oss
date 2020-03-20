package com.ixaris.commons.microservices.web.session;

import com.ixaris.commons.async.lib.Async;

public interface SessionRestorer<SESSION> {
    
    /**
     * Restore (typically reload) a session. Should reject if session is no longer authenticated
     */
    Async<SESSION> restoreSession(SessionInfo sessionInfo);
    
    /**
     * If restoreSession fails, this should determine if the failure indicates that the session is no longer
     * authentication
     */
    boolean isCausedByUnauthenticatedSession(Throwable t);
    
}
