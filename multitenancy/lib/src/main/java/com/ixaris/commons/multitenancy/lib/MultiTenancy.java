package com.ixaris.commons.multitenancy.lib;

import static com.ixaris.commons.async.lib.Async.all;
import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.CompletionStageUtil.join;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ScheduledFuture;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.locks.StampedLock;
import java.util.stream.Collectors;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.AsyncLocal;
import com.ixaris.commons.async.lib.StringAsyncLocal;
import com.ixaris.commons.async.lib.scheduler.Scheduler;
import com.ixaris.commons.collections.lib.ListenerSet;
import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.misc.lib.function.RunnableThrows;
import com.ixaris.commons.misc.lib.lock.LockUtil;

public final class MultiTenancy {
    
    public static final String SYSTEM_TENANT = "system";
    
    /**
     * Hold the currently active tenant for each thread. Null if currently thread has no active tenant. The validation allows any tenant to
     * switch to the system tenant, used e.g. to look up applications that are only available in the system tenant database. Switching between
     * activatingTenants is disallowed as there is currently no use case for it
     */
    public static final AsyncLocal<String> TENANT = new StringAsyncLocal("tenant", (value, prev) -> {
        if ((prev != null) && !prev.equals(value) && !SYSTEM_TENANT.equals(value)) {
            throw new IllegalStateException("Only SYSTEM tenant can be stacked");
        }
        return value;
    });
    
    /**
     * Performs a task within the scope of a tenant.
     *
     * @param tenantId
     * @param task The callable task to be performed within the scope of the tenant.
     * @deprecated use the asynclocal directly
     */
    @Deprecated
    public static <V, E extends Exception> V doAsTenant(final String tenantId, final CallableThrows<V, E> task) throws E {
        return TENANT.exec(tenantId, task);
    }
    
    /**
     * Performs a task within the scope of a tenant.
     *
     * @param tenantId
     * @param task The runnable task to be performed within the scope of the tenant.
     * @deprecated use the asynclocal directly
     */
    @Deprecated
    public static <E extends Exception> void doAsTenant(final String tenantId, final RunnableThrows<E> task) throws E {
        TENANT.exec(tenantId, task);
    }
    
    /**
     * Returns the currently active tenant for the current thread.
     *
     * <p>If {@code nullable} is `false` and there currently is no active tenant on this thread, throws {@code IllegalStateException}
     *
     * @param nullable If false requires that there currently is an active tenant
     * @return the tenant id of the currently active tenant
     * @throws IllegalStateException if there is no active tenant on this thread and active tenant is required.
     */
    public static String getCurrentTenant(final boolean nullable) {
        final String currentTenantId = TENANT.get();
        if (!nullable && (currentTenantId == null)) {
            // Request expected a tenant to be active.
            throw new IllegalStateException(Thread.currentThread().toString() + " does not have an active tenant. An active tenant is required.");
        }
        
        return currentTenantId;
    }
    
    /**
     * Returns the currently active tenant for the current thread. It is imperative that this method fails if there is no active tenant. Throws
     * an IllegalStateException if there currently is no active tenant.
     *
     * @return the tenant id of the currently active tenant
     * @throws IllegalStateException if there is not active tenant.
     */
    public static String getCurrentTenant() {
        return getCurrentTenant(false);
    }
    
    private static final Logger LOG = LoggerFactory.getLogger(MultiTenancy.class);
    
    private static final long DEFAULT_RETRY_DELAY_MINUTES = 5L;
    
    private enum State {
        INACTIVE(null),
        POST_DEACTIVATE(INACTIVE),
        DEACTIVATE(POST_DEACTIVATE),
        ACTIVE(null),
        ACTIVATE(ACTIVE),
        PRE_ACTIVATE(ACTIVATE);
        
        private final State next;
        
        State(final State next) {
            this.next = next;
        }
        
        public final State getNext() {
            return next;
        }
        
    }
    
    private static final class States {
        
        private final State current;
        private final State next;
        
        private States(final State current) {
            this(current, current.next);
        }
        
        private States(final State current, final State next) {
            this.current = current;
            this.next = next;
        }
        
    }
    
