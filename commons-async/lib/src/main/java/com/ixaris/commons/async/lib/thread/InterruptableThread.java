package com.ixaris.commons.async.lib.thread;

import java.util.concurrent.atomic.AtomicInteger;

/**
 * Allows a thread to do some uninterruptable work and defer the interrupt till after that work is done.
 * Particularly useful when some code does not correctly handle interrupts.
 * <p>
 * WARNING: the uninterruptable work should NOT block! The intent is to avoid interrupting this work but not
 * to ignore the interrupt indefinitely, otherwise an application might hang.
 * 
 * @author brian.vella
 */
public class InterruptableThread extends Thread {
    
    @FunctionalInterface
    public interface UninterruptableWork<T> {
        
        T doWork();
        
    }
    
    public static boolean interrupted() {
        final InterruptableThread ct = (InterruptableThread) Thread.currentThread();
        boolean interrupted = Thread.interrupted();
        
        if (interrupted) {
            // this means that thread was interrupted and flag was reset to false, so we reset internal state as well
            if (!ct.state.compareAndSet(INTERRUPTED, INTERRUPTABLE)) {
                throw new IllegalStateException("Thread was interrupted; Expecting internal state to be " + INTERRUPTED + " but was " + ct.state.get());
            }
        } else {
            // thread interrupted flag was not set but we could have been interrupted, so return (and reset) internal state
            interrupted = ct.state.compareAndSet(INTERRUPTED, INTERRUPTABLE);
        }
        
        return interrupted;
    }
    
    private static final int INTERRUPTABLE = 0;
    private static final int NON_INTERRUPTABLE = 1;
    private static final int INTERRUPTED = 2;
    
    private final AtomicInteger state = new AtomicInteger(INTERRUPTABLE);
    
    public InterruptableThread() {
        super();
    }
    
    public InterruptableThread(final String name) {
        super(name);
    }
    
    @Override
    public final void interrupt() {
        // set state to INTERRUPTED and get the previous state
        // which can be INTERRUPTABLE or NON_INTERRUPTABLE
        final int prev = state.getAndSet(INTERRUPTED);
        
        if (prev == INTERRUPTABLE) {
            // in case of INTERRUPTABLE, we interrupt() immediately
            super.interrupt();
        }
    }
    
    protected final <T> T doUninterruptableWork(final boolean skipIfInterrupted, final UninterruptableWork<T> work) {
        
        // try to change from INTERRUPTABLE to NON_INTERRUPTABLE
        // if unsuccessful, state was changed to INTERRUPTED in interrupt()
        if (state.compareAndSet(INTERRUPTABLE, NON_INTERRUPTABLE)) {
            try {
                // we will not be interrupted while doing this work
                return work.doWork();
            } finally {
                // try to change back from NON_INTERRUPTABLE to INTERRUPTABLE
                // if unsuccessful, state was changed to INTERRUPTED in interrupt()
                if (!state.compareAndSet(NON_INTERRUPTABLE, INTERRUPTABLE)) {
                    // in this case the interrupt() call was skipped to not interrupt our work above, so we interrupt() now
                    super.interrupt();
                }
            }
            
        } else if (!skipIfInterrupted) {
            // if we are here, we should not skip the work if interrupted,
            // so clear flag, do work and interrupt again
            Thread.interrupted();
            
            try {
                // we will not be interrupted while doing this work
                return work.doWork();
            } finally {
                // interrupt again
                super.interrupt();
            }
        }
        
        return null;
    }
    
}
