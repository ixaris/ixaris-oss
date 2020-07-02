package com.ixaris.commons.persistence.lib;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.idempotency.Intent;
import com.ixaris.commons.misc.lib.function.CallableThrows;

public interface AsyncInitiateTransaction {
    
    <T, E extends Exception> Async<T> transaction(CallableThrows<T, E> persistenceCallable) throws E;
    
    <T, E extends Exception> AsyncIntentTransaction<T, E> transaction(Intent intent, CallableThrows<T, E> persistenceCallable) throws E;
    
}
