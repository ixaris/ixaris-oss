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
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;

import com.ixaris.commons.async.lib.Async;
import java.time.Duration;
import java.util.concurrent.CompletableFuture;
import org.junit.jupiter.api.Test;

public class BasicTest extends BaseTest {
    
    public static class SomethingAsync {
        
        public Async<Object> doSomething(CompletableFuture<String> blocker) {
            String res = await(blocker);
            return result(":" + res);
        }
        
    }
    
    public static class SomethingWithDataMutation {
        
        public Async<Object> doSomething(CompletableFuture<String> blocker) {
            String op = "1";
            String res = "[" + await(blocker) + "]";
            op = op + "2";
            return result(":" + op + res);
        }
        
    }
    
    public static class SomethingWithLocalsAndStack {
        
        public Async<Object> doSomething(CompletableFuture<String> blocker) {
            int local = 7;
            String res = ":" + Math.max(local, await(blocker).length());
            return result(res);
        }
        
    }
    
    public static class SomethingAsyncWithEx {
        
        public Async<Object> doSomething(CompletableFuture<String> blocker) {
            try {
                String res = await(blocker);
                return result(":" + res);
            } catch (Exception ex) {
                return result(":" + ex.getMessage());
            }
        }
        
    }
    
    @Test
    public void testDirectPathNonBlocking() throws InterruptedException {
        // test an example where the async function blocks (returns incomplete future)
        // this would not work without instrumentation
        final SomethingAsync a = new SomethingAsync();
        
        CompletableFuture<String> blocker = CompletableFuture.completedFuture("x");
        final Async<Object> res = a.doSomething(blocker);
        assertEquals(":x", block(res));
    }
    
    @Test
    public void testBlocking() {
        assertTimeout(Duration.ofSeconds(2), () -> {
            final SomethingAsync a = new SomethingAsync();
            CompletableFuture<String> blocker = new CompletableFuture<>();
            final Async<Object> res = a.doSomething(blocker);
            blocker.complete("x");
            assertEquals(":x", block(res));
        });
    }
    
    @Test
    public void testBlockingWithStackAndLocal() {
        assertTimeout(Duration.ofSeconds(2), () -> {
            final SomethingWithLocalsAndStack a = new SomethingWithLocalsAndStack();
            
            CompletableFuture<String> blocker = new CompletableFuture<>();
            final Async<Object> res = a.doSomething(blocker);
            blocker.complete("0123456789");
            assertEquals(":10", block(res));
        });
    }
    
    @Test
    public void testBlockingAndException() {
        assertTimeout(Duration.ofSeconds(2), () -> {
            final SomethingAsyncWithEx a = new SomethingAsyncWithEx();
            
            CompletableFuture<String> blocker = new CompletableFuture<>();
            final Async<Object> res = a.doSomething(blocker);
            blocker.completeExceptionally(new RuntimeException("Exception"));
            assertEquals(":Exception", block(res));
        });
    }
    
    @Test
    public void testDataFlow() {
        assertTimeout(Duration.ofSeconds(2), () -> {
            final SomethingWithDataMutation a = new SomethingWithDataMutation();
            
            CompletableFuture<String> blocker = new CompletableFuture<>();
            final Async<Object> res = a.doSomething(blocker);
            blocker.complete("x");
            assertEquals(":12[x]", block(res));
        });
    }
}
