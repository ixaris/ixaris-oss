package com.ixaris.commons.async.transformer;

import static jdk.internal.org.objectweb.asm.Opcodes.ASM5;

import java.lang.reflect.Constructor;
import javax.lang.model.SourceVersion;
import jdk.internal.org.objectweb.asm.Handle;
import jdk.internal.org.objectweb.asm.Opcodes;

/**
 * Attempt to use ASM6 if available, otherwise fall back to asm 5. Intellij stops working if ASM6 is used due to some
 * internal dependency on ASM5
 */
final class AsmHelper {
    
    static final SourceVersion SOURCE_VERSION;
    static final int ASM_VERSION;
    private static final Constructor<Handle> HANDLE_CONSTRUCTOR;
    
    static {
        SourceVersion srcVer;
        try {
            srcVer = SourceVersion.valueOf("RELEASE_11");
        } catch (
            @SuppressWarnings("squid:S1166")
            final IllegalArgumentException e11
        ) {
            try {
                srcVer = SourceVersion.valueOf("RELEASE_10");
            } catch (
                @SuppressWarnings("squid:S1166")
                final IllegalArgumentException e10
            ) {
                try {
                    srcVer = SourceVersion.valueOf("RELEASE_9");
                } catch (
                    @SuppressWarnings("squid:S1166")
                    final IllegalArgumentException e9
                ) {
                    srcVer = SourceVersion.RELEASE_8;
                }
            }
        }
        SOURCE_VERSION = srcVer;
        
        int asmVer;
        try {
            asmVer = (int) Opcodes.class.getField("ASM6").get(null);
        } catch (
            @SuppressWarnings("squid:S1166")
            final ReflectiveOperationException e
        ) {
            asmVer = ASM5;
        }
        ASM_VERSION = asmVer;
        
        Constructor<Handle> handleConstructor;
        try {
            handleConstructor = Handle.class.getConstructor(
                int.class, String.class, String.class, String.class, boolean.class
            );
        } catch (
            @SuppressWarnings("squid:S1166")
            final NoSuchMethodException e
        ) {
            handleConstructor = null;
        }
        HANDLE_CONSTRUCTOR = handleConstructor;
    }
    
    static Handle createHandle(
        final int tag, final String owner, final String name, final String desc, final boolean itf
    ) {
        if (HANDLE_CONSTRUCTOR != null) {
            try {
                return HANDLE_CONSTRUCTOR.newInstance(tag, owner, name, desc, itf);
            } catch (final ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        } else {
            return new Handle(tag, owner, name, desc);
        }
    }
    
}
