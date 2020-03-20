package com.ixaris.commons.microservices.test.web;

import java.io.ByteArrayInputStream;

import javax.servlet.AsyncContext;
import javax.servlet.ServletInputStream;

import org.springframework.mock.web.MockHttpServletRequest;

/**
 * Class to work around an odd design choice/bug to not have the {@link AsyncContext} have a response object. Also adds Servlet 3.1 behaviour via
 * {@link SynchronousServletInputStream}.
 *
 * @author benjie.gatt
 */
public class MockAsyncHttpServletRequest extends MockHttpServletRequest {
    
    private byte[] content;
    
    @Override
    public AsyncContext startAsync() {
        if (this.getAsyncContext() != null) {
            return getAsyncContext();
        } else {
            return super.startAsync();
        }
    }
    
    @Override
    public ServletInputStream getInputStream() {
        return new SynchronousServletInputStream(new ByteArrayInputStream(this.content));
    }
    
    /**
     * @param content the content to be read by {@link SynchronousServletInputStream}
     */
    @Override
    public void setContent(byte[] content) {
        this.content = content;
    }
    
}
