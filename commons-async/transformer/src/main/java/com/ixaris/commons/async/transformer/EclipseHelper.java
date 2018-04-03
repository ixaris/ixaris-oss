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
 * and rewrites the written class.
 *
 * Approach was determined after looking at eclipse compiler code. This was done using mvnDebug with a dependency on
 * org.codehaus.groovy:groovy-eclipse-compiler:2.9.2-01 and org.codehaus.groovygroovy-eclipse-batch:2.4.3-01, halting
 * during annotation processing and working backwards.
 */
public class EclipseHelper implements Helper {
    
    public void init(final ProcessingEnvironment procEnv, final AsyncTransformer transformer) {
        
        try {
            // the below code works using reflection to avoid packaging the eclipse compiler with the transformer
            // through a transitive dependency, since ide support requires a dependency on the transformer
            final Object compiler = invoke(procEnv, "getCompiler");
            final Field requestorField = compiler.getClass().getField("requestor");
            requestorField.setAccessible(true);
            final Class<?> requestorInterface = requestorField.getType();
            
            final Object requestor = requestorField.get(compiler);
            final Object main = getPrivate(requestor, "compiler");
            
            // roundabout way of implementing the org.eclipse.jdt.internal.compiler.ICompilerRequestor
            // interface. The compiler calls the method acceptResult() when a class is compiled. The
            // implementor is responsible for writing the actual class file. As such, we call the original
            // implementor and then do the transformation right after. However, the compiler reuses instances
            // for compiled classes, so we need to copy the class file output path before the instance is
            // reused and this path changed (found this the hard way).
            
            // The code below follows the same logic as org.eclipse.jdt.internal.compiler.batch.Main.outputClassFiles()
            final InvocationHandler invocationHandler = (proxy, method, args) -> {
                String[] absolutePathsToTransform = null;
                if (method.getName().equals("acceptResult")) {
                    final Object unitResult = args[0];
                    
                    final String destinationPath = (String) get(main, "destinationPath");
                    final Object compilationUnit = get(unitResult, "compilationUnit");
                    final String unitDestinationPath = (String) get(compilationUnit, "destinationPath");
                    String currentDestinationPath = null;
                    if (unitDestinationPath == null) {
                        if (destinationPath == null) {
                            currentDestinationPath = extractDestinationPathFromSourceFile(compilationUnit);
                        } else if (!destinationPath.equals("none")) {
                            currentDestinationPath = destinationPath;
                        }
                    } else if (!unitDestinationPath.equals("none")) {
                        currentDestinationPath = unitDestinationPath;
                    }
                    
                    final Object[] classFiles = (Object[]) invoke(unitResult, "getClassFiles");
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
    
    private Object getPrivate(final Object instance, final String fieldName) throws NoSuchFieldException, IllegalAccessException {
        final Field field = instance.getClass().getDeclaredField(fieldName);
        field.setAccessible(true);
        return field.get(instance);
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
