package com.ixaris.commons.microservices.test.web;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.UnsupportedEncodingException;

import javax.servlet.WriteListener;

import org.springframework.mock.web.DelegatingServletOutputStream;

/**
 * Custom extension of Spring's own {@link DelegatingServletOutputStream}, updated to work (synchronously) with the Servlet 3.1 API.
 *
 * <p>Similar to {@link SynchronousServletInputStream}, but for the output stream.
 *
 * @author benjie.gatt
 */
public class SynchronousServletOutputStream extends DelegatingServletOutputStream {
    
    public SynchronousServletOutputStream(final ByteArrayOutputStream targetStream) {
        super(targetStream);
    }
    
    /**
     * Synchronous implementation - immediately writes data the moment this listener is set.
     */
    @Override
    public void setWriteListener(final WriteListener writeListener) {
        try {
            writeListener.onWritePossible();
        } catch (final IOException e) {
            throw new IllegalStateException(e);
        }
    }
    
    public String getContent() {
        try {
            return ((ByteArrayOutputStream) getTargetStream()).toString("UTF-8");
        } catch (final UnsupportedEncodingException e) {
            throw new IllegalStateException(e);
        }
    }
}
