package com.ixaris.commons.misc.lib.lock;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.StampedLock;
import java.util.function.Predicate;

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
    
    public static <T, E extends Throwable> T read(final ReadWriteLock lock, final CallableThrows<T, E> callable) throws E {
        return exec(lock.readLock(), callable);
    }
    
    public static <E extends Throwable> void write(final ReadWriteLock lock, final RunnableThrows<E> runnable) throws E {
        exec(lock.writeLock(), runnable);
    }
    
    public static <T, E extends Throwable> T write(final ReadWriteLock lock, final CallableThrows<T, E> callable) throws E {
        return exec(lock.writeLock(), callable);
    }
    
    @SuppressWarnings("squid:S2301")
    public static <T, E extends Throwable> T read(final StampedLock lock, final boolean optimistic, final CallableThrows<T, E> callable) throws E {
        if (optimistic) {
            final long stamp = lock.tryOptimisticRead();
            if (stamp != 0L) {
                final T result = callable.call();
                if (lock.validate(stamp)) {
                    return result;
                }
            }
        }
        final long stamp = lock.readLock();
        try {
            return callable.call();
        } finally {
            lock.unlockRead(stamp);
        }
    }
    
    @SuppressWarnings({ "squid:S2301", "squid:S134" })
    public static <T, E extends Throwable> T readMaybeWrite(final StampedLock lock,
                                                            final boolean optimistic,
                                                            final CallableThrows<T, E> readCallable,
                                                            final Predicate<T> readSufficient,
                                                            final CallableThrows<T, E> writeCallable) throws E {
        if (optimistic) {
            final long stamp = lock.tryOptimisticRead();
            if (stamp != 0L) {
                final T result = readCallable.call();
                if (lock.validate(stamp)) {
                    if (readSufficient.test(result)) {
                        return result;
                    }
                    return write(lock, writeCallable);
                }
            }
        }
        
        long stamp = lock.readLock();
        try {
            final T result = readCallable.call();
            if (readSufficient.test(result)) {
                return result;
            } else {
                final long writeStamp = lock.tryConvertToWriteLock(stamp);
                if (writeStamp != 0L) {
                    stamp = 0L; // no need to unlock read
                    return internalWrite(lock, writeStamp, writeCallable);
                }
            }
        } finally {
            if (stamp != 0L) {
                lock.unlockRead(stamp);
            }
        }
        
        return write(lock, writeCallable);
    }
    
    public static <E extends Throwable> void write(final StampedLock lock, final RunnableThrows<E> runnable) throws E {
        final long stamp = lock.writeLock();
        try {
            runnable.run();
        } finally {
            lock.unlockWrite(stamp);
        }
    }
    
    public static <T, E extends Throwable> T write(final StampedLock lock, final CallableThrows<T, E> callable) throws E {
        return internalWrite(lock, lock.writeLock(), callable);
    }
    
    public static <T, E extends Throwable> T internalWrite(final StampedLock lock, final long stamp, final CallableThrows<T, E> callable) throws E {
        try {
            return callable.call();
        } finally {
            lock.unlockWrite(stamp);
        }
    }
    
    private LockUtil() {}
    
}
