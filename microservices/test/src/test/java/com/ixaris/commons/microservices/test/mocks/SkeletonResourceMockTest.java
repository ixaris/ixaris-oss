package com.ixaris.commons.microservices.test.mocks;

import static com.ixaris.commons.async.lib.Async.rejected;
import static com.ixaris.commons.async.lib.Async.result;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.anyLong;
import static org.mockito.Mockito.times;

import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.mockito.exceptions.misusing.UnfinishedStubbingException;
import org.mockito.exceptions.verification.NoInteractionsWanted;

import com.ixaris.commons.async.test.CompletionStageAssert;
import com.ixaris.commons.microservices.lib.common.Nil;
import com.ixaris.commons.microservices.test.mocks.TestResource.NestedResource;

public class SkeletonResourceMockTest {
    
    @Test
    public void mockService_getPackageOnMock_shouldReturnNonNullPackage() {
        final TestSkeleton testResource = SkeletonResourceMock.mock(TestSkeleton.class);
        Assertions
            .assertThat(testResource.getClass().getPackage())
            .isNotNull()
            .isEqualTo(TestSkeleton.class.getPackage());
    }
    
    @Test
    public void mockService_invokeUnparameterisedResourceMethodWithoutStubbing_shouldReturnNonNullMock() {
        final TestSkeleton testResource = SkeletonResourceMock.mock(TestSkeleton.class);
        final NestedResource mock = testResource.resource().nested();
        Assertions.assertThat(mock).isNotNull();
    }
    
    @Test(expected = IllegalStateException.class)
    public void mockService_invokeParameterisedResourceMethodOnMockWithoutStubbing_shouldThrowException() {
        final TestSkeleton testResource = SkeletonResourceMock.mock(TestSkeleton.class);
        testResource.resource().nested().nestedId(1L);
    }
    
    @Test
    public void mockService_invokeOperationWithoutStubbing_shouldBeRejected() {
        final TestSkeleton testResource = SkeletonResourceMock.mock(TestSkeleton.class);
        CompletionStageAssert
            .assertThat(testResource.resource().nested().operation(null))
            .await()
            .isRejectedWith(IllegalStateException.class)
            .hasMessage("Operation [operation] on resource [interface com.ixaris.commons.microservices.test.mocks.TestResource$NestedResource] not mocked.");
    }
    
    @Test
    public void mockService_invokeOperationOnparametrisedWithoutStubbing_shouldBeRejected() {
        final TestSkeleton testResource = SkeletonResourceMock.mock(TestSkeleton.class);
        SkeletonResourceMock
            .doAnswer((p, i) -> Nil.getAsyncInstance())
            .when(testResource.resource().nested().nestedId(anyLong()).moreNested().moreNestedId(anyLong()))
            .operation(any());
        CompletionStageAssert
            .assertThat(testResource.resource().nested().nestedId(1L).operation(null))
            .await()
            .isRejectedWith(IllegalStateException.class)
            .hasMessageStartingWith(
                "Operation [operation] on resource [interface com.ixaris.commons.microservices.test.mocks.TestResource$NestedResource$NestedIdResource] with path params [1] not mocked. NOTE that mocking");
    }
    
    @Test
    public void mockService_doAnswer_secondWork() {
        final TestSkeleton testResource = SkeletonResourceMock.mock(TestSkeleton.class);
        SkeletonResourceMock
            .doAnswer((p, i) -> Nil.getAsyncInstance())
            .when(testResource.resource().nested())
            .operation(any());
        
        CompletionStageAssert.assertThat(testResource.resource().nested().operation(null)).await().isFulfilled();
    }
    
