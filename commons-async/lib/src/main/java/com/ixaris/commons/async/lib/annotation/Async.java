package com.ixaris.commons.async.lib.annotation;

import java.lang.annotation.ElementType;
import java.lang.annotation.Retention;
import java.lang.annotation.RetentionPolicy;
import java.lang.annotation.Target;
import java.util.concurrent.CompletionStage;

/**
 * Use this annotation to annotate methods that return an implementation of CompletionStage
 * and want to use {@link com.ixaris.commons.async.lib.Async#await(CompletionStage)}
 * or {@link com.ixaris.commons.async.lib.Async#awaitExceptions(CompletionStage)} such that
 * they can be identified and transformed be the transformer
 */
@Target(ElementType.METHOD)
@Retention(RetentionPolicy.RUNTIME)
public @interface Async {}
