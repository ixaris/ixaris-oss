package com.ixaris.commons.microservices.scslcodegen;

import static com.ixaris.commons.microservices.scslcodegen.CodegenHelper.determinePath;
import static com.ixaris.commons.microservices.scslcodegen.CodegenHelper.getJavaType;
import static com.ixaris.commons.protobuf.validationcodegen.CodegenHelper.concatenatePackage;
import static com.ixaris.commons.protobuf.validationcodegen.CodegenHelper.convertNameToType;
import static com.ixaris.commons.protobuf.validationcodegen.CodegenHelper.determineJavaInnerPackage;
import static com.ixaris.commons.protobuf.validationcodegen.CodegenHelper.determineJavaPackage;
import static com.ixaris.commons.protobuf.validationcodegen.CodegenHelper.determineOuterClassname;
import static com.ixaris.commons.protobuf.validationcodegen.CodegenHelper.getOuterClassName;
import static com.ixaris.commons.protobuf.validationcodegen.CodegenHelper.getTemplate;
import static com.ixaris.commons.protobuf.validationcodegen.CodegenHelper.resolveJavaMapping;
import static java.nio.charset.StandardCharsets.UTF_8;

import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.OutputStream;
import java.io.PrintStream;
import java.util.ArrayList;
import java.util.Base64;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;

import com.google.protobuf.DescriptorProtos.DescriptorProto;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import com.google.protobuf.ExtensionRegistry;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorRequest;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse;
import com.google.protobuf.compiler.PluginProtos.CodeGeneratorResponse.File;
import com.hubspot.jinjava.Jinjava;

import com.ixaris.commons.microservices.scslparser.ScslParser;
import com.ixaris.commons.microservices.scslparser.model.ScslDefinition;
import com.ixaris.commons.microservices.scslparser.model.ScslMethod;
import com.ixaris.commons.microservices.scslparser.model.ScslNode;
import com.ixaris.commons.microservices.scslparser.model.ScslParam;
import com.ixaris.commons.microservices.scslparser.model.ScslResource;
import com.ixaris.commons.microservices.scslparser.model.ScslResponses;
import com.ixaris.commons.protobuf.codegen.ProtocPlugin;

import scsl.Scsl;

public final class CodegenMain implements ProtocPlugin {
    
    private static final PrintStream ERROR = System.err;
    
    static {
        @SuppressWarnings("squid:S1943")
        final PrintStream err = new PrintStream(new OutputStream() {
            
            @Override
            public void write(final int b) {}
            
        });
        System.setErr(err);
    }
    
    private static final Jinjava JIN_JAVA = new Jinjava();
    private static final String TAB = "    ";
    private static final List<File> FILES = new ArrayList<>();
    // fully qualified name to java type
    private static final Map<String, String> FQN_PROTO_TO_JAVA = new HashMap<>();
    
    public static class Param extends HashMap<String, String> {
        
        public Param(final String type, final String name) {
            put("type", type);
            put("name", name);
        }
        
    }
    
    public static class PathItem extends HashMap<String, String> {
        
        public PathItem(final String capital, final String name) {
            put("capital", capital);
            put("name", name);
        }
        
    }
    
    public static class ErrorItem extends HashMap<String, String> {
        
        public ErrorItem(final String key, final String type) {
            put("key", key);
            put("type", type);
        }
        
    }
    
    public static void main(final String... args) {
        try {
            final ExtensionRegistry registry = ExtensionRegistry.newInstance();
            Scsl.registerAllExtensions(registry);
            final CodeGeneratorRequest request = CodeGeneratorRequest.parseFrom(System.in, registry);
            final CodeGeneratorResponse.Builder response = process(request,
                args.length > 0 ? args[0] : new String(Base64.getDecoder().decode(request.getParameter().split("=", -1)[1].getBytes(UTF_8)), UTF_8));
            System.out.write(response.build().toByteArray());
        } catch (final Exception e) {
            ERROR.println("Error while generating code from SCSL: " + e.getClass().getSimpleName() + " " + e.getMessage());
            System.exit(-1);
        }
    }
    
