package com.ixaris.commons.protobuf.validationcodegen;

import com.google.common.base.Charsets;
import com.google.common.io.Resources;
import com.google.protobuf.DescriptorProtos.FileDescriptorProto;
import java.io.IOException;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

public final class CodegenHelper {
    
    private static final Map<String, String> TEMPLATES = new HashMap<>();
    private static final Pattern SNAKE_CASE_PATTERN = Pattern.compile("(?:^|-|_)(.)");
    
    public static String getTemplate(final String key) {
        String template = TEMPLATES.get(key);
        if (template == null) {
            try {
                template = Resources.toString(Resources.getResource(key), Charsets.UTF_8);
            } catch (final IOException e) {
                throw new IllegalStateException(e);
            }
            TEMPLATES.put(key, template);
        }
        
        return template;
    }
    
    /**
     * Makes the first letter of the input capital
     *
     * @param input The string to convertNameToType
     * @return The String with the first letter set to capital
     */
    public static String convertNameToType(final String input) {
        // transform aaa_bbb to AaaBbb
        final Matcher m = SNAKE_CASE_PATTERN.matcher(input);
        // StringBuffer used instead of StringBuilder due to api restrictions of Matcher when using appendReplacement
        final StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, m.group(1).toUpperCase());
        }
        m.appendTail(sb);
        return sb.toString();
    }
    
    public static String concatenatePackage(final String p, final String... next) {
        return p + (p.isEmpty() ? "" : ".") + String.join(".", next);
    }
    
    public static String determineJavaPackage(final FileDescriptorProto fileDescriptor) {
        final String javaPackageOption = fileDescriptor.getOptions().getJavaPackage();
        return javaPackageOption.isEmpty() ? fileDescriptor.getPackage() : javaPackageOption;
    }
    
    public static String determineJavaInnerPackage(
        final FileDescriptorProto fileDescriptor, final String javaPackage, final String outerClassname
    ) {
        return fileDescriptor.getOptions().getJavaMultipleFiles()
            ? javaPackage : concatenatePackage(javaPackage, outerClassname);
    }
    
    public static String determineOuterClassname(final FileDescriptorProto fileDescriptor, final String className) {
        final String javaOuterClassnameOption = fileDescriptor.getOptions().getJavaOuterClassname();
        return javaOuterClassnameOption.isEmpty() ? className : javaOuterClassnameOption;
    }
    
    /**
     * @param fileDescriptor The Proto File Descriptor
     * @return The name of the outer class to use when generating the resource
     */
    public static String getOuterClassName(final FileDescriptorProto fileDescriptor) {
        // class name will differ for files that have a message with the same name
        final String className = determineClassName(fileDescriptor);
        return fileDescriptor
            .getMessageTypeList()
            .stream()
            .filter(descriptor -> descriptor.getName().equals(className))
            .findAny()
            .map(descriptor -> className + "OuterClass")
            .orElse(className);
    }
    
    private static String determineClassName(final FileDescriptorProto fileDescriptor) {
        final String filename = fileDescriptor
            .getName()
            .substring(0, fileDescriptor.getName().length() - 6) // remove .proto
            .substring(Math.max(0, fileDescriptor.getName().lastIndexOf('/') + 1)); // remove path
        
        return convertNameToType(filename);
    }
    
    public static String resolveJavaMapping(
        final String type, final String protoPackage, final Map<String, String> mapping
    ) {
        if (type.startsWith(".")) {
            // front to back
            String resolved = mapping.get(type.substring(1));
            if (resolved != null) {
                return resolved;
            }
            String prefix = "";
            final String[] parts = protoPackage.split("\\.");
            for (int i = 0; i < parts.length; i++) {
                prefix += parts[i] + ".";
                resolved = mapping.get(prefix + type);
                if (resolved != null) {
                    return resolved;
                }
            }
        } else {
            // back to front
            String resolved = mapping.get(concatenatePackage(protoPackage, type));
            if (resolved != null) {
                return resolved;
            }
            String prefix = protoPackage + ".";
            final String[] parts = protoPackage.split("\\.");
            for (int i = parts.length - 1; i >= 0; i--) {
                prefix = prefix.substring(0, prefix.length() - parts[i].length() - 1);
                resolved = mapping.get(prefix + type);
                if (resolved != null) {
                    return resolved;
                }
            }
        }
        
        return null;
    }
    
    private CodegenHelper() {}
    
}
