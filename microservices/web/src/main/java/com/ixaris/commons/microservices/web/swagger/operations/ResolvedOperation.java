package com.ixaris.commons.microservices.web.swagger.operations;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.microservices.lib.common.ServiceOperationHeader;
import com.ixaris.commons.microservices.lib.common.ServicePathHolder;

public class ResolvedOperation<C extends MessageLite> {
    
    public final String callRef;
    public final boolean create;
    public final String serviceName;
    public final ServicePathHolder path;
    public final ServicePathHolder params;
    public final ServiceOperationHeader<C> header;
    
    public ResolvedOperation(final String callRef,
                             final boolean create,
                             final String serviceName,
                             final ServicePathHolder path,
                             final ServicePathHolder params,
                             final ServiceOperationHeader<C> header) {
        this.callRef = callRef;
        this.create = create;
        this.serviceName = serviceName;
        this.path = path;
        this.params = params;
        this.header = header;
    }
    
    public ResolvedOperation(final ResolvedOperation<C> operation) {
        this.callRef = operation.callRef;
        this.create = operation.create;
        this.serviceName = operation.serviceName;
        this.path = operation.path;
        this.params = operation.params;
        this.header = operation.header;
    }
    
}