    private static CodeGeneratorResponse.Builder process(final CodeGeneratorRequest request, final String baseDir) {
        final CodeGeneratorResponse.Builder codeGeneratorResponse = CodeGeneratorResponse.newBuilder();
        final Set<String> toProcess = new HashSet<>(request.getFileToGenerateList());
        
        request.getProtoFileList().forEach(fileDescriptor -> {
            extractMappings(fileDescriptor);
            
            if (toProcess.contains(fileDescriptor.getName()) && fileDescriptor.getOptions().hasExtension(Scsl.location)) {
                try {
                    parseScslFileAndGenerateServiceInterfaces(fileDescriptor, baseDir);
                } catch (final IOException e) {
                    throw new IllegalStateException(e);
                }
            }
        });
        
        codeGeneratorResponse.addAllFile(FILES);
        
        return codeGeneratorResponse;
    }
    
    private static void extractMappings(final FileDescriptorProto fileDescriptor) {
        final String protoTypePrefix = fileDescriptor.getPackage();
        final String javaTypePrefix = determineJavaInnerPackage(fileDescriptor,
            determineJavaPackage(fileDescriptor),
            determineOuterClassname(fileDescriptor, getOuterClassName(fileDescriptor)));
        extractMappings(fileDescriptor.getMessageTypeList(), protoTypePrefix, javaTypePrefix);
    }
    
    private static void extractMappings(final List<DescriptorProto> descriptors, final String protoPackage, final String javaPackage) {
        descriptors.stream().filter(descriptor -> !descriptor.getOptions().getMapEntry()).forEach(descriptor -> {
            final String nextProtoPackage = concatenatePackage(protoPackage, descriptor.getName());
            final String nextJavaPackage = concatenatePackage(javaPackage, descriptor.getName());
            FQN_PROTO_TO_JAVA.put(nextProtoPackage, nextJavaPackage);
            extractMappings(descriptor.getNestedTypeList(), nextProtoPackage, nextJavaPackage);
        });
    }
    
    private static String resolveJavaType(final String type, final String protoPackage) {
        if (type == null || type.isEmpty()) {
            return "Nil";
        }
        
        return Optional.ofNullable(resolveJavaMapping(type, protoPackage, FQN_PROTO_TO_JAVA)).orElse("UNMATCHED_" + type);
    }
    
    /**
     * Start Here
     *
     * @param fileDescriptor
     * @param baseDir
     * @throws IOException
     */
    private static void parseScslFileAndGenerateServiceInterfaces(final FileDescriptorProto fileDescriptor, final String baseDir) throws IOException {
        final String path = baseDir + java.io.File.separator + determinePath(fileDescriptor) + java.io.File.separator;
        final String file = fileDescriptor.getOptions().getExtension(Scsl.location);
        final java.io.File scslFile = new java.io.File(path + file);
        if (scslFile.exists()) {
            // CODEGEN_LOG.append("PROCESSING SCSL: ").append(scslFile.getCanonicalPath()).append("\n");
            
            final ScslDefinition scslDefinition = ScslParser.parse(file, location -> {
                try {
                    return new FileInputStream(path + location);
                } catch (final FileNotFoundException e) {
                    throw new IllegalStateException(e);
                }
            });
            
            // Extract common information from root node of scsl model
            final Map<String, Object> context = new HashMap<>();
            context.put("contextClassname", resolveJavaType(scslDefinition.getContext(), scslDefinition.getBasePackage()));
            context.put("source", fileDescriptor.getName());
            
            generateDefinition(scslDefinition, context);
            
        } else {
            // CODEGEN_LOG.append("SCSL NOT FOUND: ").append(scslFile.getCanonicalPath()).append("\n");
            throw new IllegalStateException("SCSL file not found: " + scslFile.getCanonicalPath());
        }
    }
    
