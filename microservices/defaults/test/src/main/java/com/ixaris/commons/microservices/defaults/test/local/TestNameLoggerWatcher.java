package com.ixaris.commons.microservices.defaults.test.local;

import org.junit.rules.TestWatcher;
import org.junit.runner.Description;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public class TestNameLoggerWatcher extends TestWatcher {
    
    private static final Logger LOG = LoggerFactory.getLogger(TestNameLoggerWatcher.class);
    
    @Override
    protected void starting(final Description description) {
        LOG.info(">>>>>>>>>>>>>>>>> Starting test {}", description.getMethodName());
        super.starting(description);
    }
    
    @Override
    protected void finished(final Description description) {
        LOG.info("<<<<<<<<<<<<<<<<< Finished test {}", description.getMethodName());
        super.finished(description);
    }
    
}
