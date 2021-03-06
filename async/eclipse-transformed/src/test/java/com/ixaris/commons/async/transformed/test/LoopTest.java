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

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.stream.Collectors;
import java.util.stream.IntStream;

import org.junit.Test;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.CompletionStageUtil;

public class LoopTest extends BaseTest {
    
    @Test
    public void testForLoop() throws InterruptedException {
        int count = 5;
        final Async<Object> task = asyncFrom(() -> {
            final StringBuilder sb = new StringBuilder();
            for (int i = 0; i < count; i++) {
                sb.append(":").append(await(getBlockedFuture(i)));
            }
            return result(sb.toString());
        });
        assertFalse(CompletionStageUtil.isDone(task));
        completeFutures();
        assertTrue(CompletionStageUtil.isDone(task));
        assertEquals(":0:1:2:3:4", block(task));
    }
    
    @Test
    public void testWhileLoop() throws InterruptedException {
        int count = 5;
        final Async<Object> task = asyncFrom(() -> {
            String str = "";
            int i = 0;
            while (i < count) {
                str += ":" + await(getBlockedFuture(i));
                i++;
            }
            return result(str);
        });
        assertFalse(CompletionStageUtil.isDone(task));
        completeFutures();
        assertTrue(CompletionStageUtil.isDone(task));
        assertEquals(":0:1:2:3:4", block(task));
    }
    
    @Test
    public void testDoLoop() throws InterruptedException {
        int count = 5;
        final Async<Object> task = asyncFrom(() -> {
            String str = "";
            int i = 0;
            do {
                str += ":" + await(getBlockedFuture(i));
                i++;
            } while (i < count);
            return result(str);
        });
        assertFalse(CompletionStageUtil.isDone(task));
        completeFutures();
        assertTrue(CompletionStageUtil.isDone(task));
        assertEquals(":0:1:2:3:4", block(task));
    }
    
    @Test
    public void testForEach() throws InterruptedException {
        final List<CompletableFuture<Integer>> blockedFuts = IntStream.range(0, 5).mapToObj(this::getBlockedFuture).collect(Collectors.toList());
        final Async<Object> task = asyncFrom(() -> {
            String str = "";
            for (final CompletableFuture<?> f : blockedFuts) {
                str += ":" + await(f);
            }
            return result(str);
        });
        assertFalse(CompletionStageUtil.isDone(task));
        completeFutures();
        assertTrue(CompletionStageUtil.isDone(task));
        assertEquals(":0:1:2:3:4", block(task));
    }
    
}
