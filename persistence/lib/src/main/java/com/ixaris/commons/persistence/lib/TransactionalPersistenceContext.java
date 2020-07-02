package com.ixaris.commons.persistence.lib;

import java.util.function.Consumer;

public interface TransactionalPersistenceContext {
    
    boolean isRollbackOnly();
    
    void setRollbackOnly();
    
    void onCommit(Runnable runnable);
    
    void onRollback(Consumer<Throwable> consumer);
    
}
