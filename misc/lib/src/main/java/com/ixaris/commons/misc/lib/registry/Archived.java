package com.ixaris.commons.misc.lib.registry;

import java.lang.annotation.Documented;
import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;

/**
 * Indicates that although this registerable should still be registered, it is "Archived". As such, unless the registry is asked specifically for
 * this key via {@link Registry#resolve}, it will not be returned for use.
 *
 * <p>User: benjie.gatt
 */
@Target({ ElementType.TYPE })
@Retention(RetentionPolicy.RUNTIME)
@Documented
public @interface Archived {}
