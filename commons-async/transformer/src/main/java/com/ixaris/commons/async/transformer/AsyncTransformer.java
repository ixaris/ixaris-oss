/*
 * Copyright (C) 2015 Electronic Arts Inc. All rights reserved.
 * Copyright (C) 2017 Ixaris Systems Ltd. All rights reserved.
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

import static org.objectweb.asm.Opcodes.ACC_ABSTRACT;
import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PROTECTED;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASM5;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.DCONST_0;
import static org.objectweb.asm.Opcodes.DOUBLE;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.FCONST_0;
import static org.objectweb.asm.Opcodes.FLOAT;
import static org.objectweb.asm.Opcodes.F_FULL;
import static org.objectweb.asm.Opcodes.F_NEW;
import static org.objectweb.asm.Opcodes.F_SAME1;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.H_INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.H_INVOKESTATIC;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INTEGER;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
import static org.objectweb.asm.Opcodes.INVOKEVIRTUAL;
import static org.objectweb.asm.Opcodes.ISTORE;
import static org.objectweb.asm.Opcodes.LCONST_0;
import static org.objectweb.asm.Opcodes.LONG;
import static org.objectweb.asm.Opcodes.MONITORENTER;
import static org.objectweb.asm.Opcodes.MONITOREXIT;
import static org.objectweb.asm.Opcodes.NEW;
import static org.objectweb.asm.Opcodes.NULL;
import static org.objectweb.asm.Opcodes.POP;
import static org.objectweb.asm.Opcodes.POP2;
import static org.objectweb.asm.Opcodes.SIPUSH;
import static org.objectweb.asm.Opcodes.TOP;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.function.Consumer;
import java.util.regex.Pattern;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import org.objectweb.asm.AnnotationVisitor;
import org.objectweb.asm.ClassReader;
import org.objectweb.asm.ClassWriter;
import org.objectweb.asm.Handle;
import org.objectweb.asm.Label;
import org.objectweb.asm.MethodVisitor;
import org.objectweb.asm.Type;
import org.objectweb.asm.TypePath;
import org.objectweb.asm.tree.AbstractInsnNode;
import org.objectweb.asm.tree.ClassNode;
import org.objectweb.asm.tree.FrameNode;
import org.objectweb.asm.tree.InsnList;
import org.objectweb.asm.tree.InsnNode;
import org.objectweb.asm.tree.LabelNode;
import org.objectweb.asm.tree.LineNumberNode;
import org.objectweb.asm.tree.LocalVariableNode;
import org.objectweb.asm.tree.MethodInsnNode;
import org.objectweb.asm.tree.MethodNode;
import org.objectweb.asm.tree.TypeInsnNode;
import org.objectweb.asm.tree.VarInsnNode;
import org.objectweb.asm.tree.analysis.Analyzer;
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Value;

import com.ixaris.commons.async.transformer.FrameAnalyzer.ExtendedFrame;
import com.ixaris.commons.async.transformer.FrameAnalyzer.ExtendedValue;

/**
 * This transformer transforms seemingly blocking code, specifically using Async.await() to await future
 * results, into asynchronous code using stackless continuations.
 *
 * @author brian.vella
 */
final class AsyncTransformer {
    
    private static final String OBJECT_NAME = "java/lang/Object";
    private static final String THROWABLE_NAME = "java/lang/Throwable";
    private static final String FUNCTION_THROWS_NAME = "com/ixaris/commons/misc/lib/function/FunctionThrows";
    
    private static final String COMP_STAGE_UTIL_NAME = "com/ixaris/commons/async/lib/CompletionStageUtil";
    private static final String COMP_STAGE_NAME = "java/util/concurrent/CompletionStage";
    private static final Type COMP_STAGE_TYPE = Type.getObjectType(COMP_STAGE_NAME);
    private static final String ASYNC_NAME = "com/ixaris/commons/async/lib/Async";
    private static final Type ASYNC_TYPE = Type.getObjectType(ASYNC_NAME);
    private static final Pattern ASYNC_PARAMS = Pattern.compile("\\(.*L" + ASYNC_NAME + ";.*\\).*");
    private static final String GET_METHOD_NAME = "get";
    private static final String GET_METHOD_DESC = "(L" + COMP_STAGE_NAME + ";)L" + OBJECT_NAME + ";";
    private static final String FULFILLED_METHOD_NAME = "fulfilled";
    private static final String REJECTED_METHOD_NAME = "rejected";
    private static final String REJECTED_METHOD_DESC = "(L" + THROWABLE_NAME + ";)L" + COMP_STAGE_NAME + ";";
    
    private static final String ALL_METHOD_NAME = "all";
    private static final String ALL_SAME_METHOD_NAME = "allSame";
    private static final String MAP_METHOD_NAME = "map";
    
    private static final String BLOCK_METHOD_NAME = "block";
    private static final String AWAIT_METHOD_NAME = "await";
    private static final String AWAIT_RESULT_METHOD_NAME = "awaitResult";
    private static final String ASYNC_METHOD_NAME = "async";
    private static final String RESULT_METHOD_NAME = "result";
    
    private static final Type ACONST_NULL_TYPE = Type.getObjectType("null");
    
    private static final Handle METAFACTORY_HANDLE = new Handle(H_INVOKESTATIC,
        "java/lang/invoke/LambdaMetafactory",
        "metafactory",
        "(Ljava/lang/invoke/MethodHandles$Lookup;"
            + "Ljava/lang/String;"
            + "Ljava/lang/invoke/MethodType;"
            + "Ljava/lang/invoke/MethodType;"
            + "Ljava/lang/invoke/MethodHandle;"
            + "Ljava/lang/invoke/MethodType;"
            + ")Ljava/lang/invoke/CallSite;");
    //false); using deprecated method to improve ide support
    
    private static class AwaitSwitchEntry {
        
        private final Label resumeLabel;
        private final Label isDoneLabel;
        private final int key;
        private final ExtendedFrame frame;
        private final int index; // original instruction index
        private int[] stackToNewLocal;
        private int[] argumentToLocal;
        private int[] localToiArgument;
        
        private AwaitSwitchEntry(final int key,
                                 final ExtendedFrame frame,
                                 final int index) {
            this.key = key;
            this.frame = frame;
            this.index = index;
            this.isDoneLabel = new Label();
            this.resumeLabel = new Label();
        }
        
    }
    
    private static class Argument {
        
        private final BasicValue value;
        private String name;
        private final int iArgumentLocal;
        // working space
        private int tmpLocalMapping = -1;
        
        private Argument(final BasicValue value, final String name, final int iArgumentLocal) {
            this.value = value;
            this.name = name;
            this.iArgumentLocal = iArgumentLocal;
        }
    }
    
    private final Consumer<String> warnConsumer;
    
    AsyncTransformer(final Consumer<String> warnConsumer) {
        this.warnConsumer = warnConsumer;
    }
    
    AsyncTransformer() {
        this(System.err::println);
    }
    
    /**
     * @param inputStream
     * @return the byte array containing the transformed bytecode, or null if no transformation was required
     * @throws IOException
     */
    byte[] instrument(final InputStream inputStream) throws IOException {
        try {
            return transform(new ClassReader(inputStream));
        } catch (AnalyzerException e) {
            throw new IllegalStateException(e);
        }
    }
    
