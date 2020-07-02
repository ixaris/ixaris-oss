package com.ixaris.commons.persistence.lib;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixaris.commons.async.lib.AsyncTrace;

public final class Transaction implements TransactionalPersistenceContext {
    
    private static final Logger LOG = LoggerFactory.getLogger(Transaction.class);
    
    private boolean rollbackOnly = false;
    private final List<Runnable> commitHooks = new ArrayList<>();
    private final List<Consumer<Throwable>> rollbackHooks = new ArrayList<>();
    
    @Override
    public boolean isRollbackOnly() {
        return rollbackOnly;
    }
    
    @Override
    public void setRollbackOnly() {
        this.rollbackOnly = true;
    }
    
    @Override
    public void onCommit(final Runnable commitHook) {
        if (commitHook == null) {
            throw new IllegalArgumentException("commitHook is null");
        }
        commitHooks.add(commitHook);
    }
    
    @Override
    public void onRollback(final Consumer<Throwable> rollbackHook) {
        if (rollbackHook == null) {
            throw new IllegalArgumentException("rollbackHook is null");
        }
        rollbackHooks.add(rollbackHook);
    }
    
    void executeOnCommit() {
        for (final Runnable commitHook : commitHooks) {
            try {
                commitHook.run();
            } catch (final RuntimeException e) {
                LOG.error("commit hook threw error", AsyncTrace.join(e));
            }
        }
    }
    
    void executeOnRollback(final Throwable rollbackError) {
        for (final Consumer<Throwable> rollbackHook : rollbackHooks) {
            try {
                rollbackHook.accept(rollbackError);
            } catch (final RuntimeException e) {
                LOG.error("rollback hook threw error", AsyncTrace.join(e));
            }
        }
    }
    
}
