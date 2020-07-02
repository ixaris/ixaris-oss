package com.ixaris.commons.microservices.defaults.test;

import org.assertj.core.api.Assertions;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;

import com.ixaris.commons.microservices.defaults.test.local.AbstractLocalStackIT;
import com.ixaris.commons.microservices.secrets.CertificateLoader;

/**
 * Created by tiago.cucki on 14/07/2017.
 */
public class CertificatesConfigurationTest extends AbstractLocalStackIT {
    
    @Autowired
    private CertificateLoader certificateLoader;
    
    @Test
    public void test_read_certificates() {
        Assertions.assertThat(certificateLoader.getCertificate("any_string")).as("Testing public certificate configured").isNotNull();
        Assertions.assertThat(certificateLoader.getPrivateKey()).as("Testing private certificate configured").isNotNull();
    }
}
