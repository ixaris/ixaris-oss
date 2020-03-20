/*
 * Copyright 2003 - 2006 Ixaris Systems Ltd. All rights reserved.<br>
 * IXARIS PROPRIETARY/CONFIDENTIAL. Use is subject to license terms.
 */
package com.ixaris.commons.misc.lib.id;

import java.util.concurrent.atomic.AtomicLong;

/**
 * Generate unique ids based on a virtual timestamp with a finer grained resolution than the system clock and node id
 * for ensuring cluster uniqueness. It is not the responsibility of this class to assign a unique node id. The node id
 * starts out as 0. Some service discovery aware component needs to determine a cluster-unique number for this node
 * between 0 and 65535
 *
 * <p>ids have the following structure:
 *
 * <ul>
 *   <li>timestamp (48 bits) with a resolution up to 17/10/6429 04:45:55
 *   <li>sequence (0-8 bits) allowing 1-256 distinct ids per millisecond
 *   <li>server_id (8-16 bits) allowing 255-65535 servers in a cluster
 * </ul>
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
public final class UniqueIdGenerator {
    
    /**
     * The amount of time by which the virtual clock lags behind the system clock. This value tries to balance allowing
     * a buffer for burst creation with not being too far in the past, such that using ids as creation time still makes
     * sense, while at the same time not risk creating the same ids if the node goes down and the clock is too far in
     * the future. NOTE value is shifted (<< 16), so 0x40000000L is 4 seconds
     */
    private static final long VIRTUAL_CLOCK_LAG = 0x40000000L;
    
    /**
     * Allow virtual clock to advance beyond (system clock - lag), before adding delays. This allows bursts of id
     * creations without incurring delays and without going too far in the future. Going too far in the future would
     * pose a problem if the node goes down, as this value is not persisted, which would mean that time would reset and
     * the same ids can be generated again. This can be mitigated in 2 ways: do not reassign a node id immediately, and
     * do not advance the clock too far in the future. The first option is beyond the control of this class, but the
     * second one is handled. NOTE value is shifted (<< 16), so 0x80000000L is 8 seconds
     */
    private static final long VIRTUAL_CLOCK_MAX_ADVANCE = 0x80000000L;
    
    // 64ms
    private static final long VIRTUAL_CLOCK_MAX_WAIT_MS = 0x40L;
    
    public static final UpdateableSequence DEFAULT_SEQUENCE = new UpdateableSequence();
    
    /**
     * Virtual clock value last used for generating an id. Normally very close to the physical clock, but not always
     * (system clock can be moved backwards, id bursts can move virtual clock forwards). Note that this virtual clock is
     * guaranteed to be monotonically increasing; that is, at given absolute time points t1 and t2 (where t2 is after
     * t1), t1 <= t2 will always hold true.
     */
    private static final AtomicLong VIRTUAL_CLOCK = new AtomicLong(System.currentTimeMillis() << 16);
    
    public static long generate() {
        return generate(DEFAULT_SEQUENCE);
    }
    
    @SuppressWarnings("squid:S135")
    public static long generate(final Sequence sequence) {
        final long sysTime = (System.currentTimeMillis() << 16) - VIRTUAL_CLOCK_LAG;
        
        // repeat until we manage to swap the last timestamp or the timestamp advanced more than the system time
        boolean synced = false;
        long timestamp;
        while (true) {
            timestamp = VIRTUAL_CLOCK.get();
            if (sysTime > timestamp) {
                // try to sync virtual clock with physical clock
                if (VIRTUAL_CLOCK.compareAndSet(timestamp, sysTime)) {
                    // Timestamp will be unique since this is an atomic operation
                    timestamp = sysTime;
                    synced = true;
                    break;
                }
            } else {
                break;
            }
        }
        
        final int nodeIdAndWidth = sequence.nodeIdAndWidth;
        
        if (!synced) {
            // advance the virtual clock (possibly ahead of the physical clock)
            // timestamp will be unique since this is an atomic operation
            // we get virtual clock increment by shifting 1 left by the node id width
            timestamp = VIRTUAL_CLOCK.addAndGet(1 << (nodeIdAndWidth & 0xFFFF));
        }
        
        // even without time going backwards, virtual clock may have advanced (burst id generation)
        if (sysTime < timestamp) {
            // time difference in (ms << 16)
            long diff = timestamp - sysTime;
            
            // Need to slow down? (to keep virtual time close to physical time; ie. either catch up when system
            // clock has been moved backwards, or when coarse clock resolution has advanced virtual clock too far)
            if (diff >= VIRTUAL_CLOCK_MAX_ADVANCE) {
                // Sleep to let system clock advance closer to the virtual clock. Delay is kept small to prevent
                // excessive blocking; should be enough to eventually sync virtual clock with physical
                try {
                    Thread.sleep(Math.min(VIRTUAL_CLOCK_MAX_WAIT_MS, diff >>> 16));
                } catch (final InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            }
        }
        
        // timestamp together with node id ensure cluster-wise uniqueness
        return timestamp | (nodeIdAndWidth >>> 16);
    }
    
    /**
     * Extract the timestamp part from a unique id, taking into consideration lag
     */
    public static long extractTimestamp(final long id) {
        return (id + VIRTUAL_CLOCK_LAG) >> 16;
    }
    
    private UniqueIdGenerator() {}
    
}
