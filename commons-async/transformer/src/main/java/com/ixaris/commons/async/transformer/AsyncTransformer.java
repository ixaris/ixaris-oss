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

import static jdk.internal.org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_PROTECTED;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ACONST_NULL;
import static jdk.internal.org.objectweb.asm.Opcodes.ALOAD;
import static jdk.internal.org.objectweb.asm.Opcodes.ARETURN;
import static jdk.internal.org.objectweb.asm.Opcodes.ASM5;
import static jdk.internal.org.objectweb.asm.Opcodes.ASTORE;
import static jdk.internal.org.objectweb.asm.Opcodes.ATHROW;
import static jdk.internal.org.objectweb.asm.Opcodes.DCONST_0;
import static jdk.internal.org.objectweb.asm.Opcodes.DOUBLE;
import static jdk.internal.org.objectweb.asm.Opcodes.DUP;
import static jdk.internal.org.objectweb.asm.Opcodes.FCONST_0;
import static jdk.internal.org.objectweb.asm.Opcodes.FLOAT;
import static jdk.internal.org.objectweb.asm.Opcodes.F_FULL;
import static jdk.internal.org.objectweb.asm.Opcodes.F_NEW;
import static jdk.internal.org.objectweb.asm.Opcodes.GOTO;
import static jdk.internal.org.objectweb.asm.Opcodes.H_INVOKESPECIAL;
import static jdk.internal.org.objectweb.asm.Opcodes.H_INVOKESTATIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ICONST_0;
import static jdk.internal.org.objectweb.asm.Opcodes.IFNE;
import static jdk.internal.org.objectweb.asm.Opcodes.ILOAD;
import static jdk.internal.org.objectweb.asm.Opcodes.INTEGER;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKESTATIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ISTORE;
import static jdk.internal.org.objectweb.asm.Opcodes.LCONST_0;
import static jdk.internal.org.objectweb.asm.Opcodes.LONG;
import static jdk.internal.org.objectweb.asm.Opcodes.MONITORENTER;
import static jdk.internal.org.objectweb.asm.Opcodes.MONITOREXIT;
import static jdk.internal.org.objectweb.asm.Opcodes.NEW;
import static jdk.internal.org.objectweb.asm.Opcodes.NULL;
import static jdk.internal.org.objectweb.asm.Opcodes.POP;
import static jdk.internal.org.objectweb.asm.Opcodes.POP2;
import static jdk.internal.org.objectweb.asm.Opcodes.SIPUSH;
import static jdk.internal.org.objectweb.asm.Opcodes.TOP;

import java.io.IOException;
import java.io.InputStream;
import java.lang.reflect.Modifier;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.stream.Stream;

import com.ixaris.commons.async.transformer.FrameAnalyzer.ExtendedFrame;
import com.ixaris.commons.async.transformer.FrameAnalyzer.ExtendedValue;

import jdk.internal.org.objectweb.asm.AnnotationVisitor;
import jdk.internal.org.objectweb.asm.ClassReader;
import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.Handle;
import jdk.internal.org.objectweb.asm.Label;
import jdk.internal.org.objectweb.asm.MethodVisitor;
import jdk.internal.org.objectweb.asm.Type;
import jdk.internal.org.objectweb.asm.TypePath;
import jdk.internal.org.objectweb.asm.tree.AbstractInsnNode;
import jdk.internal.org.objectweb.asm.tree.AnnotationNode;
import jdk.internal.org.objectweb.asm.tree.ClassNode;
import jdk.internal.org.objectweb.asm.tree.FrameNode;
import jdk.internal.org.objectweb.asm.tree.InsnList;
import jdk.internal.org.objectweb.asm.tree.InsnNode;
import jdk.internal.org.objectweb.asm.tree.LabelNode;
import jdk.internal.org.objectweb.asm.tree.LineNumberNode;
import jdk.internal.org.objectweb.asm.tree.LocalVariableNode;
import jdk.internal.org.objectweb.asm.tree.MethodInsnNode;
import jdk.internal.org.objectweb.asm.tree.MethodNode;
import jdk.internal.org.objectweb.asm.tree.TypeInsnNode;
import jdk.internal.org.objectweb.asm.tree.VarInsnNode;
import jdk.internal.org.objectweb.asm.tree.analysis.Analyzer;
import jdk.internal.org.objectweb.asm.tree.analysis.AnalyzerException;
import jdk.internal.org.objectweb.asm.tree.analysis.BasicValue;
import jdk.internal.org.objectweb.asm.tree.analysis.Frame;
import jdk.internal.org.objectweb.asm.tree.analysis.Value;

