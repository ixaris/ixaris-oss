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

import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Supplier;

import org.junit.Test;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.CompletionStageUtil;

public class LocalVarsTest extends BaseTest {
    
    @Test
    public void testRepeatedLocalVarsNames() throws InterruptedException {
        final CompletionStage<Integer> res = CompletionStageUtil.fulfilled(null)
            .thenCompose((Function<Object, Async<Integer>>) r -> {
                {
                    String a = "a1";
                }
                await(async(getBlockedFuture()));
                {
                    String a = "a2";
                }
                return result(10);
            });
        completeFutures();
        assertEquals((Integer) 10, CompletionStageUtil.block(res));
    }
    
    @Test
    public void testRepeatedLocalVarsNames2() throws InterruptedException {
        final CompletionStage<Integer> res = CompletionStageUtil.fulfilled(null)
            .thenCompose((Function<Object, Async<Integer>>) r -> {
                {
                    String a = "a1";
                    String b = "b1";
                    int c = 1;
                }
                await(async(getBlockedFuture()));
                {
                    String a = "a2";
                    String b = "b2";
                    int c = 10;
                    return result(c);
                }
            });
        completeFutures();
        assertEquals((Integer) 10, CompletionStageUtil.block(res));
    }
    

}
