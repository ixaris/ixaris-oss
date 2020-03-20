package com.ixaris.commons.microservices.test;

import org.assertj.core.api.AbstractThrowableAssert;
import org.assertj.core.api.Assertions;

import com.ixaris.commons.microservices.lib.common.exception.ClientConflictException;
import com.ixaris.commons.microservices.lib.common.exception.ClientInvalidRequestException;
import com.ixaris.commons.microservices.lib.common.exception.ServiceException;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;
import com.ixaris.commons.microservices.test.validation.MessageValidationAssert;

import valid.Valid.MessageValidation;

/**
 * Custom assertion on a thrown {@link ServiceException}.
 *
 * @author <a href="mailto:sarah.cassar@ixaris.com">sarah.cassar</a>
 */
public class ServiceExceptionAssert extends AbstractThrowableAssert<ServiceExceptionAssert, Exception> {
    
    public static ServiceExceptionAssert assertThat(final ServiceException serviceException) {
        return new ServiceExceptionAssert(serviceException);
    }
    
    public static ServiceExceptionAssert assertThat(final ClientConflictException serviceException) {
        return new ServiceExceptionAssert(serviceException);
    }
    
    public ServiceExceptionAssert(final ServiceException actual) {
        super(actual, ServiceExceptionAssert.class);
    }
    
    public ServiceExceptionAssert(final ClientConflictException actual) {
        super(actual, ServiceExceptionAssert.class);
    }
    
    public ServiceExceptionAssert hasStatusCode(final ResponseStatusCode expectedResponseStatusCode) {
        final ResponseStatusCode code = actual instanceof ServiceException
            ? ((ServiceException) actual).getStatusCode() : ResponseStatusCode.CLIENT_CONFLICT;
        Assertions.assertThat(code).as("Unexpected status code.").isEqualTo(expectedResponseStatusCode);
        return this;
    }
    
    public MessageValidationAssert isThrownDueToAClientInvalidRequestWithMessageValidations() {
        hasStatusCode(ResponseStatusCode.CLIENT_INVALID_REQUEST);
        try {
            final MessageValidation messageValidation = ((ClientInvalidRequestException) actual).getMessageValidation();
            Assertions
                .assertThat(messageValidation)
                .as(String.format("Expecting %s to have a non-null %s message.",
                    ServiceException.class.getSimpleName(),
                    MessageValidation.class.getSimpleName()))
                .isNotNull();
            return MessageValidationAssert.assertThat(messageValidation);
            
        } catch (final ClassCastException ex) {
            
            Assertions.fail(String.format("Expecting %s message to be an instance of %s.",
                ServiceException.class.getSimpleName(),
                MessageValidation.class.getSimpleName()),
                ex);
            return null;
        }
    }
    
}
