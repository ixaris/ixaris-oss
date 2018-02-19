package com.ixaris.commons.async.lib.lock;

import java.util.concurrent.locks.Lock;

import com.ixaris.commons.misc.lib.function.CallableThrows;
import com.ixaris.commons.misc.lib.function.RunnableThrows;

public final class LockUtil {
    
    public static <E extends Throwable> void exec(final Lock lock, final RunnableThrows<E> runnable) throws E {
        lock.lock();
        try {
            runnable.run();
        } finally {
            lock.unlock();
        }
    }
    
    public static <T, E extends Throwable> T exec(final Lock lock, final CallableThrows<T, E> callable) throws E {
        lock.lock();
        try {
            return callable.call();
        } finally {
            lock.unlock();
        }
    }
    
    private LockUtil() {}
    
}
