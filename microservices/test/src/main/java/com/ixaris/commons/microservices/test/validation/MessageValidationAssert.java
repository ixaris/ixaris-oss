package com.ixaris.commons.microservices.test.validation;

import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectAssert;

import valid.Valid.FieldValidationErrors;
import valid.Valid.MessageValidation;

/**
 * Custom assertion for {@link MessageValidation}. This assertion is intended to be used with {@link FieldValidationAssert} in order to chain
 * assertions on the expected invalid fields.
 *
 * <p>Example of usage:
 *
 * <pre>{@code
 * MessageValidationAssert.assertThat(messageValidationForInvalidMessage)
 *      .hasInvalidFieldWithErrorsCount("s", 1) // Start FieldValidationAssert on field 's'
 *      .hasRequiredConstraintViolation()
 *      .hasInvalidFieldWithErrorsCount("another_s", 1) // Start FieldValidationAssert on field 'another_s'
 *      .hasRequiredConstraintViolation();
 *
 * }</pre>
 *
 * @author <a href="mailto:sarah.cassar@ixaris.com">sarah.cassar</a>
 * @see FieldValidationAssert
 */
public class MessageValidationAssert extends ObjectAssert<MessageValidation> {
    
    public MessageValidationAssert(final MessageValidation actual) {
        super(actual);
    }
    
    public static MessageValidationAssert assertThat(final MessageValidation actual) {
        return new MessageValidationAssert(actual);
    }
    
    public MessageValidationAssert isInvalidWithFieldsCount(final int expectedNumberOfInvalidFields) {
        
        Assertions.assertThat(actual.getInvalid()).as("Expecting Message Validation to be invalid.").isTrue();
        Assertions
            .assertThat(actual.getFieldsCount())
            .as("Expecting Message Validation to have at least one invalid field.")
            .isGreaterThan(0)
            .as("Unexpected number of invalid fields")
            .isEqualTo(expectedNumberOfInvalidFields);
        
        return this;
    }
    
    public FieldValidationAssert hasInvalidFieldWithErrorsCount(final String expectedInvalidFieldName, final int expectedNumberOfErrorsOnField) {
        
        final List<FieldValidationErrors> fieldValidations = actual
            .getFieldsList()
            .stream()
            .filter(fieldValidation -> expectedInvalidFieldName.equals(fieldValidation.getName()))
            .collect(Collectors.toList());
        
        Assertions
            .assertThat(fieldValidations)
            .as(String.format("No validations found for field [%s].", expectedInvalidFieldName))
            .isNotEmpty()
            .as(String.format("Expecting only one field validation entry for field [%s].", expectedInvalidFieldName))
            .hasSize(1);
        
        return new FieldValidationAssert(this, fieldValidations.get(0)).hasErrorsCount(expectedNumberOfErrorsOnField);
    }
    
}