    private static void generateNode(final String basePackage,
                                     final ScslNode<?> scslNode,
                                     final Map<String, Object> context,
                                     final String tab,
                                     final List<PathItem> path,
                                     final List<Param> params,
                                     final List<String> resourceNodes,
                                     final List<String> stubNodes,
                                     final List<String> skeletonNodes,
                                     final Map<String, String> resourceErrors) {
        final Map<String, Object> nodeContext = new HashMap<>(context);
        final String capitalName = convertNameToType(scslNode.getName());
        final String name = Character.toLowerCase(capitalName.charAt(0)) + capitalName.substring(1);
        nodeContext.put("capitalName", capitalName);
        nodeContext.put("name", name);
        nodeContext.put("hasSecurity", scslNode.getSecurity() != null);
        nodeContext.put("security", scslNode.getSecurity());
        nodeContext.put("hasTags", scslNode.getTags() != null);
        nodeContext.put("tags", scslNode.getTags());
        nodeContext.put("hasDescription", scslNode.getDescription() != null);
        nodeContext.put("description", scslNode.getDescription());
        nodeContext.put("tab", tab);
        
        boolean isParam = scslNode instanceof ScslParam;
        nodeContext.put("isParam", isParam);
        
        if (isParam) {
            final String type = getJavaType(((ScslParam) scslNode).getType());
            nodeContext.put("type", type);
            params.add(new Param(type, name));
        }
        path.add(new PathItem(capitalName, name));
        final List<String> nodeResourceMethods = new ArrayList<>();
        final List<String> nodeStubMethods = new ArrayList<>();
        final List<String> nodeSkeletonMethods = new ArrayList<>();
        final List<String> nodeResourceNodes = new ArrayList<>();
        final List<String> nodeStubNodes = new ArrayList<>();
        final List<String> nodeSkeletonNodes = new ArrayList<>();
        
        generateMethods(basePackage,
            scslNode.getMethods(),
            context,
            tab + TAB,
            params,
            nodeResourceMethods,
            nodeStubMethods,
            nodeSkeletonMethods,
            resourceErrors);
        generateNodes(basePackage,
            scslNode.getParam(),
            scslNode.getSubResources(),
            context,
            tab + TAB,
            path,
            params,
            nodeResourceNodes,
            nodeStubNodes,
            nodeSkeletonNodes,
            resourceErrors);
        
        final Optional<ScslMethod> watch = getWatch(scslNode.getMethods());
        if (watch.isPresent()) {
            nodeContext.put("hasWatch", true);
            nodeContext.put("event", resolveJavaType(watch.get().getResponses().get(ScslResponses.SUCCESS), basePackage));
        } else {
            nodeContext.put("hasWatch", false);
        }
        
        nodeContext.put("methods", nodeResourceMethods);
        nodeContext.put("nodes", nodeResourceNodes);
        resourceNodes.add(JIN_JAVA.render(getTemplate("ScslResourceNodeTemplate.jinja"), nodeContext));
        
        if (params.isEmpty()) {
            nodeContext.put("methods", nodeStubMethods);
            nodeContext.put("nodes", nodeStubNodes);
            nodeContext.put("javaPackage", basePackage);
            nodeContext.put("path", path);
            stubNodes.add(JIN_JAVA.render(getTemplate("ScslStubNodeTemplate.jinja"), nodeContext));
        }
        
        nodeContext.put("methods", nodeSkeletonMethods);
        nodeContext.put("resources", nodeSkeletonNodes);
        skeletonNodes.add(JIN_JAVA.render(getTemplate("ScslSkeletonNodeTemplate.jinja"), nodeContext));
        
        path.remove(path.size() - 1);
        if (isParam) {
            params.remove(params.size() - 1);
        }
    }
    
    private static void generateNodes(final String basePackage,
                                      final ScslParam param,
                                      final Set<ScslResource> resources,
                                      final Map<String, Object> context,
                                      final String tab,
                                      final List<PathItem> path,
                                      final List<Param> params,
                                      final List<String> resourceNodes,
                                      final List<String> stubNodes,
                                      final List<String> skeletonNodes,
                                      final Map<String, String> resourceErrors) {
        if (param != null) {
            generateNode(basePackage,
                param,
                context,
                tab,
                path,
                params,
                resourceNodes,
                stubNodes,
                skeletonNodes,
                resourceErrors);
        }
        resources.forEach(resource -> generateNode(basePackage,
            resource,
            context,
            tab,
            path,
            params,
            resourceNodes,
            stubNodes,
            skeletonNodes,
            resourceErrors));
    }
    
