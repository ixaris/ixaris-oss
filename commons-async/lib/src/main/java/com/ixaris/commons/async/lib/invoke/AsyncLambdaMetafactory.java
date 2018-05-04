package com.ixaris.commons.async.lib.invoke;

import static jdk.internal.org.objectweb.asm.Opcodes.ACC_PUBLIC;
import static jdk.internal.org.objectweb.asm.Opcodes.ATHROW;
import static jdk.internal.org.objectweb.asm.Opcodes.INVOKESTATIC;

import java.lang.invoke.CallSite;
import java.lang.invoke.MethodHandle;
import java.lang.invoke.MethodHandles;
import java.lang.invoke.MethodType;
import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

import jdk.internal.org.objectweb.asm.ClassWriter;
import jdk.internal.org.objectweb.asm.MethodVisitor;

public final class AsyncLambdaMetafactory {
    
    private AsyncLambdaMetafactory() {}
    
    private static final Constructor<?> InnerClassLambdaMetafactory_CONSTRUCTOR;
    private static final Method AbstractValidatingLambdaMetafactory_validateMetafactoryArgs_METHOD;
    private static final Field InnerClassLambdaMetafactory_cw_FIELD;
    private static final Method InnerClassLambdaMetafactory_buildCallSite_METHOD;
    
    private static final String COMP_STAGE_NAME = "java/util/concurrent/CompletionStage";
    private static final String ASYNC_NAME = "com/ixaris/commons/async/lib/Async";
    
    static {
        try {
            final Class<?> InnerClassLambdaMetafactoryClass = Class.forName("java.lang.invoke.InnerClassLambdaMetafactory");
            final Class<?> AbstractValidatingLambdaMetafactoryClass = Class.forName("java.lang.invoke.AbstractValidatingLambdaMetafactory");
            
            InnerClassLambdaMetafactory_CONSTRUCTOR = InnerClassLambdaMetafactoryClass.getDeclaredConstructor(MethodHandles.Lookup.class,
                MethodType.class,
                String.class,
                MethodType.class,
                MethodHandle.class,
                MethodType.class,
                boolean.class,
                Class[].class,
                MethodType[].class);
            InnerClassLambdaMetafactory_CONSTRUCTOR.setAccessible(true);
            AbstractValidatingLambdaMetafactory_validateMetafactoryArgs_METHOD = AbstractValidatingLambdaMetafactoryClass.getDeclaredMethod("validateMetafactoryArgs");
            AbstractValidatingLambdaMetafactory_validateMetafactoryArgs_METHOD.setAccessible(true);
            InnerClassLambdaMetafactory_cw_FIELD = InnerClassLambdaMetafactoryClass.getDeclaredField("cw");
            InnerClassLambdaMetafactory_cw_FIELD.setAccessible(true);
            InnerClassLambdaMetafactory_buildCallSite_METHOD = InnerClassLambdaMetafactoryClass.getDeclaredMethod("buildCallSite");
            InnerClassLambdaMetafactory_buildCallSite_METHOD.setAccessible(true);
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private static final Class<?>[] EMPTY_CLASS_ARRAY = new Class<?>[0];
    private static final MethodType[] EMPTY_MT_ARRAY = new MethodType[0];
    
    public static CallSite metafactory(final MethodHandles.Lookup caller,
                                       final String invokedName,
                                       final MethodType invokedType,
                                       final MethodType samMethodType,
                                       final MethodHandle implMethod,
                                       final MethodType instantiatedMethodType) {
        
        try {
            final Object lambdaMetaFactory = InnerClassLambdaMetafactory_CONSTRUCTOR.newInstance(caller,
                invokedType,
                invokedName,
                samMethodType,
                implMethod,
                instantiatedMethodType,
                false,
                EMPTY_CLASS_ARRAY,
                EMPTY_MT_ARRAY);
            
            AbstractValidatingLambdaMetafactory_validateMetafactoryArgs_METHOD.invoke(lambdaMetaFactory);
            
            // add implementation for Async<> returning method, otherwise java 10 fails to create the inner class
            final ClassWriter cw = (ClassWriter) InnerClassLambdaMetafactory_cw_FIELD.get(lambdaMetaFactory);
            final MethodVisitor mv = cw.visitMethod(ACC_PUBLIC,
                invokedName.substring(6),
                samMethodType.toMethodDescriptorString().replace(")L" + COMP_STAGE_NAME + ";", ")L" + ASYNC_NAME + ";"),
                null,
                null);
            mv.visitCode();
            mv.visitMethodInsn(INVOKESTATIC, ASYNC_NAME, "noTransformation", "()Ljava/lang/UnsupportedOperationException;", false);
            mv.visitInsn(ATHROW);
            mv.visitMaxs(1, 0);
            mv.visitEnd();
            
            return (CallSite) InnerClassLambdaMetafactory_buildCallSite_METHOD.invoke(lambdaMetaFactory);
        } catch (final ReflectiveOperationException e) {
            throw new IllegalStateException(e);
        }
    }
    
}
