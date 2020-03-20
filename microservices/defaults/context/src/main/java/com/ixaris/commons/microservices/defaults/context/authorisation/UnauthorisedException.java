package com.ixaris.commons.microservices.defaults.context.authorisation;

import com.ixaris.commons.microservices.defaults.context.CommonsMicroservicesDefaultsContext.Subject;

/**
 * @author <a href="mailto:maria.camenzuli@ixaris.com">maria.camenzuli</a>
 */
public class UnauthorisedException extends RuntimeException {
    
    public UnauthorisedException(final String message) {
        super(message);
    }
    
    public UnauthorisedException(final Subject subject, final Object entity) {
        super(String.format("Credential [%s] is not authorised to access entity [%s]", subject.getCredentialCode(), entity));
    }
    
    public UnauthorisedException(final Subject subject, final String entityType, final long id) {
        super(String.format("Credential [%s] is not authorised to access [%s] with id [%d]", subject.getCredentialCode(), entityType, id));
    }
}
