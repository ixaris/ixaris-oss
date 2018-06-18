package com.ixaris.commons.async.pool;

import com.ixaris.commons.async.lib.Async;

public interface AsyncConnectionPool<T> {
    
    /**
     * get a connection from the pool
     * 
     * @param timeout 0 to wait forever
     */
    Async<T> get(long timeout);
    
    /**
     * Default is equivalent to get(0)
     */
    default Async<T> get() {
        return get(0);
    }
    
}
