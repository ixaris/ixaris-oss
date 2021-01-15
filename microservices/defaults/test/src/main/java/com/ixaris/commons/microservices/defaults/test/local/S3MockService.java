package com.ixaris.commons.microservices.defaults.test.local;

import javax.annotation.PostConstruct;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import io.findify.s3mock.S3Mock;

public class S3MockService {
    
    private static final Logger LOG = LoggerFactory.getLogger(S3MockService.class);
    private S3Mock api;
    
    @PostConstruct
    public void startS3MockService() {
        try {
            api = new S3Mock.Builder().withPort(19001).withInMemoryBackend().build();
            api.start();
        } catch (RuntimeException e) {
            LOG.info(e.getMessage());
            LOG.debug("Error while starting the api mock.", e);
        }
    }
    
}
