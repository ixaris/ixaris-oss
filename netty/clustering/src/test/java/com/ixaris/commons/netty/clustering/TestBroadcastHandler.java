package com.ixaris.commons.netty.clustering;

import static com.ixaris.commons.async.lib.Async.result;

import java.util.HashSet;
import java.util.Set;

import com.ixaris.commons.async.lib.Async;
import com.ixaris.commons.clustering.lib.service.ClusterBroadcastHandler;
import com.ixaris.commons.netty.clustering.test.ClusterTest.ClusterTestEvent;

public final class TestBroadcastHandler implements ClusterBroadcastHandler<ClusterTestEvent> {
    
    final Set<Integer> received = new HashSet<>();
    
    @Override
    public String getKey() {
        return "test";
    }
    
    @Override
    public Async<Boolean> handle(final ClusterTestEvent message) {
        received.add(message.getId());
        return result(true);
    }
    
}
