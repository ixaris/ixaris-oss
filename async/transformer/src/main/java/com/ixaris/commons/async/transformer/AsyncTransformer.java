package com.ixaris.commons.async.transformer;

import static org.objectweb.asm.Opcodes.ACC_PRIVATE;
import static org.objectweb.asm.Opcodes.ACC_PROTECTED;
import static org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static org.objectweb.asm.Opcodes.ACC_SYNTHETIC;
import static org.objectweb.asm.Opcodes.ACONST_NULL;
import static org.objectweb.asm.Opcodes.ALOAD;
import static org.objectweb.asm.Opcodes.ARETURN;
import static org.objectweb.asm.Opcodes.ASM8;
import static org.objectweb.asm.Opcodes.ASTORE;
import static org.objectweb.asm.Opcodes.ATHROW;
import static org.objectweb.asm.Opcodes.DCONST_0;
import static org.objectweb.asm.Opcodes.DOUBLE;
import static org.objectweb.asm.Opcodes.DUP;
import static org.objectweb.asm.Opcodes.FCONST_0;
import static org.objectweb.asm.Opcodes.FLOAT;
import static org.objectweb.asm.Opcodes.F_FULL;
import static org.objectweb.asm.Opcodes.F_NEW;
import static org.objectweb.asm.Opcodes.GOTO;
import static org.objectweb.asm.Opcodes.H_INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.H_INVOKESTATIC;
import static org.objectweb.asm.Opcodes.ICONST_0;
import static org.objectweb.asm.Opcodes.IFNE;
import static org.objectweb.asm.Opcodes.ILOAD;
import static org.objectweb.asm.Opcodes.INTEGER;
import static org.objectweb.asm.Opcodes.INVOKEINTERFACE;
import static org.objectweb.asm.Opcodes.INVOKESPECIAL;
import static org.objectweb.asm.Opcodes.INVOKESTATIC;
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
import java.util.Optional;
import java.util.Set;
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
import org.objectweb.asm.tree.AnnotationNode;
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
import org.objectweb.asm.tree.analysis.AnalyzerException;
import org.objectweb.asm.tree.analysis.BasicValue;
import org.objectweb.asm.tree.analysis.Frame;
import org.objectweb.asm.tree.analysis.Value;

import com.ixaris.commons.async.transformer.FrameAnalyzer.ExtendedFrame;
import com.ixaris.commons.async.transformer.FrameAnalyzer.ExtendedValue;

/**
 * This transformer transforms seemingly blocking code, specifically using Async.await() to await future results, into asynchronous code using
 * stackless continuations.
 *
 * @author brian.vella
 */
final class AsyncTransformer {
    
    private static final String OBJECT_NAME = "java/lang/Object";
    private static final String FUNCTION_THROWS_NAME = "com/ixaris/commons/misc/lib/function/FunctionThrows";
    
    private static final String COMP_FUTURE_NAME = "java/util/concurrent/CompletableFuture";
    
    private static final String COMP_STAGE_NAME = "java/util/concurrent/CompletionStage";
    private static final Type COMP_STAGE_TYPE = Type.getObjectType(COMP_STAGE_NAME);
    private static final String COMP_STAGE_TO_COMPLETABLE_FUTURE_NAME = "toCompletableFuture";
    private static final String COMP_STAGE_TO_COMPLETABLE_FUTURE_DESC = String.format("()L%s;", COMP_FUTURE_NAME);
    
    private static final String ASYNC_NAME = "com/ixaris/commons/async/lib/Async";
    private static final Type ASYNC_TYPE = Type.getObjectType(ASYNC_NAME);
    private static final String ASYNC_FROM_NAME = "from";
    private static final String ASYNC_FROM_DESC = String.format("(L%s;)L%s;", COMP_STAGE_NAME, ASYNC_NAME);
    private static final String ASYNC_AWAIT_METHOD_NAME = "await";
    private static final String ASYNC_AWAIT_EXCEPTIONS_METHOD_NAME = "awaitExceptions";
    private static final String ASYNC_AWAIT_RESULT_METHOD_NAME = "awaitResult"; // TODO deprecated, eventually remove
    private static final Set<String> ASYNC_AWAIT_METHODS = Stream.of(ASYNC_AWAIT_METHOD_NAME,
        ASYNC_AWAIT_EXCEPTIONS_METHOD_NAME,
        ASYNC_AWAIT_RESULT_METHOD_NAME)
        .collect(Collectors.toSet());
    
    private static final String ASYNC_ANNOTATION_NAME = "com/ixaris/commons/async/lib/annotation/Async";
    
    private static final String COMP_STAGE_UTIL_NAME = "com/ixaris/commons/async/lib/CompletionStageUtil";
    private static final String COMP_STAGE_UTIL_IS_DONE_NAME = "isDone";
    private static final String COMP_STAGE_UTIL_IS_DONE_DESC = String.format("(L%s;)Z", COMP_STAGE_NAME);
    private static final String COMP_STAGE_UTIL_GET_NAME = "get";
    private static final String COMP_STAGE_UTIL_GET_DESC = String.format("(L%s;)L%s;", COMP_STAGE_NAME, OBJECT_NAME);
    private static final String COMP_STAGE_UTIL_DONE_COMPOSE_NAME = "doneCompose";
    private static final String COMP_STAGE_UTIL_DONE_COMPOSE_DESC = String.format("(L%s;L%s;)L%s;", COMP_STAGE_NAME, FUNCTION_THROWS_NAME, COMP_STAGE_NAME);
    
