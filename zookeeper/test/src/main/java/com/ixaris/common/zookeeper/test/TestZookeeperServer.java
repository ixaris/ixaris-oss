package com.ixaris.common.zookeeper.test;

import java.io.IOException;

import org.apache.curator.test.TestingServer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixaris.commons.misc.lib.net.Localhost;

/**
 * Responsible for setting up and managing a Zookeeper server. This class is intended for use internally on development environments as well as
 * testing.
 *
 * @author aldrin.seychell
 */
public class TestZookeeperServer {
    
    private static final Logger LOG = LoggerFactory.getLogger(TestZookeeperServer.class);
    
    public static final int TEST_ZK_PORT = Localhost.findAvailableTcpPort(12181, 12281);
    
    private final int zookeeperPort;
    
    private TestingServer zkServer;
    
    public TestZookeeperServer() {
        this.zookeeperPort = TEST_ZK_PORT;
    }
    
    public TestZookeeperServer(final int zookeeperPort) {
        this.zookeeperPort = zookeeperPort;
    }
    
    public void start() {
        try {
            LOG.info("Starting Zookeeper Test Server on port [{}]", zookeeperPort);
            zkServer = new TestingServer(zookeeperPort);
        } catch (final Exception e) {
            throw new IllegalStateException(e);
        }
    }
    
    public void stop() {
        if (zkServer != null) {
            try {
                // close zookeeper and clean data during close
                LOG.info("Shutting down Zookeeper Test Server");
                zkServer.close();
            } catch (final IOException e) {
                LOG.error("Error when stopping test zookeeper server", e);
            }
        }
    }
    
    public String getConnectString() {
        return zkServer.getConnectString();
    }
    
}
