package com.ixaris.commons.protobuf.codegen;

import static java.io.File.separatorChar;
import static javax.lang.model.SourceVersion.RELEASE_8;
import static javax.tools.Diagnostic.Kind.ERROR;
import static javax.tools.Diagnostic.Kind.NOTE;
import static javax.tools.Diagnostic.Kind.WARNING;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Reader;
import java.net.URL;
import java.net.URLClassLoader;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.LinkedList;
import java.util.List;
import java.util.Objects;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.concurrent.Callable;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;
import java.util.stream.StreamSupport;

import javax.annotation.processing.AbstractProcessor;
import javax.annotation.processing.RoundEnvironment;
import javax.annotation.processing.SupportedAnnotationTypes;
import javax.annotation.processing.SupportedSourceVersion;
import javax.lang.model.element.Element;
import javax.lang.model.element.TypeElement;
import javax.tools.Diagnostic;
import javax.tools.FileObject;
import javax.tools.JavaFileObject;
import javax.tools.StandardLocation;

import org.jboss.shrinkwrap.resolver.api.Resolvers;
import org.jboss.shrinkwrap.resolver.api.maven.ConfigurableMavenResolverSystem;

/**
 * This is the annotation processor to invoke protoc compiler.
 * <p>
 * ATTENTION: DRAGONS !
 * <p>
 * The process is complicated because we need to:
 * - invoke an external non-java executable. This processor will attempt to download it via maven if it's not found
 * - The protoc compiler needs to generate 3 types of code: java + 2 more as generated by 2 plugins:
 * These plugins are part of the ix-commons code base
 * ... which need to be compiled
 * ... which need to be converted to an executable (EXE) because protoc compiler is not capable of invoking java code
 * <p>
 * - The annotation processor API requires us to generate any files via it's API. Since we're creating files externally
 * via protoc,
 * we also require to read again these generated files and pipe them through the annotation processor java API.
 * <p>
 * - To further complicate matters:
 * The java annotation processor does not expose where our source tree. We work around this by creating a dummy file and
 * detecting where it went
 * Intellij by default writes resources to out/production/resources. Unfortunately this implies that the annotation
 * processor would not be able
 * to "see" these resources, notably the *.proto files. Therefore via gradle we tell intellij to write proto files to
 * out/production/classes instead
 * As a side effect, there's a race condition between META-INF/services/javax.annotation.processor and the actual
 * compiled class. We work around this
 * via gradle
 **/
@SupportedAnnotationTypes("*")
@SupportedSourceVersion(RELEASE_8)
public class ProtocGenerate extends AbstractProcessor {
    
    private static final Set<String> DONE = new HashSet<>();
    
    private void process() {
        try {
            // e.g. <project>\build\classes\java\main
            final String output = determineOutputPath();
            if (DONE.add(output)) {
                final File outputDir = new File(output);
                final String where = outputDir.getName();
                // this becomes: <project>
                final File buildDir = outputDir.getParentFile().getParentFile().getParentFile();
                final File pluginDir = new File(buildDir, "protoc-plugin");
                pluginDir.mkdirs();
                final File rootDir = buildDir.getParentFile();
                
                final File resources = new File(rootDir, "src" + separatorChar + where + separatorChar + "resources");
                if (resources.exists()) {
                    final List<File> filesToGenerate = getFilesToGenerate(resources);// go find the .proto files
                    if (!filesToGenerate.isEmpty()) {
                        message(NOTE, "Generating protoc files " + filesToGenerate + ", root = " + rootDir);
                        
                        // maven writes to target/ folder, gradle to build/ and intellij to out/.
                        writeProtoc(filesToGenerate, pluginDir, resources);
                    }
                }
            }
        } catch (final RuntimeException e) {
            message(ERROR, e.getMessage());
            throw e;
        } catch (final IOException | InterruptedException e) {
            message(ERROR, e.getMessage());
            throw new IllegalStateException(e);
        }
    }
    
    private String determineOutputPath() throws IOException {
        // go write a file so as to figure out where we're running
        final FileObject resource = processingEnv.getFiler().createResource(
            StandardLocation.CLASS_OUTPUT,
            "",
            "tmp" + System.currentTimeMillis(),
            (Element[]) null);
        try {
            return new File(resource.toUri()).getCanonicalFile().getParent();
        } finally {
            resource.delete();
        }
    }
    
    private List<File> getFilesToGenerate(final File resources) {
        // normally we should be figuring out the files we want via the annotation processor API but intellij as of 2019.1 does not
        // copy the resources in the right place so we have to fudge around some paths and do it manually
        return Arrays.asList(resources.listFiles(pathname -> pathname.getName().endsWith(".proto")));
    }
    
