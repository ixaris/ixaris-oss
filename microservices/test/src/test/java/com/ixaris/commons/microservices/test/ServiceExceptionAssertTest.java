package com.ixaris.commons.microservices.test;

import org.assertj.core.api.Assertions;
import org.junit.Test;

import com.ixaris.commons.microservices.lib.common.exception.ClientConflictException;
import com.ixaris.commons.microservices.lib.common.exception.ClientForbiddenRequestException;
import com.ixaris.commons.microservices.lib.common.exception.ClientInvalidRequestException;
import com.ixaris.commons.microservices.lib.common.exception.ClientNotFoundException;
import com.ixaris.commons.microservices.lib.common.exception.ServiceException;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseStatusCode;
import com.ixaris.commons.microservices.test.example.TestData.TestConflict;

import valid.Valid.MessageValidation;

/**
 * Test for {@link ServiceExceptionAssert}.
 *
 * @author <a href="mailto:sarah.cassar@ixaris.com">sarah.cassar</a>
 */
public class ServiceExceptionAssertTest {
    
    @Test
    public void hasStatusCode_serviceExceptionHasExpectedStatusCode_shouldSuccessfullyVerifyExpectedStatusCode() {
        final TestConflict conflict = TestConflict.newBuilder().setConflict("conflict").build();
        ServiceExceptionAssert.assertThat(new ClientConflictException(conflict) {}).hasStatusCode(ResponseStatusCode.CLIENT_CONFLICT);
    }
    
    @Test
    public void hasStatusCode_serviceExceptionHasUnexpectedStatusCode_shouldThrowAssertionError() {
        Assertions
            .assertThatThrownBy(() -> ServiceExceptionAssert.assertThat(new ClientForbiddenRequestException()).hasStatusCode(ResponseStatusCode.CLIENT_CONFLICT))
            .isInstanceOf(AssertionError.class);
    }
    
    @Test
    public void isThrownDueToAClientInvalidRequestWithMessageValidations_serviceExceptionStatusCodeNotClientInvalidRequest_shouldThrowAssertionError() {
        Assertions
            .assertThatThrownBy(() -> {
                ServiceExceptionAssert.assertThat(new ClientNotFoundException()).isThrownDueToAClientInvalidRequestWithMessageValidations();
            })
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining("Unexpected status code.");
    }
    
    @Test
    public void isThrownDueToAClientInvalidRequestWithMessageValidations_serviceExceptionWithClientInvalidRequestStatusCodeAndNullMessage_shouldThrowAssertionError() {
        Assertions
            .assertThatThrownBy(() -> ServiceExceptionAssert.assertThat(new ClientInvalidRequestException()).isThrownDueToAClientInvalidRequestWithMessageValidations())
            .isInstanceOf(AssertionError.class)
            .hasMessageContaining(String.format("Expecting %s to have a non-null %s message.",
                ServiceException.class.getSimpleName(),
                MessageValidation.class.getSimpleName()));
    }
    
    @Test
    public void isThrownDueToAClientInvalidRequestWithMessageValidations_serviceExceptionWithClientInvalidRequestStatusCodeAndDefinedMessageValidationMessage_shouldSuccessfullyVerifyExpectations() {
        ServiceExceptionAssert
            .assertThat(new ClientInvalidRequestException(MessageValidation.getDefaultInstance()))
            .isThrownDueToAClientInvalidRequestWithMessageValidations();
    }
    
}
