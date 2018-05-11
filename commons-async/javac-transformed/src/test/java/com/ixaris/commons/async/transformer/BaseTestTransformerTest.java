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

package com.ixaris.commons.async.transformer;

import static com.ixaris.commons.async.lib.CompletionStageUtil.block;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ALOAD;
import static jdk.internal.org.objectweb.asm.Opcodes.ARETURN;
import static jdk.internal.org.objectweb.asm.Opcodes.CHECKCAST;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKESTATIC;
import static jdk.internal.org.objectweb.asm.Opcodes.POP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Callable;

import org.junit.Test;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.CompletionStageUtil;

import jdk.internal.org.objectweb.asm.MethodVisitor;

public class BaseTestTransformerTest extends BaseTransformerTest {
    
    // sanity check of the creator
    @Test
    public void testCreateClass() throws Exception {
        assertTrue(createClass(ArrayList.class, cv -> {}) instanceof List);
        assertEquals("hello", createClass(Callable.class, cv -> {
            final MethodVisitor mv = cv.visitMethod(ACC_PUBLIC, "call", "()Ljava/lang/Object;", null, new String[] { "java/lang/Exception" });
            mv.visitCode();
            mv.visitLdcInsn("hello");
            mv.visitInsn(ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }).call());
    }
    
    // sanity check of the creator
    @Test
    public void simpleAsyncMethod() throws Exception {
        final Async<?> task = createClass(AsyncCallable.class, cw -> {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "call", "()Lcom/ixaris/commons/async/lib/Async;", null, new String[] { "java/lang/Exception" });
            mv.visitCode();
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "result", "()Lcom/ixaris/commons/async/lib/Async;", true);
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "await", "(Ljava/util/concurrent/CompletionStage;)Ljava/lang/Object;", true);
            mv.visitInsn(POP);
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "result", "()Lcom/ixaris/commons/async/lib/Async;", true);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }).call();
        assertTrue(CompletionStageUtil.isFulfilled(task));
    }
    
    // sanity check of the creator
    @Test
    @SuppressWarnings("unchecked")
    public void simpleBlockingAsyncMethod() throws Exception {
        final Async<?> task = createClass(AsyncFunction.class, cw -> {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "apply", "(Ljava/lang/Object;)Lcom/ixaris/commons/async/lib/Async;", null, new String[] { "java/lang/Exception" });
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, "java/util/concurrent/CompletionStage");
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "await", "(Ljava/util/concurrent/CompletionStage;)Ljava/lang/Object;", true);
            mv.visitInsn(POP);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, "java/util/concurrent/CompletionStage");
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "from", "(Ljava/util/concurrent/CompletionStage;)Lcom/ixaris/commons/async/lib/Async;", true);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(1, 2);
            mv.visitEnd();
        }).apply(getBlockedFuture("hello"));
        assertFalse(CompletionStageUtil.isDone(task));
        completeFutures();
        assertEquals("hello", block(task));
    }
    
}
