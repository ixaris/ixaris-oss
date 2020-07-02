package com.ixaris.commons.jooq.microservices;

import com.ixaris.commons.multitenancy.lib.data.DataUnit;

/**
 * Use this marker interface to start at least once processing of event publishing
 */
public interface AtLeastOncePublishEventDataUnit extends DataUnit {}
