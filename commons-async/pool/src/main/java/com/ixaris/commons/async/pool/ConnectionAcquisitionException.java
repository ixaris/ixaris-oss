package com.ixaris.commons.async.pool;

public class ConnectionAcquisitionException extends IllegalStateException {
    
    private static final long serialVersionUID = 8989288425609539983L;
    
    protected ConnectionAcquisitionException(final String s) {
        super(s);
    }
    
}
