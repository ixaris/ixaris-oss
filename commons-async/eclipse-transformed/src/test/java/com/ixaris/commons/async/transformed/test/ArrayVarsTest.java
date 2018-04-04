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

import static com.ixaris.commons.async.lib.Async.async;
import static com.ixaris.commons.async.lib.Async.await;
import static com.ixaris.commons.async.lib.Async.block;
import static com.ixaris.commons.async.lib.Async.result;
import static org.junit.Assert.assertEquals;

import org.junit.Test;

import com.ixaris.commons.async.lib.Async;

public class ArrayVarsTest extends BaseTest {
    
    @Test
    public void arrayParams01() throws InterruptedException {
        
        class Experiment {
            
            Async<Object> doIt(Object[] params) {
                await(result());
                return result(params[0]);
            }
            
        }
        
        assertEquals("x", block(new Experiment().doIt(new Object[] { "x" })));
    }
    
    @Test
    public void arrayParams02() throws InterruptedException {
        
        class Experiment {
            
            Async<Object> doIt(Object[][] params) {
                await(result());
                return result(params[0][0]);
            }
            
        }
        
        assertEquals("x", block(new Experiment().doIt(new Object[][] { { "x" } })));
    }
    
    @Test
    public void arrayParams03() throws InterruptedException {
        
        class Experiment {
            
            Async<Object> doIt(Object[][][] params) {
                await(result());
                return result(params[0][0][0]);
            }
            
        }
        
        assertEquals("x", block(new Experiment().doIt(new Object[][][] { { { "x" } } })));
    }
    
    @Test
    public void arrayParams04() throws InterruptedException {
        
        class Experiment {
            
            Async<Object> doIt(Object[][][][] params) {
                await(result());
                return result(params[0][0][0][0]);
            }
            
        }
        
        assertEquals("x", block(new Experiment().doIt(new Object[][][][] { { { { "x" } } } })));
    }
    
    @Test
    public void optionalParam() throws InterruptedException {
        
        class Experiment {
            
            Async<Object> doIt(Object... params) {
                await(result());
                return result(params[0]);
            }
            
        }
        
        assertEquals("x", block(new Experiment().doIt(new Object[] { "x" })));
    }
    
    @Test
    public void stringArrayParams01() throws InterruptedException {
        
        class Experiment {
            
            Async<String> doIt(String[] params) {
                await(result());
                return result(params[0]);
            }
            
        }
        
        assertEquals("x", block(new Experiment().doIt(new String[] { "x" })));
    }
    
    @Test
    public void arrayVar01() throws InterruptedException {
        
        class Experiment {
            
            Async<Object> doIt(String x) {
                Object arr[] = new Object[] { x };
                await(result());
                return result(arr[0]);
            }
            
        }
        
        assertEquals("x", block(new Experiment().doIt("x")));
    }
    
    @Test
    public void stringVar01() throws InterruptedException {
        
        class Experiment {
            
            Async<String> doIt(String x) {
                String arr[] = new String[] { x };
                await(result());
                return result(arr[0]);
            }
            
        }
        
        assertEquals("x", block(new Experiment().doIt("x")));
    }
    
    @Test
    public void primitiveArray01() throws InterruptedException {
        
        class Experiment {
            
            Async<Integer> doIt(int x) {
                int arr[] = new int[] { x };
                await(result());
                return result(arr[0]);
            }
            
        }
        
        assertEquals(10, block(new Experiment().doIt(10)).intValue());
    }
    
    @Test
    public void arraysWithAwait01() throws InterruptedException {
        
        class Experiment {
            
            Async<Object> doIt(Object[] params) {
                await(async(getBlockedFuture()));
                return result(params[0]);
            }
            
        }
        
        final Async<Object> task = new Experiment().doIt(new Object[] { "x" });
        completeFutures();
        assertEquals("x", block(task));
    }
    
    @Test
    public void arraysWithAwait02() throws InterruptedException {
        
        class Experiment {
            
            Async<Object> doIt(Object[] params) {
                Object arr[][] = new Object[][] { params };
                await(async(getBlockedFuture()));
                return result(arr[0][0]);
            }
            
        }
        
        final Async<Object> task = new Experiment().doIt(new Object[] { "x" });
        completeFutures();
        assertEquals("x", block(task));
    }
    
    @Test
    public void arraysWithAwait03() throws InterruptedException {
        
        class Experiment {
            
            Async<Object> doIt(Object[] params) {
                Object arr[][];
                if (params != null) {
                    arr = new Object[][] { params };
                    await(async(getBlockedFuture()));
                } else {
                    arr = new Object[][] { params, null };
                    await(async(getBlockedFuture()));
                }
                return result(arr[0][0]);
            }
            
        }
        
        final Async<Object> task = new Experiment().doIt(new Object[] { "x" });
        completeFutures();
        assertEquals("x", block(task));
    }
    
    @Test
    public void arraysAndIfs() throws InterruptedException {
        
        class Experiment {
            
            Async<Object> doIt(int x) {
                Object[][] arr = new Object[][] { { x } };
                if (x == 11) {
                    arr = null;
                }
                // this forces a stack frame map to be created
                else {
                    await(async(getBlockedFuture()));
                }
                return result(arr[0][0]);
            }
            
        }
        
        final Async<Object> task = new Experiment().doIt(10);
        completeFutures();
        assertEquals(10, block(task));
    }
    
}
