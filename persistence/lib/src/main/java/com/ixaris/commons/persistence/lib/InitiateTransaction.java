package com.ixaris.commons.persistence.lib;

import com.ixaris.commons.async.lib.idempotency.Intent;
import com.ixaris.commons.misc.lib.function.CallableThrows;

public interface InitiateTransaction {
    
    <T, E extends Exception> T transaction(CallableThrows<T, E> persistenceCallable) throws E;
    
    <T, E extends Exception> IntentTransaction<T, E> transaction(Intent intent, CallableThrows<T, E> persistenceCallable) throws E;
    
}
