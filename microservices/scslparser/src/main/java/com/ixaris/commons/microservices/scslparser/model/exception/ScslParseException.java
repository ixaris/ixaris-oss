package com.ixaris.commons.microservices.scslparser.model.exception;

import java.util.Map;

/**
 * Created by ian.grima on 09/03/2016.
 */
public class ScslParseException extends IllegalStateException {
    
    private static final long serialVersionUID = -5846101542007285951L;
    
    public ScslParseException(Map.Entry<String, Object> entry) {
        super("Unrecognized tokens while parsing SCSL Definition: [key: " + entry.getKey() + ", value: " + entry.getValue() + "]");
    }
    
    public ScslParseException(final String error) {
        super("Error while parsing SCSL contract: [ " + error + " ]");
    }
    
    public ScslParseException(String error, Exception e) {
        super(error, e);
    }
    
    public ScslParseException(Exception e) {
        this("Error while parsing SCSL contract", e);
    }
    
}
