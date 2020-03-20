package com.ixaris.commons.misc.lib.id;

public abstract class Sequence {
    
    // 16 bit node id (initialised to 1), 16 bit width (initialised to 8)
    protected volatile int nodeIdAndWidth = 0x00010008;
    
    Sequence() {}
    
    public int getNodeId() {
        return nodeIdAndWidth >>> 16;
    }
    
    public int getNodeWidth() {
        return nodeIdAndWidth & 0xFFFF;
    }
    
}
