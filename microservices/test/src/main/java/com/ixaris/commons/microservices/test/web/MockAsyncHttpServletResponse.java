package com.ixaris.commons.microservices.test.web;

import java.io.ByteArrayOutputStream;
import java.io.PrintWriter;
import java.nio.charset.StandardCharsets;

import javax.servlet.ServletOutputStream;

import org.springframework.mock.web.MockHttpServletResponse;

/**
 * Class to add Servlet 3.1 behaviour via {@link SynchronousServletOutputStream}.
 *
 * @author benjie.gatt
 */
public class MockAsyncHttpServletResponse extends MockHttpServletResponse {
    
    private ByteArrayOutputStream targetStream = new ByteArrayOutputStream();
    
    @Override
    public ServletOutputStream getOutputStream() {
        return new SynchronousServletOutputStream(targetStream);
    }
    
    @Override
    public PrintWriter getWriter() {
        return new PrintWriter(targetStream);
    }
    
    @Override
    public byte[] getContentAsByteArray() {
        return targetStream.toByteArray();
    }
    
    @Override
    public String getContentAsString() {
        return new String(targetStream.toByteArray(), StandardCharsets.UTF_8);
    }
    
}
