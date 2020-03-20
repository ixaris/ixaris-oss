package com.ixaris.commons.microservices.lib.common;

import com.ixaris.commons.async.lib.filter.AsyncFilter;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.RequestEnvelope;
import com.ixaris.commons.microservices.lib.proto.CommonsMicroservicesLib.ResponseEnvelope;

public interface ServiceOperationFilter extends AsyncFilter<RequestEnvelope, ResponseEnvelope> {}
