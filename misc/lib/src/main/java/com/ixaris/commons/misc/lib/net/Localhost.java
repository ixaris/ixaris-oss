package com.ixaris.commons.misc.lib.net;

import java.net.InetAddress;
import java.net.ServerSocket;
import java.net.UnknownHostException;
import java.util.Random;

import javax.net.ServerSocketFactory;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

public final class Localhost {
    
    public static final String DEFAULT_HOSTNAME = "localhost";
    public static final String HOSTNAME;
    
    private static final Logger LOG = LoggerFactory.getLogger(Localhost.class);
    private static final int PORT_RANGE_MIN = 1024;
    private static final int PORT_RANGE_MAX = 65535;
    
    static {
        String tmpName;
        try {
            tmpName = InetAddress.getLocalHost().getHostAddress();
        } catch (final UnknownHostException e) {
            LOG.warn("Unable to resolve host address of this machine. Defaulting to localhost", e);
            tmpName = DEFAULT_HOSTNAME;
        }
        HOSTNAME = tmpName;
    }
    
    public static int findAvailableTcpPort() {
        return findAvailableTcpPort(PORT_RANGE_MIN);
    }
    
    public static int findAvailableTcpPort(int minPort) {
        return findAvailableTcpPort(minPort, PORT_RANGE_MAX);
    }
    
    public static int findAvailableTcpPort(int minPort, int maxPort) {
        final Random random = new Random(System.currentTimeMillis());
        final int portRange = maxPort - minPort;
        int candidatePort;
        int count = 0;
        do {
            if (count >= 50) {
                throw new IllegalStateException(String.format(
                    "Could not find an available port in the range [%d, %d] after %d attempts",
                    minPort,
                    maxPort,
                    count));
            }
            count++;
            candidatePort = minPort + random.nextInt(portRange + 1);
        } while (!isPortAvailable(candidatePort));
        
        return candidatePort;
    }
    
    private static boolean isPortAvailable(final int port) {
        try {
            final ServerSocket serverSocket = ServerSocketFactory.getDefault().createServerSocket(
                port, 1, InetAddress.getLocalHost());
            serverSocket.close();
            return true;
        } catch (final Exception ex) {
            return false;
        }
    }
    
    private Localhost() {}
    
}