    private final AtomicBoolean active = new AtomicBoolean(false);
    private final Set<TenantLifecycleParticipant> lifecycleParticipants = new HashSet<>();
    private final ListenerSet<TenantLifecycleListener> tenantLifecycleListeners = new ListenerSet<>();
    
    private final Set<String> tenants = new HashSet<>();
    private final StampedLock tenantsLock = new StampedLock();
    
    private final Map<String, States> processingTenants = new HashMap<>();
    private final Map<String, Set<TenantLifecycleParticipant>> errorTenants = new HashMap<>();
    
    private final long retryDelay;
    private final long retryPeriod;
    private Scheduler scheduler;
    private ScheduledFuture<?> retryTask;
    
    public MultiTenancy() {
        this(DEFAULT_RETRY_DELAY_MINUTES, DEFAULT_RETRY_DELAY_MINUTES, TimeUnit.MINUTES);
    }
    
    public MultiTenancy(final long retryDelay, final long retryPeriod, final TimeUnit unit) {
        this.retryDelay = unit.toMillis(retryDelay);
        this.retryPeriod = unit.toMillis(retryPeriod);
    }
    
    public void registerTenantLifecycleParticipant(final TenantLifecycleParticipant participant) {
        LockUtil.write(tenantsLock, () -> {
            if (!tenants.isEmpty()) {
                throw new IllegalStateException("Cannot register participants after tenants are added");
            }
            
            LOG.info("Registering participation [{}]", participant.getName());
            lifecycleParticipants.add(participant);
        });
    }
    
    public void addTenantLifecycleListener(final TenantLifecycleListener listener) {
        if (tenantLifecycleListeners.add(listener)) {
            LockUtil.read(tenantsLock, false, () -> {
                tenants.forEach(listener::onTenantActive);
                return null;
            });
        }
    }
    
    public void removeTenantLifecycleListener(final TenantLifecycleListener listener) {
        tenantLifecycleListeners.remove(listener);
    }
    
    public Async<Void> addTenant(final String tenantId) {
        final State next = LockUtil.write(tenantsLock, () -> {
            if (tenants.contains(tenantId)) {
                return null;
            } else {
                final States states = processingTenants.get(tenantId);
                if (states != null) {
                    // just in case activating a deactivating tenant
                    switch (states.current) {
                        case DEACTIVATE:
                            processingTenants.put(tenantId, new States(states.current, State.ACTIVATE));
                            break;
                        case POST_DEACTIVATE:
                            processingTenants.put(tenantId, new States(states.current, State.PRE_ACTIVATE));
                            break;
                        default:
                    }
                    return null;
                } else if (!lifecycleParticipants.isEmpty()) {
                    processingTenants.put(tenantId, new States(State.PRE_ACTIVATE));
                    return State.PRE_ACTIVATE;
                } else {
                    // done here within the write lock
                    tenants.add(tenantId);
                    notifyTenantActive(tenantId, true);
                    return State.ACTIVE;
                }
            }
        });
        if (next != null) {
            LOG.info("Activating [{}]", tenantId);
            return next(tenantId, next);
        } else {
            return result();
        }
    }
    
    public Async<Void> removeTenant(final String tenantId) {
        final State next = LockUtil.write(tenantsLock, () -> {
            if (tenants.remove(tenantId)) {
                notifyTenantActive(tenantId, false);
                if (!lifecycleParticipants.isEmpty()) {
                    processingTenants.put(tenantId, new States(State.DEACTIVATE));
                    return State.DEACTIVATE;
                } else {
                    return State.INACTIVE;
                }
            } else {
                final States states = processingTenants.get(tenantId);
                if (states != null) {
                    // just in case deactivating an activating tenant
                    switch (states.current) {
                        case ACTIVATE:
                            processingTenants.put(tenantId, new States(states.current, State.DEACTIVATE));
                            break;
                        case PRE_ACTIVATE:
                            processingTenants.put(tenantId, new States(states.current, State.POST_DEACTIVATE));
                            break;
                        default:
                    }
                }
                return null;
            }
        });
        if (next != null) {
            LOG.info("Deactivating [{}]", tenantId);
            return next(tenantId, next);
        } else {
            return result();
        }
    }
    
