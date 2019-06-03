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

public class FrameTest extends BaseTest {
    
    @Test
    public void smallTest() throws InterruptedException {
        // this test fails if using COMPUTE_FRAMES in the transformerError;
        final Async<Integer> res = doIt(getBlockedFuture(1));
        completeFutures();
        assertEquals(1, block(res).intValue());
    }
    
    private Async<Integer> doIt(final CompletionStage<Integer> t) {
        Number n = 1;
        if (n.intValue() == 1) {
            n = 1L;
        } else {
            n = 1.0;
        }
        int i = await(getBlockedFuture(n.intValue()));
        return result(i);
    }
    
    @Test
    public void bigTest() throws InterruptedException {
        // this test fails if using COMPUTE_FRAMES in the transformerError;
        final Async<Integer> res = big(getBlockedFuture(1));
        completeFutures();
        assertEquals(1, block(res).intValue());
    }
    
    private Async<Integer> big(final CompletionStage<Integer> t) {
        Number n = 1;
        Integer i = 1;
        Float f = 1.0f;
        Double d = 1.0;
        Long l = 1L;
        
        switch (n.intValue()) {
            case 1:
                n = i;
                await(t);
                n = f;
                break;
            case 2:
                n = f;
                await(t);
                n = d;
                break;
            case 3:
                n = d;
                await(t);
                n = l;
                break;
            case 4:
                n = l;
                await(t);
                n = i;
                break;
        }
        return awaitExceptions(getBlockedFuture(n.intValue()));
    }
    
    @Test
    public void stackTest() throws InterruptedException {
        // this test fails if using COMPUTE_FRAMES in the transformerError;
        final Async<Integer> res = stack(getBlockedFuture(1));
        completeFutures();
        assertEquals(1, block(res).intValue());
    }
    
    private Async<Integer> stack(final CompletableFuture<Integer> t) {
        Number n = 1;
        if (t.isCancelled()) {
            n = 1.0f;
        }
        // here the type of n is uncertain Float or Integer
        // the original frame will say Number
        
        // this fails if the Transform frame analysis can't get
        // the common superclass for Float and Integer
        // the best solution is to reuse the information from the original frames.
        multiparamFunction(n, await(t));
        return Async.from(t);
    }
    
    void multiparamFunction(Number o1, Object o2) {
        // used just to have something in the stack
    }
    
    @Test
    public void constructorCallTest() throws InterruptedException {
        // this test fails if not handling uninitialized objects
        final Async<Integer> res = constructorCall(getBlockedFuture(1));
        completeFutures();
        assertEquals(1, block(res).intValue());
    }
    
    private Async<Integer> constructorCall(final CompletableFuture<Integer> t) {
        // the inline if inside the constructor forces a frame node creation.
        new SingleParamConstructor(t.isDone() ? t : "");
        await(t);
        return Async.from(t);
    }
    
    private static class SingleParamConstructor {
        SingleParamConstructor(Object o) {
            // just for testing
        }
    }
    
    @Test
    public void uninitializedThisTest() throws InterruptedException {
        // this test fails if not handling uninitialized objects
        final Async<Integer> res = uninitializedThis(getBlockedFuture(1));
        completeFutures();
        assertEquals(1, block(res).intValue());
    }
    
    private Async<Integer> uninitializedThis(final CompletionStage<Integer> t) {
        new SingleParamConstructor(await(t));
        // final SingleParamConstructor a = new SingleParamConstructor(t.isDone() ? t : "");
        // await(t);
        return Async.from(t);
    }
    
    @Test
    public void uninitializedThisMultipleConstructorsTest() throws InterruptedException {
        // this test fails if not handling uninitialized objects
        final Async<Integer> res = uninitializedThisMultipleConstructors(getBlockedFuture(1));
        completeFutures();
        assertEquals(1, block(res).intValue());
    }
    
    private Async<Integer> uninitializedThisMultipleConstructors(final CompletionStage<Integer> t) {
        new SingleParamConstructor(await(t));
        new SingleParamConstructor(await(t));
        new SingleParamConstructor(await(t));
        return Async.from(t);
    }
    
    @Test
    public void uninitializedThisMultipleConstructorsInteractionsTest() throws InterruptedException {
        // this test fails if not handling uninitialized objects
        final Async<Integer> res = uninitializedThisMultipleConstructorsInteractions(getBlockedFuture(1));
        completeFutures();
        assertEquals(1, block(res).intValue());
    }
    
    private Async<Integer> uninitializedThisMultipleConstructorsInteractions(final CompletableFuture<Integer> t) {
        Number n = 1;
        if (t.isCancelled()) {
            n = 1.0f;
        }
        // here the type of n is uncertain Float or Integer
        // the original frame will say Number
        
        // this fails if the Transform frame analysis can't get
        // the common superclass for Float and Integer
        // the best solution is to reuse the information from the original frames.
        
        // the test here is for the uninitialized "this" in the stack
        new SingleParamConstructor(await(t));
        new SingleParamConstructor(await(t));
        new SingleParamConstructor(await(t));
        new SingleParamConstructor(new SingleParamConstructor(await(t)));
        new SingleParamConstructor(new SingleParamConstructor(await(t)));
        new SingleParamConstructor(
            new SingleParamConstructor(new SingleParamConstructor(new SingleParamConstructor(await(t))))
        );
        new MultiParamConstructor(
            1,
            new SingleParamConstructor(
                new SingleParamConstructor(new SingleParamConstructor(new SingleParamConstructor(await(t))))
            )
        );
        return Async.from(t);
    }
    
    private static class MultiParamConstructor {
        MultiParamConstructor(Number o1, Object o2) {}
        
    }
    
    @Test
    public void constructorWithExceptions() throws Exception {
        // this test fails if not handling uninitialized objects
        final Async<Integer> res = constructorWithExceptions(getBlockedFuture(1));
        completeFutures();
        assertEquals(1, block(res).intValue());
    }
    
    private Async<Integer> constructorWithExceptions(final CompletionStage<Integer> t) throws Exception {
        // ensure that replacing a constructor that throws exceptions doesn't cause a verification error
        // the exceptions are not copied to the replacement initializer;
        new ConstructorWithExceptions(1, await(t));
        return Async.from(t);
    }
    
    private static class ConstructorWithExceptions {
        ConstructorWithExceptions(Number o1, Object o2) throws Exception {}
        
    }
    
    @Test
    public void uninitializedInt() throws InterruptedException {
        class UninitializedInt {
            Async<Integer> doIt(int a) {
                int x;
                if (a == 1) {
                    x = 2;
                }
                await(getBlockedFuture());
                x = 3;
                return result(3);
            }
        }
        final Async<Integer> res = new UninitializedInt().doIt(0);
        completeFutures();
        assertEquals(3, block(res).intValue());
    }
    
}
