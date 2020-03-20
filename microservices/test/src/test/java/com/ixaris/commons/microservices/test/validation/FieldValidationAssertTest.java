package com.ixaris.commons.microservices.test.validation;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import valid.Valid.FieldValidation;
import valid.Valid.FieldValidationErrors;
import valid.Valid.MessageValidation;

/**
 * Tests for {@link FieldValidationAssert}.
 *
 * @author <a href="mailto:sarah.cassar@ixaris.com">sarah.cassar</a>
 */
public class FieldValidationAssertTest {
    
    @Test
    public void hasRequiredConstraintViolation_invalidFieldDoesNotHaveRequiredConstraintViolation_shouldThrowAssertionError() {
        
        Assertions
            .assertThatThrownBy(() -> {
                final MessageValidation messageValidationForInvalidMessage = MessageValidation.newBuilder()
                    .setInvalid(true)
                    .addFields(FieldValidationErrors.newBuilder()
                        .setName("s")
                        .addErrors(FieldValidation.newBuilder().setType(FieldValidation.Type.SIZE).addParams("").addParams("3").build())
                        .build())
                    .build();
                
                MessageValidationAssert
                    .assertThat(messageValidationForInvalidMessage)
                    .hasInvalidFieldWithErrorsCount("s", 1)
                    .hasRequiredConstraintViolation();
            })
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining(String.format("%s constraint violation for field [s] not found.", FieldValidation.Type.REQUIRED));
    }
    
    @Test
    public void hasRequiredConstraintViolation_invalidFieldWithRequiredConstraintViolation_shouldSuccessfullyVerifyFieldValidation() {
        
        final MessageValidation messageValidationForInvalidMessage = MessageValidation.newBuilder()
            .setInvalid(true)
            .addFields(FieldValidationErrors.newBuilder()
                .setName("s")
                .addErrors(FieldValidation.newBuilder().setType(FieldValidation.Type.REQUIRED).build())
                .build())
            .build();
        
        MessageValidationAssert
            .assertThat(messageValidationForInvalidMessage)
            .hasInvalidFieldWithErrorsCount("s", 1)
            .hasRequiredConstraintViolation();
    }
    
    @Test
    public void hasHasTextConstraintViolation_invalidFieldWithHasTextConstraintViolation_shouldSuccessfullyVerifyFieldValidation() {
        
        final MessageValidation messageValidationForInvalidMessage = MessageValidation.newBuilder()
            .setInvalid(true)
            .addFields(FieldValidationErrors.newBuilder()
                .setName("s")
                .addErrors(FieldValidation.newBuilder().setType(FieldValidation.Type.HAS_TEXT).build())
                .build())
            .build();
        
        MessageValidationAssert
            .assertThat(messageValidationForInvalidMessage)
            .hasInvalidFieldWithErrorsCount("s", 1)
            .hasHasTextConstraintViolation();
    }
    
    @Test
    public void hasSizeConstraintViolation_invalidFieldDoesNotHaveSizeConstraintViolation_shouldThrowAssertionError() {
        
        Assertions
            .assertThatThrownBy(() -> {
                final MessageValidation messageValidationForInvalidMessage = MessageValidation.newBuilder()
                    .setInvalid(true)
                    .addFields(FieldValidationErrors.newBuilder()
                        .setName("s")
                        .addErrors(FieldValidation.newBuilder().setType(FieldValidation.Type.REQUIRED).build())
                        .build())
                    .build();
                
                MessageValidationAssert
                    .assertThat(messageValidationForInvalidMessage)
                    .hasInvalidFieldWithErrorsCount("s", 1)
                    .hasSizeConstraintViolation();
            })
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining(String.format("%s constraint violation for field [s] not found.", FieldValidation.Type.SIZE));
    }
    
    @Test
    public void hasSizeConstraintViolation_invalidFieldWithSizeConstraintViolation_shouldSuccessfullyVerifyFieldValidations() {
        
        final MessageValidation messageValidationForInvalidMessage = MessageValidation.newBuilder()
            .setInvalid(true)
            .addFields(FieldValidationErrors.newBuilder()
                .setName("s")
                .addErrors(FieldValidation.newBuilder().setType(FieldValidation.Type.SIZE).build())
                .build())
            .build();
        
        MessageValidationAssert
            .assertThat(messageValidationForInvalidMessage)
            .hasInvalidFieldWithErrorsCount("s", 1)
            .hasSizeConstraintViolation();
    }
    
    @Test
    public void hasRangeConstraintViolation_invalidFieldWithRangeConstraintViolation_shouldSuccessfullyVerifyFieldValidations() {
        final MessageValidation messageValidationForInvalidMessage = MessageValidation.newBuilder()
            .setInvalid(true)
            .addFields(FieldValidationErrors.newBuilder()
                .setName("d")
                .addErrors(FieldValidation.newBuilder().setType(FieldValidation.Type.RANGE).build())
                .build())
            .build();
        
        MessageValidationAssert
            .assertThat(messageValidationForInvalidMessage)
            .hasInvalidFieldWithErrorsCount("d", 1)
            .hasRangeConstraintViolation();
    }
    
    @Test
    public void hasRegexConstraintViolation_invalidFieldDoesNotHaveRegexConstraintViolation_shouldThrowAssertionError() {
        
        Assertions
            .assertThatThrownBy(() -> {
                final MessageValidation messageValidationForInvalidMessage = MessageValidation.newBuilder()
                    .setInvalid(true)
                    .addFields(FieldValidationErrors.newBuilder()
                        .setName("s")
                        .addErrors(FieldValidation.newBuilder().setType(FieldValidation.Type.REQUIRED).build())
                        .build())
                    .build();
                
                MessageValidationAssert
                    .assertThat(messageValidationForInvalidMessage)
                    .hasInvalidFieldWithErrorsCount("s", 1)
                    .hasRegexConstraintViolation();
            })
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining(String.format("%s constraint violation for field [s] not found.", FieldValidation.Type.REGEX));
    }
    
    @Test
    public void hasRegexConstraintViolation_invalidFieldWithRegexConstraintViolation_shouldSuccessfullyVerifyFieldValidations() {
        
        final MessageValidation messageValidationForInvalidMessage = MessageValidation.newBuilder()
            .setInvalid(true)
            .addFields(FieldValidationErrors.newBuilder()
                .setName("s")
                .addErrors(FieldValidation.newBuilder().setType(FieldValidation.Type.REGEX).build())
                .build())
            .build();
        
        MessageValidationAssert
            .assertThat(messageValidationForInvalidMessage)
            .hasInvalidFieldWithErrorsCount("s", 1)
            .hasRegexConstraintViolation();
    }
    
    @Test
    public void chainedAssertions_shouldSuccessfullyVerifyFieldValidations() {
        
        final MessageValidation messageValidationForInvalidMessage = MessageValidation.newBuilder()
            .setInvalid(true)
            .addFields(FieldValidationErrors.newBuilder()
                .setName("s")
                .addErrors(FieldValidation.newBuilder().setType(FieldValidation.Type.REQUIRED).build())
                .build())
            .addFields(FieldValidationErrors.newBuilder()
                .setName("another_s")
                .addErrors(FieldValidation.newBuilder().setType(FieldValidation.Type.SIZE).build())
                .build())
            .build();
        
        MessageValidationAssert
            .assertThat(messageValidationForInvalidMessage)
            .hasInvalidFieldWithErrorsCount("s", 1)
            .hasRequiredConstraintViolation()
            .hasInvalidFieldWithErrorsCount("another_s", 1)
            .hasSizeConstraintViolation();
    }
    
}
