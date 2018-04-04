/*
 * Copyright (C) 2015 Electronic Arts Inc. All rights reserved.
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
import static com.ixaris.commons.async.lib.Async.result;
import static org.junit.Assert.assertEquals;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.CHECKCAST;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.LCONST_0;
import static org.objectweb.asm.Opcodes.LSTORE;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.SIPUSH;

import org.junit.Test;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.tree.ClassNode;

import com.ixaris.commons.async.lib.Async;

public class TransformerTest extends BaseTransformerTest {
    
    // sanity check of the creator
    @Test
    @SuppressWarnings("unchecked")
    public void simpleAsyncMethod() throws Exception {
        final ClassNode cn = createClassNode(AsyncFunction.class, cw -> {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "apply", "(Ljava/lang/Object;)Lcom/ixaris/commons/async/lib/Async;", null, new String[] { "java/lang/Exception" });
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, "java/util/concurrent/CompletionStage");
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "async", "(Ljava/util/concurrent/CompletionStage;)Lcom/ixaris/commons/async/lib/Async;", false);
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "await", "(Lcom/ixaris/commons/async/lib/Async;)Ljava/lang/Object;", false);
            mv.visitInsn(POP);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, "java/util/concurrent/CompletionStage");
            mv.visitInsn(ARETURN);
            mv.visitMaxs(1, 2);
            mv.visitEnd();
        });
        ClassWriter cw = new ClassWriter(0);
        cn.accept(cw);
        final byte[] bytes = TRANSFORMER.transform(new ClassReader(cw.toByteArray()));
        //        DevDebug.debugSaveTrace(cn.name, bytes);
        assertEquals("hello", block(createClass(AsyncFunction.class, bytes).apply(result("hello"))));
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void withLocals() throws Exception {
        final ClassNode cn = createClassNode(AsyncFunction.class, cw -> {
            MethodVisitor mv = cw.visitMethod(ACC_PUBLIC, "apply", "(Ljava/lang/Object;)Lcom/ixaris/commons/async/lib/Async;", null, new String[] { "java/lang/Exception" });
            mv.visitCode();
            mv.visitIntInsn(SIPUSH, 1);
            mv.visitVarInsn(ISTORE, 2);
            
            mv.visitInsn(LCONST_0);
            mv.visitVarInsn(LSTORE, 3);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, "java/util/concurrent/CompletionStage");
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "async", "(Ljava/util/concurrent/CompletionStage;)Lcom/ixaris/commons/async/lib/Async;", false);
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "await", "(Lcom/ixaris/commons/async/lib/Async;)Ljava/lang/Object;", false);
            mv.visitInsn(POP);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, "java/util/concurrent/CompletionStage");
            mv.visitInsn(ARETURN);
            mv.visitMaxs(2, 5);
            mv.visitEnd();
        });
        ClassWriter cw = new ClassWriter(0);
        cn.accept(cw);
        final byte[] bytes = TRANSFORMER.transform(new ClassReader(cw.toByteArray()));
        // DevDebug.debugSaveTrace(cn.name, bytes);
        assertEquals("hello", block(createClass(AsyncFunction.class, bytes).apply(result("hello"))));
    }
    
    @Test(timeout = 10_000L)
    @SuppressWarnings("unchecked")
    public void withTwoFutures() throws Exception {
        final ClassNode cn = createClassNode(AsyncBiFunction.class, cw -> {
            MethodVisitor mv =
                cw.visitMethod(ACC_PUBLIC, "apply", "(Ljava/lang/Object;Ljava/lang/Object;)Lcom/ixaris/commons/async/lib/Async;", null, new String[] { "java/lang/Exception" });
            mv.visitCode();
            
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, "java/util/concurrent/CompletionStage");
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "async", "(Ljava/util/concurrent/CompletionStage;)Lcom/ixaris/commons/async/lib/Async;", false);
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "await", "(Lcom/ixaris/commons/async/lib/Async;)Ljava/lang/Object;", false);
            mv.visitInsn(POP);
            
            mv.visitVarInsn(ALOAD, 2);
            mv.visitTypeInsn(CHECKCAST, "java/util/concurrent/CompletionStage");
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "async", "(Ljava/util/concurrent/CompletionStage;)Lcom/ixaris/commons/async/lib/Async;", false);
            mv.visitMethodInsn(INVOKESTATIC, "com/ixaris/commons/async/lib/Async", "await", "(Lcom/ixaris/commons/async/lib/Async;)Ljava/lang/Object;", false);
            mv.visitInsn(POP);
            
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, "java/util/concurrent/CompletionStage");
            mv.visitInsn(ARETURN);
            mv.visitMaxs(2, 5);
            mv.visitEnd();
        });
        ClassWriter cw = new ClassWriter(0);
        cn.accept(cw);
        final byte[] bytes = TRANSFORMER.transform(new ClassReader(cw.toByteArray()));
        //        DevDebug.debugSaveTrace(cn.name, bytes);
        
        assertEquals("hello", block(createClass(AsyncBiFunction.class, bytes).apply(result("hello"), result("world"))));
        
        final Async rest = createClass(AsyncBiFunction.class, bytes).apply(getBlockedFuture("hello"), getBlockedFuture("world"));
        completeFutures();
        assertEquals("hello", block(rest));
    }
    
}
