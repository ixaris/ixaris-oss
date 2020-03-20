package com.ixaris.commons.microservices.lib.common;

import com.ixaris.commons.async.lib.filter.AsyncFilter;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventAckEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.EventEnvelope;

public interface ServiceEventFilter extends AsyncFilter<EventEnvelope, EventAckEnvelope> {}
