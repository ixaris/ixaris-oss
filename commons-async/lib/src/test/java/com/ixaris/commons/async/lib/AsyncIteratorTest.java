package com.ixaris.commons.async.lib;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.CompletionStageUtil.block;

import java.util.Iterator;
import java.util.NoSuchElementException;

import org.junit.Test;

import com.ixaris.commons.async.lib.AsyncExecutor.YieldingAsyncIterator;
import com.ixaris.commons.async.lib.AsyncIterator.NoMoreElementsException;

public class AsyncIteratorTest {
    
    public void iterate(final Iterator<Integer> i) {
        while (i.hasNext()) {
            use(i.next());
        }
    }
    
    public Async<Void> iterate(final AsyncIterator<Integer> i) {
        while (true) {
            try {
                use(await(i.next()));
            } catch (NoMoreElementsException e) {
                break;
            }
        }
        return result();
    }
    
    private static void use(int item) {}
    
    private class CountingAsyncIterable {
        
        private final int items;
        private final int yieldEvery;
        
        public CountingAsyncIterable(final int items, final int yieldEvery) {
            if (items < 0) {
                throw new IllegalArgumentException();
            }
            if (yieldEvery < 1) {
                throw new IllegalArgumentException();
            }
            this.items = items;
            this.yieldEvery = yieldEvery;
        }
        
        Iterator<Integer> iterator() {
            return new Iterator<Integer>() {
                
                private int current;
                
                @Override
                public boolean hasNext() {
                    return current < items;
                }
                
                @Override
                public Integer next() {
                    if (current >= items) {
                        throw new NoSuchElementException();
                    }
                    return current++;
                }
                
            };
        }
        
        AsyncIterator<Integer> asyncIterator() {
            return new YieldingAsyncIterator<>(new AsyncIterator<Integer>() {
                
                private int current;
                
                @Override
                public Async<Integer> next() throws NoMoreElementsException {
                    if (current >= items) {
                        throw new NoMoreElementsException();
                    }
                    return result(current++);
                }
                
            }, yieldEvery);
        }
        
    }
    
    @Test
    public void asyncIterator_benchmark() throws InterruptedException {
        
        final CountingAsyncIterable i1 = new CountingAsyncIterable(10000000, 250);
        final CountingAsyncIterable i2 = new CountingAsyncIterable(10000000, Integer.MAX_VALUE);
        long start;
        
        // warm up
        iterate(i1.iterator());
        block(iterate(i1.asyncIterator()));
        iterate(i2.iterator());
        block(iterate(i2.asyncIterator()));
        
        // benchmark
        start = System.nanoTime();
        iterate(i1.iterator());
        System.out.println("sync(1) took " + ((System.nanoTime() - start) / 1000000));
        
        start = System.nanoTime();
        block(iterate(i1.asyncIterator()));
        System.out.println("async(1) took " + ((System.nanoTime() - start) / 1000000));
        
        start = System.nanoTime();
        iterate(i2.iterator());
        System.out.println("sync(2) took " + ((System.nanoTime() - start) / 1000000));
        
        start = System.nanoTime();
        block(iterate(i2.asyncIterator()));
        System.out.println("async(2) took " + ((System.nanoTime() - start) / 1000000));
    }
    
}
