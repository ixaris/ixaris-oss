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

import static com.ixaris.commons.misc.lib.object.Tuple.tuple;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.RETURN;

import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.function.Consumer;

import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassVisitor;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.tree.ClassNode;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.async.transformed.test.BaseTest;

public class BaseTransformerTest extends BaseTest {
    
    protected static final AsyncTransformer TRANSFORMER = new AsyncTransformer();
    
    // utility method to create arbitrary classes.
    protected <T> T createClass(Class<T> superClass, Consumer<ClassVisitor> populate) {
        ClassNode cn = createClassNode(superClass, populate);
        return createClass(superClass, cn);
    }
    
    protected <T> T createClass(final Class<T> superClass, final ClassNode cn) {
        ClassWriter cw = new ClassWriter(0);
        cn.accept(cw);
        final byte[] bytes = cw.toByteArray();
        return createClass(superClass, bytes);
    }
    
    //
    protected <T> T createClass(final Class<T> superClass, final byte[] bytes) {
        
        // perhaps we should use the source class ClassLoader as parent.
        class Loader extends ClassLoader {
            Loader() {
                super(superClass.getClassLoader());
            }
            
            public Class<?> define(final String o, final byte[] bytes) {
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
        return out != null ? out : bytes;
    }
    
    protected <T> ClassNode createClassNode(final Class<T> superClass, final Consumer<ClassVisitor> populate) {
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
        int strLen;
        if (str == null || (strLen = str.length()) == 0) {
            return str;
        }
        
        final char firstChar = str.charAt(0);
        if (Character.isTitleCase(firstChar)) {
            // already capitalized
            return str;
        }
        
        return new StringBuilder(strLen)
            .append(Character.toTitleCase(firstChar))
            .append(str.substring(1))
            .toString();
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
    
    public boolean mentionsAsync(ClassReader cr) {
        for (int i = 1, c = cr.getItemCount(); i < c; i++) {
            final int address = cr.getItem(i);
            if (address > 0
                && cr.readByte(address - 1) == 7
                && equalsUtf8(cr, address, "com/ixaris/commons/async/lib/Async")) {
                // CONSTANT_Class_info {
                // u1 tag; = 7
                // u2 name_index; -> utf8
                // }
                return true;
            }
        }
        return false;
    }
    
    private boolean equalsUtf8(final ClassReader cr, final int pointerToUtf8Index, final String str) {
        int utf8_index = cr.readUnsignedShort(pointerToUtf8Index);
        if (utf8_index == 0) {
            return false;
        }
        
        // CONSTANT_Utf8_info {
        // u1 tag; = 1
        // u2 length;
        // u1 bytes[length];
        // }
        
        int utf8_address = cr.getItem(utf8_index);
        final int utf8_length = cr.readUnsignedShort(utf8_address);
        if (utf8_length == str.length()) {
            // assuming the str is utf8 "safe", no special chars
            int idx = utf8_address + 2;
            for (int ic = 0; ic < utf8_length; ic++, idx++) {
                if (str.charAt(ic) != (char) cr.readByte(idx)) {
                    return false;
                }
            }
            return true;
        }
        return false;
    }
    
    //    public void debugTransform(String className) throws Exception {
    //
    //        final String resourceName = "/" + className.replace(".", "/") + ".class";
    //        System.out.println(resourceName);
    //        final byte[] bytesInput = IOUtils.toByteArray(getClass().getResourceAsStream(resourceName));
    //        debugTransform(bytesInput);
    //    }
    //
    //    public void debugTransform(Class<?> clazz) throws Exception {
    //        final byte[] bytesInput = IOUtils.toByteArray(clazz.getResourceAsStream("/" + clazz.getName().replace(".", "/") + ".class"));
    //        debugTransform(bytesInput);
    //    }
    //
    //    private void debugTransform(final byte[] bytesInput) throws IOException, AnalyzerException {
    //        final ClassReader crInput = new ClassReader(bytesInput);
    //        {
    //            final ClassNode cnInput = new ClassNode();
    //            crInput.accept(cnInput, 0);
    //
    //            final Path pathInput = Paths.get("target/classes2").resolve(cnInput.name + ".class");
    //            Files.deleteIfExists(pathInput);
    //            Files.createDirectories(pathInput.getParent());
    //            Files.write(pathInput, bytesInput);
    //            System.out.println(pathInput.toUri());
    //            DevDebug.debugSaveTrace(cnInput.name, cnInput);
    //        }
    //
    //        {
    //            final Transformer TRANSFORMER = new Transformer();
    //            final byte[] bytesOutput = TRANSFORMER.transform(getClass().getClassLoader(), crInput);
    //            final ClassReader crOutput = new ClassReader(bytesOutput);
    //            final ClassNode cnOutput = new ClassNode();
    //            crOutput.accept(cnOutput, 0);
    //
    //            final Path pathOutput = Paths.get("target/classes2").resolve(cnOutput.name + ".out.class");
    //            Files.deleteIfExists(pathOutput);
    //            Files.createDirectories(pathOutput.getParent());
    //            Files.write(pathOutput, bytesOutput);
    //            System.out.println(pathOutput.toUri());
    //            DevDebug.debugSaveTrace(cnOutput.name + ".out", cnOutput);
    //        }
    //    }
    
}
