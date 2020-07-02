package com.ixaris.commons.jooq.microservices;

import com.ixaris.commons.multitenancy.lib.data.DataUnit;

/**
 * Use this marker interface to start at least once processing of event handling
 */
public interface AtLeastOnceHandleEventDataUnit extends DataUnit {}
