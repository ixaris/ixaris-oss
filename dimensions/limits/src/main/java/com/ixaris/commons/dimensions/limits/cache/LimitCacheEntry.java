package com.ixaris.commons.dimensions.limits.cache;

import java.util.Comparator;
import java.util.HashSet;
import java.util.Iterator;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
import java.util.SortedSet;
import java.util.TreeSet;
import java.util.concurrent.locks.StampedLock;

import com.ixaris.commons.dimensions.lib.context.Context;
import com.ixaris.commons.dimensions.limits.LimitDef;
import com.ixaris.commons.dimensions.limits.data.AbstractLimitExtensionEntity;
import com.ixaris.commons.dimensions.limits.data.LimitEntity;
import com.ixaris.commons.misc.lib.lock.LockUtil;

public class LimitCacheEntry<I extends AbstractLimitExtensionEntity<?, I>, L extends LimitDef<I>> {
    
    private static final Comparator<LimitEntity<?, ?>> COMPARATOR = (e1, e2) -> {
        // Sorted from most specific to least specific by dimension mask, then context depth
        final long maskDiff = e1.getContext().getDimensionsMask() - e2.getContext().getDimensionsMask();
        if (maskDiff > 0L) {
            return 1;
        } else if (maskDiff < 0L) {
            return -1;
        } else {
            return Long.compare(e1.getContext().getDepth() - e2.getContext().getDepth(), 0L);
        }
    };
    
    private final Set<LimitEntity<I, L>> futureEffectiveLimits = new LinkedHashSet<>();
    private final Set<LimitEntity<I, L>> effectiveNoExpiryLimits = new HashSet<>();
    
    private final StampedLock lock = new StampedLock();
    
    // sort by expiry for fast update
    private final SortedSet<LimitEntity<I, L>> effectiveWithExpiryLimits = new TreeSet<>((e1, e2) -> {
        final long timeDiff = e1.getLimit().getEffectiveTo() - e2.getLimit().getEffectiveTo();
        if (timeDiff > 0L) {
            return 1;
        } else if (timeDiff < 0L) {
            return -1;
        } else {
            return Long.compare(e1.getLimit().getId() - e2.getLimit().getId(), 0L);
        }
    });
    
    /**
     * Creates the limit cache entry Sorts the given list of limits according to their effective time period
     *
     * @param potentiallyEffectiveLimits the list of limits that are configured for this limit and are eiher effective now or effective in the
     *     future sorted by effective_from then by effective_to
     */
    public LimitCacheEntry(final List<LimitEntity<I, L>> potentiallyEffectiveLimits) {
        final long now = System.currentTimeMillis();
        for (final LimitEntity<I, L> limit : potentiallyEffectiveLimits) {
            if (limit.getLimit().getEffectiveFrom() > now) {
                futureEffectiveLimits.add(limit);
            } else if (limit.getLimit().getEffectiveTo() == null) {
                effectiveNoExpiryLimits.add(limit);
            } else if (limit.getLimit().getEffectiveTo() > now) {
                effectiveWithExpiryLimits.add(limit);
            } // else ignore, no longer effective
        }
    }
    
    /**
     * Gets the limits for the given query
     *
     * @param context - the ContextDef Instance containing the context dimensions for the given query
     * @return the sorted set of applicable limits
     */
    public SortedSet<LimitEntity<I, L>> getLimitsForContext(final Context<L> context) {
        final long now = System.currentTimeMillis();
        removeExpiredAndAddFutureEffective(now);
        
        final TreeSet<LimitEntity<I, L>> combinedLimits = LockUtil.read(lock, true, () -> {
            final TreeSet<LimitEntity<I, L>> c = new TreeSet<>(COMPARATOR);
            effectiveNoExpiryLimits.stream()
                .filter(entry -> LimitFilterType.MATCHING.test(entry, context))
                .forEachOrdered(c::add);
            effectiveWithExpiryLimits.stream()
                .filter(entry -> LimitFilterType.MATCHING.test(entry, context))
                .forEachOrdered(c::add);
            return c;
        });
        overrideMatchAnys(combinedLimits);
        return combinedLimits;
    }
    
    private void removeExpiredAndAddFutureEffective(final long now) {
        LockUtil.readMaybeWrite(lock,
            true,
            () -> {
                // sorted by expiry timestamp ascending, so check the first one
                final boolean hasNoExpiredLimits = effectiveWithExpiryLimits.isEmpty()
                    || (effectiveWithExpiryLimits.iterator().next().getLimit().getEffectiveTo() > now);
                // sorted by start timestamp ascending, so check the first one
                final boolean hasNoFutureLimits = futureEffectiveLimits.isEmpty()
                    || (futureEffectiveLimits.iterator().next().getLimit().getEffectiveFrom() > now);
                return hasNoExpiredLimits && hasNoFutureLimits;
            },
            r -> r,
            () -> {
                for (final Iterator<LimitEntity<I, L>> i = effectiveWithExpiryLimits.iterator(); i.hasNext();) {
                    final LimitEntity<I, L> next = i.next();
                    if (next.getLimit().getEffectiveTo() <= now) {
                        i.remove();
                    } else {
                        // sorted by expiry timestamp ascending, so stop as soon as we find the first non-expired limit
                        break;
                    }
                }
                
                for (final Iterator<LimitEntity<I, L>> i = futureEffectiveLimits.iterator(); i.hasNext();) {
                    final LimitEntity<I, L> next = i.next();
                    // add all limits that are now effective to the effective List
                    if (next.getLimit().getEffectiveFrom() <= now) {
                        i.remove();
                        if (next.getLimit().getEffectiveTo() == null) {
                            effectiveNoExpiryLimits.add(next);
                        } else if (next.getLimit().getEffectiveTo() > now) {
                            effectiveWithExpiryLimits.add(next);
                        } // else ignore, no longer effective
                    } else {
                        // sorted by start timestamp ascending, so stop as soon as we find the first future limit
                        break;
                    }
                }
                return true;
            });
    }
    
    private void overrideMatchAnys(final TreeSet<LimitEntity<I, L>> limits) {
        
        final Iterator<LimitEntity<I, L>> iterator = limits.descendingIterator();
        long lastLimitDimensionsMask = -1L;
        
        while (iterator.hasNext()) {
            /*
             * If the previous context implies the current one (i.e. they define the same dimensions, hence equal dimension mask), we remove the current
             * definition in favour of the previous since the previous is more specific.
             *
             * The previous context implies the current one if both have the same context dimensions, since we are assuming that the matching set is ordered
             * from least specific to most specific, and we're iterating in reverse order. Thus if we have the same dimensions, it means that the current one
             * (less specific) has one or more  dimensions that match the previous dimension, and if replaced, the current context instance will become equal
             * to the previous context instance.
             */
            final Context<L> context = iterator.next().getContext();
            if (lastLimitDimensionsMask == context.getDimensionsMask()) {
                // Remove the current limit with equal dimensions hash
                iterator.remove();
            }
            lastLimitDimensionsMask = context.getDimensionsMask();
        }
    }
}
