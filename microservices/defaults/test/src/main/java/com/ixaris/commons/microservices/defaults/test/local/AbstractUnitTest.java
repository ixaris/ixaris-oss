package com.ixaris.commons.microservices.defaults.test.local;

import org.junit.runner.RunWith;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringRunner;
import org.springframework.test.context.support.AnnotationConfigContextLoader;

/**
 * Base class for any unit tests that want to make use of the configuration provided in {@link UnitTestConfiguration}
 *
 * @author <a href="mailto:armand.sciberras@ixaris.com">armand.sciberras</a>
 * @author <a href="mailto:keith.spiteri@ixaris.com">keith.spiteri</a>
 */
@ContextConfiguration(loader = AnnotationConfigContextLoader.class, classes = { UnitTestConfiguration.class })
@RunWith(SpringRunner.class)
public abstract class AbstractUnitTest {}
