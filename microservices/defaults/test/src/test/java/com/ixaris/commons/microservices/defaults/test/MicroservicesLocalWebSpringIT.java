package com.ixaris.commons.microservices.defaults.test;

import static org.assertj.core.api.Java6Assertions.assertThat;

import org.junit.Test;
import org.springframework.boot.web.server.LocalServerPort;
import org.springframework.test.annotation.DirtiesContext;

import com.ixaris.commons.microservices.defaults.test.local.AbstractLocalWebStackIT;

/**
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
@DirtiesContext
public class MicroservicesLocalWebSpringIT extends AbstractLocalWebStackIT {
    
    @LocalServerPort
    private int port;
    
    @Test
    public void test() {
        // just checking stack is fine
        assertThat(port).isNotEqualTo(0);
    }
}
