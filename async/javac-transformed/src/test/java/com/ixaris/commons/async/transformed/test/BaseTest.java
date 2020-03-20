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

import static com.ixaris.commons.misc.lib.object.Tuple.tuple;

import java.util.LinkedList;
import java.util.Queue;
import java.util.function.Supplier;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.FutureAsync;
import com.ixaris.commons.misc.lib.object.Tuple2;

public class BaseTest {
    
    // pairs of completable futures and the future completions.
    private Queue<Tuple2<FutureAsync<?>, Object>> blockedFutures = new LinkedList<>();
    
    // just calls a function
    <T> Async<T> asyncFrom(final Supplier<Async<T>> supplier) {
        return supplier.get();
    }
    
    /**
     * Creates and an uncompleted future and adds it the the queue for later completion. To help with the tests
     */
    public <T> FutureAsync<T> getBlockedFuture(final T value) {
        final FutureAsync<T> future = new FutureAsync<>();
        blockedFutures.add(tuple(future, value));
        return future;
    }
    
    public <T> FutureAsync<T> getBlockedFuture() {
        return getBlockedFuture(null);
    }
    
    /**
     * Complete all the blocked futures, even new ones created while executing this method
     */
    public void completeFutures() {
        while (blockedFutures.size() > 0) {
            final Tuple2<FutureAsync<?>, Object> pair = blockedFutures.poll();
            if (pair != null) {
                complete(pair.get1(), pair.get2());
            }
        }
    }
    
    @SuppressWarnings("unchecked")
    private <T> void complete(final FutureAsync<T> future, Object o) {
        future.complete((T) o);
    }
    
}
