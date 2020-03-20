package com.ixaris.commons.microservices.lib.service.proxy;

final class ResourcePathParam {
    
    final int index;
    final PathParamType pathParamType;
    
    public ResourcePathParam(final int index, final PathParamType pathParamType) {
        this.index = index;
        this.pathParamType = pathParamType;
    }
    
}