    @Test
    public void mockService_throwInDoAnswer_shouldReject() {
        final TestSkeleton testResource = SkeletonResourceMock.mock(TestSkeleton.class);
        SkeletonResourceMock
            .doAnswer((p, i) -> {
                throw new IllegalStateException("Thrown");
            })
            .when(testResource.resource().nested())
            .operation(any());
        
        Assertions
            .assertThat(Assertions.catchThrowable(() -> testResource.resource().nested().operation(null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Thrown");
    }
    
    @Test
    public void mockService_doThrow_shouldThrow() {
        final TestSkeleton testResource = SkeletonResourceMock.mock(TestSkeleton.class);
        SkeletonResourceMock
            .doThrow(new IllegalStateException("Thrown"))
            .when(testResource.resource().nested())
            .operation(any());
        
        Assertions
            .assertThat(Assertions.catchThrowable(() -> testResource.resource().nested().operation(null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Thrown");
    }
    
    @Test
    public void mockService_doAnswerTwice_secondShouldOverrideFirst() {
        final TestSkeleton testResource = SkeletonResourceMock.mock(TestSkeleton.class);
        SkeletonResourceMock
            .doAnswer((p, i) -> Nil.getAsyncInstance())
            .when(testResource.resource().nested())
            .operation(any());
        
        CompletionStageAssert.assertThat(testResource.resource().nested().operation(null)).await().isFulfilled();
        
        SkeletonResourceMock
            .doAnswer((p, i) -> rejected(new IllegalStateException("Overridden")))
            .when(testResource.resource().nested())
            .operation(any());
        
        CompletionStageAssert
            .assertThat(testResource.resource().nested().operation(null))
            .await()
            .isRejectedWith(IllegalStateException.class)
            .hasMessage("Overridden");
        
        SkeletonResourceMock
            .doThrow(new IllegalStateException("Overridden2"))
            .when(testResource.resource().nested())
            .operation(any());
        
        Assertions
            .assertThat(Assertions.catchThrowable(() -> testResource.resource().nested().operation(null)))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("Overridden2");
    }
    
    @Test
    public void mockService_doAnswerTwiceWithParam_secondShouldOverrideFirst() {
        final TestSkeleton testResource = SkeletonResourceMock.mock(TestSkeleton.class);
        
        SkeletonResourceMock
            .doAnswer((p, i) -> Nil.getAsyncInstance())
            .when(testResource.resource().nested().nestedId(1L))
            .operation(any());
        
        CompletionStageAssert
            .assertThat(testResource.resource().nested().nestedId(1L).operation(null))
            .await()
            .isFulfilled();
        
        SkeletonResourceMock
            .doAnswer((p, i) -> rejected(new IllegalStateException("Overridden")))
            .when(testResource.resource().nested().nestedId(1L))
            .operation(any());
        
        CompletionStageAssert
            .assertThat(testResource.resource().nested().nestedId(1L).operation(null))
            .await()
            .isRejectedWith(IllegalStateException.class)
            .hasMessage("Overridden");
        
        SkeletonResourceMock
            .doAnswer((p, i) -> Nil.getAsyncInstance())
            .when(testResource.resource().nested().nestedId(anyLong()))
            .operation(any());
        
        CompletionStageAssert
            .assertThat(testResource.resource().nested().nestedId(2L).operation(null))
            .await()
            .isFulfilled();
        
        SkeletonResourceMock
            .doAnswer((p, i) -> rejected(new IllegalStateException("Overridden2")))
            .when(testResource.resource().nested().nestedId(anyLong()))
            .operation(any());
        
        CompletionStageAssert
            .assertThat(testResource.resource().nested().nestedId(2L).operation(null))
            .await()
            .isRejectedWith(IllegalStateException.class)
            .hasMessage("Overridden2");
    }
    
    @Test
    public void mockService_doAnswerWithSkeletonResourceMockOperation_shouldRetainPathParameters() {
        final TestSkeleton testResource = SkeletonResourceMock.mock(TestSkeleton.class);
        final List<List<Object>> allPathParams = new LinkedList<>();
        
        SkeletonResourceMock
            .doAnswer((p, i) -> {
                allPathParams.add(Arrays.asList(p.get(0), p.get(1)));
                return Nil.getAsyncInstance();
            })
            .when(testResource.resource().nested().nestedId(anyLong()).moreNested().moreNestedId(anyLong()))
            .operation(any());
        
        SkeletonResourceMock
            .doAnswer((p, i) -> {
                allPathParams.add(Arrays.asList(p.get(0), p.get(1)));
                return Nil.getAsyncInstance();
            })
            .when(testResource.resource().nested().nestedId(1L).moreNested().moreNestedId(1L))
            .operation(any());
        
        SkeletonResourceMock
            .doAnswer((p, i) -> {
                allPathParams.add(Arrays.asList(p.get(0), p.get(1)));
                return Nil.getAsyncInstance();
            })
            .when(testResource.resource().nested().nestedId(2L).moreNested().moreNestedId(anyLong()))
            .operation(any());
        
        SkeletonResourceMock
            .doAnswer((p, i) -> {
                allPathParams.add(Arrays.asList(p.get(0), p.get(1)));
                return Nil.getAsyncInstance();
            })
            .when(testResource.resource().nested().nestedId(2L).moreNested().moreNestedId(2L))
            .operation(any());
        
        testResource.resource().nested().nestedId(100L).moreNested().moreNestedId(100L).operation(null);
        testResource.resource().nested().nestedId(1L).moreNested().moreNestedId(1L).operation(null);
        testResource.resource().nested().nestedId(2L).moreNested().moreNestedId(100L).operation(null);
        testResource.resource().nested().nestedId(2L).moreNested().moreNestedId(2L).operation(null);
        
        Assertions.assertThat(allPathParams.size()).isEqualTo(4);
        
        final List<Object> pathParamsForFirstInvocation = allPathParams.get(0);
        Assertions.assertThat(pathParamsForFirstInvocation.size()).isEqualTo(2);
        Assertions.assertThat(pathParamsForFirstInvocation.get(0)).isEqualTo(100L);
        Assertions.assertThat(pathParamsForFirstInvocation.get(1)).isEqualTo(100L);
        
        final List<Object> pathParamsForSecondInvocation = allPathParams.get(1);
        Assertions.assertThat(pathParamsForSecondInvocation.size()).isEqualTo(2);
        Assertions.assertThat(pathParamsForSecondInvocation.get(0)).isEqualTo(1L);
        Assertions.assertThat(pathParamsForSecondInvocation.get(1)).isEqualTo(1L);
        
        final List<Object> pathParamsForThirdInvocation = allPathParams.get(2);
        Assertions.assertThat(pathParamsForThirdInvocation.size()).isEqualTo(2);
        Assertions.assertThat(pathParamsForThirdInvocation.get(0)).isEqualTo(2L);
        Assertions.assertThat(pathParamsForThirdInvocation.get(1)).isEqualTo(100L);
        
        final List<Object> pathParamsForFourthInvocation = allPathParams.get(3);
        Assertions.assertThat(pathParamsForFourthInvocation.size()).isEqualTo(2);
        Assertions.assertThat(pathParamsForFourthInvocation.get(0)).isEqualTo(2L);
        Assertions.assertThat(pathParamsForFourthInvocation.get(1)).isEqualTo(2L);
        
        SkeletonResourceMock
            .verify()
            .called(testResource.resource().nested().nestedId(anyLong()).moreNested().moreNestedId(anyLong()))
            .operation(any());
        SkeletonResourceMock
            .verify()
            .called(testResource.resource().nested().nestedId(1L).moreNested().moreNestedId(1L))
            .operation(any());
        SkeletonResourceMock
            .verify()
            .called(testResource.resource().nested().nestedId(2L).moreNested().moreNestedId(anyLong()))
            .operation(any());
        SkeletonResourceMock
            .verify()
            .called(testResource.resource().nested().nestedId(2L).moreNested().moreNestedId(2L))
            .operation(any());
    }
    
    @Test
    public void mockService_unmocked_shouldNotAffectPathParameters() {
        final TestSkeleton testResource = SkeletonResourceMock.mock(TestSkeleton.class);
        final List<List<Object>> allPathParams = new LinkedList<>();
        
        SkeletonResourceMock
            .doAnswer((p, i) -> {
                allPathParams.add(Arrays.asList(p.get(0), p.get(1)));
                return Nil.getAsyncInstance();
            })
            .when(testResource.resource().nested().nestedId(1L).moreNested().moreNestedId(1L))
            .operation(any());
        
        testResource.resource().nested().nestedId(1L).moreNested().moreNestedId(1L).operation(null);
        testResource.resource().nested().nestedId(1L).moreNested().moreNestedId(1L).otherOperation(null);
        testResource.resource().nested().nestedId(1L).moreNested().moreNestedId(1L).operation(null);
        
        Assertions.assertThat(allPathParams.size()).isEqualTo(2);
        
        final List<Object> pathParamsForFirstInvocation = allPathParams.get(0);
        Assertions.assertThat(pathParamsForFirstInvocation.size()).isEqualTo(2);
        Assertions.assertThat(pathParamsForFirstInvocation.get(0)).isEqualTo(1L);
        Assertions.assertThat(pathParamsForFirstInvocation.get(1)).isEqualTo(1L);
        
        final List<Object> pathParamsForSecondInvocation = allPathParams.get(1);
        Assertions.assertThat(pathParamsForSecondInvocation.size()).isEqualTo(2);
        Assertions.assertThat(pathParamsForSecondInvocation.get(0)).isEqualTo(1L);
        Assertions.assertThat(pathParamsForSecondInvocation.get(1)).isEqualTo(1L);
        
        SkeletonResourceMock
            .verify()
            .called(testResource.resource().nested().nestedId(1L).moreNested().moreNestedId(1L), times(2))
            .operation(any());
        SkeletonResourceMock
            .verify()
            .called(testResource.resource().nested().nestedId(1L).moreNested().moreNestedId(1L))
            .otherOperation(any());
        
        allPathParams.clear();
        
        testResource.resource().nested().nestedId(1L).moreNested().moreNestedId(1L).operation(null);
        
        Assertions.assertThat(allPathParams.size()).isEqualTo(1);
        
        final List<Object> pathParamsForNewInvocation = allPathParams.get(0);
        Assertions.assertThat(pathParamsForNewInvocation.size()).isEqualTo(2);
        Assertions.assertThat(pathParamsForNewInvocation.get(0)).isEqualTo(1L);
        Assertions.assertThat(pathParamsForNewInvocation.get(1)).isEqualTo(1L);
    }
    
    @Test
    public void mockService_nestedCall_shouldHaveCorrectPathParameters() {
        final TestSkeleton testResource = SkeletonResourceMock.mock(TestSkeleton.class);
        final List<List<Object>> allPathParams = new LinkedList<>();
        
        SkeletonResourceMock
            .doAnswer((p, i) -> {
                allPathParams.add(Arrays.asList(p.get(0), p.get(1)));
                return testResource.resource().nested().nestedId(1L).moreNested().moreNestedId(2L).operation(null);
            })
            .when(testResource.resource().nested().nestedId(1L).moreNested().moreNestedId(1L))
            .operation(any());
        
        SkeletonResourceMock
            .doAnswer((p, i) -> {
                allPathParams.add(Arrays.asList(p.get(0), p.get(1)));
                return Nil.getAsyncInstance();
            })
            .when(testResource.resource().nested().nestedId(1L).moreNested().moreNestedId(2L))
            .operation(any());
        
        testResource.resource().nested().nestedId(1L).moreNested().moreNestedId(1L).operation(null);
        
        Assertions.assertThat(allPathParams.size()).isEqualTo(2);
        
        final List<Object> pathParamsForFirstInvocation = allPathParams.get(0);
        Assertions.assertThat(pathParamsForFirstInvocation.size()).isEqualTo(2);
        Assertions.assertThat(pathParamsForFirstInvocation.get(0)).isEqualTo(1L);
        Assertions.assertThat(pathParamsForFirstInvocation.get(1)).isEqualTo(1L);
        
        final List<Object> pathParamsForNestedInvocation = allPathParams.get(0);
        Assertions.assertThat(pathParamsForNestedInvocation.size()).isEqualTo(2);
        Assertions.assertThat(pathParamsForNestedInvocation.get(0)).isEqualTo(1L);
        Assertions.assertThat(pathParamsForNestedInvocation.get(1)).isEqualTo(1L);
        
        SkeletonResourceMock
            .verify()
            .called(testResource.resource().nested().nestedId(1L).moreNested().moreNestedId(1L))
            .operation(any());
        SkeletonResourceMock
            .verify()
            .called(testResource.resource().nested().nestedId(1L).moreNested().moreNestedId(2L))
            .operation(any());
    }
    
    // @Test
    // public void mockServiceCallbackResultMockAnswer_nestedCall_shouldHaveCorrectResponse() {
    // final TestSkeleton testResource = SkeletonResourceMock.mock(TestSkeleton.class);
    //
    // SkeletonResourceMock.doAnswer(CallbackResultMockAnswer.success(com.ixaris.commons.async.reactive.Nil.getInstance()))
    // .when(testResource.resource().nested().nestedId(1L).moreNested().moreNestedId(1L))
    // .operation(any(), any());
    //
    // testResource.resource().nested().nestedId(1L).moreNested().moreNestedId(1L).operation(null, callback);
    //
    // SkeletonResourceMock.verify().called(callback).result(eq(com.ixaris.commons.async.reactive.Nil.getInstance()));
    // }
    
    @Test
    public void mockService_multithreaded_shouldHaveCorrectVerification() throws InterruptedException {
        
        final TestSkeleton testResource = SkeletonResourceMock.mock(TestSkeleton.class);
        final List<List<Object>> allPathParams = new LinkedList<>();
        SkeletonResourceMock
            .doAnswer((p, i) -> {
                synchronized (allPathParams) {
                    allPathParams.add(Arrays.asList(p.get(0), p.get(1)));
                }
                return Nil.getAsyncInstance();
            })
            .when(testResource.resource().nested().nestedId(1L).moreNested().moreNestedId(1L))
            .operation(any());
        
        final TestThread[] threads = new TestThread[10];
        for (int i = 0; i < threads.length; i++) {
            threads[i] = new TestThread(testResource);
            threads[i].start();
        }
        for (final TestThread thread : threads) {
            thread.join();
        }
        
        Assertions.assertThat(allPathParams.size()).isEqualTo(threads.length);
        for (int i = 0; i < threads.length; i++) {
            final List<Object> pathParamsForThreadInvocation = allPathParams.get(i);
            Assertions.assertThat(pathParamsForThreadInvocation.size()).isEqualTo(2);
            Assertions.assertThat(pathParamsForThreadInvocation.get(0)).isEqualTo(1L);
            Assertions.assertThat(pathParamsForThreadInvocation.get(1)).isEqualTo(1L);
        }
        
        SkeletonResourceMock.verify().noMoreInteractions(testResource.resource().nested().nestedId(1L));
        
        try {
            SkeletonResourceMock
                .verify()
                .noMoreInteractions(testResource.resource().nested().nestedId(1L).moreNested().moreNestedId(1L));
        } catch (final NoInteractionsWanted ignored) {
            // this should be thrown
        }
        
        SkeletonResourceMock
            .verify()
            .called(testResource.resource().nested().nestedId(1L).moreNested().moreNestedId(1L), times(threads.length))
            .operation(any());
    }
    
    @Test
    public void mockService_noMoreInteractions_shouldHaveCorrectVerification() throws InterruptedException {
        final TestSkeleton testResource = SkeletonResourceMock.mock(TestSkeleton.class);
        
        SkeletonResourceMock.verify().noMoreInteractions(testResource.resource().nested().nestedId(1L));
    }
    
    @Test
    public void mockService_unfinishedStubbing_shouldThrowException() throws InterruptedException {
        
        final TestSkeleton testResource = SkeletonResourceMock.mock(TestSkeleton.class);
        
        SkeletonResourceMock
            .doAnswer((p, i) -> {
                throw new RuntimeException();
            })
            .when(testResource.resource().nested().nestedId(1L).moreNested().moreNestedId(1L))
            .operation(any());
        
        SkeletonResourceMock
            .doAnswer((p, i) -> {
                throw new RuntimeException();
            })
            .when(testResource.resource().nested().nestedId(2L).moreNested().moreNestedId(1L));
        
        try {
            SkeletonResourceMock
                .doAnswer((p, i) -> {
                    throw new RuntimeException();
                })
                .when(testResource.resource().nested().nestedId(3).moreNested().moreNestedId(1L))
                .operation(any());
        } catch (final UnfinishedStubbingException e) {
            e.printStackTrace();
        }
        
        SkeletonResourceMock
            .doAnswer((p, i) -> {
                throw new RuntimeException();
            })
            .when(testResource.resource().nested().nestedId(4).moreNested().moreNestedId(1L))
            .operation(any());
    }
    
    private static class TestThread extends Thread {
        
        private final TestSkeleton testResource;
        
        public TestThread(final TestSkeleton testResource) {
            this.testResource = testResource;
        }
        
        @Override
        public void run() {
            testResource.resource().nested().nestedId(1L).moreNested().moreNestedId(1L).operation(null);
        }
        
    }
    
}
