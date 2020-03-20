package com.ixaris.commons.microservices.test.mocks;

import org.mockito.exceptions.stacktrace.StackTraceCleaner;
import org.mockito.internal.exceptions.stacktrace.DefaultStackTraceCleaner;
import org.mockito.plugins.StackTraceCleanerProvider;

public final class SkeletonResourceMockStackTraceCleanerProvider implements StackTraceCleanerProvider {
    
    private static final DefaultStackTraceCleaner DEFAULT_STACK_TRACE_CLEANER = new DefaultStackTraceCleaner();
    
    private static final StackTraceCleaner STACK_TRACE_CLEANER =
        stackTraceElement -> DEFAULT_STACK_TRACE_CLEANER.isIn(stackTraceElement) && !isFromSkeletonResourceMockStubber(stackTraceElement.getClassName());
    
    private static boolean isFromSkeletonResourceMockStubber(String className) {
        return className.startsWith("com.ixaris.commons.microservices.test.mocks.SkeletonResourceMockStubber");
    }
    
    @Override
    public StackTraceCleaner getStackTraceCleaner(final StackTraceCleaner stackTraceCleaner) {
        return STACK_TRACE_CLEANER;
    }
    
}