/**
 * This transformer transforms seemingly blocking code, specifically using Async.await() to await future
 * results, into asynchronous code using stackless continuations.
 *
 * @author brian.vella
 */
final class AsyncTransformer {
    
    static {
        // eclipse compiler does something, probably with access control, that prevents classes from being loaded during compilation
        // so we preload all the classes from this package
        Preloader.preloadClassesInPackage(AsyncTransformer.class.getClassLoader(), "com.ixaris.commons.async.transformer");
    }
    
    private static final String OBJECT_NAME = "java/lang/Object";
    private static final String FUNCTION_THROWS_NAME = "com/ixaris/commons/misc/lib/function/FunctionThrows";
    
    private static final String COMP_FUTURE_NAME = "java/util/concurrent/CompletableFuture";
    
    private static final String COMP_STAGE_NAME = "java/util/concurrent/CompletionStage";
    private static final Type COMP_STAGE_TYPE = Type.getObjectType(COMP_STAGE_NAME);
    private static final String COMP_STAGE_TO_COMPLETABLE_FUTURE_NAME = "toCompletableFuture";
    private static final String COMP_STAGE_TO_COMPLETABLE_FUTURE_DESC = "()L" + COMP_FUTURE_NAME + ";";
    
    private static final String ASYNC_NAME = "com/ixaris/commons/async/lib/Async";
    private static final Type ASYNC_TYPE = Type.getObjectType(ASYNC_NAME);
    private static final String ASYNC_FROM_NAME = "from";
    private static final String ASYNC_FROM_DESC = "(L" + COMP_STAGE_NAME + ";)L" + ASYNC_NAME + ";";
    private static final String ASYNC_AWAIT_METHOD_NAME = "await";
    private static final String ASYNC_AWAIT_RESULT_METHOD_NAME = "awaitResult";
    private static final String ASYNC_AWAIT_EXCEPTIONS_METHOD_NAME = "awaitExceptions";
    
    private static final String ASYNC_ANNOTATION_NAME = "com/ixaris/commons/async/lib/annotation/Async";
    
    private static final String COMP_STAGE_UTIL_NAME = "com/ixaris/commons/async/lib/CompletionStageUtil";
    private static final String COMP_STAGE_UTIL_IS_DONE_NAME = "isDone";
    private static final String COMP_STAGE_UTIL_IS_DONE_DESC = "(L" + COMP_STAGE_NAME + ";)Z";
    private static final String COMP_STAGE_UTIL_GET_NAME = "get";
    private static final String COMP_STAGE_UTIL_GET_DESC = "(L" + COMP_STAGE_NAME + ";)L" + OBJECT_NAME + ";";
    private static final String COMP_STAGE_UTIL_DONE_COMPOSE_NAME = "doneCompose";
    private static final String COMP_STAGE_UTIL_DONE_COMPOSE_DESC = "(L" + COMP_STAGE_NAME + ";L" + FUNCTION_THROWS_NAME + ";)L" + COMP_STAGE_NAME + ";";
    
    private static final String STATIC_FROM_COMPLETION_STAGE_NAME = "fromCompletionStage";
    
    private static final Type ACONST_NULL_TYPE = Type.getObjectType("null");
    
    private static final Handle LAMBDAMETAFACTORY_HANDLE = new Handle(H_INVOKESTATIC,
        "java/lang/invoke/LambdaMetafactory",
        "metafactory",
        "("
            + "Ljava/lang/invoke/MethodHandles$Lookup;"
            + "Ljava/lang/String;"
            + "Ljava/lang/invoke/MethodType;"
            + "Ljava/lang/invoke/MethodType;"
            + "Ljava/lang/invoke/MethodHandle;"
            + "Ljava/lang/invoke/MethodType;"
            + ")"
            + "Ljava/lang/invoke/CallSite;");
    //false);
    
    private static class AwaitSwitchEntry {
        
        private final Label resumeLabel;
        private final Label isDoneLabel;
        private final int key;
        private final ExtendedFrame frame;
        private final int index; // original instruction index
        private int[] stackToNewLocal;
        private int[] argumentToLocal;
        private int[] localToiArgument;
        
