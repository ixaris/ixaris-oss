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

import static com.ixaris.commons.async.lib.Async.result;
import static com.ixaris.commons.async.lib.CompletionStageUtil.block;
import static com.ixaris.commons.misc.lib.object.Tuple.tuple;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ALOAD;
import static jdk.internal.org.objectweb.asm.Opcodes.ARETURN;
import static jdk.internal.org.objectweb.asm.Opcodes.CHECKCAST;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKESTATIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ISTORE;
import static jdk.internal.org.objectweb.asm.Opcodes.LCONST_0;
import static jdk.internal.org.objectweb.asm.Opcodes.LSTORE;
import static jdk.internal.org.objectweb.asm.Opcodes.SIPUSH;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTimeout;

import com.ixaris.commons.async.lib.Async;
import java.time.Duration;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.tree.ClassNode;
import org.junit.jupiter.api.Test;

public class TransformerTest extends BaseTransformerTest {
    
    // sanity check of the creator
    @Test
    @SuppressWarnings("unchecked")
    public void simpleAsyncMethod() throws Exception {
        final ClassNode cn = createClassNode(AsyncFunction.class, cw -> {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "apply",
                "(Ljava/lang/Object;)Lcom/ixaris/commons/async/lib/Async;",
                null,
                new String[] { "java/lang/Exception" }
            );
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, "com/ixaris/commons/async/lib/Async");
            mv.visitMethodInsn(
                INVOKESTATIC,
                "com/ixaris/commons/async/lib/Async",
                "await",
                "(Ljava/util/concurrent/CompletionStage;)Ljava/lang/Object;",
                true
            );
            mv.visitMethodInsn(
                INVOKESTATIC,
                "com/ixaris/commons/async/lib/Async",
                "result",
                "(Ljava/lang/Object;)Lcom/ixaris/commons/async/lib/Async;",
                true
            );
            mv.visitInsn(ARETURN);
            mv.visitMaxs(1, 2);
            mv.visitEnd();
        });
        ClassWriter cw = new ClassWriter(0);
        cn.accept(cw);
        //        DevDebug.debugSaveTrace(cn.name, bytes);
        final AsyncFunction fn = createClass(AsyncFunction.class, cw.toByteArray());
        assertEquals("hello", block(fn.apply(result("hello"))));
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void withLocals() throws Exception {
        final ClassNode cn = createClassNode(AsyncFunction.class, cw -> {
            MethodVisitor mv = cw.visitMethod(
                ACC_PUBLIC,
                "apply",
                "(Ljava/lang/Object;)Lcom/ixaris/commons/async/lib/Async;",
                null,
                new String[] { "java/lang/Exception" }
            );
            mv.visitCode();
            mv.visitIntInsn(SIPUSH, 1);
            mv.visitVarInsn(ISTORE, 2);
            
            mv.visitInsn(LCONST_0);
            mv.visitVarInsn(LSTORE, 3);
            mv.visitVarInsn(ALOAD, 1);
            mv.visitTypeInsn(CHECKCAST, "com/ixaris/commons/async/lib/Async");
            mv.visitMethodInsn(
                INVOKESTATIC,
                "com/ixaris/commons/async/lib/Async",
                "await",
                "(Ljava/util/concurrent/CompletionStage;)Ljava/lang/Object;",
                true
            );
            mv.visitMethodInsn(
                INVOKESTATIC,
                "com/ixaris/commons/async/lib/Async",
                "result",
                "(Ljava/lang/Object;)Lcom/ixaris/commons/async/lib/Async;",
                true
            );
            mv.visitInsn(ARETURN);
            mv.visitMaxs(2, 5);
            mv.visitEnd();
        });
        ClassWriter cw = new ClassWriter(0);
        cn.accept(cw);
        // DevDebug.debugSaveTrace(cn.name, bytes);
        final AsyncFunction fn = createClass(AsyncFunction.class, cw.toByteArray());
        assertEquals("hello", block(fn.apply(result("hello"))));
    }
    
    @Test
    @SuppressWarnings("unchecked")
    public void withTwoFutures() {
        assertTimeout(Duration.ofSeconds(10L), () -> {
            final ClassNode cn = createClassNode(AsyncBiFunction.class, cw -> {
                MethodVisitor mv = cw.visitMethod(
                    ACC_PUBLIC,
                    "apply",
                    "(Ljava/lang/Object;Ljava/lang/Object;)Lcom/ixaris/commons/async/lib/Async;",
                    null,
                    new String[] { "java/lang/Exception" }
                );
                mv.visitCode();
                
                mv.visitVarInsn(ALOAD, 1);
                mv.visitTypeInsn(CHECKCAST, "com/ixaris/commons/async/lib/Async");
                mv.visitVarInsn(ALOAD, 2);
                mv.visitTypeInsn(CHECKCAST, "com/ixaris/commons/async/lib/Async");
                mv.visitMethodInsn(
                    INVOKESTATIC,
                    "com/ixaris/commons/async/lib/Async",
                    "all",
                    "(Ljava/util/concurrent/CompletionStage;Ljava/util/concurrent/CompletionStage;)Lcom/ixaris/commons/async/lib/Async;",
                    true
                );
                mv.visitMethodInsn(
                    INVOKESTATIC,
                    "com/ixaris/commons/async/lib/Async",
                    "await",
                    "(Ljava/util/concurrent/CompletionStage;)Ljava/lang/Object;",
                    true
                );
                mv.visitMethodInsn(
                    INVOKESTATIC,
                    "com/ixaris/commons/async/lib/Async",
                    "result",
                    "(Ljava/lang/Object;)Lcom/ixaris/commons/async/lib/Async;",
                    true
                );
                mv.visitInsn(ARETURN);
                mv.visitMaxs(2, 5);
                mv.visitEnd();
            });
            ClassWriter cw = new ClassWriter(0);
            cn.accept(cw);
            //        DevDebug.debugSaveTrace(cn.name, bytes);
            AsyncBiFunction fn = createClass(AsyncBiFunction.class, cw.toByteArray());
            assertEquals(tuple("hello", "world"), block(fn.apply(result("hello"), result("world"))));
            
            final Async<?> rest = fn.apply(getBlockedFuture("hello"), getBlockedFuture("world"));
            completeFutures();
            assertEquals(tuple("hello", "world"), block(rest));
        });
    }
    
}
