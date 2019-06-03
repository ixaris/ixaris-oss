package com.ixaris.commons.async.lib.net;

import java.nio.channels.SelectionKey;

public interface ChannelProcessor {
    
    void accept(SelectionKey key);
    
    void connect(SelectionKey key);
    
    void read(SelectionKey key);
    
    void write(SelectionKey key);
    
    void close(SelectionKey key);
    
}
