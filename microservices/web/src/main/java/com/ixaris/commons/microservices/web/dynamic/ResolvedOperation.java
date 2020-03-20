package com.ixaris.commons.microservices.web.dynamic;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;

public class ResolvedOperation<C extends MessageLite> {
    
    public final String callRef;
    public final String serviceName;
    public final ServicePathHolder path;
    public final String method;
    public final ServiceOperationHeader<C> header;
    public final String payload;
    
    public ResolvedOperation(final String callRef,
                             final String serviceName,
                             final ServicePathHolder path,
                             final String method,
                             final ServiceOperationHeader<C> header,
                             final String payload) {
        this.callRef = callRef;
        this.serviceName = serviceName;
        this.path = path;
        this.method = method;
        this.header = header;
        this.payload = payload;
    }
    
}
