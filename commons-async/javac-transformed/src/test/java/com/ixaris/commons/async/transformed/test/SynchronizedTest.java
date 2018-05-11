/*
 * Copyright (C) 2015 Electronic Arts Inc. All rights reserved.
 * Copyright (C) 2018 Ixaris Systems Ltd. All rights reserved.
 * Redistribution and use in source and binary forms, with or without
 * modification, are permitted provided that the following conditions
 * are met:
 * 1. Redistributions of source code must retain the above copyright
 * notice, this list of conditions and the following disclaimer.
 * 2. Redistributions in binary form must reproduce the above copyright
 * notice, this list of conditions and the following disclaimer in the
 * documentation and/or other materials provided with the distribution.
 * 3. Neither the name of Electronic Arts, Inc. ("EA") nor the names of
 * its contributors may be used to endorse or promote products derived
 * from this software without specific prior written permission.
 * THIS SOFTWARE IS PROVIDED BY ELECTRONIC ARTS AND ITS CONTRIBUTORS "AS IS" AND ANY
 * EXPRESS OR IMPLIED WARRANTIES, INCLUDING, BUT NOT LIMITED TO, THE IMPLIED
 * WARRANTIES OF MERCHANTABILITY AND FITNESS FOR A PARTICULAR PURPOSE ARE
 * DISCLAIMED. IN NO EVENT SHALL ELECTRONIC ARTS OR ITS CONTRIBUTORS BE LIABLE FOR ANY
 * DIRECT, INDIRECT, INCIDENTAL, SPECIAL, EXEMPLARY, OR CONSEQUENTIAL DAMAGES
 * (INCLUDING, BUT NOT LIMITED TO, PROCUREMENT OF SUBSTITUTE GOODS OR SERVICES;
 * LOSS OF USE, DATA, OR PROFITS; OR BUSINESS INTERRUPTION) HOWEVER CAUSED AND
 * ON ANY THEORY OF LIABILITY, WHETHER IN CONTRACT, STRICT LIABILITY, OR TORT
 * (INCLUDING NEGLIGENCE OR OTHERWISE) ARISING IN ANY WAY OUT OF THE USE OF
 * THIS SOFTWARE, EVEN IF ADVISED OF THE POSSIBILITY OF SUCH DAMAGE.
 */

package com.ixaris.commons.async.transformed.test;

import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.CompletionStageUtil.block;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Method;
import java.lang.reflect.Modifier;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Stream;

import org.junit.Test;

import com.ixaris.commons.async.lib.Async;

public class SynchronizedTest extends BaseTest {
    @Test
    public void outOfTheWay() throws InterruptedException {
        class Experiment {
            int x;
            
            Async<Integer> doIt(Object mutex, int a) {
                synchronized (mutex) {
                    x = 1;
                    mutex.notify();
                }
                await(getBlockedFuture());
                return result(x + a);
            }
        }
        final Async<Integer> res = new Experiment().doIt(new Object(), 1);
        completeFutures();
        assertEquals(2, block(res).intValue());
    }
    
    @Test
    public void inThePath() throws InterruptedException {
        class Experiment {
            int x;
            
            Async<Integer> doIt(Object mutex, int a) {
                synchronized (mutex) {
                    mutex.notify();
                    x = 1;
                    await(getBlockedFuture());
                    mutex.notify();
                }
                return result(x + a);
            }
        }
        final Async<Integer> res = new Experiment().doIt(new Object(), 1);
        completeFutures();
        assertEquals(2, block(res).intValue());
    }
    
    @Test
    public void twoMutexes() throws InterruptedException {
        class Experiment {
            int x;
            
            Async<Integer> doIt(Object mutex1, Object mutex2, int a) {
                synchronized (mutex1) {
                    mutex1.notify();
                    synchronized (mutex2) {
                        x = 1;
                        mutex1.notify();
                        mutex2.notify();
                        await(getBlockedFuture());
                        mutex1.notify();
                        mutex2.notify();
                    }
                    mutex1.notify();
                }
                return result(x + a);
            }
        }
        final Async<Integer> res = new Experiment().doIt("a", "b", 1);
        completeFutures();
        assertEquals(2, block(res).intValue());
    }
    
