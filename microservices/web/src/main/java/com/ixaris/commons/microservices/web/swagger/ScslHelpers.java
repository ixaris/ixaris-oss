package com.ixaris.commons.microservices.web.swagger;

import java.util.regex.Matcher;
import java.util.regex.Pattern;

import com.ixaris.commons.microservices.lib.client.ServiceStub;
import com.ixaris.commons.microservices.lib.service.ServiceSkeleton;
import com.ixaris.commons.microservices.scslparser.model.ScslDefinition;

/**
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public final class ScslHelpers {
    
    private ScslHelpers() {}
    
    private static final Pattern SNAKE_CASE_PATTERN = Pattern.compile("(?:^|-|_)(.)");
    
    @SuppressWarnings({ "unchecked", "squid:S2658" })
    public static Class<? extends ServiceSkeleton> extractSkeletonInterfaceFromScslDefinition(final ScslDefinition scslDefinition) {
        final String resourceClassName = scslDefinition.getBasePackage()
            + ".service."
            + convertNameToType(scslDefinition.getName())
            + "Skeleton";
        final Class<? extends ServiceSkeleton> resourceType;
        try {
            resourceType = (Class<? extends ServiceSkeleton>) Class.forName(resourceClassName, false, Thread.currentThread().getContextClassLoader());
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException("Unable to resolve skeleton class", e);
        }
        return resourceType;
    }
    
    @SuppressWarnings({ "unchecked", "squid:S2658" })
    public static Class<? extends ServiceStub> extractStubInterfaceFromScslDefinition(final ScslDefinition scslDefinition) {
        final String resourceClassName = scslDefinition.getBasePackage()
            + ".client."
            + convertNameToType(scslDefinition.getName())
            + "Stub";
        final Class<? extends ServiceStub> resourceType;
        try {
            resourceType = (Class<? extends ServiceStub>) Class.forName(resourceClassName, false, Thread.currentThread().getContextClassLoader());
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException("Unable to resolve skeleton class", e);
        }
        return resourceType;
    }
    
    @SuppressWarnings({ "unchecked", "squid:S2658" })
    public static Class<? extends ServiceSkeleton> extractResourceInterfaceFromScslDefinition(final ScslDefinition scslDefinition) {
        final String resourceClassName = scslDefinition.getBasePackage()
            + ".resource."
            + convertNameToType(scslDefinition.getName())
            + "Resource";
        final Class<? extends ServiceSkeleton> resourceType;
        try {
            resourceType = (Class<? extends ServiceSkeleton>) Class.forName(resourceClassName, false, Thread.currentThread().getContextClassLoader());
        } catch (final ClassNotFoundException e) {
            throw new IllegalStateException("Unable to resolve resource class", e);
        }
        return resourceType;
    }
    
    // Copied from com.ixaris.commons.protobuf.validationcodegen.CodegenHelper.convertNameToType to avoid extra dep
    private static String convertNameToType(final String input) {
        // transform aaa_bbb to AaaBbb
        final Matcher m = SNAKE_CASE_PATTERN.matcher(input);
        // StringBuffer is used instead of StringBuilder due to the api restrictions of Matcher when using
        // appendReplacement
        final StringBuffer sb = new StringBuffer();
        while (m.find()) {
            m.appendReplacement(sb, m.group(1).toUpperCase());
        }
        m.appendTail(sb);
        return sb.toString();
    }
    
}
