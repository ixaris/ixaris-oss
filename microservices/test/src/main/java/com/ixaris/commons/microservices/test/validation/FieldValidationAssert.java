package com.ixaris.commons.microservices.test.validation;

import java.util.List;
import java.util.stream.Collectors;

import org.assertj.core.api.Assertions;
import org.assertj.core.api.ObjectAssert;

import valid.Valid.FieldValidation;
import valid.Valid.FieldValidationErrors;

/**
 * Custom assertion for a {@link FieldValidation}. This assertion may be used together with the {@link MessageValidationAssert} to enable chained
 * assertions on invalid fields.
 *
 * <p>Example of usage:
 *
 * <pre>{@code
 * MessageValidationAssert.assertThat(MessageValidation.getDefaultInstance())
 *         .isInvalidWithFieldsCount(1)
 *         .hasInvalidFieldWithErrorsCount("s", 1) // Start FieldValidationAssert on field "s"
 *         .hasRequiredConstraintViolation();
 *
 * }</pre>
 *
 * Field assertions can be chained together by using the {@code hasInvalidFieldWithErrorsCount}.
 *
 * @author <a href="mailto:sarah.cassar@ixaris.com">sarah.cassar</a>
 */
public class FieldValidationAssert extends ObjectAssert<FieldValidationErrors> {
    
    private final MessageValidationAssert messageValidationAssert;
    
    public FieldValidationAssert(final MessageValidationAssert messageValidationAssert, final FieldValidationErrors actual) {
        super(actual);
        this.messageValidationAssert = messageValidationAssert;
    }
    
    protected FieldValidationAssert hasErrorsCount(final int expectedNumberOfErrors) {
        Assertions
            .assertThat(actual.getErrorsCount())
            .as(String.format("Unexpected number of errors count for field [%s].", actual.getName()))
            .isEqualTo(expectedNumberOfErrors);
        return this;
    }
    
    public FieldValidationAssert hasInvalidFieldWithErrorsCount(final String expectedInvalidFieldName, final int expectedNumberOfErrorsOnField) {
        return messageValidationAssert.hasInvalidFieldWithErrorsCount(expectedInvalidFieldName, expectedNumberOfErrorsOnField);
    }
    
    public FieldValidationAssert hasRequiredConstraintViolation() {
        return hasConstraintViolation(FieldValidation.Type.REQUIRED);
    }
    
    public FieldValidationAssert hasHasTextConstraintViolation() {
        return hasConstraintViolation(FieldValidation.Type.HAS_TEXT);
    }
    
    public FieldValidationAssert hasSizeConstraintViolation() {
        return hasConstraintViolation(FieldValidation.Type.SIZE);
    }
    
    public FieldValidationAssert hasRangeConstraintViolation() {
        return hasConstraintViolation(FieldValidation.Type.RANGE);
    }
    
    public FieldValidationAssert hasRegexConstraintViolation() {
        return hasConstraintViolation(FieldValidation.Type.REGEX);
    }
    
    public FieldValidationAssert hasInConstraintViolation() {
        return hasConstraintViolation(FieldValidation.Type.IN);
    }
    
    public FieldValidationAssert hasNotInConstraintViolation() {
        return hasConstraintViolation(FieldValidation.Type.NOT_IN);
    }
    
    private FieldValidationAssert hasConstraintViolation(final FieldValidation.Type type) {
        final List<FieldValidation> fieldValidationErrors = actual
            .getErrorsList()
            .stream()
            .filter(fieldValidationError -> fieldValidationError.getType() == type)
            .collect(Collectors.toList());
        
        Assertions
            .assertThat(fieldValidationErrors)
            .as(String.format("%s constraint violation for field [%s] not found.", type, actual.getName()))
            .isNotEmpty()
            .as(String.format("More than one %s constraint violation found for field [%s].", type, actual.getName()))
            .hasSize(1);
        
        return this;
    }
}
