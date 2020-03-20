package com.ixaris.commons.microservices.scslparser.model.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Marks a SCSL field as required
 *
 * <p>Created by ian.grima on 09/03/2016.
 */
@Retention(RetentionPolicy.RUNTIME)
@Target(ElementType.FIELD)
public @interface Required {
    
    boolean required() default true;
}
