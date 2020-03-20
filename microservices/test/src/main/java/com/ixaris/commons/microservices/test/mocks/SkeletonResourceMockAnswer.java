package com.ixaris.commons.microservices.test.mocks;

import static org.mockito.internal.progress.ThreadSafeMockingProgress.mockingProgress;

import java.lang.invoke.MethodHandles.Lookup;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationHandler;
import java.lang.reflect.InvocationTargetException;
import java.lang.reflect.Method;
import java.lang.reflect.Parameter;
import java.lang.reflect.Proxy;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CompletionStage;

import org.mockito.Answers;
import org.mockito.ArgumentMatcher;
import org.mockito.Mockito;
import org.mockito.internal.creation.bytebuddy.MockAccess;
import org.mockito.internal.matchers.Equals;
import org.mockito.internal.matchers.LocalizedMatcher;
import org.mockito.invocation.InvocationOnMock;
import org.mockito.stubbing.Answer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.microservices.lib.service.ServiceSkeleton;
import com.ixaris.commons.misc.lib.exception.ExceptionUtil;

import net.bytebuddy.ByteBuddy;
import net.bytebuddy.description.modifier.TypeManifestation;
import net.bytebuddy.description.modifier.Visibility;
import net.bytebuddy.dynamic.loading.ClassLoadingStrategy;

public final class SkeletonResourceMockAnswer implements Answer<Object>, InvocationHandler {
    
    private static final Logger LOG = LoggerFactory.getLogger(SkeletonResourceMockAnswer.class);
    
    static <T> T mockAndWrap(final Class<T> mockType, final boolean parametrised) {
        return mockAndWrap(mockType, parametrised, null);
    }
    
    @SuppressWarnings("unchecked")
    static <T> T mockAndWrap(final Class<T> mockType, final boolean parametrised, final Answer nonResourceAnswer) {
        if (!mockType.isInterface()) {
            throw new UnsupportedOperationException("Unable to mock non-interface");
        }
        
        // Create a private interface in the same package as the target class, so that the created proxy will be in the
        // same package as the class being mocked
        final Class<?> packageSetterInterface = getOrCreatePrivateInterfaceInSamePackageAs(mockType);
        
        // MockAccess added to allow mockito to identify the proxy as a mock (discovered from trawling through source)
        return (T) Proxy.newProxyInstance(mockType.getClassLoader(),
            new Class<?>[] { MockAccess.class, packageSetterInterface, mockType },
            new SkeletonResourceMockAnswer(mockType, parametrised, nonResourceAnswer));
    }
    
    private static Class<?> getOrCreatePrivateInterfaceInSamePackageAs(final Class<?> classToMock) {
        final String expectedInterfaceName = String.format("%s.PackageSetter%s", classToMock.getPackage().getName(), classToMock.getSimpleName());
        try {
            return Class.forName(expectedInterfaceName);
        } catch (final ClassNotFoundException e) {
            return new ByteBuddy()
                .makeInterface()
                .modifiers(TypeManifestation.INTERFACE, Visibility.PRIVATE)
                .name(expectedInterfaceName)
                .make()
                .load(classToMock.getClassLoader(), ClassLoadingStrategy.Default.INJECTION)
                .getLoaded();
        }
    }
    
    @SuppressWarnings("unchecked")
    static <T> T unwrap(final T mock) {
        if (Proxy.isProxyClass(mock.getClass())) {
            final InvocationHandler invocationHandler = Proxy.getInvocationHandler(mock);
            if (invocationHandler instanceof SkeletonResourceMockAnswer) {
                return (T) ((SkeletonResourceMockAnswer) invocationHandler).target;
            }
        }
        
        return mock;
    }
    
    private static boolean isMethodEqualsOrHashCodeOrToString(final String methodBeingInvokedOnMock) {
        return "equals".equals(methodBeingInvokedOnMock)
            || "hashCode".equals(methodBeingInvokedOnMock)
            || "toString".equals(methodBeingInvokedOnMock);
    }
    
    private static boolean isHeaderFirstParameter(final Parameter[] parameters) {
        return (parameters.length > 0) && ServiceOperationHeader.class.equals(parameters[0].getType());
    }
    
    private static boolean isServiceSkeletonMock(final Object mockInvoked) {
        return ServiceSkeleton.class.isAssignableFrom(mockInvoked.getClass());
    }
    
    private final Class<?> mockType;
    private final boolean parametrised;
    private final Answer nonResourceAnswer;
    private final Object target;
    private final Map<Object, Object> mocks = new HashMap<>();
    
    SkeletonResourceMockAnswer(final Class<?> mockType, final boolean parametrised) {
        this(mockType, parametrised, null);
    }
    
    SkeletonResourceMockAnswer(final Class<?> mockType, final boolean parametrised, final Answer nonResourceAnswer) {
        this.mockType = mockType;
        this.parametrised = parametrised;
        this.nonResourceAnswer = nonResourceAnswer;
        this.target = Mockito.mock(mockType, this);
    }
    
    @Override
    public Object answer(final InvocationOnMock invocation) throws Throwable {
        if (nonResourceAnswer != null) {
            return nonResourceAnswer.answer(invocation);
        } else {
            LOG.warn(
                "Unmocked operation [{}] with params [{}] on mock [{}]{}, rejecting answer",
                invocation.getMethod().getName(),
                invocation.getArguments(),
                mockType,
                parametrised ? " with path params " + SkeletonResourceMock.getPathParams() : "");
            if (invocation.getMethod().getReturnType().equals(Async.class) || invocation.getMethod().getReturnType().equals(CompletionStage.class)) {
                return Async.rejected(new IllegalStateException(String.format("Operation [%s] on resource [%s]%s not mocked.%s",
                    invocation.getMethod().getName(),
                    mockType,
                    parametrised ? " with path params " + SkeletonResourceMock.getPathParams() : "",
                    parametrised
                        ? " NOTE that mocking r(anyLong()).op1() and r(eq(123)).op2() means that r(123).op1() is not mocked because mock for r(eq(123)) will match when op1() was mocked on r(anyLong())"
                        : "")));
            } else {
                return Answers.RETURNS_DEFAULTS.answer(invocation);
            }
        }
    }
    