    /**
     * Does the actual instrumentation generating new bytecode
     *
     * @param cr          the class reader for this class
     * @return the byte array containing the transformed bytecode, or null if no transformation was required
     */
    byte[] transform(final ClassReader cr) throws AnalyzerException {
        final ClassNode classNode = new ClassNode();
        // using EXPAND_FRAMES because F_SAME causes problems when inserting new frames
        cr.accept(classNode, ClassReader.EXPAND_FRAMES);
        
        if (classNode.name.equals(ASYNC_NAME)) {
            // avoid transforming Async itself
            return null;
        }
        boolean changed = false;
        
        final Map<String, Integer> nameUseCount = new HashMap<>();
        
        for (final MethodNode original : new ArrayList<>(classNode.methods)) {
            
            if (ASYNC_PARAMS.matcher(original.desc).matches()) {
                // disallow passing Async as parameters. Allowing this causes 2 major problems:
                // 1. we would need to transform a lot of code to replace Async with CompletionStage for code that just passes around the Async
                // 2. error handling would become misleading since methods that return Async<> throw exceptions on the assumption that the
                // same method that calls the async method handles the exceptions around await(), or propagates the async and exception by
                // re-declaring the exception. If one handles the declared exception but passes the Async as a parameter, that situation is
                // not representative of what would really happen, because the exception would technically be throws when the recipient of
                // the Async does await().
                // NOTE This is still applicable to Async<> returning methods - doing try..catch around op(), then await()ing on the returned
                // async outside the try..catch leads to an incorrect assumption that the exception was handled when in reality it was not.
                // There is, however, no way of representing this in the java language, so a developer needs to be aware that the exceptions
                // declared by an async method are really throws by await()
                throw error(classNode.name, findFirstLineNumber(original), "Async<?> should not be passed as a parameter. Use CompletionStage<?> instead.");
            }
            
            final Integer countOriginalUses = nameUseCount.get(original.name);
            nameUseCount.put(original.name, countOriginalUses == null ? 1 : countOriginalUses + 1);
            
            if (isAsync(original)) {
                changed |= transformAsyncMethod(classNode, original, nameUseCount);
            } else {
                changed |= transformSyncMethod(classNode, original);
            }
        }
        
        // no changes.
        if (!changed) {
            return null;
        }
        
        // avoiding using COMPUTE_FRAMES as it mangles, among others, stack locations for multi catch, turning it
        // into java.lang.Object and causing class verification to fail
        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS) {
            
            @Override
            protected String getCommonSuperClass(final String type1, final String type2) {
                // this is only called if COMPUTE_FRAMES is enabled
                
                // implementing this properly would require loading information
                // for type1 and type2 from the class path.
                // that's too expensive and it also creates problems for offline instrumentation.
                // reusing the old frames and manually creating new ones is a lot cheaper.
                return type1.equals(type2) ? type1 : OBJECT_NAME;
            }
            
        };
        
