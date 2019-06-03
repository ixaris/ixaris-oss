package com.ixaris.commons.async.transformer;

import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.atomic.AtomicBoolean;
import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.ProcessingEnvironment;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.lang.model.SourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.ExecutableElement;
import javax.lang.model.element.TypeElement;
import javax.lang.model.element.VariableElement;
import javax.lang.model.util.ElementFilter;
import javax.tools.Diagnostic;

/**
 * Entrypoint to async code transformation, in the form of an annotation processor. Does not really do any annotation
 * processing, instead transforms every compiled class's bytecode.
 *
 * <p>Annotation processing can occur in rounds, where an annotation processor may signal that another round is
 * required. It is therefore required for the AsyncTransformer to be idempotent (which it is).
 */
@SupportedAnnotationTypes("*")
public class AsyncProcessor extends AbstractProcessor {
    
    @FunctionalInterface
    interface Helper {
        
        void init(ProcessingEnvironment procEnv, AsyncTransformer transformer);
        
    }
    
    private static AtomicBoolean initialised = new AtomicBoolean(false);
    
    private static final class ElementInfo {
        
        private final TypeElement element;
        private final Map<String, ExecutableElement> methods = new HashMap<>();
        
        private ElementInfo(final TypeElement element) {
            this.element = element;
        }
        
    }
    
    private final Map<String, ElementInfo> elements = new HashMap<>();
    
    @Override
    public SourceVersion getSupportedSourceVersion() {
        return AsmHelper.SOURCE_VERSION;
    }
    
    @Override
    public synchronized void init(final ProcessingEnvironment processingEnv) {
        super.init(processingEnv);
        
        if (initialised.compareAndSet(false, true)) {
            final AsyncTransformer transformer = new AsyncTransformer((fqcn, sig, ln, message) -> {
                final Element element = Optional.ofNullable(elements.get(fqcn))
                    .map(info -> Optional.<Element>ofNullable(info.methods.get(sig)).orElse(info.element))
                    .orElse(null);
                if (element != null) {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING, message, element);
                } else {
                    processingEnv.getMessager().printMessage(Diagnostic.Kind.MANDATORY_WARNING, message);
                }
            });
            
            final Helper helper = determineAndConstructHelper(processingEnv);
            helper.init(processingEnv, transformer);
        }
    }
    
    /**
     * No actual annotation processing
     *
     * @param annotations
     * @param roundEnv
     * @return
     */
    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        for (final TypeElement element : ElementFilter.typesIn(roundEnv.getRootElements())) {
            addElement(element);
        }
        return false;
    }
    
    private void addElement(final TypeElement element) {
        final ElementInfo elementInfo = new ElementInfo(element);
        elements.put(element.getQualifiedName().toString(), elementInfo);
        
        for (final Element nested : element.getEnclosedElements()) {
            if (nested instanceof TypeElement) {
                addElement((TypeElement) nested);
            } else if (nested instanceof ExecutableElement) {
                final ExecutableElement method = (ExecutableElement) nested;
                elementInfo.methods.put(extractMethodSignature(method), method);
            }
        }
    }
    
    private String extractMethodSignature(final ExecutableElement method) {
        final StringBuilder sig = new StringBuilder(method.getSimpleName()).append('(');
        boolean first = true;
        for (final VariableElement variableElement : method.getParameters()) {
            if (first) {
                first = false;
            } else {
                sig.append(",");
            }
            sig.append(variableElement.asType().toString());
        }
        
        return sig.append(')').toString();
    }
    
    @SuppressWarnings("squid:S2658")
    private Helper determineAndConstructHelper(final ProcessingEnvironment procEnv) {
        if (procEnv.getClass().getName().startsWith("org.eclipse")) {
            try {
                return (Helper)
                    Class.forName("com.ixaris.commons.async.transformer.EclipseHelper")
                        .getDeclaredConstructor()
                        .newInstance();
            } catch (final ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        } else {
            // by default fall back to javac
            try {
                return (Helper)
                    Class.forName("com.ixaris.commons.async.transformer.JavacHelper")
                        .getDeclaredConstructor()
                        .newInstance();
            } catch (final ReflectiveOperationException e) {
                throw new IllegalStateException(e);
            }
        }
    }
    
}
