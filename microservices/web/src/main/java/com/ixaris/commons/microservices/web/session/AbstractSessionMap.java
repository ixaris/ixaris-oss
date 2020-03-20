package com.ixaris.commons.microservices.web.session;

import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.misc.lib.object.Tuple.tuple;
import static com.ixaris.commons.multitenancy.lib.MultiTenancy.TENANT;

import java.util.HashSet;
import java.util.Optional;
import java.util.Set;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.collections.lib.LockedMap;
import com.ixaris.commons.misc.lib.object.Tuple2;

/**
 * Extends this class to manage the storage and clustering of session information. Expiry is the responsibility of
 * implementors
 *
 * @param <SESSION> the session type
 * @param <T> the token type
 * @param <I> the information on the session to be clustered. This is typically transient information not stored
 *     elsewhere
 */
public abstract class AbstractSessionMap<SESSION, T, I extends SessionInfo> {
    
    private static final Logger LOG = LoggerFactory.getLogger(AbstractSessionMap.class);
    
    private final SessionRestorer<SESSION> sessionRestorer;
    private final LockedMap<Long, SESSION> activeSessions;
    private final Set<SessionExpiryListener<I>> listeners = new HashSet<>();
    
    public AbstractSessionMap(final SessionRestorer<SESSION> sessionRestorer) {
        this.sessionRestorer = sessionRestorer;
        activeSessions = new LockedMap<>();
    }
    
    /**
     * May update access time for session expiry here
     */
    protected abstract I getByToken(T token);
    
    protected abstract I get(long sessionId);
    
    protected abstract boolean put(T token, I session);
    
    protected abstract boolean replace(I session);
    
    protected abstract I removeByToken(T token);
    
    protected abstract I remove(long sessionId);
    
    /**
     * To be called when a session is terminated due to inactivity
     *
     * @param session
     */
    protected void onSessionInactive(final I session) {
        activeSessions.remove(session.getId());
        TENANT.exec(session.getTenantId(), () -> publishExpiryEvent(session, SessionTerminationType.INACTIVE));
    }
    
    public final void addExpiryListener(final SessionExpiryListener<I> listener) {
        synchronized (listeners) {
            listeners.add(listener);
        }
    }
    
    public final void removeExpiryListener(final SessionExpiryListener<I> listener) {
        synchronized (listeners) {
            listeners.remove(listener);
        }
    }
    
    public final void createTokenForSession(final T token, final SESSION session, final I clusteredSession) {
        if (put(token, clusteredSession)) {
            activeSessions.put(clusteredSession.getId(), session);
        } else {
            throw new IllegalStateException("Session [" + clusteredSession.getId() + "] already has a token");
        }
    }
    
    public final void updateSessionInfo(I clusteredSession) {
        if (!replace(clusteredSession)) {
            throw new IllegalStateException("Session [" + clusteredSession.getId() + "] is unknown");
        }
    }
    
    /**
     * Get session for token. May need to look up the session from a service, hence the promise
     *
     * @param token
     * @return
     */
    public final Async<Optional<Tuple2<SESSION, I>>> getSessionForToken(final T token) {
        return fetchSession(getByToken(token));
    }
    
    /**
     * Get session for id. May need to look up the session from a service, hence the promise
     *
     * @param sessionId
     * @return
     */
    public final Async<Optional<Tuple2<SESSION, I>>> getSessionForId(final long sessionId) {
        return fetchSession(get(sessionId));
    }
    
    private Async<Optional<Tuple2<SESSION, I>>> fetchSession(final I clusteredSession) {
        if (clusteredSession != null) {
            final SESSION session = activeSessions.get(clusteredSession.getId());
            if (session == null) {
                try {
                    return sessionRestorer
                        .restoreSession(clusteredSession)
                        .map(s -> {
                            activeSessions.put(clusteredSession.getId(), s);
                            return Optional.of(tuple(s, clusteredSession));
                        });
                } catch (final Throwable t) {
                    if (sessionRestorer.isCausedByUnauthenticatedSession(t)) {
                        remove(clusteredSession.getId());
                        return result(Optional.empty());
                    } else {
                        throw t;
                    }
                }
            } else {
                return result(Optional.of(tuple(session, clusteredSession)));
            }
        } else {
            return result(Optional.empty());
        }
    }
    
    public final void terminateSessionByToken(final T token, final SessionTerminationType type) {
        final I session = removeByToken(token);
        if (session != null) {
            activeSessions.remove(session.getId());
            publishExpiryEvent(session, type);
        }
    }
    
    public final void terminateSessionById(final long sessionId, final SessionTerminationType type) {
        final I session = remove(sessionId);
        if (session != null) {
            activeSessions.remove(sessionId);
            publishExpiryEvent(session, type);
        }
    }
    
    private void publishExpiryEvent(final I session, final SessionTerminationType type) {
        synchronized (listeners) {
            for (final SessionExpiryListener<I> listener : listeners) {
                try {
                    listener.onSessionExpired(session, type);
                } catch (final RuntimeException e) {
                    LOG.error("listener threw exception", e);
                }
            }
        }
    }
    
}
