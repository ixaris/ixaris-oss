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

import static com.ixaris.commons.async.lib.Async.block;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ALOAD;
import static jdk.internal.org.objectweb.asm.Opcodes.ARETURN;
import static jdk.internal.org.objectweb.asm.Opcodes.ASTORE;
import static jdk.internal.org.objectweb.asm.Opcodes.CHECKCAST;
import static jdk.internal.org.objectweb.asm.Opcodes.DSTORE;
import static jdk.internal.org.objectweb.asm.Opcodes.DUP;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKESTATIC;
import static jdk.internal.org.objectweb.asm.Opcodes.LSTORE;
import static jdk.internal.org.objectweb.asm.Opcodes.NEW;
import static jdk.internal.org.objectweb.asm.Opcodes.POP;
import static jdk.internal.org.objectweb.asm.Opcodes.SIPUSH;
import static jdk.internal.org.objectweb.asm.Opcodes.SWAP;
import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;

import org.junit.Test;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.lib.CompletionStageUtil;

import jdk.internal.org.objectweb.asm.MethodVisitor;

// testing scenarios that might have been produced by other bytecode libraries
// sometimes valid bytecode is different from what the java compiler produces.
public class UnorthodoxFrameTest extends BaseTransformerTest {
    
    // sanity check of the creator
    @Test
    @SuppressWarnings("unchecked")
    public void regularConstructorCall() throws Exception {
        final Async<?> task = createClass(AsyncFunction.class, cw -> {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "apply", "(Ljava/lang/Object;)Lcom/ixaris/commons/async/lib/Async;", null, new String[] { "java/lang/Exception" });
            mv.visitCode();
            mv.visitTypeInsn(NEW, "java/lang/Integer");
            mv.visitInsn(DUP);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, "java/util/concurrent/CompletionStage");
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "async", "(Ljava/util/concurrent/CompletionStage;)Lcom/ixaris/commons/async/lib/Async;", true);
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "await", "(Lcom/ixaris/commons/async/lib/Async;)Ljava/lang/Object;", true);
            mv.visitTypeInsn(CHECKCAST, "java/lang/String");
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(Ljava/lang/String;)V", false);
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/CompletionStageUtil", "fulfilled", "(Ljava/lang/Object;)Ljava/util/concurrent/CompletionStage;", false);
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "async", "(Ljava/util/concurrent/CompletionStage;)Lcom/ixaris/commons/async/lib/Async;", true);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(3, 2);
            mv.visitEnd();
        }).apply(getBlockedFuture("100"));
        assertFalse(CompletionStageUtil.isDone(task));
        completeFutures();
        assertEquals(100, block(task));
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void uninitializedStore() throws Exception {
        // check what happens when the uninitialized object is stored in a local variable
        final Async<?> task = createClass(AsyncFunction.class, cw -> {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "apply", "(Ljava/lang/Object;)Lcom/ixaris/commons/async/lib/Async;", null, new String[] { "java/lang/Exception" });
            mv.visitCode();
            mv.visitTypeInsn(NEW, "java/lang/Integer");
            mv.visitInsn(DUP);
            
            // this is valid bytecode: storing the uninitialized object
            mv.visitInsn(DUP);
            mv.visitVarInsn(ASTORE, 2);
            
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, "java/util/concurrent/CompletionStage");
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "async", "(Ljava/util/concurrent/CompletionStage;)Lcom/ixaris/commons/async/lib/Async;", true);
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "await", "(Lcom/ixaris/commons/async/lib/Async;)Ljava/lang/Object;", true);
            mv.visitTypeInsn(CHECKCAST, "java/lang/String");
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(Ljava/lang/String;)V", false);
            
            // discarding the object and getting the one that was stored (without instrumentation they are the same)
            mv.visitInsn(POP);
            mv.visitVarInsn(ALOAD, 2);
            
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/CompletionStageUtil", "fulfilled", "(Ljava/lang/Object;)Ljava/util/concurrent/CompletionStage;", false);
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "async", "(Ljava/util/concurrent/CompletionStage;)Lcom/ixaris/commons/async/lib/Async;", true);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(4, 3);
            mv.visitEnd();
        }).apply(getBlockedFuture("101"));
        assertFalse(CompletionStageUtil.isDone(task));
        completeFutures();
        assertEquals(101, block(task));
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void uninitializedStoreWithWideVarsAndGaps() throws Exception {
        // check what happens when the uninitialized object is stored in a local variable
        // and with gaps and wide var (long and double)
        final Async<?> task = createClass(AsyncFunction.class, cw -> {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "apply", "(Ljava/lang/Object;)Lcom/ixaris/commons/async/lib/Async;", null, new String[] { "java/lang/Exception" });
            mv.visitCode();
            mv.visitLdcInsn(2.0);
            mv.visitVarInsn(DSTORE, 9);
            
            mv.visitTypeInsn(NEW, "java/lang/Integer");
            mv.visitInsn(DUP);
            
            mv.visitLdcInsn(1L);
            mv.visitVarInsn(LSTORE, 2);
            
            // this is valid bytecode: storing the uninitialized object
            mv.visitInsn(DUP);
            mv.visitVarInsn(ASTORE, 6);
            
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, "java/util/concurrent/CompletionStage");
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "async", "(Ljava/util/concurrent/CompletionStage;)Lcom/ixaris/commons/async/lib/Async;", true);
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "await", "(Lcom/ixaris/commons/async/lib/Async;)Ljava/lang/Object;", true);
            mv.visitTypeInsn(CHECKCAST, "java/lang/String");
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(Ljava/lang/String;)V", false);
            
            // discarding the object and getting the one that was stored (without instrumentation they are the same)
            mv.visitInsn(POP);
            mv.visitVarInsn(ALOAD, 6);
            
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/CompletionStageUtil", "fulfilled", "(Ljava/lang/Object;)Ljava/util/concurrent/CompletionStage;", false);
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "async", "(Ljava/util/concurrent/CompletionStage;)Lcom/ixaris/commons/async/lib/Async;", true);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(5, 15);
            mv.visitEnd();
        }).apply(getBlockedFuture("101"));
        assertFalse(CompletionStageUtil.isDone(task));
        completeFutures();
        assertEquals(101, block(task));
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void uninitializedInTheStackSingle() throws Exception {
        // check what happens if there only one copy of the uninitialized object in the stack.
        // the java compiler usually leaves two copies
        final Async<?> task = createClass(AsyncFunction.class, cw -> {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "apply", "(Ljava/lang/Object;)Lcom/ixaris/commons/async/lib/Async;", null, new String[] { "java/lang/Exception" });
            mv.visitCode();
            mv.visitTypeInsn(NEW, "java/lang/Integer");
            // note a missing DUP here
            // the java compiler usually puts a dup here.
            
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, "java/util/concurrent/CompletionStage");
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "async", "(Ljava/util/concurrent/CompletionStage;)Lcom/ixaris/commons/async/lib/Async;", true);
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "await", "(Lcom/ixaris/commons/async/lib/Async;)Ljava/lang/Object;", true);
            mv.visitTypeInsn(CHECKCAST, "java/lang/String");
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(Ljava/lang/String;)V", false);
            // no pop need as the constructor consumes the one copy
            
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/CompletionStageUtil", "fulfilled", "()Ljava/util/concurrent/CompletionStage;", false);
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "async", "(Ljava/util/concurrent/CompletionStage;)Lcom/ixaris/commons/async/lib/Async;", true);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(4, 3);
            mv.visitEnd();
        }).apply(getBlockedFuture("101"));
        assertFalse(CompletionStageUtil.isDone(task));
        completeFutures();
        // return should be null as we are using task.done()
        assertEquals(null, block(task));
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void uninitializedInTheStackOneExtraCopy() throws Exception {
        // check what happens when the uninitialized object appears 3 consecutive times in the stack
        // the java compiler usually puts two copies.
        final Async<?> task = createClass(AsyncFunction.class, cw -> {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "apply", "(Ljava/lang/Object;)Lcom/ixaris/commons/async/lib/Async;", null, new String[] { "java/lang/Exception" });
            mv.visitCode();
            mv.visitTypeInsn(NEW, "java/lang/Integer");
            mv.visitInsn(DUP);
            
            // this is valid bytecode: creating a 3rd copy of the uninitialized object
            mv.visitInsn(DUP);
            
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, "java/util/concurrent/CompletionStage");
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "async", "(Ljava/util/concurrent/CompletionStage;)Lcom/ixaris/commons/async/lib/Async;", true);
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "await", "(Lcom/ixaris/commons/async/lib/Async;)Ljava/lang/Object;", true);
            mv.visitTypeInsn(CHECKCAST, "java/lang/String");
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(Ljava/lang/String;)V", false);
            
            // discarding the object and getting the one that is in the stack (without instrumentation they are the same)
            mv.visitInsn(POP);
            
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/CompletionStageUtil", "fulfilled", "(Ljava/lang/Object;)Ljava/util/concurrent/CompletionStage;", false);
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "async", "(Ljava/util/concurrent/CompletionStage;)Lcom/ixaris/commons/async/lib/Async;", true);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(4, 3);
            mv.visitEnd();
        }).apply(getBlockedFuture("101"));
        assertFalse(CompletionStageUtil.isDone(task));
        completeFutures();
        assertEquals(101, block(task));
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void uninitializedInTheStackMultipleExtraCopies() throws Exception {
        // check what happens when the uninitialized object appears several times in the stack
        // the java compiler usually puts two copies.
        for (int extraCopies = 0; extraCopies < 10; extraCopies++) {
            int extra = extraCopies;
            // check what happens when the uninitialized object is stored in a local variable
            // without proper stack management this will fail in the jvm verifier
            final Async<?> task = createClass(AsyncFunction.class, cw -> {
                MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "apply", "(Ljava/lang/Object;)Lcom/ixaris/commons/async/lib/Async;", null, new String[] { "java/lang/Exception" });
                mv.visitCode();
                mv.visitTypeInsn(NEW, "java/lang/Integer");
                mv.visitInsn(DUP);
                
                // this is valid bytecode: creating a 3rd copy of the uninitialized object
                for (int i = 0; i < extra; i++) {
                    mv.visitInsn(DUP);
                }
                
                mv.visitVarInsn(ALOAD, 1);
                mv.visitTypeInsn(CHECKCAST, "java/util/concurrent/CompletionStage");
                mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "async", "(Ljava/util/concurrent/CompletionStage;)Lcom/ixaris/commons/async/lib/Async;", true);
                mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "await", "(Lcom/ixaris/commons/async/lib/Async;)Ljava/lang/Object;", true);
                mv.visitTypeInsn(CHECKCAST, "java/lang/String");
                mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(Ljava/lang/String;)V", false);
                
                // discarding the object and getting the one that is in the stack (without instrumentation they are the same)
                for (int i = 0; i < extra; i++) {
                    mv.visitInsn(POP);
                }
                
                mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/CompletionStageUtil", "fulfilled", "(Ljava/lang/Object;)Ljava/util/concurrent/CompletionStage;", false);
                mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "async", "(Ljava/util/concurrent/CompletionStage;)Lcom/ixaris/commons/async/lib/Async;", true);
                mv.visitInsn(ARETURN);
                mv.visitMaxs(4 + extra, 3);
                mv.visitEnd();
            }).apply(getBlockedFuture("101"));
            assertFalse(CompletionStageUtil.isDone(task));
            completeFutures();
            assertEquals(101, block(task));
        }
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void uninitializedInTheStackWithInterleavedCopies() throws Exception {
        // check that the constructor replacement is able to replace interleaved elements in the stack
        // obs.: the java compiler doesn't usually produce code like this
        // regular java compiler will do:
        // -- { new dup dup ... <init> }
        // this tests what happens if:
        // -- { new dup push_1 swap ... <init> pop }
        final Async<?> task = createClass(AsyncFunction.class, cw -> {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "apply", "(Ljava/lang/Object;)Lcom/ixaris/commons/async/lib/Async;", null, new String[] { "java/lang/Exception" });
            mv.visitCode();
            mv.visitTypeInsn(NEW, "java/lang/Integer");
            
            mv.visitInsn(DUP);
            // interleaving copies in the stack
            mv.visitIntInsn(SIPUSH, 1);
            mv.visitInsn(SWAP);
            // stack: { uobj, int, uobj }
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, "java/util/concurrent/CompletionStage");
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "async", "(Ljava/util/concurrent/CompletionStage;)Lcom/ixaris/commons/async/lib/Async;", true);
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "await", "(Lcom/ixaris/commons/async/lib/Async;)Ljava/lang/Object;", true);
            mv.visitTypeInsn(CHECKCAST, "java/lang/String");
            mv.visitMethodInsn(INVOKESPECIAL, "java/lang/Integer", "<init>", "(Ljava/lang/String;)V", false);
            // stack: { uobj, int }
            mv.visitInsn(POP);
            // stack: { uobj }
            
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/CompletionStageUtil", "fulfilled", "(Ljava/lang/Object;)Ljava/util/concurrent/CompletionStage;", false);
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "async", "(Ljava/util/concurrent/CompletionStage;)Lcom/ixaris/commons/async/lib/Async;", true);
            mv.visitInsn(ARETURN);
            mv.visitMaxs(16, 3);
            mv.visitEnd();
        }).apply(getBlockedFuture("101"));
        assertFalse(CompletionStageUtil.isDone(task));
        completeFutures();
        assertEquals(101, block(task));
    }
}
