package com.ixaris.commons.async.transformer;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.Processor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.JavaFileManager;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import org.kohsuke.MetaInfServices;

import com.ixaris.commons.async.transformer.AsyncProcessor.Helper;
import com.sun.source.util.JavacTask;
import com.sun.source.util.TaskEvent;
import com.sun.source.util.TaskEvent.Kind;
import com.sun.source.util.TaskListener;
import com.sun.tools.javac.code.Symbol.ClassSymbol;
import com.sun.tools.javac.processing.JavacProcessingEnvironment;
import com.sun.tools.javac.util.Context;

public class JavacHelper implements Helper {
    
    public void init(final ProcessingEnvironment procEnv, final AsyncTransformer transformer) {
        // obtain the java file manager. Depends directly on JavacProcessingEnvironment, so will not work with other compilers
        final JavaFileManager javaFileManager = getJavaFileManager(procEnv);
        
        // set up the listener to react to every written class file
        JavacTask.instance(procEnv).addTaskListener(new TaskListener() {
            
            @Override
            public void started(final TaskEvent taskEvent) {
                
            }
            
            @Override
            public void finished(final TaskEvent taskEvent) {
                if (Kind.GENERATE.equals(taskEvent.getKind())) {
                    try {
                        // extract the javac object to get to the class file written
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
