package com.ixaris.commons.microservices.scslparser.model.exception;

/**
 * Created by ian.grima on 09/03/2016.
 */
public class ScslRequiredFieldNotFoundException extends IllegalStateException {
    
    private static final long serialVersionUID = -5846101544578285951L;
    
    public ScslRequiredFieldNotFoundException(final String arg) {
        super("Missing required field in SCSL contract: " + (arg == null ? "key" : arg));
    }
}
