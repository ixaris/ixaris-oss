package com.ixaris.commons.microservices.web.swagger.events;

import com.ixaris.commons.async.lib.filter.AsyncFilter;
import com.ixaris.commons.misc.lib.object.Ordered;

/**
 * @author <a href="mailto:aldrin.seychell@ixaris.com">aldrin.seychell</a>
 */
public interface SwaggerEventFilter extends AsyncFilter<SwaggerEvent, SwaggerEventAck>, Ordered {}
