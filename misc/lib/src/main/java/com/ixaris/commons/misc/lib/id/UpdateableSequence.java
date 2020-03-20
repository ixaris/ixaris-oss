package com.ixaris.commons.misc.lib.id;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class UpdateableSequence extends Sequence {
    
    private static final Logger LOG = LoggerFactory.getLogger(UpdateableSequence.class);
    
    public synchronized void setNodeId(final int newNodeIdWidth, final int newNodeId) {
        if ((newNodeIdWidth < 8) || (newNodeIdWidth > 16)) {
            throw new IllegalArgumentException(String.format("Invalid nodeIdWidth [%d]: should be between 8 and 16 inclusive", newNodeIdWidth));
        }
        final int newVirtualClockIncrement = 1 << newNodeIdWidth;
        if (newNodeId >= newVirtualClockIncrement) {
            throw new IllegalArgumentException(String.format("Invalid node id [%d]: out of range for width %d", newNodeId, newNodeIdWidth));
        }
        
        final int nodeWidth = getNodeWidth();
        final int nodeId = getNodeId();
        if (newNodeIdWidth != nodeWidth) {
            LOG.info(String.format("Changing virtual clock increment from %d to %d", nodeWidth, newNodeIdWidth));
        }
        if (newNodeId != nodeId) {
            LOG.info(String.format("Changing node id from %d to %d", nodeId, newNodeId));
        }
        nodeIdAndWidth = (newNodeId << 16) | newNodeIdWidth;
    }
    
}
