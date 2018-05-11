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
import static org.junit.Assert.assertNull;

import java.util.concurrent.CompletionStage;

import org.junit.Test;

import com.ixaris.commons.async.lib.Async;

public class AConstNullTest extends BaseTest {
    
    @Test
    public void nullInitialization() throws InterruptedException {
        final Async<Void> res = NullInit.doIt(getBlockedFuture());
        completeFutures();
        assertNull(block(res));
    }
    
    public static class NullInit {
        
        public static Async<Void> doIt(CompletionStage<Void> stage) {
            String x = null;
            try {
                await(stage);
                return result();
            } catch (Exception ex) {
                return result();
            }
        }
        
    }
    
    @Test
    public void nullInitialization2() throws InterruptedException {
        final Async<Void> res = NullInit2.doIt(getBlockedFuture());
        completeFutures();
        assertNull(block(res));
    }
    
    public static class NullInit2 {
        
        public static Async<Void> doIt(CompletionStage<Void> stage) {
            String x = null;
            String y = x;
            try {
                await(stage);
                return result();
            } catch (Exception ex) {
                return result();
            }
        }
        
    }
    
    @Test
    public void nullInitialization3() throws InterruptedException {
        final Async<Void> res = NullInit3.doIt(getBlockedFuture());
        completeFutures();
        assertNull(block(res));
    }
    
    public static class NullInit3 {
        
        public static Async<Void> doIt(CompletionStage<String> stage) {
            String x = null;
            call0(x, await(stage));
            return result();
        }
        
    }
    
    public static void call0(final Object a, final Object b) {}
    
    @Test
    public void nullInitialization4() throws InterruptedException {
        final Async<Void> res = NullInit4.doIt(getBlockedFuture());
        completeFutures();
        assertNull(block(res));
    }
    
    public static class NullInit4 {
        
        public static Async<Void> doIt(CompletionStage<String> stage) {
            String x = null;
            call0(await(stage), x);
            return result();
        }
        
    }
    
    @Test
    public void nullInTheStack() throws InterruptedException {
        // debugTransform(AConstNullTest.class.getName() + "$NullInTheStack");
        final Async<Void> res = NullInTheStack.doIt(getBlockedFuture());
        completeFutures();
        assertNull(block(res));
    }
    
    public static class NullInTheStack {
        
        public static Async<Void> doIt(CompletionStage<Void> stage) {
            call2(null, await(stage));
            return result();
        }
        
        private static void call2(final Object o, final Object p) {}
        
    }
    
    @Test
    public void nullInTheStack2() throws InterruptedException {
        // debugTransform(AConstNullTest.class.getName() + "$NullInTheStack2");
        final Async<Void> res = NullInTheStack2.doIt(getBlockedFuture());
        completeFutures();
        assertNull(block(res));
    }
    
    public static class NullInTheStack2 {
        
        public static Async<Void> doIt(CompletionStage<Void> stage) {
            call2(null, await(stage));
            return result();
        }
        
        private static void call2(final String o, final Object p) {}
        
    }
}