    @Test
    public void usingThis() throws InterruptedException {
        class Experiment {
            int x;
            
            Async<Integer> doIt(int a) {
                synchronized (this) {
                    x = 1;
                    this.notify();
                    await(getBlockedFuture());
                    this.notify();
                }
                return result(x + a);
            }
        }
        final Async<Integer> res = new Experiment().doIt(0);
        completeFutures();
        assertEquals(1, block(res).intValue());
    }
    
    @Test
    public void synchronizedMethod() throws InterruptedException {
        class SynchronizedMethodExperiment {
            int x;
            
            synchronized Async<Integer> doIt(int a) {
                x = 1;
                this.notify();
                await(getBlockedFuture());
                this.notify();
                return result(x);
            }
        }
        final Async<Integer> res = new SynchronizedMethodExperiment().doIt(0);
        Method asyncMethod = Stream.of(SynchronizedMethodExperiment.class.getDeclaredMethods())
            .filter(m -> m.getName().startsWith("continuation$"))
            .findFirst()
            .orElse(null);
        completeFutures();
        assertEquals(1, block(res).intValue());
        
        assertTrue(Modifier.isSynchronized(asyncMethod.getModifiers()));
        assertFalse(Modifier.isStatic(asyncMethod.getModifiers()));
    }
    
    @Test
    public void synchronizedMethodWithExtraSync() throws InterruptedException {
        class SynchronizedMethodWithExtraSyncExperiment {
            int x;
            
            synchronized Async<Integer> doIt(int a) {
                x = 1;
                this.notify();
                await(getBlockedFuture());
                synchronized (this) {
                    await(getBlockedFuture());
                    this.notify();
                }
                this.notify();
                await(getBlockedFuture());
                this.notify();
                return result(x);
            }
        }
        final Async<Integer> res = new SynchronizedMethodWithExtraSyncExperiment().doIt(0);
        completeFutures();
        assertEquals(1, block(res).intValue());
    }
    
    @Test
    public void staticSynchronizedMethod() throws InterruptedException {
        
        final Async<Integer> res = StaticSynchronizedMethod_Experiment.doIt(getBlockedFuture(), 1);
        Method asyncMethod = Stream.of(StaticSynchronizedMethod_Experiment.class.getDeclaredMethods())
            .filter(m -> m.getName().startsWith("continuation$"))
            .findFirst()
            .orElse(null);
        completeFutures();
        assertEquals(1, block(res).intValue());
        // this is not strictly necessary
        assertTrue(Modifier.isSynchronized(asyncMethod.getModifiers()));
    }
    
    static class StaticSynchronizedMethod_Experiment {
        static synchronized Async<Integer> doIt(CompletableFuture<?> blocker, int a) {
            StaticSynchronizedMethod_Experiment.class.notify();
            await(blocker);
            StaticSynchronizedMethod_Experiment.class.notify();
            return result(a);
        }
    }
    
    @Test
    public void mixed() throws InterruptedException {
        class MixedExperiment {
            int x;
            
            synchronized Async<Integer> doIt(String mutex1, String mutex2, int a) {
                String mutex3 = "c";
                x = 1;
                this.notify();
                await(getBlockedFuture());
                this.notify();
                synchronized (mutex1) {
                    await(getBlockedFuture());
                    this.notify();
                    mutex1.notify();
                }
                synchronized (mutex2) {
                    await(getBlockedFuture());
                    this.notify();
                    mutex2.notify();
                }
                synchronized (mutex1) {
                    synchronized (mutex2) {
                        synchronized (mutex3) {
                            await(getBlockedFuture());
                            this.notify();
                            mutex1.notify();
                            mutex2.notify();
                            mutex3.notify();
                        }
                    }
                }
                this.notify();
                await(getBlockedFuture());
                this.notify();
                return result(x);
            }
        }
        final Async<Integer> res = new MixedExperiment().doIt("a", "b", 0);
        completeFutures();
        assertEquals(1, block(res).intValue());
    }
    
}
