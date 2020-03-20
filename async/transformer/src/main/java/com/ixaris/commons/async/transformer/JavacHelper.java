package com.ixaris.commons.async.transformer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

import javax.annotation.processing.ProcessingEnvironment;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;

import com.ixaris.commons.async.transformer.AsyncProcessor.Helper;

public class JavacHelper implements Helper {
    
    public void init(final ProcessingEnvironment procEnv, final AsyncTransformer transformer) {
        // obtain the java file manager. Depends directly on JavacProcessingEnvironment, so will only work with javac
        final JavaFileManager javaFileManager = getJavaFileManager(procEnv);
        
        // Set up the listener to react to every written class file
        // This approach was discovered by breaking during annotation processing and working backwards
        // this listener is invoked in com.sun.tools.javac.main.JavaCompiler.generate(Queue<>, Queue<>)
        JavacTask.instance(procEnv).addTaskListener(new TaskListener() {
            
            @Override
            public void started(final TaskEvent taskEvent) {}
            
            @Override
            public void finished(final TaskEvent taskEvent) {
                if (Kind.GENERATE.equals(taskEvent.getKind())) {
                    try {
                        // extract the javac object to get to the class file written
                        // this code follows logic in com.sun.tools.javac.jvm.ClassWriter.writeClass(ClassSymbol)
                        final ClassSymbol typeElement = (ClassSymbol) taskEvent.getTypeElement();
                        final JavaFileObject file = javaFileManager.getJavaFileForOutput(StandardLocation.CLASS_OUTPUT,
                            typeElement.flatname.toString(),
                            JavaFileObject.Kind.CLASS,
                            typeElement.sourcefile);
                        
                        // transform
                        final byte[] out;
                        try (final InputStream is = file.openInputStream()) {
                            out = transformer.instrument(is);
                        }
                        // transformation is idempotent. if no transformation is needed, null is returned,
                        // so avoid rewriting the same contents
                        if (out != null) {
                            try (final OutputStream os = file.openOutputStream()) {
                                os.write(out);
                            }
                        }
                    } catch (final IOException e) {
                        throw new IllegalStateException(e);
                    }
                }
            }
            
        });
    }
    
    private JavaFileManager getJavaFileManager(final ProcessingEnvironment procEnv) {
        final Context context = ((JavacProcessingEnvironment) procEnv).getContext();
        return context.get(JavaFileManager.class);
    }
    
}