    private static void generateConstants(final Map<String, Object> constantsMap, final List<String> constants) {
        if (constantsMap != null) {
            for (final Map.Entry<String, Object> entry : constantsMap.entrySet()) {
                final Map<String, Object> context = new HashMap<>();
                context.put("key", entry.getKey().toUpperCase());
                context.put("value", entry.getValue());
                try {
                    Long.parseLong(entry.getValue().toString());
                    constants.add(JIN_JAVA.render(getTemplate("ScslConstantTemplateLong.jinja"), context));
                } catch (final NumberFormatException e) {
                    constants.add(JIN_JAVA.render(getTemplate("ScslConstantTemplateString.jinja"), context));
                }
            }
        }
    }
    
    private static void generateMethods(final String basePackage,
                                        final Set<ScslMethod> methods,
                                        final Map<String, Object> context,
                                        final String tab,
                                        final List<Param> params,
                                        final List<String> resourceMethods,
                                        final List<String> stubMethods,
                                        final List<String> skeletonMethods,
                                        final Map<String, String> errors) {
        methods.forEach(method -> {
            final Map<String, Object> methodContext = new HashMap<>(context);
            final String capitalName = convertNameToType(method.getName());
            final String error = method.getResponses().get(ScslResponses.CONFLICT);
            final String errorType = resolveJavaType(error, basePackage);
            final boolean hasError = !"Nil".equals(errorType);
            final String resolvedErrorKey;
            if (hasError) {
                final int lastDotIndex = error.lastIndexOf(".");
                final String lastErrorPart = lastDotIndex > 0 ? error.substring(lastDotIndex + 1) : error;
                resolvedErrorKey = putErrorType(lastErrorPart, errorType, errors, 0);
            } else {
                resolvedErrorKey = null;
            }
            methodContext.put("capitalName", capitalName);
            methodContext.put("name", Character.toLowerCase(capitalName.charAt(0)) + capitalName.substring(1));
            methodContext.put("hasSecurity", method.getSecurity() != null);
            methodContext.put("security", method.getSecurity());
            methodContext.put("hasTags", !method.getTags().isEmpty());
            methodContext.put("tags", method.getTags());
            methodContext.put("success", resolveJavaType(method.getResponses().get(ScslResponses.SUCCESS), basePackage));
            methodContext.put("hasError", hasError);
            methodContext.put("errorKey", resolvedErrorKey);
            methodContext.put("error", errorType);
            methodContext.put("hasRequest", (method.getRequest() != null) && !method.getRequest().isEmpty());
            methodContext.put("request", method.getRequest() == null ? null : resolveJavaType(method.getRequest(), basePackage));
            methodContext.put("hasDescription", method.getDescription() != null);
            methodContext.put("description", method.getDescription());
            methodContext.put("tab", tab);
            methodContext.put("params", params);
            
            if (method.getName().equals("watch")) {
                stubMethods.add(0, JIN_JAVA.render(getTemplate("ScslStubWatchTemplate.jinja"), methodContext));
                skeletonMethods.add(0, JIN_JAVA.render(getTemplate("ScslSkeletonWatchTemplate.jinja"), methodContext));
            } else {
                resourceMethods.add(JIN_JAVA.render(getTemplate("ScslResourceMethodTemplate.jinja"), methodContext));
                skeletonMethods.add(JIN_JAVA.render(getTemplate("ScslSkeletonMethodTemplate.jinja"), methodContext));
            }
        });
    }
    
    private static String putErrorType(final String error, final String errorType, final Map<String, String> errors, final int i) {
        final String key = i == 0 ? error : error + "$" + i;
        final String prevErrorType = errors.get(key);
        if ((prevErrorType != null) && !prevErrorType.equals(errorType)) {
            return putErrorType(error, errorType, errors, i + 1);
        }
        errors.put(key, errorType);
        return key;
    }
    
