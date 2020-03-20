package com.ixaris.commons.microservices.web.session;

import java.util.HashMap;
import java.util.Iterator;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Map.Entry;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.StampedLock;

import javax.annotation.PostConstruct;
import javax.annotation.PreDestroy;

import com.ixaris.commons.async.lib.scheduler.Scheduler;
import com.ixaris.commons.misc.lib.lock.LockUtil;

public class LocalSessionMap<S, T, I extends SessionInfo> extends AbstractSessionMap<S, T, I> {
    
    private static final class SessionInfoAndLastAccess<I> {
        
        private final I info;
        private long lastAccessed;
        
        SessionInfoAndLastAccess(final I info) {
            this.info = info;
            accessed();
        }
        
        SessionInfoAndLastAccess(final SessionInfoAndLastAccess<I> toReplace, final I newInfo) {
            this.info = newInfo;
            this.lastAccessed = toReplace.lastAccessed;
        }
        
        void accessed() {
            lastAccessed = System.currentTimeMillis();
        }
        
    }
    
    private final LinkedHashMap<T, SessionInfoAndLastAccess<I>> tokenToSessionMap = new LinkedHashMap<>(0, 0.75f, true);
    private final StampedLock lock = new StampedLock();
    private final long sessionTimeout;
    private final Map<Long, T> sessionToTokenMap;
    
    private Scheduler scheduler;
    private ScheduledFuture<?> expiryTask;
    
    public LocalSessionMap(final long sessionTimeout, final SessionRestorer<S> sessionRestorer) {
        super(sessionRestorer);
        this.sessionTimeout = sessionTimeout;
        sessionToTokenMap = new HashMap<>();
    }
    
    @PostConstruct
    public final void postConstruct() {
        if (scheduler == null) {
            scheduler = Scheduler.newScheduler(LocalSessionMap.class.getSimpleName());
            expiryTask = scheduler.scheduleWithFixedDelay(this::expire, 1, 1, TimeUnit.MINUTES);
        }
    }
    
    @PreDestroy
    public final void preDestroy() {
        if (scheduler != null) {
            expiryTask.cancel(false);
            expiryTask = null;
            scheduler.shutdown();
            scheduler = null;
        }
    }
    
    @Override
    protected final I getByToken(final T token) {
        return LockUtil.write(lock, () -> {
            final SessionInfoAndLastAccess<I> session = tokenToSessionMap.get(token);
            if (session != null) {
                session.accessed();
                tokenToSessionMap.replace(token, session);
                return session.info;
            }
            return null;
        });
    }
    
    @Override
    protected final I get(final long sessionId) {
        return LockUtil.read(lock, true, () -> {
            final T token = sessionToTokenMap.get(sessionId);
            if (token != null) {
                return tokenToSessionMap.get(token).info;
            } else {
                return null;
            }
        });
    }
    
    @Override
    protected final boolean put(final T token, final I info) {
        return LockUtil.write(lock, () -> {
            if (sessionToTokenMap.putIfAbsent(info.getId(), token) == null) {
                tokenToSessionMap.put(token, new SessionInfoAndLastAccess<>(info));
                return true;
            } else {
                return false;
            }
        });
    }
    
    @Override
    protected final boolean replace(final I info) {
        return LockUtil.write(lock, () -> {
            final T token = sessionToTokenMap.get(info.getId());
            if (token != null) {
                tokenToSessionMap.computeIfPresent(token, (k, v) -> new SessionInfoAndLastAccess<>(v, info));
                return true;
            } else {
                return false;
            }
        });
    }
    
    @Override
    protected final I removeByToken(final T token) {
        return LockUtil.write(lock, () -> {
            final SessionInfoAndLastAccess<I> session = tokenToSessionMap.get(token);
            if (session != null) {
                tokenToSessionMap.remove(token);
                sessionToTokenMap.remove(session.info.getId());
                return session.info;
            } else {
                return null;
            }
        });
    }
    
    @Override
    protected I remove(final long sessionId) {
        return LockUtil.write(lock, () -> {
            final T token = sessionToTokenMap.remove(sessionId);
            if (token != null) {
                return tokenToSessionMap.remove(token).info;
            } else {
                return null;
            }
        });
    }
    
    private void expire() {
        final long now = System.currentTimeMillis();
        LockUtil.write(lock, () -> {
            final Iterator<Entry<T, SessionInfoAndLastAccess<I>>> iterator = tokenToSessionMap.entrySet().iterator();
            while (iterator.hasNext()) {
                final Entry<T, SessionInfoAndLastAccess<I>> next = iterator.next();
                if ((next.getValue().lastAccessed + sessionTimeout) <= now) {
                    iterator.remove();
                    onSessionInactive(next.getValue().info);
                } else {
                    break;
                }
            }
        });
    }
    
}
