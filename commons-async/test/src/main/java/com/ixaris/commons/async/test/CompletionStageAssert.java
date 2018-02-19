package com.ixaris.commons.async.test;

import java.util.concurrent.CompletionStage;

import org.assertj.core.api.ObjectAssert;

/**
 * Assertions for the {@link CompletionStage}. This class <strong>will block eagerly</strong> until a result/error/timeout happens, and once this is done, it will assert
 * on the result of this wait. This means that if the promise is fulfilled after the timeout, this assertion class <strong>will NOT</strong> reflect that.
 *
 * Example usage: <pre>{@code
 *   // can be constructed via the {@link CompletionStageAssert } class using {@link CompletionStageAssert#await}
 *   CompletionStageAssert
 *      .assertThat(someCompletionStage)
 *      .await()
 *      .isFulfilled()
 *      .isInstanceOf(String.class)
 *      .isEqualTo("completed!");
 *
 *   CompletionStageAssert
 *      .assertThat(someCompletionStage)
 *      .await()
 *      .isRejected()
 *      .isInstanceOf(RuntimeException.class)
 *      .hasMessageContaining("failure");
 *
 *   // can also accept a timeout specifying how long to wait before starting to assert
 *   // this will block the calling thread
 *   CompletionStageAssert
 *      .assertThat(somePromise)
 *      .await(1000)
 *      .isTimedOut();
 * }</pre>
 *
 * If the assertions on the resolved result of the completion stage need to be done using some other custom assertion, a conversion function
 * may be passed to the {@code isFulfilled()} function to allow chaining as follows:
 * <pre>{@code
 *   CompletionStageAssert
 *      .assertThat(someCompletionStage)
 *      .await(1000L)
 *      .isFulfilled(CustomAssert::assertThat); // The successful result of the promise will be converted into a CustomAssert.
 * }</pre>
 */
public final class CompletionStageAssert<T> extends ObjectAssert<CompletionStage<T>> {
    
    public static <T> CompletionStageAssert<T> assertThat(final CompletionStage<T> actual) {
        return new CompletionStageAssert<>(actual);
    }
    
    private CompletionStageAssert(final CompletionStage<T> actual) {
        super(actual);
    }
    
    public DoneCompletionStageAssert<T> await() {
        return new DoneCompletionStageAssert<>(actual);
    }
    
    public DoneCompletionStageAssert<T> await(final long timeoutMs) {
        return new DoneCompletionStageAssert<>(actual, timeoutMs);
    }
    
}
