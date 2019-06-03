package com.ixaris.commons.misc.lib.id;

import com.ixaris.commons.misc.lib.logging.Logger;
import com.ixaris.commons.misc.lib.logging.LoggerFactory;

public final class UpdateableSequence extends Sequence {
    
    private static final Logger LOG = LoggerFactory.forEnclosingClass();
    
    public synchronized void setNodeId(final int newNodeIdWidth, final int newNodeId) {
        if ((newNodeIdWidth < 8) || (newNodeIdWidth > 16)) {
            throw new IllegalArgumentException(
                "Invalid nodeIdWidth [" + newNodeIdWidth + "]: should be between 8 and 16 inclusive"
            );
        }
        final int newVirtualClockIncrement = 1 << newNodeIdWidth;
        if (newNodeId >= newVirtualClockIncrement) {
            throw new IllegalArgumentException(
                "Invalid node id [" + newNodeId + "]: out of range for width " + newNodeIdWidth
            );
        }
        
        final int nodeWidth = getNodeWidth();
        final int nodeId = getNodeId();
        if (newNodeIdWidth != nodeWidth) {
            LOG.atInfo().log("Changing virtual clock increment from " + nodeWidth + " to " + newNodeIdWidth);
        }
        if (newNodeId != nodeId) {
            LOG.atInfo().log("Changing node id from " + nodeId + " to " + newNodeId);
        }
        nodeIdAndWidth = newNodeId << 16 | newNodeIdWidth;
    }
    
}
