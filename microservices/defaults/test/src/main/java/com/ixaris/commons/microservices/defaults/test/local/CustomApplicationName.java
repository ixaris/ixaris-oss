package com.ixaris.commons.microservices.defaults.test.local;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Allows to modify spring.application.name property for tests extending from AbstractLocalStackIT. This needs to match the docker container name
 * as per the docker-compose files used to start the containers.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.TYPE)
public @interface CustomApplicationName {
    
    String value() default "";
}