    private void writeProtoc(final List<File> filesToGenerate, final File pluginDir, final File resources) throws IOException, InterruptedException {
        // Protoc should be available in maven repos due to dependency
        final Set<String> classPath = Arrays.stream(((URLClassLoader) (getClass().getClassLoader())).getURLs())
            .map(URL::getPath)
            .collect(Collectors.toSet());
        
        final List<String> args = new LinkedList<>();
        args.add(determineProtocPath(classPath));
        
        // annotation processor API forces us to write files via it's own API otherwise it will not be picked up
        // in subsequent rounds (= it will not compile the generated .java files). So first we write to somewhere temp
        // then we re-read the files and write them again via annotation processor API (remember protoc is an external EXE
        // so we cannot make it write using the AP's API)
        final String target = Files.createTempDirectory("protoc-out").toFile().getAbsolutePath();
        new File(target).mkdirs();
        args.add("--java_out=" + target);
        
        includeProtocPlugins(args, classPath, pluginDir, resources, target);
        args.add("-I" + resources.getAbsolutePath());
        processProtoFiles(args, filesToGenerate, resources.getAbsolutePath());
        filesToGenerate.forEach(f -> args.add(f.getAbsolutePath()));
        runProtoc(args);
        
        pipeThroughAnnotationProcessor(target); // after we generate files, go read and write the generated files via API (see comments above)
    }
    