        private AwaitSwitchEntry(final ExtendedFrame frame,
                                 final Label resumeLabel) {
            this.key = 0;
            this.frame = frame;
            this.index = 0;
            this.isDoneLabel = null;
            this.resumeLabel = resumeLabel;
        }
        
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
    
    @FunctionalInterface
    interface WarningConsumer {
        
        void warn(final String fullyQualifiedClassName, final String methodSignature, final String message);
        
    }
    
    private final WarningConsumer warnConsumer;
    
    AsyncTransformer(final WarningConsumer warnConsumer) {
        this.warnConsumer = warnConsumer;
    }
    
    AsyncTransformer() {
        this((fqcn, method, message) -> System.err.println(fqcn + message));
    }
    
    /**
     * @param inputStream
     * @return the byte array containing the transformed bytecode, or null if no transformation was required
     * @throws IOException
     */
    byte[] instrument(final InputStream inputStream) throws IOException {
        try {
            return transform(new ClassReader(inputStream));
        } catch (final AnalyzerException e) {
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
            final Integer countOriginalUses = nameUseCount.get(original.name);
            nameUseCount.put(original.name, countOriginalUses == null ? 1 : countOriginalUses + 1);
            
            final Type retType = Type.getReturnType(original.desc);
            final boolean synthetic = (original.access & ACC_SYNTHETIC) == ACC_SYNTHETIC;
            // cater for
            // - Async<> returning methods
            // - Methods annotated with @Async
            // - synthetic methods (typically lambda bodies) we err on the lenient side and transform all lambdas that
            // use await(), even if they don't return an implementation of CompletionStage
            if (retType.equals(ASYNC_TYPE) || hasAnnotation(original, ASYNC_ANNOTATION_NAME) || (synthetic && retType.getSort() == Type.OBJECT)) {
                // transformation is idempotent, since it removed all usages of await() and does not change the method if await() is not used
                changed |= transformAsyncMethod(classNode, original, nameUseCount, retType.getInternalName());
            } else {
                checkSyncMethod(classNode, original);
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
    
    private boolean hasAnnotation(final MethodNode original, final String annotationName) {
        if (original.visibleAnnotations != null) {
            for (final AnnotationNode visibleAnnotation : original.visibleAnnotations) {
                if (visibleAnnotation.desc.equals("L" + annotationName + ";")) {
                    // skip already transformed
                    return true;
                }
            }
        }
        return false;
    }
    
    private static class CheckSyncMethodVisitor extends MethodVisitor {
        
        private final ClassNode classNode;
        
        private int lastLine;
        
        private CheckSyncMethodVisitor(final ClassNode classNode) {
            super(ASM5, null);
            this.classNode = classNode;
        }
        
        @Override
        public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc, final boolean itf) {
            if (isStaticAsyncMethod(opcode,
                owner,
                name,
                ASYNC_AWAIT_METHOD_NAME,
                ASYNC_AWAIT_RESULT_METHOD_NAME,
                ASYNC_AWAIT_EXCEPTIONS_METHOD_NAME)) {
                
                throw error(extractFullyQualifiedClassName(classNode),
                    lastLine,
                    "non-async methods should not call await() and awaitResult(). To block, use block()");
                
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }
        
        @Override
        public void visitLineNumber(final int line, final Label start) {
            lastLine = line;
            super.visitLineNumber(line, start);
        }
        
    }
    
    private static class TransformAsyncMethodVisitor extends MethodVisitor {
        
        private final MethodNode methodToAnnotate;
        private final ClassNode classNode;
        private final MethodNode original;
        private final String lambdaDesc;
        private final List<Argument> arguments;
        private final Handle handle;
        private final List<AwaitSwitchEntry> switchEntries;
        
        private int awaitIndex = 0;
        private int lastLine;
        private AwaitSwitchEntry awaitSwitchEntry;
        
        private TransformAsyncMethodVisitor(final MethodNode method,
                                            final MethodNode methodToAnnotate,
                                            final ClassNode classNode,
                                            final MethodNode original,
                                            final String lambdaDesc,
                                            final List<Argument> arguments,
                                            final Handle handle,
                                            final AwaitSwitchEntry entryPoint,
                                            final List<AwaitSwitchEntry> switchEntries) {
            super(ASM5, method);
            this.methodToAnnotate = methodToAnnotate;
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
            if (isStaticAsyncMethod(opcode, owner, name, ASYNC_AWAIT_METHOD_NAME, ASYNC_AWAIT_RESULT_METHOD_NAME, ASYNC_AWAIT_EXCEPTIONS_METHOD_NAME)) {
                awaitSwitchEntry = switchEntries.get(awaitIndex++);
                transformAwait(ASYNC_AWAIT_METHOD_NAME.equals(name),
                    Modifier.isStatic(original.access),
                    classNode,
                    original,
                    this,
                    awaitSwitchEntry,
                    lambdaDesc,
                    arguments,
                    handle,
                    lastLine);
                
            } else {
                super.visitMethodInsn(opcode, owner, name, desc, itf);
            }
        }
        
        @Override
        public void visitFrame(final int type, final int nLocal, final Object[] local, final int nStack, final Object[] stack) {
            // the use of EXPAND_FRAMES adds F_NEW which creates problems if not removed.
            super.visitFrame((type == F_NEW) ? F_FULL : type, nLocal, local, nStack, stack);
        }
        
        @Override
        public void visitLineNumber(final int line, final Label start) {
            lastLine = line;
            super.visitLineNumber(line, start);
        }
        
    }
    
    private void checkSyncMethod(final ClassNode classNode, final MethodNode original) {
        original.accept(new CheckSyncMethodVisitor(classNode));
    }
    
    private boolean transformAsyncMethod(final ClassNode classNode,
                                         final MethodNode original,
                                         final Map<String, Integer> nameUseCount,
                                         final String retType) throws AnalyzerException {
        final boolean isAbstract = Modifier.isAbstract(original.access);
        if (isAbstract) {
            return false;
        }
        
        final Analyzer<BasicValue> analyzer = new FrameAnalyzer();
        final Frame<BasicValue>[] frames = analyzer.analyze(classNode.name, original);
        final List<AwaitSwitchEntry> switchEntries = new ArrayList<>();
        final List<Label> switchLabels = new ArrayList<>();
        
        final AwaitSwitchEntry entryPoint;
        final boolean entryPointLabelFromOriginal;
        if (original.instructions.getFirst() instanceof LabelNode) {
            // if original method starts with a label, use that as the entry point label
            // we will add this ourselves instead of using the visitor
            entryPoint = new AwaitSwitchEntry((ExtendedFrame) frames[0], ((LabelNode) original.instructions.getFirst()).getLabel());
            entryPointLabelFromOriginal = true;
        } else {
            entryPoint = new AwaitSwitchEntry((ExtendedFrame) frames[0], new Label());
            entryPointLabelFromOriginal = false;
        }
        
        switchLabels.add(entryPoint.resumeLabel);
        
        {
            int ii = 0;
            int count = 0;
            int lastLine = 0;
            
            // create a switch entry for every await() calls - these will result in a continuation invocation
            for (AbstractInsnNode insn = original.instructions.getFirst(); insn != null; ii++, insn = insn.getNext()) {
                if ((insn instanceof MethodInsnNode
                    && isStaticAsyncMethod((MethodInsnNode) insn, ASYNC_AWAIT_METHOD_NAME, ASYNC_AWAIT_RESULT_METHOD_NAME, ASYNC_AWAIT_EXCEPTIONS_METHOD_NAME))) {
                    final AwaitSwitchEntry se = new AwaitSwitchEntry(++count, (ExtendedFrame) frames[ii], ii);
                    switchLabels.add(se.resumeLabel);
                    switchEntries.add(se);
                } else if ((insn instanceof InsnNode) && (insn.getOpcode() == POP)) {
                    // while we're here, also check if we have POP instructions where the operand stack head is an Async or a CompletionStage
                    // which indicates an abandoned stage, for which we warn
                    final BasicValue headOfOperandStack = frames[ii].getStack(frames[ii].getStackSize() - 1);
                    if (headOfOperandStack.getType().equals(ASYNC_TYPE) || headOfOperandStack.getType().equals(COMP_STAGE_TYPE)) {
                        warn(extractFullyQualifiedClassName(classNode),
                            extractMethodSignature(original),
                            lastLine,
                            "Asynchronous result is abandoned. This may cause execution to fork if not intended, so take care of shared mutable data in such cases");
                    }
                } else if (insn instanceof LineNumberNode) {
                    lastLine = ((LineNumberNode) insn).line;
                }
            }
        }
        
        if (switchEntries.isEmpty()) {
            return false;
        }
        
        final boolean isStatic = Modifier.isStatic(original.access);
        
        final List<Argument> arguments = new ArrayList<>();
        
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
            // skipping the last one which will be the Async which we will get back in doneCompose
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
        final Argument stageArgument =
            mapLocalToLambdaArgument(isStatic, original, null, arguments, 0, new BasicValue(COMP_STAGE_TYPE));
        stateArgument.name = "async$state";
        stageArgument.name = "async$stage";
        final Object[] defaultFrame;
        {
            final List<Object> frame = new ArrayList<>(arguments.size() + (isStatic ? 0 : 1));
            if (!isStatic) {
                frame.add(classNode.name);
            }
            
            arguments
                .stream()
                .map(a -> toFrameType(a.value.getType()))
                .forEach(frame::add);
            defaultFrame = frame.toArray();
        }
        final Type[] typeArguments = arguments
            .stream()
            .map(a -> a.value.getType())
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
        
        final MethodNode replacement = new MethodNode(original.access,
            original.name,
            original.desc,
            original.signature,
            original.exceptions.toArray(new String[0]));
        
        final String continuationName = "continuation$" + replacement.name;
        final Integer countUses = nameUseCount.get(continuationName);
        nameUseCount.put(continuationName, countUses == null ? 1 : countUses + 1);
        
        final MethodNode continuation = new MethodNode(
            ACC_SYNTHETIC | ACC_PRIVATE | (original.access & ~ACC_PUBLIC & ~ACC_PROTECTED),
            continuationName + (countUses == null ? "" : "$" + countUses),
            Type.getMethodDescriptor(COMP_STAGE_TYPE, typeArguments),
            null,
            null);
        
        replacement.visitCode();
        continuation.visitCode();
        
        // get index for switch
        continuation.visitVarInsn(ILOAD, stateArgument.iArgumentLocal);
        
        final Label defaultLabel = new Label();
        continuation.visitTableSwitchInsn(0, switchLabels.size() - 1, defaultLabel, switchLabels.toArray(new Label[0]));
        
        // original entry point
        continuation.visitLabel(entryPoint.resumeLabel);
        if (entryPointLabelFromOriginal) {
            // remove this instruction as we already added the label
            original.instructions.remove(original.instructions.getFirst());
            if (original.instructions.getFirst() instanceof LineNumberNode) {
                // if we are reusing the entry point label from the original method,
                // also add the line number and remove it from the method
                final LineNumberNode lineNumberNode = (LineNumberNode) original.instructions.getFirst();
                continuation.visitLineNumber(lineNumberNode.line, entryPoint.resumeLabel);
                original.instructions.remove(original.instructions.getFirst());
            }
            if (!(original.instructions.getFirst() instanceof FrameNode)) {
                // we add a full frame if there isn't one corresponding to the reused label
                // otherwise use the frame from the original method
                continuation.visitFrame(F_FULL, defaultFrame.length, defaultFrame, 0, new Object[0]);
            }
        } else {
            continuation.visitFrame(F_FULL, defaultFrame.length, defaultFrame, 0, new Object[0]);
        }
        
        // transform the original code to the continuation method
        // (this will use the switch labels to allow jumping right after the await calls on future completion)
        original.accept(new TransformAsyncMethodVisitor(continuation,
            replacement,
            classNode,
            original,
            lambdaDesc,
            arguments,
            new Handle(isStatic ? H_INVOKESTATIC : H_INVOKESPECIAL, classNode.name, continuation.name, continuation.desc), //, false),
            entryPoint,
            switchEntries));
        
        // add switch entries for the continuation state machine
        for (final AwaitSwitchEntry se : switchEntries) {
            // code: resumeLabel:
            continuation.visitLabel(se.resumeLabel);
            continuation.visitFrame(F_FULL, defaultFrame.length, defaultFrame, 0, new Object[0]);
            
            // code: restoreStack;
            // code: restoreLocals;
            restoreStackAndLocals(isStatic, classNode, original, continuation, se, arguments);
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
        // adding the continuation method
        continuation.accept(classNode);
        
        // the async method delegates to the continuation, catching any exceptions
        // any exceptions thrown by the continuation method following await calls are
        // handled by the enclosing completion stages
        pushInitial(isStatic, classNode, replacement, arguments);
        replacement.visitMethodInsn(isStatic ? INVOKESTATIC : INVOKESPECIAL, classNode.name, continuation.name, continuation.desc, false);
        switch (retType) {
            case ASYNC_NAME:
                // convert CompletionStage to Async
                replacement.visitMethodInsn(INVOKESTATIC, ASYNC_NAME, ASYNC_FROM_NAME, ASYNC_FROM_DESC, true);
                break;
            case COMP_STAGE_NAME:
                // nothing to do, already a completion stage
                break;
            case COMP_FUTURE_NAME:
                // call toCompletableFuture();
                replacement.visitMethodInsn(INVOKEINTERFACE, COMP_STAGE_NAME, COMP_STAGE_TO_COMPLETABLE_FUTURE_NAME, COMP_STAGE_TO_COMPLETABLE_FUTURE_DESC, true);
                break;
            default:
                // call static fromCompletionStage() method on return type
                replacement.visitMethodInsn(INVOKESTATIC, retType, STATIC_FROM_COMPLETION_STAGE_NAME, "(L" + COMP_STAGE_NAME + ";)L" + retType + ";", false);
        }
        replacement.visitInsn(ARETURN);
        replacement.visitEnd();
        
        // replace the method
        classNode.methods.remove(original);
        replacement.accept(classNode);
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
        
        // original: Async.await(async) / Async.awaitResult(async)
        
        // turns into:
        
        // code: if (!CompletionStageUtil.isDone(async)) {
        // code: saveStack to new locals
        // code: push lambda parameters (locals and stack)
        // code: return CompletionStageUtil.doneCompose(async, async -> continuation$method(arguments, state, async));
        // code: }
        // code: isDoneLabel:
        // code: CompletionStageUtil.get(async); -> causes exception to be thrown
        // (only for awaitResult, restore async on stack)
        
        // and this is added to the switch
        // code: resumeLabel:
        // code: restoreStack;
        // code: restoreLocals;
        // code: jump isDoneLabel:
        
        // current stack head is the async reference passed to await()
        // stack: { ... async }
        mv.visitInsn(DUP);
        // stack: { ... async async }
        mv.visitMethodInsn(INVOKESTATIC, COMP_STAGE_UTIL_NAME, COMP_STAGE_UTIL_IS_DONE_NAME, COMP_STAGE_UTIL_IS_DONE_DESC, false);
        // stack: { ... async is_done_result }
        // code: jump if true (non 0) to isDoneLabel:
        mv.visitJumpInsn(IFNE, awaitSwitchEntry.isDoneLabel);
        
        // code: saveStack to new state
        // code: push the async
        // code: push all lambda parameters
        // code: create the lambda
        
        if (awaitSwitchEntry.stackToNewLocal.length > 0) {
            // clears the stack and leaves promise in the top (by using the lambda parameter as temporary variable)
            final int lambdaParamIndex = lambdaArguments.get(lambdaArguments.size() - 1).iArgumentLocal;
            // stack: { ... async }
            mv.visitVarInsn(ASTORE, lambdaParamIndex);
            // stack: { ... }
            saveStack(mv, awaitSwitchEntry);
            // stack: { }
            mv.visitVarInsn(ALOAD, lambdaParamIndex);
            // stack: { async }
        }
        
        // code: return CompletionStageUtil.doneCompose(async -> _func([arguments-2, state], async));
        // where [arguments-2, state] are bound to the lambda
        
        pushArguments(isStatic, classNode, mv, awaitSwitchEntry, lambdaArguments, lineNumber);
        // stack: { async ...arguments-2... }
        mv.visitIntInsn(SIPUSH, awaitSwitchEntry.key);
        // stack: { async ...arguments-2... state }
        
        mv.visitInvokeDynamicInsn("apply",
            lambdaDesc,
            LAMBDAMETAFACTORY_HANDLE,
            Type.getType("(L" + OBJECT_NAME + ";)L" + OBJECT_NAME + ";"),
            handle,
            Type.getType("(L" + COMP_STAGE_NAME + ";)L" + COMP_STAGE_NAME + ";"));
        
        // stack: { async lambda_function_ref }
        mv.visitMethodInsn(INVOKESTATIC, COMP_STAGE_UTIL_NAME, COMP_STAGE_UTIL_DONE_COMPOSE_NAME, COMP_STAGE_UTIL_DONE_COMPOSE_DESC, false);
        
        // stack: { composed_async }
        
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
                    throw error(extractFullyQualifiedClassName(classNode),
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
            // leave a reference to async on the stack, as we only call get() below to throw exception in case of rejection
            mv.visitInsn(DUP);
            // {  ... async async }
        }
        
        // {  ... async? async }
        mv.visitMethodInsn(INVOKESTATIC, COMP_STAGE_UTIL_NAME, COMP_STAGE_UTIL_GET_NAME, COMP_STAGE_UTIL_GET_DESC, false);
        // {  ... async? result } or throw exception.
        // For first iteration, replacement method catches exception and rejects the async
        // For continuations, an exception rejects the composed async
        
        if (!asyncUnwrapResult) {
            // remove the result
            mv.visitInsn(POP);
            // {  ... async }
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
        
        // replace the last type on the stack with CompletionStage,
        // since we can have any implementation of this interface here
        stack[nStack - 1] = toFrameType(COMP_STAGE_TYPE);
        
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
                                    final List<Argument> lambdaArguments) {
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
                pushDefault(classNode, method, argument.value, -1);
            }
        }
        method.visitInsn(ICONST_0); // initial state
        method.visitInsn(ACONST_NULL); // null stage
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
                throw error(extractFullyQualifiedClassName(classNode), lineNumber, "Unknown type: " + value.getType().getSort());
        }
    }
    
    private static void restoreStackAndLocals(final boolean isStatic,
                                              final ClassNode classNode,
                                              final MethodNode original,
                                              final MethodVisitor mv,
                                              final AwaitSwitchEntry se,
                                              final List<Argument> lambdaArguments) {
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
                pushDefault(classNode, mv, value, -1);
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
                pushDefault(classNode, mv, value, -1);
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
                throw error(extractFullyQualifiedClassName(classNode),
                    -1,
                    "Error restoring monitors in synchronized method. monitorLocal=%d, at %s.%s",
                    monitorLocal,
                    classNode.name,
                    original.name);
            }
        }
    }
    