    public void setTenants(final Collection<String> tenants) {
        // Starting with the full set of tenants, remove the already existing tenants from the tenants to ensure and add
        // only the missing ones
        final HashSet<String> currentTenants = LockUtil.read(tenantsLock, true, () -> new HashSet<>(this.tenants));
        final Set<String> addedTenants = new HashSet<>(tenants);
        addedTenants.removeAll(currentTenants);
        addedTenants.forEach(this::addTenant);
        currentTenants.removeAll(tenants);
        currentTenants.forEach(this::removeTenant);
    }
    
    /**
     * @return A set containing the tenant id of each tenant.
     * @throws IllegalStateException If there is currently an active tenant. No tenant should be aware of other activatingTenants.
     */
    public Set<String> getActiveTenants() {
        if (TENANT.get() != null) {
            // We have an active tenant. No tenant should be aware of other tenants (even if it is just the name)
            throw new IllegalStateException(String.format("Cannot retrieve information about other tenants with an active tenant [%s]", TENANT.get()));
        }
        
        return LockUtil.read(tenantsLock, true, () -> new HashSet<>(tenants));
    }
    
    /**
     * Checks if the tenant with id {@code tenantId} exists within this provider.
     *
     * <p>Throws IllegalStateException if a different tenant is active on the current thread.
     *
     * @param tenantId Id of tenant
     * @return True if tenant is loaded within provider, false otherwise.
     */
    public boolean isTenantActive(final String tenantId) {
        final String activeTenant = TENANT.get();
        if (activeTenant != null && !activeTenant.equals(tenantId)) {
            // We have an active tenant. No tenant should be aware of other tenants (even if it is just the name)
            throw new IllegalStateException(String.format("Cannot retrieve information about tenant [%s] with an active tenant [%s]", tenantId, activeTenant));
        }
        return LockUtil.read(tenantsLock, true, () -> tenants.contains(tenantId));
    }
    
    /**
     * Checks if the tenant with id {@code tenantId} exists within this provider.
     *
     * <p>Throws IllegalStateException if a different tenant is active on the current thread.
     *
     * @param tenantId Id of tenant
     * @return True if tenant is loaded within provider, false otherwise.
     */
    public void verifyTenantIsActive(final String tenantId) throws TenantInactiveException {
        final String activeTenant = TENANT.get();
        if (activeTenant != null && !activeTenant.equals(tenantId)) {
            // We have an active tenant. No tenant should be aware of other tenants (even if it is just the name)
            throw new IllegalStateException(String.format("Cannot retrieve information about tenant [%s] with an active tenant [%s]", tenantId, activeTenant));
        }
        LockUtil.read(tenantsLock, true, () -> {
            if (!tenants.contains(tenantId)) {
                final States states = processingTenants.get(tenantId);
                if (states == null) {
                    throw new TenantInactiveException(tenantId);
                } else {
                    throw new TenantInactiveException(tenantId, states.current.name(), errorTenants.get(tenantId));
                }
            }
            return null;
        });
    }
    
    /**
     * Should only by used by edge services.
     *
     * <p>To be used in order to support a single tenant, without requiring that tenant to send its tenantID with every request.
     *
     * <p>If a single tenant is loaded, return that tenant's id. else throw {@link IllegalStateException}
     *
     * @return If a single tenant is loaded, return that tenant's id
     */
    public String getDefaultTenant() {
        if (TENANT.get() != null) {
            throw new IllegalStateException("A tenant is already active ... then why are you trying to get the default tenant?");
        }
        
        return LockUtil.read(tenantsLock, true, () -> {
            if (tenants.isEmpty()) {
                throw new IllegalStateException("No tenants loaded. Cannot get the default tenant if it is not loaded.");
            }
            if (tenants.size() > 1) {
                throw new IllegalStateException(String.format("Tenants %s are active. Default tenant requires single tenant", tenants));
            }
            
            return tenants.iterator().next();
        });
    }
    
    public void start() {
        if (active.compareAndSet(false, true)) {
            scheduler = Scheduler.newScheduler(MultiTenancy.class.getSimpleName());
            retryTask = scheduler.scheduleWithFixedDelay(this::retryProcessInError, retryDelay, retryPeriod, TimeUnit.MILLISECONDS);
        }
    }
    
