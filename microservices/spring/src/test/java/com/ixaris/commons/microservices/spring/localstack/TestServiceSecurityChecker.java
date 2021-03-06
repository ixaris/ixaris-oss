package com.ixaris.commons.microservices.spring.localstack;

import java.util.List;
import java.util.Objects;

import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.microservices.lib.service.support.ServiceSecurityChecker;
import com.ixaris.commons.microservices.spring.example.Example.ExampleContext;

public class TestServiceSecurityChecker implements ServiceSecurityChecker {
    
    @Override
    public boolean check(final ServiceOperationHeader<?> header, final String security, final List<String> tags) {
        if (header.getContext() instanceof ExampleContext) {
            return security == null || Objects.equals(((ExampleContext) header.getContext()).getSecurity(), security);
        } else {
            return false;
        }
    }
    
}
