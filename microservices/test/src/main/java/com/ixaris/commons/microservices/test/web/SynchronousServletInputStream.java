package com.ixaris.commons.microservices.test.web;

import java.io.IOException;
import java.io.InputStream;

import javax.servlet.ReadListener;

import org.springframework.mock.web.DelegatingServletInputStream;

/**
 * Custom extension of Spring's own {@link DelegatingServletInputStream}, updated to work (synchronously) with the Servlet 3.1 API. Spring (even
 * at 5.0.0M3) does not have its own implementation yet. As of version 4, the test actually fails with an AbstractMethodError due to some
 * mismatches in the servlet API dependencies between Spring and our own code. This class fixes that!
 *
 * @author benjie.gatt
 */
public class SynchronousServletInputStream extends DelegatingServletInputStream {
    
    public SynchronousServletInputStream(final InputStream sourceStream) {
        super(sourceStream);
    }
    
    /**
     * Synchronous implementation - the moment the read listener is set, we immediately attempt to read the data. Prepare the content to read via
     * {@link MockAsyncHttpServletRequest#setContent(byte[])}.
     *
     * @param readListener the read listener to read data from
     */
    @Override
    public void setReadListener(final ReadListener readListener) {
        try {
            readListener.onDataAvailable();
            readListener.onAllDataRead();
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }
    
    @Override
    public boolean isReady() {
        // although odd-looking, the way the listener is setup basically means it keeps reading until the input stream
        // is not ready.
        // this way, it reads the data once, then keeps working.
        return false;
    }
}
