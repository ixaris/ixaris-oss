package com.ixaris.commons.microservices.defaults.test;

import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;

import com.ixaris.commons.microservices.defaults.test.local.AbstractLocalStackIT;

/**
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
@DirtiesContext
public class MicroservicesLocalSpringIT extends AbstractLocalStackIT {
    
    @Test
    public void test() {
        // just checking stack is fine
    }
}
