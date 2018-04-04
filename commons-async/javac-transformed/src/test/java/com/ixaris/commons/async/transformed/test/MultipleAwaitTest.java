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

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;

import org.junit.Test;

import com.ixaris.commons.async.lib.Async;

public class MultipleAwaitTest extends BaseTest {
    public static class TaskSomethingAsync {
        private List<CompletableFuture> blockers = new ArrayList<>();
        
        public Async<String> doSomething() {
            String res1 = await(blocker());
            String res2 = await(blocker());
            return result(res1 + ":" + res2);
        }
        
        public Async<String> doSomething(CompletionStage<String> blocker1, CompletionStage<String> blocker2) {
            String res1 = await(async(blocker1));
            String res2 = await(async(blocker2));
            return result(res1 + ":" + res2);
        }
        
        private Async<String> blocker() {
            final CompletableFuture<String> blocker = new CompletableFuture<>();
            blockers.add(blocker);
            return async(blocker);
        }
        
        public void completeBlockers() {
            for (int i = 0; i < blockers.size(); i++) {
                final CompletableFuture<String> fut = blockers.get(i);
                fut.complete("str " + i);
            }
        }
    }
    
    @Test
    public void testMultipleAwaitsAndMethods() throws InterruptedException {
        final TaskSomethingAsync a = new TaskSomethingAsync();
        
        Async<String> res = a.doSomething();
        a.completeBlockers();
        assertEquals("str 0:str 1", block(res));
    }
    
    @Test
    public void testMultipleAwaitsFromParams() throws InterruptedException {
        final TaskSomethingAsync a = new TaskSomethingAsync();
        CompletableFuture<String> blocker1 = new CompletableFuture<>();
        CompletableFuture<String> blocker2 = new CompletableFuture<>();
        Async<String> res = a.doSomething(blocker1, blocker2);
        blocker1.complete("a");
        blocker2.complete("b");
        assertEquals("a:b", block(res));
    }
}