    public void stop() {
        if (active.compareAndSet(true, false)) {
            if (scheduler != null) {
                retryTask.cancel(false);
                retryTask = null;
                scheduler.shutdown();
                scheduler = null;
            }
            join(all(getActiveTenants().stream().map(this::removeTenant).collect(Collectors.toList())));
        }
    }
    
    private Async<Void> next(final String tenantId, final State next) {
        switch (next) {
            case ACTIVE:
                LOG.info("Activated [{}]", tenantId);
                return result();
            case INACTIVE:
                LOG.info("Deactivated [{}]", tenantId);
                return result();
            default:
                return process(tenantId, lifecycleParticipants);
        }
    }
    
    private Async<Void> process(final String tenantId, final Set<TenantLifecycleParticipant> participants) {
        final Set<TenantLifecycleParticipant> errors = new HashSet<>();
        final Set<TenantLifecycleParticipant> remaining = new HashSet<>(participants);
        final List<Async<Void>> allParticipants = new ArrayList<>(participants.size());
        for (final TenantLifecycleParticipant participant : participants) {
            allParticipants.add(tryProcess(tenantId, participant, errors, remaining));
        }
        return all(allParticipants).map(r -> null);
    }
    
    @SuppressWarnings("SynchronizationOnLocalVariableOrMethodParameter")
    private Async<Void> tryProcess(final String tenantId,
                                   final TenantLifecycleParticipant participant,
                                   final Set<TenantLifecycleParticipant> errors,
                                   final Set<TenantLifecycleParticipant> remaining) {
        final State current = LockUtil.read(tenantsLock, true, () -> processingTenants.get(tenantId).current);
        try {
            LOG.debug("Trying to {} [{}] for [{}]", current, participant.getName(), tenantId);
            switch (current) {
                case PRE_ACTIVATE:
                    await(participant.preActivate(tenantId));
                    break;
                case ACTIVATE:
                    await(participant.activate(tenantId));
                    break;
                case DEACTIVATE:
                    await(participant.deactivate(tenantId));
                    break;
                case POST_DEACTIVATE:
                    await(participant.postDeactivate(tenantId));
                    break;
            }
            LOG.debug("Trying to {} [{}] for [{}]", current, participant.getName(), tenantId);
        } catch (final Throwable t) {
            LOG.error("Unable to {} [{}] for [{}]", current, participant.getName(), tenantId, t);
            synchronized (errors) {
                errors.add(participant);
            }
        }
        
        final boolean done;
        synchronized (remaining) {
            remaining.remove(participant);
            done = remaining.isEmpty();
        }
        
        if (done) {
            final State next = LockUtil.write(tenantsLock, () -> {
                if (errors.isEmpty()) {
                    final State n = processingTenants.remove(tenantId).next;
                    if (n.equals(State.ACTIVE)) {
                        // done here within the write lock
                        tenants.add(tenantId);
                        notifyTenantActive(tenantId, true);
                    } else if (!n.equals(State.INACTIVE)) {
                        processingTenants.put(tenantId, new States(n));
                    }
                    return n;
                } else {
                    errorTenants.put(tenantId, errors);
                    return null;
                }
            });
            if (next != null) {
                return next(tenantId, next);
            }
        }
        return result();
    }
    
    private Async<Void> retryProcessInError() {
        final Map<String, Set<TenantLifecycleParticipant>> errors = LockUtil.write(tenantsLock, () -> {
            final Map<String, Set<TenantLifecycleParticipant>> copy = new HashMap<>(errorTenants);
            errorTenants.clear();
            return copy;
        });
        
        return all(errors.entrySet().stream().map(e -> process(e.getKey(), e.getValue())).collect(Collectors.toList()))
            .map(r -> null);
    }
    
    private void notifyTenantActive(final String tenantId, final boolean active) {
        tenantLifecycleListeners.publish(l -> {
            if (active) {
                l.onTenantActive(tenantId);
            } else {
                l.onTenantInactive(tenantId);
            }
        });
    }
    
}