    @Override
    public Object invoke(final Object proxy, final Method method, final Object[] args) throws Throwable {
        // forward any MockAccess method to the wrapped mock
        if (MockAccess.class == method.getDeclaringClass()) {
            return method.invoke(target, args);
        }
        
        final String methodBeingInvokedOnMock = method.getName();
        
        if ("handle".equals(methodBeingInvokedOnMock) || "aroundAsync".equals(methodBeingInvokedOnMock)) {
            return callDefaultMethod(proxy, method, args);
        }
        
        if (isServiceSkeletonMock(proxy)) {
            SkeletonResourceMock.clearPathParams();
            
            if ("resource".equals(methodBeingInvokedOnMock) && method.getReturnType().isAssignableFrom(proxy.getClass())) {
                return proxy;
            }
        }
        
        if (!isMethodEqualsOrHashCodeOrToString(methodBeingInvokedOnMock) && !isHeaderFirstParameter(method.getParameters())) {
            // this must be an invocation on a resource method since other operations have at least 2 parameters
            return handleInvocationOfResourceMethod(method, args);
        }
        
        try {
            return method.invoke(target, args);
        } catch (final InvocationTargetException e) {
            throw ExceptionUtil.sneakyThrow(e.getCause());
        }
    }
    
    private Object callDefaultMethod(final Object proxy, final Method method, final Object[] args) throws Throwable {
        final Class<?> mockClass = proxy.getClass();
        return getLookupWithCorrectPrivileges(mockClass)
            .in(mockClass)
            .unreflectSpecial(method, mockClass)
            .bindTo(proxy)
            .invokeWithArguments(args);
    }
    
    private Lookup getLookupWithCorrectPrivileges(final Class<?> mockClass) throws NoSuchMethodException, IllegalAccessException, InvocationTargetException, InstantiationException {
        // hack to allow default method to be called on proxy,
        // fails due to getOrCreatePrivateInterfaceInSamePackageAs() used to set the proxy's package
        final Constructor<Lookup> constructor = Lookup.class.getDeclaredConstructor(Class.class);
        constructor.setAccessible(true);
        return constructor.newInstance(mockClass);
    }
    
    private Object handleInvocationOfResourceMethod(final Method method, final Object[] args) {
        final Class<?> resourceType = method.getReturnType();
        
        if (method.getParameterCount() == 0) {
            return getMockForResourceType(resourceType);
        } else {
            return getMockForParameterisedResourceType(resourceType, args[0]);
        }
    }
    
    private <T> Object getMockForResourceType(final Class<T> resourceType) {
        Object mock = mocks.get(resourceType);
        
        if (mock == null) {
            if (!SkeletonResourceMock.isStubbing() && !SkeletonResourceMock.isVerifying()) {
                LOG.warn("You have invoked a resource method [{}] without properly stubbing it first."
                    + " Are you sure you did not mean to specify what should be returned when this method is invoked using SkeletonResourceMock.doAnswer, or a similar method?",
                    resourceType);
            }
            mock = mockAndWrap(resourceType, parametrised);
            mocks.put(resourceType, mock);
        }
        
        return mock;
    }
    
    private <T> Object getMockForParameterisedResourceType(final Class<T> resourceType, final Object invocationParameter) {
        final ParameterisedMocks parameterisedMocks = (ParameterisedMocks) mocks.computeIfAbsent(resourceType, k -> new ParameterisedMocks());
        
        if (SkeletonResourceMock.isStubbing() || SkeletonResourceMock.isVerifying()) {
            return getParameterisedMockForStubbingOrVerifying(resourceType, invocationParameter, parameterisedMocks);
        } else {
            return getParameterisedMockForInvocation(resourceType, invocationParameter, parameterisedMocks);
        }
    }
    
    private Object getParameterisedMockForStubbingOrVerifying(final Class<?> resourceType,
                                                              final Object invocationParameter,
                                                              final ParameterisedMocks parameterisedMocks) {
        final ArgumentMatcher<?> matcher;
        final List<LocalizedMatcher> matchers = mockingProgress().getArgumentMatcherStorage().pullLocalizedMatchers();
        if (matchers.isEmpty()) {
            matcher = new Equals(invocationParameter);
        } else if (matchers.size() == 1) {
            matcher = matchers.get(0).getMatcher();
        } else {
            throw new IllegalStateException("Expected 1 argument matcher but found " + matchers.size());
        }
        
        final Object mockForGivenMatcher = parameterisedMocks.getMock(matcher);
        if (mockForGivenMatcher != null) {
            return mockForGivenMatcher;
        }
        
        final Object mock = mockAndWrap(resourceType, true);
        parameterisedMocks.addMock(matcher, mock);
        return mock;
    }
    
    private Object getParameterisedMockForInvocation(final Class<?> resourceType, final Object pathParameter, final ParameterisedMocks parameterisedMocks) {
        SkeletonResourceMock.addPathParams(pathParameter);
        
        final Object mockForGivenPathParam = parameterisedMocks.getMock(pathParameter);
        if (mockForGivenPathParam != null) {
            return mockForGivenPathParam;
        }
        
        throw new IllegalStateException(String.format("Resource [%s] path [%s] with parameter matching [%s] not mocked", mockType, resourceType, pathParameter));
    }
    
}
