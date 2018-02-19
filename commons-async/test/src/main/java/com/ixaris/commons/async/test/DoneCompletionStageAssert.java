package com.ixaris.commons.async.test;

import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Function;

import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.ObjectAssert;

import com.ixaris.commons.async.lib.CompletionStageUtil;
import com.ixaris.commons.misc.lib.exception.ExceptionUtil;

public final class DoneCompletionStageAssert<T> extends ObjectAssert<CompletionStage<T>> {
    
    private T result;
    private Throwable throwable;
    private boolean timedOut;
    
    protected DoneCompletionStageAssert(final CompletionStage<T> actual) {
        super(actual);
        try {
            result = CompletionStageUtil.block(actual);
        } catch (final Throwable t) { // NOSONAR
            throwable = t;
        }
    }
    
    protected DoneCompletionStageAssert(final CompletionStage<T> actual, final long timeoutMs) {
        super(actual);
        try {
            result = CompletionStageUtil.block(actual, timeoutMs, TimeUnit.MILLISECONDS);
        } catch (final InterruptedException e) {
            Thread.currentThread().interrupt();
            timedOut = true;
        } catch (final TimeoutException e) {
            timedOut = true;
        } catch (final Throwable t) { // NOSONAR
            throwable = t;
        }
    }
    
    /**
     * Assert that the promise has been fulfilled (i.e. completed successfully with a result) and convert this result using the provided converter function.
     */
    public <U> U isFulfilled(final Function<T, U> resultConverter) {
        if (timedOut || throwable != null) {
            failWithMessage("Expected promise [%s] to be fulfilled - instead it was [%s]", actual, getFormattedOutcomeMessage());
        }
        
        return resultConverter.apply(result);
    }
    
    /**
     * Assert that the promise has been fulfilled (i.e. completed successfully with a result)
     */
    public ObjectAssert<T> isFulfilled() {
        return isFulfilled(ObjectAssert::new);
    }
    
    /**
     * Assert that the promise has been completed with an exception and convert the failure using the provided converter function.
     */
    public <U> U isRejected(final Function<Throwable, U> failureConverter) {
        if (timedOut || throwable == null) {
            failWithMessage("Expected promise [%s] to be rejected - instead it was [%s]", actual, getFormattedOutcomeMessage());
        }
        
        return failureConverter.apply(throwable);
    }
    
    /**
     * Assert that the promise has been completed with a failure
     */
    public ExceptionAssert<?> isRejected() {
        return isRejected(ExceptionAssert::new);
    }
    
    @SuppressWarnings("unchecked")
    public <E extends Throwable> ExceptionAssert<E> isRejectedWith(final Class<E> exceptionType) {
        return (ExceptionAssert<E>) isRejected().isInstanceOf(exceptionType);
    }
    
    @SuppressWarnings("unchecked")
    public <E extends Throwable, U> U isRejectedWith(final Class<E> exceptionType, final Function<E, U> assertFunction) {
        isRejected().isInstanceOf(exceptionType);
        return assertFunction.apply((E) throwable);
    }
    
    /**
     * Assert that the promise has timed out, and we have no result or failure yet.
     */
    @SuppressWarnings("unchecked")
    public DoneCompletionStageAssert<T> isTimedOut() {
        if (!timedOut) {
            failWithMessage("Expected promise [%s] to be timed out, instead it was [%s]", actual, getFormattedOutcomeMessage());
        }
        return this;
    }
    
    private String getFormattedOutcomeMessage() {
        if (timedOut) {
            return "Timed Out";
        } else if (throwable == null) {
            return String.format("Fulfilled with result [%s]", result);
        } else {
            return Optional.ofNullable(getFormattedThrowableMessage(throwable)).orElse(
                String.format("Rejected with exception [%s]%n%s", throwable.getMessage(), ExceptionUtil.getStackTrace(throwable)));
        }
    }
    
    /**
     * @param t the throwable
     * @return a custom message for the throwable, or null to use the default rejection message
     */
    protected String getFormattedThrowableMessage(final Throwable t) {
        return null;
    }
    
    public static final class ExceptionAssert<E extends Throwable> extends AbstractThrowableAssert<ExceptionAssert<E>, E> {
        
        public ExceptionAssert(final E actual) {
            super(actual, ExceptionAssert.class);
        }
        
    }
}