        classNode.accept(cw);
        return cw.toByteArray();
    }
    
    private int findFirstLineNumber(final MethodNode method) {
        for (AbstractInsnNode insn = method.instructions.getFirst(); insn != null; insn = insn.getNext()) {
            if (insn instanceof LineNumberNode) {
                return ((LineNumberNode) insn).line;
            }
        }
        return -1;
    }
    
    private boolean isAsync(final MethodNode original) {
        return original.desc.endsWith(")L" + ASYNC_NAME + ";");
    }
    
    private static class TransformMethodVisitor extends MethodVisitor {
        
        private final MethodNode methodToAnnotate;
        private final boolean asyncMethod;
        private final ClassNode classNode;
        private final MethodNode original;
        private final String lambdaDesc;
        private final List<Argument> arguments;
        private final Handle handle;
        private final List<AwaitSwitchEntry> switchEntries;
        
        private int awaitIndex = 0;
        private int lastLine;
        private AwaitSwitchEntry awaitSwitchEntry;
        private boolean changed = false;
        
        private TransformMethodVisitor(final MethodNode method,
                                       final MethodNode methodToAnnotate,
                                       final boolean asyncMethod,
                                       final ClassNode classNode,
                                       final MethodNode original,
                                       final String lambdaDesc,
                                       final List<Argument> arguments,
                                       final Handle handle,
                                       final AwaitSwitchEntry entryPoint,
                                       final List<AwaitSwitchEntry> switchEntries) {
            super(ASM5, method);
            this.methodToAnnotate = methodToAnnotate;
            this.asyncMethod = asyncMethod;
            this.classNode = classNode;
            this.original = original;
            this.lambdaDesc = lambdaDesc;
            this.arguments = arguments;
            this.handle = handle;
            this.switchEntries = switchEntries;
            this.awaitSwitchEntry = entryPoint;
        }
        
        @Override
        public AnnotationVisitor visitAnnotationDefault() {
            return methodToAnnotate.visitAnnotationDefault();
        }
        
        @Override
        public AnnotationVisitor visitAnnotation(final String desc, final boolean visible) {
            // copy annotations to the async method
            return methodToAnnotate.visitAnnotation(desc, visible);
        }
        
        @Override
        public AnnotationVisitor visitParameterAnnotation(final int parameter, final String desc, final boolean visible) {
            return methodToAnnotate.visitParameterAnnotation(parameter, desc, visible);
        }
        
        @Override
        public AnnotationVisitor visitTypeAnnotation(final int typeRef, final TypePath typePath, final String desc, final boolean visible) {
            return methodToAnnotate.visitTypeAnnotation(typeRef, typePath, desc, visible);
        }
        
        @Override
        public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc, final boolean itf) {
            if (isStaticAsyncMethod(opcode, owner, name, AWAIT_METHOD_NAME, AWAIT_RESULT_METHOD_NAME)) {
                if (asyncMethod) {
                    awaitSwitchEntry = switchEntries.get(awaitIndex++);
                    transformAwait(AWAIT_METHOD_NAME.equals(name),
                        Modifier.isStatic(original.access),
                        classNode,
                        original,
                        this,
                        awaitSwitchEntry,
                        lambdaDesc,
                        arguments,
                        handle,
                        lastLine);
                    changed = true;
                } else {
                    throw error(classNode.name, lastLine, "non-async methods should not call await(). To block, use block()");
                }
                
            } else if (isStaticAsyncMethod(opcode, owner, name, ASYNC_METHOD_NAME)) {
                // skip async() calls since for async(op()), op will already have been replaced by async$op()
                // effectively, async(op()) becomes async$op()
                changed = true;
                
            } else if (isStaticAsyncMethod(opcode, owner, name, BLOCK_METHOD_NAME)
                || isStaticAsyncMethod(opcode, owner, name, ALL_METHOD_NAME)
                || isStaticAsyncMethod(opcode, owner, name, ALL_SAME_METHOD_NAME)) {
                super.visitMethodInsn(opcode,
                    COMP_STAGE_UTIL_NAME,
                    name,
                    desc.replace("L" + ASYNC_NAME + ";", "L" + COMP_STAGE_NAME + ";"),
                    itf);
                changed = true;
                
            } else if (isAsyncMethod(opcode, owner, name, MAP_METHOD_NAME)) {
                // transform from a.map(f) to COMP_UTIL.map(a, f)
                super.visitMethodInsn(INVOKESTATIC,
                    COMP_STAGE_UTIL_NAME,
                    name,
                    desc
                        .replace(")L" + ASYNC_NAME + ";", ")L" + COMP_STAGE_NAME + ";")
                        .replace("(", "(L" + COMP_STAGE_NAME + ";"),
                    itf);
                changed = true;
                
            } else if (desc.endsWith(")L" + ASYNC_NAME + ";")) {
                // returns Async<?>
                // so either replace Async.result(...) with CompletionStageUtil.fulfilled(...)
                // or replace opReturningAsync(...) with async$opReturningAsync(...)
                boolean isResultMethod = isStaticAsyncMethod(opcode, owner, name, RESULT_METHOD_NAME);
                super.visitMethodInsn(opcode,
                    isResultMethod ? COMP_STAGE_UTIL_NAME : owner,
                    isResultMethod ? FULFILLED_METHOD_NAME : "async$" + name,
                    desc.replace(")L" + ASYNC_NAME + ";", ")L" + COMP_STAGE_NAME + ";"),
                    itf);
                changed = true;
                
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }
        
        @Override
        public void visitInvokeDynamicInsn(final String name, final String desc, final Handle bsm, final Object... bsmArgs) {
            if (bsmArgs[0] instanceof String) {
                // in jdk9 this is sometimes a string?? strange but otherwise no changes needed
                // TODO investigate whether this is the correct way to handle this situation in jdk9
                super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
                return;
            }
            
            final Type target = (Type) bsmArgs[0];
            final Handle handle = (Handle) bsmArgs[1];
            final Type type = (Type) bsmArgs[2];
            if (!handle.getDesc().endsWith(")L" + ASYNC_NAME + ";")) {
                super.visitInvokeDynamicInsn(name, desc, bsm, bsmArgs);
                return;
            }
            
            // returns Async<?>
            if (target.getDescriptor().endsWith(")L" + ASYNC_NAME + ";")) {
                // target descriptor returns Async when targeting synthetic method containing the lambda body
                // so replace lambds$0(...) with async$lambda$0(...)
                super.visitInvokeDynamicInsn("async$" + name,
                    desc,
                    bsm,
                    Type.getType(target.getDescriptor().replace(")L" + ASYNC_NAME + ";", ")L" + COMP_STAGE_NAME + ";")),
                    new Handle(handle.getTag(),
                        handle.getOwner(),
                        "async$" + handle.getName(),
                        handle.getDesc().replace(")L" + ASYNC_NAME + ";", ")L" + COMP_STAGE_NAME + ";")),
                    // handle.isInterface()), using deprecated method to improve ide support
                    Type.getType(type.getDescriptor().replace(")L" + ASYNC_NAME + ";", ")L" + COMP_STAGE_NAME + ";")));
            } else {
                // this case is for generic interfaces, e.g. T doSomething(Callable<T> callable) where
                // the lambda for the Callable.call() method returns Async<>
                // the only difference is that we do not need to change the target method descriptor from Async to CompletionStage
                super.visitInvokeDynamicInsn(name,
                    desc,
                    bsm,
                    target,
                    new Handle(handle.getTag(),
                        handle.getOwner(),
                        "async$" + handle.getName(),
                        handle.getDesc().replace(")L" + ASYNC_NAME + ";", ")L" + COMP_STAGE_NAME + ";")),
                    // handle.isInterface()), using deprecated method to improve ide support
                    Type.getType(type.getDescriptor().replace(")L" + ASYNC_NAME + ";", ")L" + COMP_STAGE_NAME + ";")));
            }
            changed = true;
        }
        
        @Override
        public void visitTypeInsn(final int opcode, final String type) {
            super.visitTypeInsn(opcode, type.equals(ASYNC_NAME) ? COMP_STAGE_NAME : type);
        }
        
        @Override
        public void visitMultiANewArrayInsn(final String desc, final int dims) {
            super.visitMultiANewArrayInsn(desc.replace(ASYNC_NAME, COMP_STAGE_NAME), dims);
        }
        
        @Override
        public void visitFrame(final int type, final int nLocal, final Object[] local, final int nStack, final Object[] stack) {
            for (int i = 0; i < local.length; i++) {
                if (local[i].equals(ASYNC_NAME)) {
                    local[i] = COMP_STAGE_NAME;
                }
            }
            for (int i = 0; i < stack.length; i++) {
                if (stack[i].equals(ASYNC_NAME)) {
                    stack[i] = COMP_STAGE_NAME;
                }
            }
            // the use of EXPAND_FRAMES adds F_NEW which creates problems if not removed.
            super.visitFrame((type == F_NEW) ? F_FULL : type, nLocal, local, nStack, stack);
        }
        
        @Override
        public void visitLineNumber(final int line, final Label start) {
            lastLine = line;
            super.visitLineNumber(line, start);
        }
        
    }
    
    private boolean transformSyncMethod(final ClassNode classNode, final MethodNode original) {
        
        final MethodNode replacement = new MethodNode(original.access,
            original.name,
            original.desc,
            original.signature,
            original.exceptions.toArray(new String[original.exceptions.size()]));
        
        final TransformMethodVisitor methodVisitor = new TransformMethodVisitor(replacement,
            replacement,
            false,
            classNode,
            original,
            null,
            null,
            null,
            null,
            null);
        original.accept(methodVisitor);
        
        if (methodVisitor.changed) {
            classNode.methods.remove(original);
            replacement.accept(classNode);
            return true;
        } else {
            return false;
        }
    }
    
    private boolean transformAsyncMethod(final ClassNode classNode, final MethodNode original, final Map<String, Integer> nameUseCount) throws AnalyzerException {
        final boolean isStatic = Modifier.isStatic(original.access);
        final boolean isAbstract = Modifier.isAbstract(original.access);
        
        final MethodNode async = new MethodNode(original.access,
            "async$" + original.name,
            original.desc.replace(")L" + ASYNC_NAME + ";", ")L" + COMP_STAGE_NAME + ";"),
            original.signature == null ? null : original.signature.replace(")L" + ASYNC_NAME, ")L" + COMP_STAGE_NAME),
            null);
        
        for (final MethodNode m : classNode.methods) {
            if (m == original) {
                continue;
            }
            if (m.name.equals(async.name) && m.desc.equals(async.desc) && Objects.equals(m.exceptions, async.exceptions)) {
                // skip already declared async equivalents
                return false;
            }
        }
        
        if (isAbstract) {
            // create async method implemented by throwing UnsupportedOperationException()
            // so that we do not force implementations to implement the async method as it will be generated using this transformer
            final MethodNode defaultMethod = new MethodNode(original.access & ~ACC_ABSTRACT,
                "async$" + original.name,
                original.desc.replace(")L" + ASYNC_NAME + ";", ")L" + COMP_STAGE_NAME + ";"),
                original.signature.replace(")L" + ASYNC_NAME, ")L" + COMP_STAGE_NAME),
                null);
            
            defaultMethod.visitCode();
            final Label l0 = new Label();
            defaultMethod.visitLabel(l0);
            defaultMethod.visitTypeInsn(NEW, "java/lang/UnsupportedOperationException");
            defaultMethod.visitInsn(DUP);
            defaultMethod.visitLdcInsn("Use AsyncTransformer to transform classes overriding abstract method " + original.name + "()");
            defaultMethod.visitMethodInsn(INVOKESPECIAL, "java/lang/UnsupportedOperationException", "<init>", "(Ljava/lang/String;)V", false);
            defaultMethod.visitMethodInsn(INVOKESTATIC, COMP_STAGE_UTIL_NAME, REJECTED_METHOD_NAME, REJECTED_METHOD_DESC, false);
            defaultMethod.visitInsn(ARETURN);
            defaultMethod.visitEnd();
            defaultMethod.accept(classNode);
            return true;
        }
        
        classNode.methods.remove(original);
        final MethodNode replacement = new MethodNode(original.access,
            original.name,
            original.desc,
            original.signature,
            original.exceptions.toArray(new String[original.exceptions.size()]));
        replacement.visitCode();
        replacement.visitMethodInsn(INVOKESTATIC, ASYNC_NAME, "noTransformation", "()Ljava/lang/UnsupportedOperationException;", false);
        replacement.visitInsn(ATHROW);
        replacement.visitEnd();
        replacement.accept(classNode);
        
        final List<AwaitSwitchEntry> switchEntries = new ArrayList<>();
        final List<Argument> arguments = new ArrayList<>();
        final List<Label> switchLabels = new ArrayList<>();
        final Analyzer<BasicValue> analyzer = new FrameAnalyzer();
        final Frame<BasicValue>[] frames = analyzer.analyze(classNode.name, original);
        
        final AwaitSwitchEntry entryPoint = new AwaitSwitchEntry(0, (ExtendedFrame) frames[0], 0);
        switchLabels.add(entryPoint.resumeLabel);
        
        {
            int ii = 0;
            int count = 0;
            int lastLine = 0;
            
            // create a switch entry for every await() calls - these will result in a continuation invocation
            for (AbstractInsnNode insn = original.instructions.getFirst(); insn != null; ii++, insn = insn.getNext()) {
                if ((insn instanceof MethodInsnNode && isStaticAsyncMethod((MethodInsnNode) insn, AWAIT_METHOD_NAME, AWAIT_RESULT_METHOD_NAME))) {
                    final AwaitSwitchEntry se = new AwaitSwitchEntry(++count, (ExtendedFrame) frames[ii], ii);
                    switchLabels.add(se.resumeLabel);
                    switchEntries.add(se);
                } else if ((insn instanceof InsnNode) && (insn.getOpcode() == POP)) {
                    // while we're here, also check if we have POP instructions where the operand stack head is an Async or a CompletionStage
                    // which indicates an abandoned stage, for which we warn
                    final BasicValue headOfOperandStack = frames[ii].getStack(frames[ii].getStackSize() - 1);
                    if (headOfOperandStack.getType().equals(ASYNC_TYPE) || headOfOperandStack.getType().equals(COMP_STAGE_TYPE)) {
                        warn(classNode.name, lastLine, "Possibly abandoning asynchronous result of type %s", headOfOperandStack.getType());
                    }
                } else if (insn instanceof LineNumberNode) {
                    lastLine = ((LineNumberNode) insn).line;
                }
            }
        }
        
        // compute variable mapping
        // map stack->new locals
        // map (locals + new locals) -> parameters
        // result:
        // stackToNewLocal mapping (stack_index, local_index)
        // - original stack to new local
        // - as an int array: int[stack_index] = local_index
        // localToArgumentMapping (parameter,local_index)
        // - in this order, the push order
        // - as an int array: int[parameter_index] = local_index
        int newMaxLocals = 0;
        final Set<AbstractInsnNode> uninitializedObjects = new HashSet<>();
        
        // maps the locals to arguments
        for (int iLocal = isStatic ? 0 : 1; iLocal < entryPoint.frame.getLocals(); iLocal += valueSize(entryPoint.frame.getLocal(iLocal))) {
            final BasicValue value = entryPoint.frame.getLocal(iLocal);
            if (value != null) {
                // marks uninitialized objects
                if (isUninitialized(value)) {
                    uninitializedObjects.add(((ExtendedValue) value).insnNode);
                } else if ((value.getType() != null) && !ACONST_NULL_TYPE.equals(value.getType())) {
                    mapLocalToLambdaArgument(isStatic, original, entryPoint, arguments, iLocal, value);
                }
            }
        }
        
        for (final AwaitSwitchEntry se : switchEntries) {
            // clear the used state
            arguments.forEach(arg -> arg.tmpLocalMapping = -1);
            se.stackToNewLocal = new int[se.frame.getStackSize() - 1];
            Arrays.fill(se.stackToNewLocal, -1);
            int iNewLocal = original.maxLocals;
            for (int j = 0; j < se.frame.getStackSize() - 1; j++) {
                final BasicValue value = se.frame.getStack(j);
                if (value != null) {
                    // marks uninitialized objects
                    if (isUninitialized(value)) {
                        uninitializedObjects.add(((ExtendedValue) value).insnNode);
                    } else if ((value.getType() != null) && !ACONST_NULL_TYPE.equals(value.getType())) {
                        se.stackToNewLocal[j] = iNewLocal;
                        iNewLocal += valueSize(se.frame.getStack(j));
                    }
                } else {
                    se.stackToNewLocal[j] = -1;
                }
            }
            
            newMaxLocals = Math.max(iNewLocal, newMaxLocals);
            se.localToiArgument = new int[newMaxLocals];
            Arrays.fill(se.localToiArgument, -1);
            // maps the locals to arguments
            for (int iLocal = isStatic ? 0 : 1; iLocal < se.frame.getLocals(); iLocal += valueSize(se.frame.getLocal(iLocal))) {
                final BasicValue value = se.frame.getLocal(iLocal);
                if (value != null) {
                    // marks uninitialized objects
                    if (isUninitialized(value)) {
                        uninitializedObjects.add(((ExtendedValue) value).insnNode);
                    } else if ((value.getType() != null) && !ACONST_NULL_TYPE.equals(value.getType())) {
                        mapLocalToLambdaArgument(isStatic, original, se, arguments, iLocal, value);
                    }
                }
            }
            // maps the stack locals to arguments
            // skipping the last one which will be the Async transformed to CompletionStage and
            // which we will get back in doneConsume
            for (int j = 0; j < se.frame.getStackSize() - 1; j++) {
                final int iLocal = se.stackToNewLocal[j];
                if (iLocal >= 0) {
                    mapLocalToLambdaArgument(isStatic, original, se, arguments, iLocal, se.frame.getStack(j));
                }
            }
            // extract local-to-argument mapping
            se.argumentToLocal = new int[arguments.size()];
            for (int j = 0; j < arguments.size(); j++) {
                se.argumentToLocal[j] = arguments.get(j).tmpLocalMapping;
                if (se.argumentToLocal[j] >= 0) {
                    se.localToiArgument[se.argumentToLocal[j]] = arguments.get(j).iArgumentLocal;
                }
            }
        }
        // only replaces object initialization
        // if uninitialized objects are present in the stack during a await call.
        if (uninitializedObjects.size() > 0) {
            replaceObjectInitialization(original, frames, uninitializedObjects);
        }
        original.maxLocals = Math.max(original.maxLocals, newMaxLocals);
        
        arguments.forEach(p -> p.tmpLocalMapping = -2);
        final Argument stateArgument =
            mapLocalToLambdaArgument(isStatic, original, null, arguments, 0, BasicValue.INT_VALUE);
        final Argument completionStageArgument =
            mapLocalToLambdaArgument(isStatic, original, null, arguments, 0, new BasicValue(COMP_STAGE_TYPE));
        stateArgument.name = "async$state";
        completionStageArgument.name = "async$promise";
        final Object[] defaultFrame;
        {
            final List<Object> frame = new ArrayList<>(arguments.size() + (isStatic ? 0 : 1));
            if (!isStatic) {
                frame.add(classNode.name);
            }
            
            arguments
                .stream()
                .map(a -> {
                    final Object o = toFrameType(a.value.getType());
                    if (o.equals(ASYNC_NAME)) {
                        return COMP_STAGE_NAME;
                    }
                    return o;
                })
                .forEach(frame::add);
            defaultFrame = frame.toArray();
        }
        final Type[] typeArguments = arguments
            .stream()
            .map(a -> {
                final Type type = a.value.getType();
                if (type.getSort() == Type.OBJECT && type.getInternalName().equals(ASYNC_NAME)) {
                    return COMP_STAGE_TYPE;
                }
                return type;
            })
            .toArray(Type[]::new);
        final String lambdaDesc;
        {
            // lambda description needs an extra parameter if not static, representing "this"
            final Type[] lambdaArguments;
            if (isStatic) {
                lambdaArguments = Arrays.copyOf(typeArguments, typeArguments.length - 1);
            } else {
                lambdaArguments = new Type[typeArguments.length];
                lambdaArguments[0] = Type.getObjectType(classNode.name);
                System.arraycopy(typeArguments, 0, lambdaArguments, 1, typeArguments.length - 1);
            }
            
            lambdaDesc = Type.getMethodDescriptor(Type.getType("L" + FUNCTION_THROWS_NAME + ";"), lambdaArguments);
        }
        
        // TODO is the below comment relevant? was in EA transformer
        // adding the switch entries and restore code
        // the local variable restoration has to occur outside
        // the try-catch blocks to avoid problems with the
        // local-frame analysis
        //
        // Where the jvm is unsure of the actual type of a local var
        // when inside the exception handler because the var has changed.
        
        if (!switchEntries.isEmpty()) {
            final String continuationName = "continuation$" + async.name;
            final Integer countUses = nameUseCount.get(continuationName);
            nameUseCount.put(continuationName, countUses == null ? 1 : countUses + 1);
            
            final MethodNode continuation = new MethodNode(
                ACC_SYNTHETIC | ACC_PRIVATE | (original.access & ~ACC_PUBLIC & ~ACC_PROTECTED),
                continuationName + (countUses == null ? "" : "$" + countUses),
                Type.getMethodDescriptor(COMP_STAGE_TYPE, typeArguments),
                null,
                new String[] { THROWABLE_NAME });
            
            final Handle handle = new Handle(isStatic ? H_INVOKESTATIC : H_INVOKESPECIAL,
                classNode.name,
                continuation.name,
                continuation.desc);
            // false); using deprecated method to improve ide support
            
            async.visitCode();
            continuation.visitCode();
            
            // get index for switch
            continuation.visitVarInsn(ILOAD, stateArgument.iArgumentLocal);
            
            final Label defaultLabel = new Label();
            continuation.visitTableSwitchInsn(0, switchLabels.size() - 1, defaultLabel, switchLabels.toArray(new Label[switchLabels.size()]));
            
            // original entry point
            continuation.visitLabel(entryPoint.resumeLabel);
            continuation.visitFrame(F_FULL, defaultFrame.length, defaultFrame, 0, new Object[0]);
            
            // transform the original code to the continuation method
            // (this will use the switch labels to allow jumping right after the await calls on future completion)
            original.accept(new TransformMethodVisitor(continuation,
                async,
                true,
                classNode,
                original,
                lambdaDesc,
                arguments,
                handle,
                entryPoint,
                switchEntries));
            
            // add switch entries for the continuation state machine
            for (final AwaitSwitchEntry se : switchEntries) {
                // code: resumeLabel:
                continuation.visitLabel(se.resumeLabel);
                continuation.visitFrame(F_FULL, defaultFrame.length, defaultFrame, 0, new Object[0]);
                
                // code: restoreStack;
                // code: restoreLocals;
                restoreStackAndLocals(isStatic, classNode, original, continuation, se, arguments, -1);
                continuation.visitJumpInsn(GOTO, se.isDoneLabel);
            }
            
            // last switch case
            continuation.visitLabel(defaultLabel);
            continuation.visitFrame(F_FULL, defaultFrame.length, defaultFrame, 0, new Object[0]);
            continuation.visitTypeInsn(NEW, "java/lang/IllegalArgumentException");
            continuation.visitInsn(DUP);
            continuation.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "()V", false);
            continuation.visitInsn(ATHROW);
            continuation.visitEnd();
            // continuation.maxStack = Math.max(16, continuation.maxStack + 16);
            // continuation.maxLocals = Math.max(16, continuation.maxLocals + 16);
            
            // the async method delegates to the continuation, catching any exceptions
            // any exceptions thrown by the continuation method following await calls are
            // handled by the enclosing completion stages
            final Label l0 = new Label();
            final Label l1 = new Label();
            final Label l2 = new Label();
            async.visitTryCatchBlock(l0, l1, l2, THROWABLE_NAME);
            async.visitLabel(l0);
            pushInitial(isStatic, classNode, async, arguments, -1);
            async.visitMethodInsn(isStatic ? INVOKESTATIC : INVOKESPECIAL, classNode.name, continuation.name, continuation.desc, false);
            async.visitLabel(l1);
            async.visitInsn(ARETURN);
            async.visitLabel(l2);
            async.visitFrame(F_SAME1, 0, null, 1, new Object[] { THROWABLE_NAME });
            async.visitVarInsn(ASTORE, 0);
            final Label l3 = new Label();
            async.visitLabel(l3);
            async.visitVarInsn(ALOAD, 0);
            async.visitMethodInsn(INVOKESTATIC, COMP_STAGE_UTIL_NAME, REJECTED_METHOD_NAME, "(L" + THROWABLE_NAME + ";)L" + COMP_STAGE_NAME + ";", false);
            async.visitInsn(ARETURN);
            final Label l4 = new Label();
            async.visitLabel(l4);
            async.visitLocalVariable("t", "L" + THROWABLE_NAME + ";", null, l3, l4, 0);
            async.visitEnd();
            
            // adding the continuation method
            continuation.accept(classNode);
        } else {
            async.visitCode();
            final Label l0 = new Label();
            async.visitLabel(l0);
            // transform the original code to the async method
            // we are do not need the continuation method when there are no await calls
            final TransformMethodVisitor visitor = new TransformMethodVisitor(async,
                async,
                true,
                classNode,
                original,
                lambdaDesc,
                arguments,
                null,
                entryPoint,
                switchEntries);
            original.accept(visitor);
            
            final Label l1 = new Label();
            // register last to not override existing try..catch blocks registered
            async.visitTryCatchBlock(l0, l1, l1, THROWABLE_NAME);
            async.visitLabel(l1);
            async.visitFrame(F_FULL, 0, new Object[0], 1, new Object[] { THROWABLE_NAME });
            async.visitVarInsn(ASTORE, 0);
            final Label l2 = new Label();
            async.visitLabel(l2);
            async.visitVarInsn(ALOAD, 0);
            async.visitMethodInsn(INVOKESTATIC, COMP_STAGE_UTIL_NAME, REJECTED_METHOD_NAME, "(L" + THROWABLE_NAME + ";)L" + COMP_STAGE_NAME + ";", false);
            async.visitInsn(ARETURN);
            final Label l3 = new Label();
            async.visitLabel(l3);
            async.visitLocalVariable("t", "L" + THROWABLE_NAME + ";", null, l2, l3, 0);
            async.visitEnd();
        }
        
        // adding the async method
        async.accept(classNode);
        
        return true;
    }
    
    private Argument mapLocalToLambdaArgument(final boolean isStatic,
                                              final MethodNode originalMethod,
                                              final AwaitSwitchEntry se,
                                              final List<Argument> arguments,
                                              final int local,
                                              final BasicValue value) {
        if (ACONST_NULL_TYPE.equals(value.getType())) {
            return null;
        }
        
        final String name;
        if (local < originalMethod.maxLocals) {
            name = guessName(originalMethod, se, local);
        } else {
            name = "_stack$" + (local - originalMethod.maxLocals);
        }
        
        // first try to match by name and type
        Optional<Argument> oargument = arguments.stream()
            .filter(x -> x.tmpLocalMapping == -1 && x.value.equals(value) && x.name.equals(name))
            .findFirst();
        
        if (!oargument.isPresent()) {
            // next try to match by type only
            oargument = arguments.stream()
                .filter(x -> x.tmpLocalMapping == -1 && x.value.equals(value))
                .findFirst();
        }
        
        // if no match, create a new one
        final Argument argument = oargument.orElseGet(() -> {
            final int argLocal;
            if (arguments.isEmpty()) {
                argLocal = isStatic ? 0 : 1;
            } else {
                final Argument lastArg = arguments.get(arguments.size() - 1);
                argLocal = lastArg.iArgumentLocal + lastArg.value.getSize();
            }
            final Argument np = new Argument(value, name, argLocal);
            arguments.add(np);
            return np;
        });
        argument.tmpLocalMapping = local;
        return argument;
    }
    
    private String guessName(final MethodNode method, final AwaitSwitchEntry se, final int local) {
        if (se != null) {
            for (LocalVariableNode node : method.localVariables) {
                if (node.index == local
                    && method.instructions.indexOf(node.start) <= se.index
                    && method.instructions.indexOf(node.end) >= se.index) {
                    return node.name;
                }
            }
        }
        return "_local$" + local;
    }
    
    private boolean isUninitialized(final Value value) {
        return value instanceof ExtendedValue && ((ExtendedValue) value).isUninitialized();
    }
    
    private static int valueSize(final Value local) {
        return local == null ? 1 : local.getSize();
    }
    
    // replacing only the initialization of objects that are uninitialized at the moment of an await call.
    static void replaceObjectInitialization(final MethodNode methodNode,
                                            final Frame<BasicValue>[] frames,
                                            final Set<AbstractInsnNode> objectCreationNodes) {
        int originalLocals = methodNode.maxLocals;
        final Set<AbstractInsnNode> uninitializedObjects = objectCreationNodes != null
            ? objectCreationNodes
            : Stream.of(methodNode.instructions.toArray()).filter(i -> i.getOpcode() == NEW).collect(Collectors.toSet());
        
        // since we can't store uninitialized objects they have to be removed or replaced.
        // this works for bytecodes where the initialization is implemented like:
        // NEW T
        // DUP
        // ...
        // T.<init>(..)V
        
        // and the stack before <init> is: {... T' T' args}
        // and the stack after <init> is: {... T}
        // this conforms all cases of java derived bytecode that I'm aware of.
        // but it might not be always true.
        
        // replace frameNodes and constructor calls
        int index = 0;
        for (AbstractInsnNode insnNode = methodNode.instructions.getFirst(); insnNode != null; index++, insnNode = insnNode.getNext()) {
            if (insnNode instanceof FrameNode) {
                FrameNode frameNode = (FrameNode) insnNode;
                frameNode.stack = replaceUninitializedFrameValues(uninitializedObjects, frameNode.stack);
                frameNode.local = replaceUninitializedFrameValues(uninitializedObjects, frameNode.local);
            } else if (insnNode.getOpcode() == INVOKESPECIAL) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                if (methodInsnNode.name.equals("<init>")) {
                    insnNode = replaceConstructorCall(methodNode, frames[index], uninitializedObjects, originalLocals, methodInsnNode);
                }
            }
        }
        // replace new calls
        for (AbstractInsnNode insnNode = methodNode.instructions.getFirst(); insnNode != null; insnNode = insnNode.getNext()) {
            if (insnNode.getOpcode() == NEW && (uninitializedObjects.contains(insnNode))) {
                InsnNode newInsn = new InsnNode(ACONST_NULL);
                methodNode.instructions.insertBefore(insnNode, newInsn);
                methodNode.instructions.remove(insnNode);
                insnNode = newInsn;
            }
        }
    }
    
    private static AbstractInsnNode replaceConstructorCall(final MethodNode methodNode,
                                                           final Frame<BasicValue> frame,
                                                           final Set<AbstractInsnNode> uninitializedObjects,
                                                           final int originalLocals,
                                                           final MethodInsnNode methodInsnNode) {
        final Type[] oldArguments = Type.getArgumentTypes(methodInsnNode.desc);
        final int targetStackIndex = frame.getStackSize() - (1 + oldArguments.length);
        final ExtendedValue target = (ExtendedValue) frame.getStack(targetStackIndex);
        if (uninitializedObjects != null && !uninitializedObjects.contains(target.insnNode)) {
            // only replaces the objects that need replacement
            return methodInsnNode;
        }
        
        final InsnList instructions = methodNode.instructions;
        // later, methodInsnNode is moved to the end of all inserted instructions
        AbstractInsnNode currentInsn = methodInsnNode;
        
        // find the first reference to the target in the stack and saves everything after it.
        final int[] stackToLocal = new int[frame.getStackSize()];
        Arrays.fill(stackToLocal, -1);
        int firstOccurrence = 0;
        while ((firstOccurrence < targetStackIndex) && !target.equals(frame.getStack(firstOccurrence))) {
            firstOccurrence++;
        }
        // number of repetitions in the stack
        int repetitions = 1;
        for (int i = firstOccurrence + 1; (i < frame.getStackSize()) && target.equals(frame.getStack(i)); i++) {
            repetitions++;
        }
        
        // stores relevant stack values to new local variables
        int newMaxLocals = 0;
        int newObject = -1;
        for (int iLocal = originalLocals, j = frame.getStackSize(); --j >= firstOccurrence;) {
            BasicValue value = frame.getStack(j);
            if (value.getType() == null) {
                // some uninitialized value, shouldn't happen, just in case
                instructions.insert(currentInsn, currentInsn = new InsnNode(POP));
                instructions.insert(currentInsn, currentInsn = new InsnNode(ACONST_NULL));
                value = BasicValue.REFERENCE_VALUE;
            }
            if (!target.equals(value)) {
                stackToLocal[j] = iLocal;
                // storing to a temporary local variable
                instructions.insert(currentInsn, currentInsn = new VarInsnNode(value.getType().getOpcode(ISTORE), iLocal));
                iLocal += valueSize(value);
            } else {
                // position where we will put the new object if needed
                if (j >= firstOccurrence + repetitions) {
                    stackToLocal[j] = newObject != -1 ? newObject : (newObject = iLocal++);
                }
                instructions.insert(currentInsn, currentInsn = new InsnNode(POP));
            }
            newMaxLocals = iLocal;
        }
        methodNode.maxLocals = Math.max(newMaxLocals, methodNode.maxLocals);
        
        // creates the object
        instructions.insert(currentInsn, currentInsn = new TypeInsnNode(NEW, target.getType().getInternalName()));
        
        // stores the new object to all locals that should contain it, if any
        for (int j = 0; j < frame.getLocals();) {
            // replaces all locals that used to reference the old value
            BasicValue local = frame.getLocal(j);
            if (target.equals(local)) {
                instructions.insert(currentInsn, currentInsn = new InsnNode(DUP));
                instructions.insert(currentInsn, currentInsn = new VarInsnNode(ASTORE, j));
            }
            j += local.getSize();
        }
        
        if (firstOccurrence < targetStackIndex) {
            // duping instead of putting it to a local, just to look as regular java code.
            for (int i = 1; i < repetitions; i++) {
                instructions.insert(currentInsn, currentInsn = new InsnNode(DUP));
            }
            if (newObject != -1) {
                instructions.insert(currentInsn, currentInsn = new InsnNode(DUP));
                instructions.insert(currentInsn, currentInsn = new VarInsnNode(ASTORE, newObject));
            }
        }
        
        // restoring the the stack
        for (int j = firstOccurrence + repetitions; j < frame.getStackSize(); j++) {
            final BasicValue value = frame.getStack(j);
            if (value.getType() != null) {
                instructions.insert(currentInsn, currentInsn = new VarInsnNode(value.getType().getOpcode(ILOAD), stackToLocal[j]));
            } else {
                // uninitialized value
                instructions.insert(currentInsn, currentInsn = new InsnNode(ACONST_NULL));
            }
        }
        // move the constructor call to here
        instructions.remove(methodInsnNode);
        instructions.insert(currentInsn, currentInsn = methodInsnNode);
        
        // checks if there is stack reconstruction to do:
        return currentInsn;
    }
    
    private static List<Object> replaceUninitializedFrameValues(final Set<AbstractInsnNode> uninitializedObjects,
                                                                final List<Object> list) {
        if (list == null) {
            return null;
        }
        final List<Object> newList = new ArrayList<>(list);
        for (int i = 0, l = newList.size(); i < l; i++) {
            final Object v = newList.get(i);
            // replaces uninitialized object nodes with the actual type from the newList
            if (v instanceof LabelNode) {
                AbstractInsnNode node = (AbstractInsnNode) v;
                while (!(node instanceof TypeInsnNode && node.getOpcode() == NEW)) {
                    node = node.getNext();
                }
                if (uninitializedObjects.contains(node)) {
                    newList.set(i, Type.getType(((TypeInsnNode) node).desc).getInternalName());
                }
            }
        }
        return newList;
    }
    
    /**
     * Replaces calls to Await.await with getting a promise
     * if done: keep executing
     * otherwise, call continuation and jump to the correct place when done
     *
     * @param isStatic
     * @param classNode
     * @param original
     * @param mv the method node whose instructions are being modified
     * @param awaitSwitchEntry
     * @param lambdaDesc
     * @param lambdaArguments
     * @param handle
     * @param lineNumber
     */
    private static void transformAwait(final boolean asyncUnwrapResult,
                                       final boolean isStatic,
                                       final ClassNode classNode,
                                       final MethodNode original,
                                       final MethodVisitor mv,
                                       final AwaitSwitchEntry awaitSwitchEntry,
                                       final String lambdaDesc,
                                       final List<Argument> lambdaArguments,
                                       final Handle handle,
                                       final int lineNumber) {
        
        // original: Async.await(future) / Async.awaitResult(future)
        
        // turns into:
        
        // code: if (!isDone(future)) {
        // code: saveStack to new locals
        // code: push lambda parameters (locals and stack)
        // code: return CompletionStageUtil.doneCompose(future -> continuation$async$method(arguments, state, future));
        // code: }
        // code: isDoneLabel:
        // code: get(future); -> causes exception to be thrown
        // (only for awaitResult, restore future on stack)
        
        // and this is added to the switch
        // code: resumeLabel:
        // code: restoreStack;
        // code: restoreLocals;
        // code: jump isDoneLabel:
        
        // current stack head is the future reference passed to await()
        // stack: { ... future }
        mv.visitInsn(DUP);
        // stack: { ... future future }
        mv.visitMethodInsn(INVOKESTATIC, COMP_STAGE_UTIL_NAME, "isDone", "(L" + COMP_STAGE_NAME + ";)Z", false);
        // stack: { ... future is_done_result }
        // code: jump if true (non 0) to isDoneLabel:
        mv.visitJumpInsn(IFNE, awaitSwitchEntry.isDoneLabel);
        
        // code: saveStack to new state
        // code: push the future
        // code: push all lambda parameters
        // code: create the lambda
        
        if (awaitSwitchEntry.stackToNewLocal.length > 0) {
            // clears the stack and leaves promise in the top (by using the lambda parameter as temporary variable)
            final int lambdaParamIndex = lambdaArguments.get(lambdaArguments.size() - 1).iArgumentLocal;
            // stack: { ... future }
            mv.visitVarInsn(ASTORE, lambdaParamIndex);
            // stack: { ... }
            saveStack(mv, awaitSwitchEntry);
            // stack: { }
            mv.visitVarInsn(ALOAD, lambdaParamIndex);
            // stack: { future }
        }
        
        // code: return CompletionStageUtil.doneCompose(future, future -> _func([arguments-2, state], future));
        // where [arguments-2, state] are bound to the lambda
        
        pushArguments(isStatic, classNode, mv, awaitSwitchEntry, lambdaArguments, lineNumber);
        // stack: { future ...arguments-2... }
        mv.visitIntInsn(SIPUSH, awaitSwitchEntry.key);
        // stack: { future ...arguments-2... state }
        
        mv.visitInvokeDynamicInsn("apply",
            lambdaDesc,
            METAFACTORY_HANDLE,
            Type.getType("(L" + OBJECT_NAME + ";)L" + OBJECT_NAME + ";"),
            handle,
            Type.getType("(L" + COMP_STAGE_NAME + ";)L" + COMP_STAGE_NAME + ";"));
        
        // stack: { future lambda_function_ref }
        mv.visitMethodInsn(INVOKESTATIC, COMP_STAGE_UTIL_NAME, "doneCompose", "(L" + COMP_STAGE_NAME + ";L" + FUNCTION_THROWS_NAME + ";)L" + COMP_STAGE_NAME + ";", false);
        
        // stack: { new_future }
        
        // release monitors
        if (awaitSwitchEntry.frame.monitors.length > 0) {
            for (int i = awaitSwitchEntry.frame.monitors.length; --i >= 0;) {
                final BasicValue monitorValue = awaitSwitchEntry.frame.monitors[i];
                int monitorLocal = -1;
                for (int iLocal = 0; iLocal < awaitSwitchEntry.frame.getLocals(); iLocal += valueSize(awaitSwitchEntry.frame.getLocal(iLocal))) {
                    if (awaitSwitchEntry.frame.getLocal(iLocal) == monitorValue) {
                        monitorLocal = iLocal;
                        // don't break; prefer using the last one
                    }
                }
                // only release if a local variable containing the monitor was found
                if (monitorLocal != -1) {
                    mv.visitVarInsn(ALOAD, monitorLocal);
                    mv.visitInsn(MONITOREXIT);
                } else {
                    throw error(classNode.name,
                        lineNumber,
                        "Error restoring monitors in synchronized method. monitorLocal=%d, at %s.%s",
                        monitorLocal,
                        classNode.name,
                        original.name);
                }
            }
        }
        
        mv.visitInsn(ARETURN);
        // code: isDoneLabel:
        mv.visitLabel(awaitSwitchEntry.isDoneLabel);
        
        fullFrame(mv, awaitSwitchEntry.frame);
        
        if (!asyncUnwrapResult) {
            // leave a reference to future on the stack, as we only call get() below to throw exception in case of rejection
            mv.visitInsn(DUP);
            // {  ... future future }
        }
        
        // {  ... future? future }
        mv.visitMethodInsn(INVOKESTATIC, COMP_STAGE_UTIL_NAME, GET_METHOD_NAME, GET_METHOD_DESC, false);
        // {  ... future? result } or throw exception.
        // For first iteration, async$ method catches exeption and rejects the future
        // For continuations, an exception rejects the composing future
        
        if (!asyncUnwrapResult) {
            // remove the result
            mv.visitInsn(POP);
            // {  ... future }
        }
    }
    
    private static void fullFrame(final MethodVisitor mv, final Frame frame) {
        Object[] locals = new Object[frame.getLocals()];
        Object[] stack = new Object[frame.getStackSize()];
        int nStack = 0;
        int nLocals = 0;
        int maxLocal = 0;
        for (int i = 0; i < locals.length; i++) {
            final BasicValue value = (BasicValue) frame.getLocal(i);
            final Type type = value.getType();
            if (type == null) {
                locals[nLocals++] = TOP;
            } else {
                locals[nLocals++] = toFrameType(type);
                if (value.getSize() == 2)
                    i++;
                maxLocal = nLocals;
            }
        }
        for (int i = 0; i < frame.getStackSize(); i++) {
            final BasicValue value = (BasicValue) frame.getStack(i);
            final Type type = value.getType();
            if (type == null) {
                continue;
            }
            stack[nStack++] = toFrameType(type);
        }
        stack = nStack == stack.length ? stack : Arrays.copyOf(stack, nStack);
        locals = nLocals == locals.length ? locals : Arrays.copyOf(locals, maxLocal);
        mv.visitFrame(F_FULL, maxLocal, locals, nStack, stack);
    }
    
    private static Object toFrameType(final Type type) {
        switch (type.getSort()) {
            case Type.BOOLEAN:
            case Type.BYTE:
            case Type.INT:
            case Type.CHAR:
            case Type.SHORT:
                return INTEGER;
            case Type.LONG:
                return LONG;
            case Type.FLOAT:
                return FLOAT;
            case Type.DOUBLE:
                return DOUBLE;
        }
        final String internalName = type.getInternalName();
        if (ACONST_NULL_TYPE.equals(type)) {
            return NULL;
        }
        return internalName;
    }
    
    private static boolean isStaticAsyncMethod(final MethodInsnNode methodIns, final String... methods) {
        return isStaticAsyncMethod(methodIns.getOpcode(), methodIns.owner, methodIns.name, methods);
    }
    
    private static boolean isStaticAsyncMethod(final int opcode, final String owner, final String name, final String... methods) {
        return opcode == INVOKESTATIC && ASYNC_NAME.equals(owner) && Arrays.asList(methods).contains(name);
    }
    
    private static boolean isAsyncMethod(final int opcode, final String owner, final String name, final String... methods) {
        return opcode == INVOKEVIRTUAL && ASYNC_NAME.equals(owner) && Arrays.asList(methods).contains(name);
    }
    
    private static void saveStack(final MethodVisitor method, final AwaitSwitchEntry se) {
        // stack: { ... }
        for (int i = se.stackToNewLocal.length - 1; i >= 0; i--) {
            int iLocal = se.stackToNewLocal[i];
            if (iLocal >= 0) {
                method.visitVarInsn(se.frame.getStack(i).getType().getOpcode(ISTORE), iLocal);
            } else {
                method.visitInsn(se.frame.getStack(i).getType().getSize() == 2 ? POP2 : POP);
            }
        }
        // stack: { } empty
    }
    
    private static void pushArguments(final boolean isStatic,
                                      final ClassNode classNode,
                                      final MethodVisitor method,
                                      final AwaitSwitchEntry awaitSwitchEntry,
                                      final List<Argument> lambdaArguments,
                                      final int lineNumber) {
        // stack: { ... }
        if (!isStatic) {
            // stack: { ... this }
            method.visitVarInsn(ALOAD, 0);
        }
        // push all arguments except the last 2 arguments (state and future)
        for (int i = 0, l = lambdaArguments.size() - 2; i < l; i++) {
            int iLocal = i < awaitSwitchEntry.argumentToLocal.length ? awaitSwitchEntry.argumentToLocal[i] : -1;
            final BasicValue value = lambdaArguments.get(i).value;
            if (iLocal >= 0) {
                method.visitVarInsn(value.getType().getOpcode(ILOAD), iLocal);
            } else {
                pushDefault(classNode, method, value, lineNumber);
            }
        }
        // stack: { ... this? lambda_arguments }
    }
    
    private static void pushInitial(final boolean isStatic,
                                    final ClassNode classNode,
                                    final MethodNode method,
                                    final List<Argument> lambdaArguments,
                                    final int lineNumber) {
        // stack: { ... }
        if (!isStatic) {
            method.visitVarInsn(ALOAD, 0);
        }
        // push arguments followed by defaults for the extra local and stack continuation arguments
        // except the last 2 arguments (state and promise)
        final Type[] methodArgument = Type.getArgumentTypes(method.desc);
        for (int i = 0, l = lambdaArguments.size() - 2; i < l; i++) {
            final Argument argument = lambdaArguments.get(i);
            if (i < methodArgument.length) {
                method.visitVarInsn(argument.value.getType().getOpcode(ILOAD), argument.iArgumentLocal);
            } else {
                pushDefault(classNode, method, argument.value, lineNumber);
            }
        }
        method.visitInsn(ICONST_0); // initial state
        method.visitInsn(ACONST_NULL); // null promise
        // stack: { ... lambdaArguments}
    }
    
    private static void pushDefault(final ClassNode classNode,
                                    final MethodVisitor mv,
                                    final BasicValue value,
                                    final int lineNumber) {
        if (value.getType() == null) {
            mv.visitInsn(ACONST_NULL);
            return;
        }
        switch (value.getType().getSort()) {
            case Type.VOID:
                mv.visitInsn(ACONST_NULL);
                return;
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                mv.visitInsn(ICONST_0);
                return;
            case Type.FLOAT:
                mv.visitInsn(FCONST_0);
                return;
            case Type.LONG:
                mv.visitInsn(LCONST_0);
                return;
            case Type.DOUBLE:
                mv.visitInsn(DCONST_0);
                return;
            case Type.ARRAY:
            case Type.OBJECT:
                mv.visitInsn(ACONST_NULL);
                return;
            default:
                throw error(classNode.name, lineNumber, "Unknown type: " + value.getType().getSort());
        }
    }
    
    private static void restoreStackAndLocals(final boolean isStatic,
                                              final ClassNode classNode,
                                              final MethodNode original,
                                              final MethodVisitor mv,
                                              final AwaitSwitchEntry se,
                                              final List<Argument> lambdaArguments,
                                              final int lineNumber) {
        // restore the stack: just push in the right order.
        // restore the locals: push all that changed place then load
        if (se.argumentToLocal == null) {
            return;
        }
        final ExtendedFrame frame = se.frame;
        for (int i = 0; i < frame.getStackSize() - 1; i++) {
            final BasicValue value = frame.getStack(i);
            // stack_index -> stackToNewLocal -> argumentToLocal -> arg_index
            int iLocal = se.stackToNewLocal[i];
            if (iLocal >= 0 && se.localToiArgument[iLocal] >= 0) {
                mv.visitVarInsn(value.getType().getOpcode(ILOAD), se.localToiArgument[iLocal]);
            } else {
                pushDefault(classNode, mv, value, lineNumber);
            }
        }
        // restore promise in stack from last argument
        mv.visitVarInsn(ALOAD, lambdaArguments.get(lambdaArguments.size() - 1).iArgumentLocal);
        
        // push the arguments that must be copied to locals
        // must only push
        for (int iLocal = isStatic ? 0 : 1; iLocal < frame.getLocals(); iLocal += valueSize(frame.getLocal(iLocal))) {
            final BasicValue value = frame.getLocal(iLocal);
            if (se.localToiArgument[iLocal] >= 0) {
                if (se.localToiArgument[iLocal] != iLocal) {
                    mv.visitVarInsn(value.getType().getOpcode(ILOAD), se.localToiArgument[iLocal]);
                }
            }
        }
        // stores the values that came from arguments
        for (int iLocal = frame.getLocals(); --iLocal >= (isStatic ? 0 : 1);) {
            if (se.localToiArgument[iLocal] >= 0 && se.localToiArgument[iLocal] != iLocal) {
                mv.visitVarInsn(frame.getLocal(iLocal).getType().getOpcode(ISTORE), iLocal);
            }
        }
        // restore values that were initialized as constants (i.e.: ACONST_NULL)
        for (int iLocal = isStatic ? 0 : 1; iLocal < frame.getLocals(); iLocal += valueSize(frame.getLocal(iLocal))) {
            final BasicValue value = frame.getLocal(iLocal);
            if (se.localToiArgument[iLocal] < 0 && value != null && value.getType() != null) {
                pushDefault(classNode, mv, value, lineNumber);
                mv.visitVarInsn(value.getType().getOpcode(ISTORE), iLocal);
            }
        }
        
        // reacquire monitors
        for (int i = 0; i < se.frame.monitors.length; i++) {
            final BasicValue monitorValue = frame.monitors[i];
            int monitorLocal = -1;
            for (int iLocal = 0; iLocal < frame.getLocals(); iLocal += valueSize(frame.getLocal(iLocal))) {
                if (frame.getLocal(iLocal) == monitorValue) {
                    monitorLocal = iLocal;
                    // don't break; prefer using the last one
                }
            }
            // only acquire if a local variable containing the monitor was found
            if (monitorLocal != -1) {
                mv.visitVarInsn(ALOAD, monitorLocal);
                mv.visitInsn(MONITORENTER);
            } else {
                throw error(classNode.name, lineNumber, "Error restoring monitors in synchronized method. monitorLocal=%d, at %s.%s", monitorLocal, classNode.name, original.name);
            }
        }
    }
    
    private void warn(final String className, final int lineNumber, final String format, final Object... args) {
        final String error = className + " " + ((lineNumber > 0) ? "LINE " + lineNumber + ": " : "") + String.format(format, args);
        warnConsumer.accept(error);
    }
    
    private static AsyncTransformerException error(final String className, final int lineNumber, final String format, final Object... args) {
        final String error = className + " " + ((lineNumber > 0) ? "LINE " + lineNumber + ": " : "") + String.format(format, args);
        return new AsyncTransformerException(error);
    }
    
}
