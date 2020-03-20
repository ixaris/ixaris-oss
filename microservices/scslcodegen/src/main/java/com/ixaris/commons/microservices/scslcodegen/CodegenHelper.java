package com.ixaris.commons.microservices.scslcodegen;

import java.util.Arrays;

import com.google.protobuf.DescriptorProtos.FileDescriptorProto;

public final class CodegenHelper {
    
    /**
     * Resolves the Java type from the Protobuf type
     *
     * @param type The protobuf type
     * @return the java type
     */
    public static String getJavaType(final String type) {
        if (Arrays.asList("double", "float").contains(type)) {
            return type;
        } else if (Arrays.asList("int32", "uint32", "sint32", "fixed32", "sfixed32").contains(type)) {
            return "int";
        } else if (Arrays.asList("int64", "uint64", "sint64", "fixed64", "sfixed64").contains(type)) {
            return "long";
        } else if ("bool".equals(type)) {
            return "boolean";
        } else if ("bytes".equals(type)) {
            return "ByteString";
        } else if ("string".equals(type)) {
            return "String";
        } else {
            throw new IllegalStateException("Unknown protobuf type: " + type);
        }
    }
    
    public static String determinePath(final FileDescriptorProto fileDescriptor) {
        return fileDescriptor.getName().substring(0, Math.max(0, fileDescriptor.getName().lastIndexOf('/') + 1));
    }
    
    private CodegenHelper() {}
    
}
