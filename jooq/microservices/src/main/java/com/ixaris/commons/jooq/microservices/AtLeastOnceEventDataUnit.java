package com.ixaris.commons.jooq.microservices;

/**
 * Use this marker interface to start at least once processing of event handling
 */
public interface AtLeastOnceEventDataUnit extends AtLeastOncePublishEventDataUnit, AtLeastOnceHandleEventDataUnit {}
