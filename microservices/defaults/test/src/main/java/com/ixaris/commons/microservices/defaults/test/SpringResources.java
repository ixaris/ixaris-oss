package com.ixaris.commons.microservices.defaults.test;

import org.springframework.context.annotation.ImportResource;

/**
 * A configuration which imports any resource defined by the indicated xml file.
 *
 * @author <a href="mailto:sarah.cassar@ixaris.com">sarah.cassar</a>
 */
@ImportResource("classpath*:spring/*.xml")
public class SpringResources {}
