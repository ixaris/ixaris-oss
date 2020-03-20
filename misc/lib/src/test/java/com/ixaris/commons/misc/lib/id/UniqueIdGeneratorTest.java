package com.ixaris.commons.misc.lib.id;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

import org.junit.jupiter.api.Test;

public class UniqueIdGeneratorTest {
    
    @Test
    public void testUniqueIdGeneration() {
        
        UniqueIdGenerator.DEFAULT_SEQUENCE.setNodeId(8, 1);
        
        // Generate Id
        long l1 = UniqueIdGenerator.generate();
        long l2 = UniqueIdGenerator.generate();
        long l3 = UniqueIdGenerator.generate();
        long l4 = UniqueIdGenerator.generate();
        long l5 = UniqueIdGenerator.generate();
        
        final Set<Long> set = new HashSet<>();
        set.add(l1);
        set.add(l2);
        set.add(l3);
        set.add(l4);
        set.add(l5);
        assertThat(set.size()).isEqualTo(5);
    }
    
    private static final int SIZE = 100;
    private static final int ITERATIONS = 700;
    
    @Test
    public void testParallel() {
        
        UniqueIdGenerator.DEFAULT_SEQUENCE.setNodeId(12, 1);
        
        try {
            final long start = System.nanoTime();
            Long[] ids = new Long[SIZE * ITERATIONS];
            Worker[] workers = new Worker[SIZE];
            
            for (int i = 0; i < SIZE; i++) {
                workers[i] = new Worker(i, ids);
            }
            
            for (int i = 0; i < SIZE; i++) {
                workers[i].start();
            }
            
            for (int i = 0; i < SIZE; i++) {
                try {
                    workers[i].join();
                } catch (InterruptedException e) {}
            }
            
            final long time = System.nanoTime() - start;
            
            // generating 70000 ids
            // - with id width of 12 we can generate 16 (2^4) ids per millisecond.
            // - since at the start we can advance ~4 seconds (~12 seconds of lag and ~16 seconds or advance), we can
            // burst generate 4096 * 16 ids = 65536
            // - the rest of the ids (4464) can be generated over about 279 milliseconds (4464 / 15)
            // - we sleep max of 64 ms, so max we sleep for (279 / 64) 4.3 times
            // - we give some allowance, so expect ids to be generated in 64 * 10 = 640ms
            assertThat(time).isLessThan(640000000L);
            
            final Set<Long> set = new HashSet<>(Arrays.asList(ids));
            assertThat(set.size()).isEqualTo(SIZE * ITERATIONS);
        } finally {
            UniqueIdGenerator.DEFAULT_SEQUENCE.setNodeId(8, 1);
        }
    }
    
    private static final class Worker extends Thread {
        
        private final int offset;
        private final Long[] ids;
        
        public Worker(final int idx, final Long[] ids) {
            this.offset = idx * ITERATIONS;
            this.ids = ids;
        }
        
        @Override
        public void run() {
            for (int i = 0; i < ITERATIONS; i++) {
                ids[offset + i] = UniqueIdGenerator.generate();
            }
        }
    }
}
