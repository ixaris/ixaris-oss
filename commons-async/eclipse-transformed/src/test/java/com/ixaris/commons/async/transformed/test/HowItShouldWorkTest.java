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

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.junit.Test;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.CompletionStageUtil;
import com.ixaris.commons.async.lib.annotation.AsyncTransformed;

public class HowItShouldWorkTest {
    
    private static class WhatWillBeWritten {
        
        public Async<Object> doSomething(CompletionStage<String> blocker) {
            String res = await(blocker);
            return result(":" + res);
        }
        
        public Async<Object> doSomethingElse(CompletionStage<String> blocker) {
            return Async.from(blocker).map(res -> ":" + res);
        }
        
    }
    
    private static class HowItShouldBeInstrumented {
        
        @AsyncTransformed
        public Async<Object> doSomething(final CompletionStage<String> blocker) {
            try {
                return Async.from(continuation$doSomething(blocker, 0, null));
            } catch (final Throwable t) {
                return Async.rejected(t);
            }
        }
        
        @AsyncTransformed
        public CompletionStage<Object> continuation$doSomething(final CompletionStage<String> blocker, final int async$state, CompletionStage<?> async$async) throws Throwable {
            switch (async$state) {
                case 0:
                    async$async = blocker;
                    if (!CompletionStageUtil.isDone(async$async)) {
                        return CompletionStageUtil.doneCompose(async$async, f -> continuation$doSomething(blocker, 1, f));
                    }
                case 1:
                    String res = (String) CompletionStageUtil.get(async$async);
                    return result(":" + res);
                default:
                    throw new IllegalArgumentException();
            }
        }
        
        @AsyncTransformed
        public Async<Object> doSomethingElse(final CompletionStage<String> blocker) {
            try {
                return Async.from(blocker).map(res -> ":" + res);
            } catch (final Throwable t) {
                return Async.rejected(t);
            }
        }
        
    }
    
    @Test
    public void testMockInstrumentation() throws InterruptedException {
        CompletableFuture<String> blocker = new CompletableFuture<>();
        final CompletionStage<Object> res = new HowItShouldBeInstrumented().doSomething(blocker);
        assertFalse(blocker.isDone());
        assertFalse(CompletionStageUtil.isDone(res));
        blocker.complete("x");
        assertEquals(":x", block(res));
        
        CompletableFuture<String> blocker2 = new CompletableFuture<>();
        final CompletionStage<Object> res2 = new HowItShouldBeInstrumented().doSomethingElse(blocker2);
        assertFalse(blocker2.isDone());
        assertFalse(CompletionStageUtil.isDone(res2));
        blocker2.complete("x");
        assertEquals(":x", block(res2));
    }
    
    @Test
    public void testHowItShouldBehave() throws InterruptedException {
        CompletableFuture<String> blocker = new CompletableFuture<>();
        final CompletionStage<Object> res = new WhatWillBeWritten().doSomething(blocker);
        assertFalse(blocker.isDone());
        assertFalse(CompletionStageUtil.isDone(res));
        blocker.complete("x");
        assertEquals(":x", block(res));
        
        CompletableFuture<String> blocker2 = new CompletableFuture<>();
        final CompletionStage<Object> res2 = new WhatWillBeWritten().doSomethingElse(blocker2);
        assertFalse(blocker2.isDone());
        assertFalse(CompletionStageUtil.isDone(res2));
        blocker2.complete("x");
        assertEquals(":x", block(res2));
    }
}
