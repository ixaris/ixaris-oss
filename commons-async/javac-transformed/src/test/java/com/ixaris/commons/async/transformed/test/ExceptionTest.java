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
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.CompletionStageUtil;
import java.util.concurrent.CompletionStage;
import org.junit.jupiter.api.Test;

public class ExceptionTest extends BaseTest {
    
    @Test
    public void testTryCatch() throws InterruptedException {
        final Async<Integer> res = doTryCatch();
        completeFutures();
        assertEquals((Integer) 10, block(res));
    }
    
    private Async<Integer> doTryCatch() {
        try {
            if (await(getBlockedFuture(10)) == 10) {
                throw new IllegalArgumentException(String.valueOf(10));
            }
            
        } catch (IllegalArgumentException ex) {
            return result(10);
        }
        return null;
    }
    
    @Test
    public void testFinally() throws InterruptedException {
        final Async<Integer> res = doTestFinally();
        completeFutures();
        assertEquals(1337, block(res).intValue());
    }
    
    private Async<Integer> doTestFinally() {
        try {
            if (await(getBlockedFuture(10)) == 10) {
                throw new IllegalArgumentException(String.valueOf(10));
            }
            
        } finally {
            return result(1337);
        }
    }
    
    @Test
    public void testTryCatch2() {
        final Async<Integer> res = doTryCatch2();
        completeFutures();
        assertTrue(CompletionStageUtil.isDone(res));
    }
    
    private Async<Integer> doTryCatch2() {
        int c = 1;
        try {
            await(getBlockedFuture());
        } catch (Exception ex) {
            // once this was causing a verification error, now fixed
            c = c + 1;
        }
        return result(c);
    }
    
    @Test
    public void testTryCatch3() throws InterruptedException {
        final Async<String> res = doTryCatch3();
        completeFutures();
        assertEquals("fail", block(res));
    }
    
    private Async<String> doTryCatch3() {
        int c = 1;
        try {
            await(getBlockedFuture());
            await(getBlockedFuture());
            await(getBlockedFuture());
            await(getBlockedFuture());
            await(Async.rejected(new Exception("fail")));
        } catch (Exception ex) {
            await(getBlockedFuture());
            await(getBlockedFuture());
            await(getBlockedFuture());
            await(getBlockedFuture());
            return result(ex.getMessage());
        }
        return result("done");
    }
    
    @Test
    public void testTryCatch4() throws InterruptedException {
        final Async<String> res = doTryCatch4();
        completeFutures();
        assertEquals("fail", block(res));
    }
    
    private Async<String> doTryCatch4() {
        int c = 1;
        try {
            await(getBlockedFuture());
            await(getBlockedFuture());
            if (await(getBlockedFuture()) == null) {
                throw new Exception("fail");
            }
            await(getBlockedFuture());
        } catch (Exception ex) {
            await(getBlockedFuture());
            await(getBlockedFuture());
            await(getBlockedFuture());
            await(getBlockedFuture());
            return result(ex.getMessage());
        }
        return result("done");
    }
    
    private Object stackAnalysisProblem(final Exception ex) {
        Throwable t = ex;
        
        while (t.getCause() != null && !(t instanceof IllegalAccessException)) {
            t = t.getCause();
        }
        
        if (t instanceof IllegalAccessException) {
            IllegalAccessException wex = (IllegalAccessException) t;
        }
        
        return null;
    }
    
    @Test
    public void smallTest() throws InterruptedException {
        final Async<?> res = doIt(CompletionStageUtil.fulfilled(null));
        completeFutures();
        assertNull(block(res));
    }
    
    public Async<?> doIt(CompletionStage<?> t) {
        try {
            await(t);
        } catch (Exception ex) {
            await(t);
        }
        return Async.from(t);
    }
}
