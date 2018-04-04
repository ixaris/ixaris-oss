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
import static com.ixaris.commons.async.lib.Async.result;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.junit.Test;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.CompletionStageUtil;

public class HowItShouldWorkTest {
    private static class WhatWillBeWritten {
        
        public Async<Object> doSomething(CompletionStage<String> blocker) {
            String res = await(async(blocker));
            return result(":" + res);
        }
        
        public Async<Object> doSomethingElse(CompletionStage<String> blocker) {
            return async(blocker).map(res -> ":" + res);
        }
        
    }
    
    private static class HowItShouldBehave {
        
        public CompletionStage<Object> doSomething(CompletionStage<String> blocker) {
            return blocker.thenApply(res -> ":" + res);
        }
        
        public CompletionStage<Object> doSomethingElse(CompletionStage<String> blocker) {
            return blocker.thenApply(res -> ":" + res);
        }
        
    }
    
    private static class HowItShouldBeInstrumented {
        
        public Async<Object> doSomething(CompletionStage<String> blocker) {
            throw Async.noTransformation();
        }
        
        public CompletionStage<Object> async$doSomething(CompletionStage<String> blocker) {
            try {
                return continuation$async$doSomething(blocker, 0, null);
            } catch (Throwable t) {
                return CompletionStageUtil.rejected(t);
            }
        }
        
        public CompletionStage<Object> continuation$async$doSomething(CompletionStage<String> blocker, int async$state, CompletionStage<?> async$promise) throws Throwable {
            switch (async$state) {
                case 0:
                    if (!CompletionStageUtil.isDone(blocker)) {
                        return CompletionStageUtil.doneCompose(blocker, f -> continuation$async$doSomething(blocker, 1, f));
                    }
                case 1:
                    String res = CompletionStageUtil.get(blocker);
                    return CompletionStageUtil.fulfilled(":" + res);
                default:
                    throw new IllegalArgumentException();
            }
        }
        
        public Async<Object> doSomethingElse(CompletionStage<String> blocker) {
            throw Async.noTransformation();
        }
        
        public CompletionStage<Object> async$doSomethingElse(CompletionStage<String> blocker) {
            try {
                return CompletionStageUtil.map(blocker, res -> ":" + res);
            } catch (Throwable t) {
                return CompletionStageUtil.rejected(t);
            }
        }
        
    }
    
    @Test
    public void testMockInstrumentation() throws InterruptedException {
        CompletableFuture<String> blocker = new CompletableFuture<>();
        final CompletionStage<Object> res = new HowItShouldBeInstrumented().async$doSomething(blocker);
        assertFalse(blocker.isDone());
        assertFalse(CompletionStageUtil.isDone(res));
        blocker.complete("x");
        assertEquals(":x", CompletionStageUtil.block(res));
        
        CompletableFuture<String> blocker2 = new CompletableFuture<>();
        final CompletionStage<Object> res2 = new HowItShouldBeInstrumented().async$doSomethingElse(blocker2);
        assertFalse(blocker2.isDone());
        assertFalse(CompletionStageUtil.isDone(res2));
        blocker2.complete("x");
        assertEquals(":x", CompletionStageUtil.block(res2));
    }
    
    @Test
    public void testHowItShouldBehave() throws InterruptedException {
        CompletableFuture<String> blocker = new CompletableFuture<>();
        final CompletionStage<Object> res = new HowItShouldBehave().doSomething(blocker);
        assertFalse(blocker.isDone());
        assertFalse(CompletionStageUtil.isDone(res));
        blocker.complete("x");
        assertEquals(":x", CompletionStageUtil.block(res));
        
        CompletableFuture<String> blocker2 = new CompletableFuture<>();
        final CompletionStage<Object> res2 = new HowItShouldBehave().doSomethingElse(blocker2);
        assertFalse(blocker2.isDone());
        assertFalse(CompletionStageUtil.isDone(res2));
        blocker2.complete("x");
        assertEquals(":x", CompletionStageUtil.block(res2));
    }
}
