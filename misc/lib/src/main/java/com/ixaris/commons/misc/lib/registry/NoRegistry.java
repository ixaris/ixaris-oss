package com.ixaris.commons.misc.lib.registry;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that a registerable interface does not have a corresponding registry. This is normally used by interfaces at the top of a hierarchy,
 * or intermediate interfaces along the hierarchy
 *
 * @author <a href="mailto:brian.vella@ixaris.com">Brian Vella</a>
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface NoRegistry {}
