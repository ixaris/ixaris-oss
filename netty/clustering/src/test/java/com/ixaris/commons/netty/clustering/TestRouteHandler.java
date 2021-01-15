package com.ixaris.commons.netty.clustering;

import static com.ixaris.commons.async.lib.Async.result;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.clustering.lib.service.ClusterRouteHandler;
import com.ixaris.commons.netty.clustering.test.ClusterTest.ClusterTestResponse;
import com.ixaris.commons.protobuf.lib.CommonsProtobufLib.Empty;

public final class TestRouteHandler implements ClusterRouteHandler<Empty, ClusterTestResponse> {
    
    private final int node;
    
    public TestRouteHandler(final int node) {
        this.node = node;
    }
    
    @Override
    public String getKey() {
        return "test";
    }
    
    @Override
    public Async<ClusterTestResponse> handle(final long id, final String key, final Empty request) {
        return result(ClusterTestResponse.newBuilder().setId(id).setNode(node).build());
    }
    
}
