package com.ixaris.commons.microservices.test.validation;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.ixaris.commons.microservices.test.example.TestData.TestMessage;
import com.ixaris.commons.protobuf.lib.MessageValidator;

import valid.Valid.FieldValidation;
import valid.Valid.FieldValidationErrors;
import valid.Valid.MessageValidation;

/**
 * Tests for {@link MessageValidationAssert}
 *
 * @author <a href="mailto:sarah.cassar@ixaris.com">sarah.cassar</a>
 */
public class MessageValidationAssertTest {
    
    @Test
    public void isInvalidWithFieldsCount_validMessage_shouldThrowAssertionError() {
        
        Assertions
            .assertThatThrownBy(() -> {
                final MessageValidation messageValidationForValidMessage = MessageValidator.validate(TestMessage.getDefaultInstance());
                MessageValidationAssert.assertThat(messageValidationForValidMessage).isInvalidWithFieldsCount(0);
            })
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Expecting Message Validation to be invalid.");
    }
    
    @Test
    public void isInvalidWithFieldsCount_invalidMessageWithUnexpectedNumberOfInvalidFields_shouldThrowAssertionError() {
        
        Assertions
            .assertThatThrownBy(() -> {
                final MessageValidation messageValidationForInvalidMessage = MessageValidation.newBuilder()
                    .setInvalid(true)
                    .addFields(FieldValidationErrors.newBuilder()
                        .setName("s")
                        .addErrors(FieldValidation.newBuilder().setType(FieldValidation.Type.REQUIRED).build())
                        .build())
                    .build();
                
                MessageValidationAssert.assertThat(messageValidationForInvalidMessage).isInvalidWithFieldsCount(2);
            })
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Unexpected number of invalid fields");
    }
    
    @Test
    public void isInvalidWithFieldsCount_invalidMessageWithExpectedNumberOfInvalidFields_shouldSuccessfullyVerifyValidationMessage() {
        
        final MessageValidation messageValidationForInvalidMessage = MessageValidation.newBuilder()
            .setInvalid(true)
            .addFields(FieldValidationErrors.newBuilder()
                .setName("s")
                .addErrors(FieldValidation.newBuilder().setType(FieldValidation.Type.REQUIRED).build())
                .build())
            .build();
        
        MessageValidationAssert.assertThat(messageValidationForInvalidMessage).isInvalidWithFieldsCount(1);
    }
    
    @Test
    public void hasInvalidFieldsWithErrorsCount_invalidMessageDoesNotContainExpectedField_shouldThrowAssertionError() {
        
        Assertions
            .assertThatThrownBy(() -> {
                final MessageValidation messageValidationForInvalidMessage = MessageValidation.newBuilder()
                    .setInvalid(true)
                    .addFields(FieldValidationErrors.newBuilder()
                        .setName("s")
                        .addErrors(FieldValidation.newBuilder().setType(FieldValidation.Type.REQUIRED).build())
                        .build())
                    .build();
                
                MessageValidationAssert.assertThat(messageValidationForInvalidMessage).hasInvalidFieldWithErrorsCount("some-field", 0);
            })
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("No validations found for field [some-field].");
    }
    
    @Test
    public void hasInvalidFieldsWithErrorsCount_invalidMessageContainsExpectedFieldButIsValid_shouldThrowAssertionError() {
        Assertions
            .assertThatThrownBy(() -> {
                final MessageValidation messageValidationForInvalidMessage = MessageValidation.newBuilder()
                    .setInvalid(true)
                    .addFields(FieldValidationErrors.newBuilder()
                        .setName("s2")
                        .addErrors(FieldValidation.newBuilder().setType(FieldValidation.Type.REQUIRED).build())
                        .build())
                    .build();
                
                MessageValidationAssert.assertThat(messageValidationForInvalidMessage).hasInvalidFieldWithErrorsCount("s", 0);
            })
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("No validations found for field [s].");
    }
    
    @Test
    public void hasInvalidFieldsWithErrorsCount_invalidMessageContainsExpectedInvalidFieldWithUnexpectedErrorsCount_shouldThrowAssertionError() {
        
        Assertions
            .assertThatThrownBy(() -> {
                final MessageValidation messageValidationForInvalidMessage = MessageValidation.newBuilder()
                    .setInvalid(true)
                    .addFields(FieldValidationErrors.newBuilder()
                        .setName("s")
                        .addErrors(FieldValidation.newBuilder().setType(FieldValidation.Type.REQUIRED).build())
                        .build())
                    .build();
                
                MessageValidationAssert.assertThat(messageValidationForInvalidMessage).hasInvalidFieldWithErrorsCount("s", 2);
            })
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Unexpected number of errors count for field [s].");
    }
    
    @Test
    public void hasInvalidFieldWithErrorsCount_invalidMessageContainsExpectedInvalidFieldWithExpectedErrorsCount_shouldSuccessfullyVerifyValidationMessage() {
        
        final MessageValidation messageValidationForInvalidMessage = MessageValidation.newBuilder()
            .setInvalid(true)
            .addFields(FieldValidationErrors.newBuilder()
                .setName("s")
                .addErrors(FieldValidation.newBuilder().setType(FieldValidation.Type.REQUIRED).build())
                .build())
            .build();
        
        MessageValidationAssert.assertThat(messageValidationForInvalidMessage).hasInvalidFieldWithErrorsCount("s", 1);
    }
}