    private static String extractFullyQualifiedClassName(final ClassNode classNode) {
        return classNode.name.replace('/', '.');
    }
    
    private String extractMethodSignature(final MethodNode original) {
        final StringBuilder sig = new StringBuilder(original.name).append('(');
        final String signature = original.signature != null ? original.signature : original.desc;
        if (signature.charAt(1) == ')') {
            sig.append(')');
        } else {
            boolean done = false;
            boolean type = false;
            int genericDepth = 0;
            String arraySuffix = "";
            for (int i = 1; !done;) {
                if (type) {
                    sig.append(",");
                }
                
                type = true;
                switch (signature.charAt(i)) {
                    case 'Z':
                        sig.append("boolean");
                        break;
                    case 'B':
                        sig.append("byte");
                        break;
                    case 'C':
                        sig.append("char");
                        break;
                    case 'S':
                        sig.append("short");
                        break;
                    case 'I':
                        sig.append("int");
                        break;
                    case 'J':
                        sig.append("long");
                        break;
                    case 'F':
                        sig.append("float");
                        break;
                    case 'D':
                        sig.append("double");
                        break;
                    case 'L':
                        type = false;
                        final int semiIndex = signature.indexOf(';', i);
                        final int angleIndex = signature.indexOf('<', i);
                        final int end = angleIndex > -1 ? Math.min(semiIndex, angleIndex) : semiIndex;
                        sig.append(signature.substring(i + 1, end).replace('/', '.').replace('$', '.'));
                        i = end - 1;
                        break;
                    case ';':
                        break;
                    case '<':
                        type = false;
                        genericDepth++;
                        sig.append('<');
                        break;
                    case '>':
                        type = false;
                        genericDepth--;
                        sig.append('>');
                        break;
                    case '[':
                        type = false;
                        arraySuffix += "[]";
                        break;
                }
                
                if (type) {
                    sig.append(arraySuffix);
                }
                if (genericDepth > 0) {
                    type = false;
                }
                
                if (signature.charAt(++i) == ')') {
                    sig.append(')');
                    done = true;
                }
            }
        }
        return sig.toString();
    }
    
    private void warn(final String fqcn, final String method, final int lineNumber, final String format, final Object... args) {
        final String message = fqcn + ((lineNumber > 0) ? ":" + lineNumber + " " : " ") + String.format(format, args);
        warnConsumer.warn(fqcn, method, message);
    }
    
    private static AsyncTransformerException error(final String fqcn, final int lineNumber, final String format, final Object... args) {
        final String message = fqcn + ((lineNumber > 0) ? ":" + lineNumber + " " : " ") + String.format(format, args);
        return new AsyncTransformerException(message);
    }
    
}
