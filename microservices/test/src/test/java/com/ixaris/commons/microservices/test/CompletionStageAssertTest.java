package com.ixaris.commons.microservices.test;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.ixaris.commons.async.lib.CompletionStageUtil;
import com.ixaris.commons.async.test.CompletionStageAssert;
import com.ixaris.commons.microservices.lib.common.exception.ClientConflictException;
import com.ixaris.commons.microservices.test.example.TestData.TestConflict;

/**
 * Tests for {@link CompletionStageAssert}
 *
 * @author <a href="mailto:sarah.cassar@ixaris.com">sarah.cassar</a>
 */
public class CompletionStageAssertTest {
    
    @Test
    public void hasResult_CompletionStageUtilHasResult_shouldSuccessfullyVerifyExistenceOffulfilled() {
        CompletionStageAssert.assertThat(CompletionStageUtil.fulfilled("result")).await().isFulfilled();
    }
    
    @Test
    public void hasResult_CompletionStageUtilDoesNotHaveResult_shouldThrowAssertionError() {
        final TestConflict conflict = TestConflict.newBuilder().setConflict("conflict").build();
        Assertions
            .assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> CompletionStageAssert.assertThat(CompletionStageUtil.rejected(new ClientConflictException(conflict) {})).await().isFulfilled());
    }
    
    @Test
    public void hasResultWithConverter_CompletionStageUtilHasResult_shouldSuccessfullyVerifyExistenceOfResultAndConvert() {
        final String convertedResult = CompletionStageAssert
            .assertThat(CompletionStageUtil.fulfilled("result"))
            .await()
            .isFulfilled(someString -> "converted-value");
        Assertions.assertThat(convertedResult).isEqualTo("converted-value");
    }
    
    @Test
    public void hasResultWithConverter_CompletionStageUtilDoesNotHaveResult_shouldThrowAssertionError() {
        final TestConflict conflict = TestConflict.newBuilder().setConflict("test").build();
        Assertions
            .assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> CompletionStageAssert
                .assertThat(CompletionStageUtil.rejected(new ClientConflictException(conflict) {}))
                .await()
                .isFulfilled(someString -> "converted-value"));
    }
    
    @Test
    public void hasConflict_CompletionStageUtilHasConflict_shouldSuccessfullyVerifyExistenceOfConflict() {
        final TestConflict conflict = TestConflict.newBuilder().setConflict("conflict").build();
        CompletionStageAssert
            .assertThat(CompletionStageUtil.rejected(new ClientConflictException(conflict) {}))
            .await()
            .isRejectedWith(ClientConflictException.class);
    }
    
    @Test
    public void hasConflict_CompletionStageUtilDoesNotHaveConflict_shouldThrowAssertionError() {
        Assertions
            .assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> CompletionStageAssert.assertThat(CompletionStageUtil.fulfilled("result")).await().isRejectedWith(ClientConflictException.class));
    }
    
    @Test
    public void hasConflictWithConverter_CompletionStageUtilHasConflict_shouldSuccessfullyVerifyExistenceOfConflictAndConvert() {
        final TestConflict conflict = TestConflict.newBuilder().setConflict("conflict").build();
        final String convertedConflict = CompletionStageAssert
            .assertThat(CompletionStageUtil.rejected(new ClientConflictException(conflict) {}))
            .await()
            .isRejectedWith(ClientConflictException.class, c -> "converted-conflict");
        Assertions.assertThat(convertedConflict).isEqualTo("converted-conflict");
    }
    
    @Test
    public void hasConflictWithConverter_CompletionStageUtilDoesNotHaveconflict_shouldThrowAssertionError() {
        Assertions
            .assertThatExceptionOfType(AssertionError.class)
            .isThrownBy(() -> CompletionStageAssert
                .assertThat(CompletionStageUtil.fulfilled("result"))
                .await()
                .isRejectedWith(ClientConflictException.class, c -> "converted-conflict"));
    }
    
}
