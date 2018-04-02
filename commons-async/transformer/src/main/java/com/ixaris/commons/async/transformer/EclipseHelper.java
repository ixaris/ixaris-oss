package com.ixaris.commons.async.transformer;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.lang.reflect.Field;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Proxy;

import javax.annotation.processing.ProcessingEnvironment;

import com.ixaris.commons.async.transformer.AsyncProcessor.Helper;

/**
 * Handler for the eclipse compiler that intercepts calls to org.eclipse.jdt.internal.compiler.ICompilerRequestor.acceptResult()
 * and rewrites the written class
 */
public class EclipseHelper implements Helper {
    
    public void init(final ProcessingEnvironment procEnv, final AsyncTransformer transformer) {
        
        try {
            final Object compiler = invoke(procEnv, "getCompiler");
            final Field requestorField = compiler.getClass().getField("requestor");
            requestorField.setAccessible(true);
            final Class<?> requestorInterface = requestorField.getType();
            
            final Object requestor = requestorField.get(compiler);
            final Object main = getParentInstance(requestor);
            
            final InvocationHandler invocationHandler = (proxy, method, args) -> {
                String[] absolutePathsToTransform = null;
                if (method.getName().equals("acceptResult")) {
                    String currentDestinationPath = null;
                    final String destinationPath = (String) get(main, "destinationPath");
                    final Object unitResult = args[0];
                    final Object[] classFiles = (Object[]) invoke(unitResult, "getClassFiles");
                    final Object compilationUnit = get(unitResult, "compilationUnit");
                    final String unitDestinationPath = (String) get(compilationUnit, "destinationPath");
                    if (unitDestinationPath == null) {
                        if (destinationPath == null) {
                            currentDestinationPath = extractDestinationPathFromSourceFile(compilationUnit);
                        } else if (!destinationPath.equals("none")) {
                            currentDestinationPath = destinationPath;
                        }
                    } else if (!unitDestinationPath.equals("none")) {
                        currentDestinationPath = unitDestinationPath;
                    }
                    
                    if (currentDestinationPath != null) {
                        absolutePathsToTransform = new String[classFiles.length];
                        int i = 0;
                        for (int fileCount = classFiles.length; i < fileCount; ++i) {
                            final Object classFile = classFiles[i];
                            final char[] filename = (char[]) invoke(classFile, "fileName");
                            absolutePathsToTransform[i] = currentDestinationPath
                                + File.separatorChar
                                + new String(filename).replace('/', File.separatorChar)
                                + ".class";
                        }
                    }
                }
                
                // execute method on original requestor
                final Object result = method.invoke(requestor, args);
                
                if (absolutePathsToTransform != null) {
                    for (final String absolutePathToTransform : absolutePathsToTransform) {
                        final File fileToTransform = new File(absolutePathToTransform);
                        // transform
                        final byte[] out;
                        try (final InputStream is = new FileInputStream(fileToTransform)) {
                            out = transformer.instrument(is);
                        }
                        // transformation is idempotent. if no transformation is needed, null is returned,
                        // so avoid rewriting the same contents
                        if (out != null) {
                            try (final OutputStream os = new FileOutputStream(fileToTransform)) {
                                os.write(out);
                            }
                        }
                    }
                }
                return result;
            };
            
            requestorField.set(compiler,
                Proxy.newProxyInstance(requestorInterface.getClassLoader(), new Class<?>[] { requestorInterface }, invocationHandler));
        } catch (final IllegalAccessException | InvocationTargetException | NoSuchMethodException | NoSuchFieldException e) {
            throw new IllegalStateException(e);
        }
    }
    
    private Object invoke(final Object instance, final String methodName) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        final Method method = instance.getClass().getMethod(methodName);
        method.setAccessible(true);
        return method.invoke(instance);
    }
    
    private Object get(final Object instance, final String fieldName) throws NoSuchFieldException, IllegalAccessException {
        final Field field = instance.getClass().getField(fieldName);
        field.setAccessible(true);
        return field.get(instance);
    }
    
    private Object getParentInstance(final Object child) throws NoSuchFieldException, IllegalAccessException {
        Field parentField = child.getClass().getDeclaredField("this$0");
        parentField.setAccessible(true);
        return parentField.get(child);
    }
    
    private String extractDestinationPathFromSourceFile(final Object compilationUnit) throws NoSuchMethodException, InvocationTargetException, IllegalAccessException {
        if (compilationUnit != null) {
            char[] fileName = (char[]) compilationUnit.getClass().getMethod("getFileName").invoke(compilationUnit);
            int lastIndex = lastIndexOf(File.separatorChar, fileName);
            if (lastIndex != -1) {
                String outputPathName = new String(fileName, 0, lastIndex);
                File output = new File(outputPathName);
                if (output.exists() && output.isDirectory()) {
                    return outputPathName;
                }
            }
        }
        
        return System.getProperty("user.dir");
    }
    
    private static int lastIndexOf(char toBeFound, char[] array) {
        int i = array.length;
        do {
            --i;
            if (i < 0) {
                return -1;
            }
        } while (toBeFound != array[i]);
        
        return i;
    }
    
}