    private static void generateDefinition(final ScslDefinition scslDefinition, final Map<String, Object> context) {
        final String name = scslDefinition.getName();
        final String capitalName = convertNameToType(name);
        
        final List<PathItem> path = new ArrayList<>();
        path.add(new PathItem(capitalName, name));
        
        final List<Param> params = new ArrayList<>();
        
        final List<String> constants = new ArrayList<>();
        final List<String> resourceMethods = new ArrayList<>();
        final List<String> stubMethods = new ArrayList<>();
        final List<String> skeletonMethods = new ArrayList<>();
        final List<String> resourceNodes = new ArrayList<>();
        final List<String> stubNodes = new ArrayList<>();
        final List<String> skeletonNodes = new ArrayList<>();
        final Map<String, String> resourceErrors = new HashMap<>();
        
        generateConstants(scslDefinition.getConstants(), constants);
        generateMethods(scslDefinition.getBasePackage(),
            scslDefinition.getMethods(),
            context,
            "",
            params,
            resourceMethods,
            stubMethods,
            skeletonMethods,
            resourceErrors);
        generateNodes(scslDefinition.getBasePackage(),
            scslDefinition.getParam(),
            scslDefinition.getSubResources(),
            context,
            "",
            path,
            params,
            resourceNodes,
            stubNodes,
            skeletonNodes,
            resourceErrors);
        
        final Map<String, Object> definitionContext = new HashMap<>(context);
        definitionContext.put("javaPackage", scslDefinition.getBasePackage());
        definitionContext.put("capitalName", capitalName);
        definitionContext.put("name", name);
        definitionContext.put("isSpi", scslDefinition.isSpi());
        definitionContext.put("hasSecurity", scslDefinition.getSecurity() != null);
        definitionContext.put("security", scslDefinition.getSecurity());
        definitionContext.put("hasTags", scslDefinition.getTags() != null);
        definitionContext.put("tags", scslDefinition.getTags());
        definitionContext.put("hasDescription", scslDefinition.getDescription() != null);
        definitionContext.put("description", scslDefinition.getDescription());
        definitionContext.put("constants", constants);
        
        final Optional<ScslMethod> watch = getWatch(scslDefinition.getMethods());
        if (watch.isPresent()) {
            definitionContext.put("hasWatch", true);
            definitionContext.put("event",
                resolveJavaType(watch.get().getResponses().get(ScslResponses.SUCCESS), scslDefinition.getBasePackage()));
        } else {
            definitionContext.put("hasWatch", false);
        }
        
        definitionContext.put("methods", resourceMethods);
        definitionContext.put("nodes", resourceNodes);
        definitionContext.put("errors",
            resourceErrors.entrySet()
                .stream()
                .map(e -> new ErrorItem(e.getKey(), e.getValue()))
                .collect(Collectors.toList()));
        final String renderedResource = JIN_JAVA.render(getTemplate("ScslResourceTemplate.jinja"), definitionContext);
        
        definitionContext.put("nodes", stubNodes);
        definitionContext.put("methods", stubMethods);
        final String renderedStub = JIN_JAVA.render(getTemplate("ScslStubTemplate.jinja"), definitionContext);
        
        definitionContext.put("nodes", skeletonNodes);
        definitionContext.put("methods", skeletonMethods);
        final String renderedSkeleton = JIN_JAVA.render(getTemplate("ScslSkeletonTemplate.jinja"), definitionContext);
        
        FILES.add(File.newBuilder()
            .setName((scslDefinition.getBasePackage() + "/resource/" + convertNameToType(name)).replaceAll("\\.", "/")
                + "Resource.java")
            .setContent(renderedResource)
            .build());
        FILES.add(File.newBuilder()
            .setName((scslDefinition.getBasePackage() + "/client/" + convertNameToType(name)).replaceAll("\\.", "/")
                + "Stub.java")
            .setContent(renderedStub)
            .build());
        FILES.add(File.newBuilder()
            .setName((scslDefinition.getBasePackage() + "/service/" + convertNameToType(name)).replaceAll("\\.", "/")
                + "Skeleton.java")
            .setContent(renderedSkeleton)
            .build());
    }
    
    private static Optional<ScslMethod> getWatch(final Set<ScslMethod> methods) {
        return methods.stream().filter(method -> method.getName().equals("watch")).findAny();
    }
    
    public CodegenMain() {}
    
    @Override
    public String getName() {
        return "scsl";
    }
    
}