    private String determineProtocPath(final Set<String> classPath) {
        final ConfigurableMavenResolverSystem resolver = Resolvers.configure(
            ConfigurableMavenResolverSystem.class,
            this.getClass().getClassLoader());
        final String version = classPath.stream()
            .map(s -> {
                final int index = s.indexOf("protobuf-java/");
                if (index >= 0) {
                    return s.substring(index + 14, s.indexOf("/", index + 14));
                } else {
                    return null;
                }
            })
            .filter(Objects::nonNull)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException("Unable to locate protobuf version from classpath"));
        final File protoc = resolver.resolve("com.google.protobuf:protoc:exe:" + OsDetector.getClassifier() + ":" + version).withoutTransitivity().asSingleFile();
        protoc.setExecutable(true);
        return protoc.getAbsolutePath();
    }
    
    /**
     * Generates native launchers for java protoc plugins.
     * These launchers will later be added as parameters for protoc compiler.
     */
    protected void includeProtocPlugins(final List<String> args, final Set<String> classPath, final File pluginDir, final File resources, final String target) {
        final ClassLoader cl = Thread.currentThread().getContextClassLoader();
        Thread.currentThread().setContextClassLoader(ProtocGenerate.class.getClassLoader());
        final Set<ProtocPlugin> plugins = StreamSupport
            .stream(ServiceLoader.load(ProtocPlugin.class).spliterator(), false)
            .collect(Collectors.toSet());
        Thread.currentThread().setContextClassLoader(cl);
        
        if (!plugins.isEmpty()) {
            // generate exe
            final ProtocPluginAssembler assembler = new ProtocPluginAssembler(classPath, pluginDir);
            
            for (final ProtocPlugin plugin : plugins) {
                final File protocExecutor = assembler.build(plugin);
                protocExecutor.setExecutable(true);
                // We pass to the plugin the path where the proto files are located, in case the plugin needs to read other files
                // The base64 magic is due to protoc using ":" as a delimiter i.e. it gets mangled when it sees C:\somewhere
                args.add("--plugin=protoc-gen-" + plugin.getName() + "=" + protocExecutor);
                args.add("--" + plugin.getName() + "_out=baseDir=" + new String(Base64.getEncoder().encode(resources.getAbsolutePath().getBytes())) + "=:" + target);
                
            }
        }
    }
    
    private void pipeThroughAnnotationProcessor(final String javaoutTarget) throws IOException {
        /* The annotation processor rounds will only see files as created via the Filer. This implies that the files created by the external protoc compiler aren't visible
        to the next rounds implying they do not get compiled. Therefore this method will go through what was generated and rewrite them via the filer API
         */
        Files.walk(Paths.get(javaoutTarget)).forEach(p -> {
            final File file = p.toFile();
            if (file.isFile()) {
                final String packageName = file
                    .getAbsolutePath()
                    .replace(javaoutTarget + separatorChar, "")
                    .replace(file.getName(), "")
                    .replace(File.separator, ".");
                final String filename = file.getName().replace(".java", "");
                
                try {
                    final JavaFileObject newSourceFile = processingEnv.getFiler().createSourceFile(packageName + filename);
                    try (final OutputStream out = newSourceFile.openOutputStream()) {
                        Files.copy(p, out);
                    }
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
    }
    
    private void processProtoFiles(final List<String> args, final List<File> filesToGenerate, final String basePath) {
        final List<String> toProcess = filesToGenerate.stream().map(File::getName).collect(Collectors.toList());
        final Set<String> extracted = new HashSet<>(toProcess);
        toProcess.forEach(file -> processProtoFile(basePath, extracted, () -> new BufferedReader(getReader(basePath, file)))
            .forEach(i -> args.add("-I" + i.getAbsolutePath())));
    }
    
    /**
     * This method opens up the files as found in this project. It goes through all the file lines searching for
     * imports. For each one it finds,
     * it will try to open that file too from the classpath. Each file that is read is copied over to a temp folder
     * because protoc expects
     * to be given a folder where it can find the file.
     */
    private Set<File> processProtoFiles(final List<String> toProcess, final String basePath, final Set<String> extracted) {
        final Set<File> includeFiles = new HashSet<>();
        toProcess.forEach(file -> {
            includeFiles.addAll(processProtoFile(basePath, extracted, () -> {
                File temp = Files.createTempDirectory("protoc").toFile();
                includeFiles.add(temp);
                temp.deleteOnExit();
                
                String resourceFile = file;
                if (file.contains("/")) { // eg where includes has google/protobuf/descriptor.proto
                    temp = new File(temp.getAbsolutePath() + "/" + file.substring(0, file.lastIndexOf("/")));
                    resourceFile = file.substring(file.lastIndexOf("/") + 1);
                    temp.mkdirs();
                    temp.deleteOnExit();
                } else if (file.contains("\\")) { // eg where includes has google/protobuf/descriptor.proto
                    temp = new File(temp.getAbsolutePath() + "/" + file.substring(0, file.lastIndexOf("\\")));
                    resourceFile = file.substring(file.lastIndexOf("\\") + 1);
                    temp.mkdirs();
                    temp.deleteOnExit();
                }
                
                final File targetFile = new File(temp, resourceFile);
                targetFile.deleteOnExit();
                
                return new BufferedReader(new TeeReader(getReader(basePath, file), new FileWriter(targetFile)));
            }));
            extracted.add(file);
        });
        return includeFiles;
    }
    
    private Set<File> processProtoFile(final String basePath, final Set<String> extracted, final Callable<BufferedReader> readerSupplier) {
        try {
            final List<String> filesToExtract;
            try (final BufferedReader reader = readerSupplier.call()) {
                filesToExtract = determineFilesToExtract(reader, extracted);
            }
            return processProtoFiles(filesToExtract, basePath, extracted);
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }
    
    @SuppressWarnings("squid:S1166")
    private Reader getReader(final String basePath, final String file) throws IOException {
        Reader reader;
        try {
            reader = processingEnv.getFiler().getResource(StandardLocation.CLASS_PATH, "", file).openReader(false);
        } catch (final RuntimeException | FileNotFoundException e) {
            File f = new File(basePath, file);
            if (f.exists()) {
                reader = new FileReader(f);
            } else {
                throw new FileNotFoundException("Unable to locate " + file);
            }
        }
        
        return reader;
    }
    
    private List<String> determineFilesToExtract(final BufferedReader reader, final Set<String> extracted) {
        return reader.lines()
            .filter(l -> l.trim().startsWith("import"))
            .map(l -> l.replace("import", "").replaceAll("\"", "").replace(";", "").trim())
            .filter(l -> !extracted.contains(l))
            .collect(Collectors.toList());
    }
    
    private void runProtoc(final List<String> args) throws IOException, InterruptedException {
        Process process = new ProcessBuilder(args).start();
        final StringBuilder error = new StringBuilder();
        try (final BufferedReader br = new BufferedReader(new InputStreamReader(process.getErrorStream()))) {
            String line;
            
            while ((line = br.readLine()) != null) {
                if (line.trim().contains("warning:")) {
                    message(WARNING, line);
                } else {
                    message(ERROR, line);
                    error.append(line).append('\n');
                }
            }
        }
        
        if (!process.waitFor(1, TimeUnit.MINUTES)) {
            throw new IllegalStateException("Timed out waiting for protoc compiler");
        }
        
        if (error.length() > 0) {
            throw new IllegalStateException(error.toString());
        }
    }
    
    @Override
    public boolean process(final Set<? extends TypeElement> annotations, final RoundEnvironment roundEnv) {
        if (!roundEnv.errorRaised() && !roundEnv.processingOver()) {
            process();
        }
        return false;
    }
    
    private void message(final Diagnostic.Kind kind, final String message) {
        processingEnv.getMessager().printMessage(kind, message);
    }
    
}
