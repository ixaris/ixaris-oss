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

import static jdk.internal.org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ALOAD;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static jdk.internal.org.objectweb.asm.Opcodes.RETURN;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.function.Consumer;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.transformed.test.BaseTest;

import jdk.internal.org.objectweb.asm.ClassVisitor;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Type;
import jdk.internal.org.objectweb.asm.tree.ClassNode;

public class BaseTransformerTest extends BaseTest {
    
    private static final AsyncTransformer TRANSFORMER = new AsyncTransformer();
    
    // utility method to create arbitrary classes.
    <T> T createClass(final Class<T> superClass, final Consumer<ClassVisitor> populate) {
        ClassNode cn = createClassNode(superClass, populate);
        return createClass(superClass, cn);
    }
    
    <T> T createClass(final Class<T> superClass, final ClassNode cn) {
        ClassWriter cw = new ClassWriter(0);
        cn.accept(cw);
        final byte[] bytes = cw.toByteArray();
        return createClass(superClass, bytes);
    }
    
    //
    <T> T createClass(final Class<T> superClass, final byte[] bytes) {
        
        // perhaps we should use the source class ClassLoader as parent.
        class Loader extends ClassLoader {
            private Loader() {
                super(superClass.getClassLoader());
            }
            
            private Class<?> define(final String o, final byte[] bytes) {
                return super.defineClass(o, bytes, 0, bytes.length);
            }
        }
        // noinspection unchecked
        try {
            return (T) new Loader().define(null, transform(bytes)).newInstance();
        } catch (Exception e) {
            throw new RuntimeException(e);
        }
    }
    
    private byte[] transform(final byte[] bytes) throws IOException {
        final byte[] out = TRANSFORMER.instrument(new ByteArrayInputStream(bytes));
        if (out != null) {
            final byte[] out2 = TRANSFORMER.instrument(new ByteArrayInputStream(out));
            if (out2 != null) {
                throw new IllegalStateException("Transformation not idempotent");
            }
        }
        return out != null ? out : bytes;
    }
    
    <T> ClassNode createClassNode(final Class<T> superClass, final Consumer<ClassVisitor> populate) {
        ClassNode cn = new ClassNode();
        String[] interfaces = null;
        Type superType;
        if (superClass.isInterface()) {
            superType = Type.getType(Object.class);
            interfaces = new String[] { Type.getType(superClass).getInternalName() };
        } else {
            superType = Type.getType(superClass);
        }
        String superName = superType.getInternalName();
        StackTraceElement caller = new Exception().getStackTrace()[1];
        final String name = "Experiment" + capitalize(caller.getMethodName()) + caller.getLineNumber();
        
        cn.visit(52, ACC_PUBLIC, Type.getType(getClass()).getInternalName() + "$" + name, null, superName, interfaces);
        if (populate != null) {
            populate.accept(cn);
        }
        MethodVisitor mv;
        {
            mv = cn.visitMethod(ACC_PUBLIC, "<init>", "()V", null, null);
            mv.visitCode();
            mv.visitVarInsn(ALOAD, 0);
            mv.visitMethodInsn(INVOKESPECIAL, superName, "<init>", "()V", false);
            mv.visitInsn(RETURN);
            mv.visitMaxs(1, 1);
            mv.visitEnd();
        }
        cn.visitEnd();
        return cn;
    }
    
    private static String capitalize(final String str) {
        if (str == null || str.length() == 0) {
            return str;
        }
        
        final char firstChar = str.charAt(0);
        if (Character.isTitleCase(firstChar)) {
            // already capitalized
            return str;
        }
        
        return String.valueOf(Character.toTitleCase(firstChar)) + str.substring(1);
    }
    
    public interface AsyncCallable<V> {
        Async<V> call() throws Exception;
    }
    
    public interface AsyncFunction<T, R> {
        Async<R> apply(T t) throws Exception;
    }
    
    public interface AsyncBiFunction<T, U, R> {
        Async<R> apply(T t, U u) throws Exception;
    }
    
}