    private static final String STATIC_FROM_COMPLETION_STAGE_NAME = "fromCompletionStage";
    
    private static final Type NULL_TYPE = Type.getObjectType("null");
    
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
            + "Ljava/lang/invoke/CallSite;",
        false);
    
    private static class AwaitSwitchEntry {
        
        private final Label resumeLabel;
        private final Label isDoneLabel;
        private final int key;
        private final ExtendedFrame frame;
        private final int index; // original instruction index
        
        private int[] stackToNewLocal;
        private int[] argumentToLocal;
        private int[] localToOffset;
        
        private AwaitSwitchEntry(final ExtendedFrame frame, final Label resumeLabel) {
            this.key = 0;
            this.frame = frame;
            this.index = 0;
            this.isDoneLabel = null;
            this.resumeLabel = resumeLabel;
        }
        
        private AwaitSwitchEntry(final int key, final ExtendedFrame frame, final int index) {
            this.key = key;
            this.frame = frame;
            this.index = index;
            this.isDoneLabel = new Label();
            this.resumeLabel = new Label();
        }
        
    }
    
    private static class Argument {
        
        private final BasicValue value;
        private final String name;
        private final int argumentOffset;
        
        private int tmpLocalMapping = -1; // -1 means not used, -2 means used, >= 0 is local mapping
        
        private Argument(final BasicValue value, final String name, final int argumentOffset) {
            this.value = value;
            this.name = name;
            this.argumentOffset = argumentOffset;
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
     * Return the byte array containing the transformed bytecode, or null if no transformation was required
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
     * @param cr the class reader for this class
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
            nameUseCount.compute(original.name, (k, v) -> (v == null) ? 0 : v + 1);
        }
        
        for (final MethodNode original : new ArrayList<>(classNode.methods)) {
            final Type retType = Type.getReturnType(original.desc);
            final boolean isSynthetic = (original.access & ACC_SYNTHETIC) == ACC_SYNTHETIC;
            // Should transform
            // - Async<> returning methods
            // - Methods annotated with @Async
            //   (we cannot determine whether they return an implementation of CompletionStage without complicating
            //   the transformer excessively)
            // - synthetic methods
            //   (These are typically lambda bodies; we err on the lenient side and transform all lambdas that use
            //   await(), as it is even harder to determine whether they return an implementation of CompletionStage,
            //   since the @FunctionalInterface method might declare a more generic contravariant return type)
            final boolean shouldTransform;
            if (retType.equals(ASYNC_TYPE)) {
                shouldTransform = true;
            } else {
                shouldTransform = (retType.getSort() == Type.OBJECT) && (hasAnnotation(original) || isSynthetic);
            }
            if (shouldTransform) {
                final boolean isAbstract = Modifier.isAbstract(original.access);
                // transformation is idempotent; either transform and remove all calls to await() or leave method intact
                changed |= !isAbstract && transformAsyncMethod(classNode, original, nameUseCount, retType.getInternalName());
            } else {
                checkSyncMethod(classNode, original);
            }
        }
        
        // no changes.
        if (!changed) {
            return null;
        }
        
        // avoid using COMPUTE_FRAMES as it mangles, among others, stack location for multi catch, turning it
        // into java.lang.Object and causes class verification to fail
        final ClassWriter cw = new ClassWriter(ClassWriter.COMPUTE_MAXS) {
            
            @Override
            protected String getCommonSuperClass(final String type1, final String type2) {
                // this is only called if COMPUTE_FRAMES is enabled
                
                // implementing this properly would require loading information for type1 and type2 from the class path,
                // which is not feasible for stateless transformation (i.e. transform a class without creating a
                // classpath with all its direct and transitive dependencies) which is required for transformation
                // during compilation as an annotation processor)
                return type1.equals(type2) ? type1 : OBJECT_NAME;
            }
        };
        
        classNode.accept(cw);
        return cw.toByteArray();
    }
    
    private boolean hasAnnotation(final MethodNode original) {
        if (original.visibleAnnotations != null) {
            for (final AnnotationNode visibleAnnotation : original.visibleAnnotations) {
                if (visibleAnnotation.desc.equals("L" + AsyncTransformer.ASYNC_ANNOTATION_NAME + ";")) {
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
            super(ASM8, null);
            this.classNode = classNode;
        }
        
        @Override
        public void visitMethodInsn(final int opcode, final String owner, final String name, final String desc, final boolean itf) {
            if (isOneOfAwaitMethods(opcode, owner, name)) {
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
        private final String continuationLambdaDesc;
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
                                            final String continuationLambdaDesc,
                                            final List<Argument> arguments,
                                            final Handle handle,
                                            final List<AwaitSwitchEntry> switchEntries,
                                            final AwaitSwitchEntry entryPoint) {
            super(ASM8, method);
            this.methodToAnnotate = methodToAnnotate;
            this.classNode = classNode;
            this.original = original;
            this.continuationLambdaDesc = continuationLambdaDesc;
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
            if (isOneOfAwaitMethods(opcode, owner, name)) {
                awaitSwitchEntry = switchEntries.get(awaitIndex++);
                transformAwait(ASYNC_AWAIT_METHOD_NAME.equals(name),
                    classNode,
                    original,
                    this,
                    awaitSwitchEntry,
                    continuationLambdaDesc,
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
        final Frame<BasicValue>[] frames = new FrameAnalyzer().analyze(classNode.name, original);
        
        final List<AwaitSwitchEntry> switchEntries = determineSwitchEntriesForAwaitCalls(classNode, original, frames);
        if (switchEntries.isEmpty()) {
            // no await() calls, so no transformation to do
            return false;
        }
        
        final AwaitSwitchEntry entryPoint;
        final boolean entryPointLabelFromOriginal;
        if (original.instructions.getFirst() instanceof LabelNode) {
            // if original method starts with a label, use that as the entry point label
            // we will add this ourselves instead of using the visitor
            final Label entryPointLabel = ((LabelNode) original.instructions.getFirst()).getLabel();
            entryPoint = new AwaitSwitchEntry((ExtendedFrame) frames[0], entryPointLabel);
            entryPointLabelFromOriginal = true;
        } else {
            entryPoint = new AwaitSwitchEntry((ExtendedFrame) frames[0], new Label());
            entryPointLabelFromOriginal = false;
        }
        
        final List<Argument> arguments = determineArgumentsAndFixObjectInitialization(original, frames, switchEntries);
        final Type[] typeArguments = arguments.stream().map(a -> a.value.getType()).toArray(Type[]::new);
        
        final boolean isStatic = Modifier.isStatic(original.access);
        final Object[] continuationFrame = determineContinuationFrame(isStatic, classNode, arguments);
        final String continuationLambdaDesc = determineContinuationLambdaDesc(isStatic, classNode, typeArguments);
        
        final MethodNode replacement = new MethodNode(original.access,
            original.name,
            original.desc,
            original.signature,
            original.exceptions.toArray(new String[0]));
        
        final String continuationName = "continuation$" + replacement.name;
        final int continuationNameCount = nameUseCount.compute(continuationName, (k, v) -> v == null ? 0 : v + 1);
        final MethodNode continuation = new MethodNode(ACC_SYNTHETIC | ACC_PRIVATE | (original.access & ~ACC_PUBLIC & ~ACC_PROTECTED),
            continuationName + (continuationNameCount == 0 ? "" : "$" + continuationNameCount),
            Type.getMethodDescriptor(COMP_STAGE_TYPE, typeArguments),
            null,
            null);
        
        replacement.visitCode();
        continuation.visitCode();
        
        // get index for switch (argument before last)
        continuation.visitVarInsn(ILOAD, arguments.get(arguments.size() - 2).argumentOffset);
        
        final Label defaultLabel = new Label();
        continuation.visitTableSwitchInsn(0,
            switchEntries.size(),
            defaultLabel,
            Stream.concat(Stream.of(entryPoint.resumeLabel), switchEntries.stream().map(e -> e.resumeLabel)).toArray(Label[]::new));
        
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
                continuation.visitFrame(F_FULL, continuationFrame.length, continuationFrame, 0, new Object[0]);
            }
        } else {
            continuation.visitFrame(F_FULL, continuationFrame.length, continuationFrame, 0, new Object[0]);
        }
        
        // transform the original code to the continuation method
        // (this will use the switch labels to allow jumping right after the await calls on future completion)
        original.accept(new TransformAsyncMethodVisitor(continuation,
            replacement,
            classNode,
            original,
            continuationLambdaDesc,
            arguments,
            new Handle(isStatic ? H_INVOKESTATIC : H_INVOKESPECIAL, classNode.name, continuation.name, continuation.desc, false),
            switchEntries,
            entryPoint));
        
        // add switch entries for the continuation state machine
        for (final AwaitSwitchEntry se : switchEntries) {
            // code: resumeLabel:
            continuation.visitLabel(se.resumeLabel);
            continuation.visitFrame(F_FULL, continuationFrame.length, continuationFrame, 0, new Object[0]);
            
            // code: restoreStack;
            // code: restoreLocals;
            restoreStackAndLocals(classNode, original, continuation, se, arguments);
            continuation.visitJumpInsn(GOTO, se.isDoneLabel);
        }
        
        // last switch case
        continuation.visitLabel(defaultLabel);
        continuation.visitFrame(F_FULL, continuationFrame.length, continuationFrame, 0, new Object[0]);
        continuation.visitTypeInsn(NEW, "java/lang/IllegalArgumentException");
        continuation.visitInsn(DUP);
        continuation.visitMethodInsn(INVOKESPECIAL, "java/lang/IllegalArgumentException", "<init>", "()V", false);
        continuation.visitInsn(ATHROW);
        continuation.visitEnd();
        // continuation.maxStack = Math.max(16, continuation.maxStack + 16);
        // continuation.maxLocals = Math.max(16, continuation.maxLocals + 16);
        // adding the continuation method
        continuation.accept(classNode);
        
        // the async method delegates to the continuation, converting the returned CompletionStage
        pushInitial(classNode, replacement, arguments);
        
        final boolean isInterface = Modifier.isInterface(classNode.access);
        // jave 8 uses INVOKESPECIAL for private interface methods, while java 9+ uses INVOKEINTERFACE
        replacement.visitMethodInsn(isStatic ? INVOKESTATIC : ((classNode.version <= 52) ? INVOKESPECIAL : (isInterface ? INVOKEINTERFACE : INVOKESPECIAL)),
            classNode.name,
            continuation.name,
            continuation.desc,
            isInterface);
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
                replacement.visitMethodInsn(INVOKEINTERFACE,
                    COMP_STAGE_NAME,
                    COMP_STAGE_TO_COMPLETABLE_FUTURE_NAME,
                    COMP_STAGE_TO_COMPLETABLE_FUTURE_DESC,
                    true);
                break;
            default:
                // call static fromCompletionStage() method on return type
                replacement.visitMethodInsn(INVOKESTATIC,
                    retType,
                    STATIC_FROM_COMPLETION_STAGE_NAME,
                    "(L" + COMP_STAGE_NAME + ";)L" + retType + ";",
                    false);
        }
        replacement.visitInsn(ARETURN);
        replacement.visitEnd();
        
        // replace the method
        classNode.methods.remove(original);
        replacement.accept(classNode);
        return true;
    }
    
    private List<Argument> determineArgumentsAndFixObjectInitialization(final MethodNode original,
                                                                        final Frame<BasicValue>[] frames,
                                                                        final List<AwaitSwitchEntry> switchEntries) {
        final boolean isStatic = Modifier.isStatic(original.access);
        final List<Argument> arguments = new ArrayList<>();
        
        // compute variable mapping
        // map stack->new locals
        // map (locals + new locals) -> continuation lambda arguments
        // result:
        // stackToNewLocal mapping (stack_index, local_index)
        // - original stack to new local
        // - as an int array: int[stack_index] = local_index
        // localToArgumentMapping (parameter, local_index)
        // - in this order, the push order
        // - as an int array: int[parameter_index] = local_index
        int newMaxLocals = 0;
        final Set<AbstractInsnNode> uninitializedObjects = new HashSet<>();
        
        for (final AwaitSwitchEntry se : switchEntries) {
            // clear the used state
            arguments.forEach(arg -> arg.tmpLocalMapping = -1);
            se.stackToNewLocal = new int[se.frame.getStackSize()];
            Arrays.fill(se.stackToNewLocal, -1);
            int iNewLocal = original.maxLocals;
            for (int j = 0; j < se.frame.getStackSize(); j++) {
                final BasicValue value = se.frame.getStack(j);
                if (value != null) {
                    if (isUninitialized(value)) {
                        uninitializedObjects.add(((ExtendedValue) value).insnNode);
                    } else if ((value.getType() != null) && !NULL_TYPE.equals(value.getType())) {
                        se.stackToNewLocal[j] = iNewLocal;
                        iNewLocal += valueSize(se.frame.getStack(j));
                    }
                } else {
                    se.stackToNewLocal[j] = -1;
                }
            }
            
            // maps the locals to arguments
            for (int local = isStatic ? 0 : 1; local < se.frame.getLocals(); local += valueSize(se.frame.getLocal(local))) {
                final BasicValue value = se.frame.getLocal(local);
                if (value != null) {
                    // marks uninitialized objects
                    if (isUninitialized(value)) {
                        uninitializedObjects.add(((ExtendedValue) value).insnNode);
                    } else if ((value.getType() != null) && !NULL_TYPE.equals(value.getType())) {
                        mapLocalToLambdaArgument(original, se, arguments, local, value);
                    }
                }
            }
            // maps the stack locals to arguments
            // skipping the last one which will be the stage which we will get back in doneCompose
            for (int j = 0; j < se.frame.getStackSize() - 1; j++) {
                final int local = se.stackToNewLocal[j];
                if (local >= 0) {
                    final BasicValue value = se.frame.getStack(j);
                    if ((value.getType() != null) && !NULL_TYPE.equals(value.getType())) {
                        mapLocalToLambdaArgument(original, se, arguments, local, value);
                    }
                }
            }
            
            // extract local-to-argument mapping
            newMaxLocals = Math.max(iNewLocal, newMaxLocals);
            se.localToOffset = new int[newMaxLocals];
            Arrays.fill(se.localToOffset, -1);
            se.argumentToLocal = new int[arguments.size()];
            for (int j = 0; j < arguments.size(); j++) {
                se.argumentToLocal[j] = arguments.get(j).tmpLocalMapping;
                if (se.argumentToLocal[j] >= 0) {
                    se.localToOffset[se.argumentToLocal[j]] = arguments.get(j).argumentOffset;
                }
            }
        }
        // only replaces object initialization
        // if uninitialized objects are present in the stack during a await call.
        if (uninitializedObjects.size() > 0) {
            replaceObjectInitialization(original, frames, uninitializedObjects);
        }
        original.maxLocals = Math.max(original.maxLocals, newMaxLocals);
        
        addArgument("async$index", BasicValue.INT_VALUE, isStatic, arguments);
        addArgument("async$stage", new BasicValue(COMP_STAGE_TYPE), isStatic, arguments);
        
        arguments.forEach(p -> p.tmpLocalMapping = -2);
        return arguments;
    }
    
    private List<AwaitSwitchEntry> determineSwitchEntriesForAwaitCalls(final ClassNode classNode,
                                                                       final MethodNode original,
                                                                       final Frame<BasicValue>[] frames) {
        final List<AwaitSwitchEntry> switchEntries = new ArrayList<>();
        // create a switch entry for every await() calls - these will result in a continuation invocation
        AbstractInsnNode insn = original.instructions.getFirst();
        for (int i = 0, count = 0, lastLine = 0; insn != null; i++, insn = insn.getNext()) {
            if ((insn instanceof MethodInsnNode) && isOneOfAwaitMethods((MethodInsnNode) insn)) {
                final AwaitSwitchEntry se = new AwaitSwitchEntry(++count, (ExtendedFrame) frames[i], i);
                switchEntries.add(se);
            } else if ((insn instanceof InsnNode) && (insn.getOpcode() == POP)) {
                // while we're here, also check if we have POP instructions where the operand stack head is
                // an Async or a CompletionStage which indicates an abandoned stage, for which we warn
                final Type typeAtTopOfStack = frames[i].getStack(frames[i].getStackSize() - 1).getType();
                if (typeAtTopOfStack.equals(ASYNC_TYPE) || typeAtTopOfStack.equals(COMP_STAGE_TYPE)) {
                    warn(extractFullyQualifiedClassName(classNode),
                        extractMethodSignature(original),
                        lastLine,
                        "Asynchronous result is abandoned, which may cause execution to fork");
                }
            } else if (insn instanceof LineNumberNode) {
                lastLine = ((LineNumberNode) insn).line;
            }
        }
        return switchEntries;
    }
    
    private Object[] determineContinuationFrame(final boolean isStatic, final ClassNode classNode, final List<Argument> arguments) {
        final Object[] frame = new Object[arguments.size() + (isStatic ? 0 : 1)];
        int i = 0;
        if (!isStatic) {
            frame[i++] = classNode.name;
        }
        for (final Argument argument : arguments) {
            frame[i++] = toFrameType(argument.value.getType());
        }
        return frame;
    }
    
    private String determineContinuationLambdaDesc(final boolean isStatic, final ClassNode classNode, final Type[] typeArguments) {
        final Type[] lambdaArguments;
        if (isStatic) {
            lambdaArguments = Arrays.copyOf(typeArguments, typeArguments.length - 1);
        } else {
            // lambda description needs an extra parameter if not static, representing "this"
            lambdaArguments = new Type[typeArguments.length];
            lambdaArguments[0] = Type.getObjectType(classNode.name);
            System.arraycopy(typeArguments, 0, lambdaArguments, 1, typeArguments.length - 1);
        }
        
        return Type.getMethodDescriptor(Type.getType("L" + FUNCTION_THROWS_NAME + ";"), lambdaArguments);
    }
    
    private void mapLocalToLambdaArgument(final MethodNode method,
                                          final AwaitSwitchEntry se,
                                          final List<Argument> arguments,
                                          final int local,
                                          final BasicValue value) {
        if (NULL_TYPE.equals(value.getType())) {
            return;
        }
        
        final String name;
        if (local < method.maxLocals) {
            name = guessName(method, se, local);
        } else {
            name = "_stack$" + (local - method.maxLocals);
        }
        
        // first try to match by name and type
        Optional<Argument> oargument = arguments.stream()
            .filter(x -> x.tmpLocalMapping == -1 && x.value.equals(value) && x.name.equals(name))
            .findFirst();
        
        if (!oargument.isPresent()) {
            // next try to match by type only
            oargument = arguments.stream().filter(x -> x.tmpLocalMapping == -1 && x.value.equals(value)).findFirst();
        }
        
        // if no match, create a new one
        final Argument argument = oargument.orElseGet(() -> addArgument(name, value, Modifier.isStatic(method.access), arguments));
        argument.tmpLocalMapping = local;
    }
    
    private static Argument addArgument(final String name, final BasicValue value, final boolean isStatic, final List<Argument> arguments) {
        final int argOffset;
        if (arguments.isEmpty()) {
            argOffset = isStatic ? 0 : 1;
        } else {
            final Argument lastArg = arguments.get(arguments.size() - 1);
            argOffset = lastArg.argumentOffset + lastArg.value.getSize();
        }
        final Argument argument = new Argument(value, name, argOffset);
        arguments.add(argument);
        return argument;
    }
    
    private static String guessName(final MethodNode method, final AwaitSwitchEntry se, final int local) {
        if (se != null) {
            for (final LocalVariableNode node : method.localVariables) {
                if (node.index == local && isLocalVariableInScope(method, se, node)) {
                    return node.name;
                }
            }
        }
        return "_local$" + local;
    }
    
    private static boolean isLocalVariableInScope(final MethodNode method, final AwaitSwitchEntry se, final LocalVariableNode node) {
        final InsnList instructions = method.instructions;
        return instructions.indexOf(node.start) <= se.index && instructions.indexOf(node.end) >= se.index;
    }
    
    private static boolean isUninitialized(final Value value) {
        return value instanceof ExtendedValue && ((ExtendedValue) value).isUninitialized();
    }
    
    private static int valueSize(final Value local) {
        return local == null ? 1 : local.getSize();
    }
    
    @SuppressWarnings("squid:ForLoopCounterChangedCheck")
    // replacing only the initialization of objects that are uninitialized at the moment of an await call.
    static void replaceObjectInitialization(final MethodNode method,
                                            final Frame<BasicValue>[] frames,
                                            final Set<AbstractInsnNode> objectCreationNodes) {
        int originalLocals = method.maxLocals;
        final Set<AbstractInsnNode> uninitializedObjects = objectCreationNodes != null
            ? objectCreationNodes : Stream.of(method.instructions.toArray()).filter(i -> i.getOpcode() == NEW).collect(Collectors.toSet());
        
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
        for (AbstractInsnNode insnNode = method.instructions.getFirst(); insnNode != null; index++, insnNode = insnNode.getNext()) {
            if (insnNode instanceof FrameNode) {
                FrameNode frameNode = (FrameNode) insnNode;
                frameNode.stack = replaceUninitializedFrameValues(uninitializedObjects, frameNode.stack);
                frameNode.local = replaceUninitializedFrameValues(uninitializedObjects, frameNode.local);
            } else if (insnNode.getOpcode() == INVOKESPECIAL) {
                MethodInsnNode methodInsnNode = (MethodInsnNode) insnNode;
                if (methodInsnNode.name.equals("<init>")) {
                    insnNode = replaceConstructorCall(method, frames[index], uninitializedObjects, originalLocals, methodInsnNode);
                }
            }
        }
        // replace new calls
        for (AbstractInsnNode insnNode = method.instructions.getFirst(); insnNode != null; insnNode = insnNode.getNext()) {
            if (insnNode.getOpcode() == NEW && (uninitializedObjects.contains(insnNode))) {
                InsnNode newInsn = new InsnNode(ACONST_NULL);
                method.instructions.insertBefore(insnNode, newInsn);
                method.instructions.remove(insnNode);
                insnNode = newInsn;
            }
        }
    }
    
    private static AbstractInsnNode replaceConstructorCall(final MethodNode method,
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
        
        final InsnList instructions = method.instructions;
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
                currentInsn = insertInstruction(instructions, currentInsn, new InsnNode(POP));
                currentInsn = insertInstruction(instructions, currentInsn, new InsnNode(ACONST_NULL));
                value = BasicValue.REFERENCE_VALUE;
            }
            if (!target.equals(value)) {
                stackToLocal[j] = iLocal;
                // storing to a temporary local variable
                currentInsn = insertInstruction(instructions, currentInsn, new VarInsnNode(value.getType().getOpcode(ISTORE), iLocal));
                iLocal += valueSize(value);
            } else {
                // position where we will put the new object if needed
                if (j >= firstOccurrence + repetitions) {
                    stackToLocal[j] = newObject != -1 ? newObject : (newObject = iLocal++);
                }
                currentInsn = insertInstruction(instructions, currentInsn, new InsnNode(POP));
            }
            newMaxLocals = iLocal;
        }
        method.maxLocals = Math.max(newMaxLocals, method.maxLocals);
        
        // creates the object
        instructions.insert(currentInsn, currentInsn = new TypeInsnNode(NEW, target.getType().getInternalName()));
        
        // stores the new object to all locals that should contain it, if any
        for (int j = 0; j < frame.getLocals();) {
            // replaces all locals that used to reference the old value
            BasicValue local = frame.getLocal(j);
            if (target.equals(local)) {
                currentInsn = insertInstruction(instructions, currentInsn, new InsnNode(DUP));
                currentInsn = insertInstruction(instructions, currentInsn, new VarInsnNode(ASTORE, j));
            }
            j += local.getSize();
        }
        
        if (firstOccurrence < targetStackIndex) {
            // duping instead of putting it to a local, just to look as regular java code.
            for (int i = 1; i < repetitions; i++) {
                instructions.insert(currentInsn, currentInsn = new InsnNode(DUP));
            }
            if (newObject != -1) {
                currentInsn = insertInstruction(instructions, currentInsn, new InsnNode(DUP));
                currentInsn = insertInstruction(instructions, currentInsn, new VarInsnNode(ASTORE, newObject));
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
    
    private static AbstractInsnNode insertInstruction(final InsnList instructions,
                                                      final AbstractInsnNode currentInsn,
                                                      final AbstractInsnNode nextInsn) {
        instructions.insert(currentInsn, nextInsn);
        return nextInsn;
    }
    
    private static List<Object> replaceUninitializedFrameValues(final Set<AbstractInsnNode> uninitializedObjects, final List<Object> list) {
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
                    newList.set(i, Type.getObjectType(((TypeInsnNode) node).desc).getInternalName());
                }
            }
        }
        return newList;
    }
    
    /**
     * Replaces calls to Await.await with getting a promise if done: keep executing otherwise, call continuation and jump to the correct place
     * when done
     *
     * @param unwrapResult true if the result should be unwrapped, false to hold on to the completion stage
     * @param mv the method node whose instructions are being modified
     */
    private static void transformAwait(final boolean unwrapResult,
                                       final ClassNode classNode,
                                       final MethodNode original,
                                       final MethodVisitor mv,
                                       final AwaitSwitchEntry awaitSwitchEntry,
                                       final String continuationLambdaDesc,
                                       final List<Argument> lambdaArguments,
                                       final Handle handle,
                                       final int lineNumber) {
        // original: Async.await(stage) / Async.awaitExceptions(stage)
        
        // turns into:
        
        // code: if (!CompletionStageUtil.isDone(stage)) {
        // code:   saveStack to new locals
        // code:   push lambda parameters (locals and stack)
        // code:   return CompletionStageUtil.doneCompose(stage, completed_stage ->
        // code:     continuation$method(arguments, state, completed_stage)
        // code:   );
        // code: }
        // code: isDoneLabel:
        // code: CompletionStageUtil.get(stage); -> causes exception to be thrown
        // (only for awaitExceptions, restore stage on stack)
        
        // and this is added to the switch
        // code: resumeLabel:
        // code: restoreStack;
        // code: restoreLocals;
        // code: jump isDoneLabel:
        
        // current stack head is the async reference passed to await()
        // stack: { ... stage }
        mv.visitInsn(DUP);
        // stack: { ... stage stage }
        mv.visitMethodInsn(INVOKESTATIC, COMP_STAGE_UTIL_NAME, COMP_STAGE_UTIL_IS_DONE_NAME, COMP_STAGE_UTIL_IS_DONE_DESC, false);
        // stack: { ... stage is_done_result }
        // code: jump if true (non 0) to isDoneLabel:
        mv.visitJumpInsn(IFNE, awaitSwitchEntry.isDoneLabel);
        
        // code: saveStack to new state
        // code: push the stage
        // code: push all lambda parameters
        // code: create the lambda
        
        // stack: { ... stage }
        saveStack(mv, awaitSwitchEntry);
        // stack: { }
        
        mv.visitVarInsn(ALOAD, awaitSwitchEntry.stackToNewLocal[awaitSwitchEntry.frame.getStackSize() - 1]);
        // stack: { stage }
        
        // code: return CompletionStageUtil.doneCompose(async -> _func([arguments-2, state], async));
        // where [arguments-2, state] are bound to the lambda
        
        pushArguments(Modifier.isStatic(original.access), classNode, mv, awaitSwitchEntry, lambdaArguments, lineNumber);
        // stack: { stage ...arguments-2... }
        mv.visitIntInsn(SIPUSH, awaitSwitchEntry.key);
        // stack: { stage ...arguments-2... state }
        
        mv.visitInvokeDynamicInsn("apply",
            continuationLambdaDesc,
            LAMBDAMETAFACTORY_HANDLE,
            Type.getType("(L" + OBJECT_NAME + ";)L" + OBJECT_NAME + ";"),
            handle,
            Type.getType("(L" + COMP_STAGE_NAME + ";)L" + COMP_STAGE_NAME + ";"));
        
        // stack: { stage lambda_function_ref }
        mv.visitMethodInsn(INVOKESTATIC, COMP_STAGE_UTIL_NAME, COMP_STAGE_UTIL_DONE_COMPOSE_NAME, COMP_STAGE_UTIL_DONE_COMPOSE_DESC, false);
        
        // stack: { composed_stage }
        
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
        
        if (!unwrapResult) {
            // leave a reference to async on the stack, as we only call get() below to throw exception in case of
            // rejection
            mv.visitInsn(DUP);
            // {  ... async async }
        }
        
        // {  ... async? async }
        mv.visitMethodInsn(INVOKESTATIC, COMP_STAGE_UTIL_NAME, COMP_STAGE_UTIL_GET_NAME, COMP_STAGE_UTIL_GET_DESC, false);
        // {  ... async? result } or throw exception.
        // For first iteration, replacement method catches exception and rejects the async
        // For continuations, an exception rejects the composed async
        
        if (!unwrapResult) {
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
                if (value.getSize() == 2) {
                    i++;
                }
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
        if (NULL_TYPE.equals(type)) {
            return NULL;
        }
        return internalName;
    }
    
    private static boolean isOneOfAwaitMethods(final MethodInsnNode methodIns) {
        return isOneOfAwaitMethods(methodIns.getOpcode(), methodIns.owner, methodIns.name);
    }
    
    private static boolean isOneOfAwaitMethods(final int opcode, final String owner, final String name) {
        return opcode == INVOKESTATIC && ASYNC_NAME.equals(owner) && ASYNC_AWAIT_METHODS.contains(name);
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
    
    private static void pushInitial(final ClassNode classNode, final MethodNode method, final List<Argument> lambdaArguments) {
        // stack: { ... }
        if (!Modifier.isStatic(method.access)) {
            method.visitVarInsn(ALOAD, 0);
        }
        // push arguments followed by defaults for the extra local and stack continuation arguments
        // except the last 2 arguments (state and promise)
        final Type[] methodArgument = Type.getArgumentTypes(method.desc);
        for (int i = 0, l = lambdaArguments.size() - 2; i < l; i++) {
            final Argument argument = lambdaArguments.get(i);
            if (i < methodArgument.length) {
                method.visitVarInsn(argument.value.getType().getOpcode(ILOAD), argument.argumentOffset);
            } else {
                pushDefault(classNode, method, argument.value, -1);
            }
        }
        method.visitInsn(ICONST_0); // initial state
        method.visitInsn(ACONST_NULL); // null stage
        // stack: { ... lambdaArguments}
    }
    
    @SuppressWarnings("squid:MethodCyclomaticComplexity")
    private static void pushDefault(final ClassNode classNode, final MethodVisitor mv, final BasicValue value, final int lineNumber) {
        if (value.getType() == null) {
            mv.visitInsn(ACONST_NULL);
            return;
        }
        switch (value.getType().getSort()) {
            case Type.VOID:
                mv.visitInsn(ACONST_NULL);
                break;
            case Type.BOOLEAN:
            case Type.CHAR:
            case Type.BYTE:
            case Type.SHORT:
            case Type.INT:
                mv.visitInsn(ICONST_0);
                break;
            case Type.FLOAT:
                mv.visitInsn(FCONST_0);
                break;
            case Type.LONG:
                mv.visitInsn(LCONST_0);
                break;
            case Type.DOUBLE:
                mv.visitInsn(DCONST_0);
                break;
            case Type.ARRAY:
            case Type.OBJECT:
                mv.visitInsn(ACONST_NULL);
                break;
            default:
                throw error(extractFullyQualifiedClassName(classNode), lineNumber, "Unknown type: %s", value.getType().getSort());
        }
    }
    
    private static void restoreStackAndLocals(final ClassNode classNode,
                                              final MethodNode original,
                                              final MethodVisitor mv,
                                              final AwaitSwitchEntry se,
                                              final List<Argument> lambdaArguments) {
        final boolean isStatic = Modifier.isStatic(original.access);
        // restore the stack: just push in the right order.
        // restore the locals: push all that changed place then load
        if (se.argumentToLocal == null) {
            return;
        }
        final ExtendedFrame frame = se.frame;
        
        restoreStack(classNode, mv, se, lambdaArguments, frame);
        
        // push the arguments that must be copied to locals
        // must only push
        for (int local = isStatic ? 0 : 1; local < frame.getLocals(); local += valueSize(frame.getLocal(local))) {
            final BasicValue value = frame.getLocal(local);
            if (se.localToOffset[local] >= 0) {
                if (se.localToOffset[local] != local) {
                    mv.visitVarInsn(value.getType().getOpcode(ILOAD), se.localToOffset[local]);
                }
            }
        }
        // stores the values that came from arguments
        for (int local = frame.getLocals(); --local >= (isStatic ? 0 : 1);) {
            if (se.localToOffset[local] >= 0 && se.localToOffset[local] != local) {
                mv.visitVarInsn(frame.getLocal(local).getType().getOpcode(ISTORE), local);
            }
        }
        // restore values that were initialized as constants (i.e.: ACONST_NULL)
        for (int local = isStatic ? 0 : 1; local < frame.getLocals(); local += valueSize(frame.getLocal(local))) {
            final BasicValue value = frame.getLocal(local);
            if (se.localToOffset[local] < 0 && value != null && value.getType() != null) {
                pushDefault(classNode, mv, value, -1);
                mv.visitVarInsn(value.getType().getOpcode(ISTORE), local);
            }
        }
        
        reacquireMonitors(classNode, original, mv, se, frame);
    }
    
    private static void restoreStack(final ClassNode classNode,
                                     final MethodVisitor mv,
                                     final AwaitSwitchEntry se,
                                     final List<Argument> lambdaArguments,
                                     final ExtendedFrame frame) {
        for (int i = 0; i < frame.getStackSize() - 1; i++) {
            final BasicValue value = frame.getStack(i);
            // stack_index -> stackToNewLocal -> argumentToLocal -> arg_index
            int local = se.stackToNewLocal[i];
            if (local >= 0 && se.localToOffset[local] >= 0) {
                mv.visitVarInsn(value.getType().getOpcode(ILOAD), se.localToOffset[local]);
            } else {
                pushDefault(classNode, mv, value, -1);
            }
        }
        // restore promise in stack from last argument
        mv.visitVarInsn(ALOAD, lambdaArguments.get(lambdaArguments.size() - 1).argumentOffset);
    }
    
    private static void reacquireMonitors(final ClassNode classNode,
                                          final MethodNode original,
                                          final MethodVisitor mv,
                                          final AwaitSwitchEntry se,
                                          final ExtendedFrame frame) {
        for (int i = 0; i < se.frame.monitors.length; i++) {
            final BasicValue monitorValue = frame.monitors[i];
            int monitorLocal = -1;
            for (int local = 0; local < frame.getLocals(); local += valueSize(frame.getLocal(local))) {
                if (frame.getLocal(local) == monitorValue) {
                    monitorLocal = local;
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
            // method signature has no arguments
            sig.append(')');
        } else {
            boolean type = false;
            int genericDepth = 0;
            String arraySuffix = "";
            int i = 1;
            while (true) {
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
                    break;
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
