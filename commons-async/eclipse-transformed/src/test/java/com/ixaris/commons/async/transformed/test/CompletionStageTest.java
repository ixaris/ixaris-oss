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
import static com.ixaris.commons.async.lib.Async.awaitExceptions;
import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.CompletionStageUtil.block;
import static org.junit.jupiter.api.Assertions.assertEquals;

import com.ixaris.commons.async.lib.Async;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

public class CompletionStageTest extends BaseTest {
    
    @Test
    public void outOfTheWay() throws InterruptedException {
        
        class Experiment {
            
            Async<Integer> doIt(int a, int b) {
                await(getBlockedFuture());
                return result(a + b);
            }
        }
        
        Async<Integer> res = new Experiment().doIt(1, 2);
        
        completeFutures();
        assertEquals(3, block(res).intValue());
    }
    
    @Test
    public void completionStageParam() throws InterruptedException {
        
        class Experiment {
            
            Async<String> doIt(CompletionStage<String> stage) {
                return awaitExceptions(Async.from(stage));
            }
            
        }
        
        final CompletableFuture<String> future = new CompletableFuture<>();
        Async<String> res = new Experiment().doIt(future);
        future.complete("test");
        assertEquals("test", block(res));
    }
    
    @Test
    public void completionStageArgument() throws InterruptedException {
        
        class Experiment {
            
            Async<Integer> doIt(CompletionStage<Integer> stage, int b) {
                int a = await(stage);
                return result(a + b);
            }
        }
        
        CompletionStage<Integer> task = getBlockedFuture(1);
        CompletionStage<Integer> res = new Experiment().doIt(task, 2);
        
        completeFutures();
        assertEquals(3, block(res).intValue());
    }
}
