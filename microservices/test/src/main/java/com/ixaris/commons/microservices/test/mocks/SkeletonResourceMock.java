package com.ixaris.commons.microservices.test.mocks;

import java.util.ArrayList;
import java.util.List;

import org.mockito.stubbing.Answer;

import com.ixaris.commons.microservices.lib.service.ServiceSkeleton;

/**
 * This is used to mock a service implementation. See README.md for examples.
 *
 * <p>To mock multiple SPI implementations, create interface extending the required ServiceProviderSkeleton interface in its own sub-package and
 * define the keys in service_keys for the sub-packages. This is because the mock will take the package of the implemented interface.
 */
public final class SkeletonResourceMock {
    
    public static final class InvocationPathParams {
        
        private InvocationPathParams() {}
        
        @SuppressWarnings("unchecked")
        public <T> T get(final int index) {
            return (T) PATH_PARAMS.get().get(index);
        }
        
        public int size() {
            return PATH_PARAMS.get().size();
        }
        
    }
    
    private static final ThreadLocal<Boolean> STUBBING = ThreadLocal.withInitial(() -> false);
    
    private static final ThreadLocal<Boolean> VERIFYING = ThreadLocal.withInitial(() -> false);
    
    private static final ThreadLocal<List<Object>> PATH_PARAMS = ThreadLocal.withInitial(ArrayList::new);
    
    private SkeletonResourceMock() {}
    
    public static <T extends ServiceSkeleton> T mock(final Class<T> mockType) {
        return SkeletonResourceMockAnswer.mockAndWrap(mockType, false);
    }
    
    public static <T extends ServiceSkeleton> T mock(final Class<T> mockType, final Answer nonResourceAnswer) {
        return SkeletonResourceMockAnswer.mockAndWrap(mockType, false, nonResourceAnswer);
    }
    
    public static SkeletonResourceMockStubber doAnswer(final AnswerWithPathParamsAndHeaderAndRequest answer) {
        return new SkeletonResourceMockStubber(new InvocationPathParams()).doAnswer(answer);
    }
    
    public static SkeletonResourceMockStubber doAnswer(final AnswerWithPathParams answer) {
        return new SkeletonResourceMockStubber(new InvocationPathParams()).doAnswer(answer);
    }
    
    public static SkeletonResourceMockStubber doAnswer(final Answer answer) {
        return new SkeletonResourceMockStubber(new InvocationPathParams()).doAnswer(answer);
    }
    
    public static SkeletonResourceMockStubber doCallRealMethod() {
        return new SkeletonResourceMockStubber(new InvocationPathParams()).doCallRealMethod();
    }
    
    public static SkeletonResourceMockStubber doNothing() {
        return new SkeletonResourceMockStubber(new InvocationPathParams()).doNothing();
    }
    
    public static SkeletonResourceMockStubber doReturn(final Object toBeReturned) {
        return new SkeletonResourceMockStubber(new InvocationPathParams()).doReturn(toBeReturned);
    }
    
    public static SkeletonResourceMockStubber doReturn(final Object toBeReturned, final Object... nextToBeReturned) {
        return new SkeletonResourceMockStubber(new InvocationPathParams()).doReturn(toBeReturned, nextToBeReturned);
    }
    
    public static SkeletonResourceMockStubber doThrow(final Class<? extends Throwable> toBeThrown) {
        return new SkeletonResourceMockStubber(new InvocationPathParams()).doThrow(toBeThrown);
    }
    
    @SafeVarargs
    public static SkeletonResourceMockStubber doThrow(final Class<? extends Throwable> toBeThrown,
                                                      final Class<? extends Throwable>... nextToBeThrown) {
        return new SkeletonResourceMockStubber(new InvocationPathParams()).doThrow(toBeThrown, nextToBeThrown);
    }
    
    public static SkeletonResourceMockStubber doThrow(final Throwable... toBeThrown) {
        return new SkeletonResourceMockStubber(new InvocationPathParams()).doThrow(toBeThrown);
    }
    
    public static SkeletonResourceMockVerifier verify() {
        return new SkeletonResourceMockVerifier();
    }
    
    static boolean isStubbing() {
        return STUBBING.get();
    }
    
    static boolean isVerifying() {
        return VERIFYING.get();
    }
    
    public static List<Object> getPathParams() {
        return PATH_PARAMS.get();
    }
    
    static void clearPathParams() {
        PATH_PARAMS.remove();
    }
    
    static void addPathParams(final Object param) {
        PATH_PARAMS.get().add(param);
    }
    
    static void setStubbing(final boolean isStubbing) {
        STUBBING.set(isStubbing);
    }
    
    static void setVerifying(final boolean isVerifying) {
        VERIFYING.set(isVerifying);
    }
    
}
