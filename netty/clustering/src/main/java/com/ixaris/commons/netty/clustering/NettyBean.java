package com.ixaris.commons.netty.clustering;

import static com.ixaris.commons.misc.lib.object.Tuple.tuple;

import java.net.BindException;
import java.util.Locale;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicInteger;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.ixaris.commons.misc.lib.object.EqualsUtil;
import com.ixaris.commons.misc.lib.object.Tuple2;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.EventLoopGroup;
import io.netty.channel.MultithreadEventLoopGroup;
import io.netty.channel.epoll.EpollEventLoopGroup;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.unix.Errors.NativeIoException;
import io.netty.util.internal.SystemPropertyUtil;

public final class NettyBean {
    
    private static final Logger LOG = LoggerFactory.getLogger(NettyBean.class);
    
    public static final int MAX_NO_OF_BIND_TRIES = 50;
    public static final int SO_BACKLOG = 64;
    
    public static final class HostAndPort {
        
        public final String host;
        
        public final int port;
        
        public HostAndPort(final String host, final int port) {
            this.host = host;
            this.port = port;
        }
        
        @Override
        public boolean equals(final Object o) {
            return EqualsUtil.equals(this, o, other -> host.equals(other.host) && (port == other.port));
        }
        
        @Override
        public int hashCode() {
            return Objects.hash(host, port);
        }
        
        @Override
        public String toString() {
            return host + ":" + port;
        }
        
    }
    
    private final boolean linux;
    private final MultithreadEventLoopGroup group;
    private final AtomicInteger portNumber;
    private final String hostname;
    
    public NettyBean(final int numThreads, final String hostname, final int startPort) {
        this.linux = SystemPropertyUtil.get("os.name").toLowerCase(Locale.UK).trim().startsWith("linux");
        this.group = linux ? new EpollEventLoopGroup(numThreads) : new NioEventLoopGroup(numThreads);
        this.hostname = hostname;
        this.portNumber = new AtomicInteger(startPort);
    }
    
    public boolean isLinux() {
        return linux;
    }
    
    public EventLoopGroup getGroup() {
        return group;
    }
    
    @SuppressWarnings({ "squid:S2221", "squid:S1193" })
    public Tuple2<Channel, HostAndPort> bind(final ServerBootstrap serverBootstrap) {
        int tries = 0;
        while (true) {
            try {
                final HostAndPort operationUrl = new HostAndPort(hostname, portNumber.getAndIncrement());
                LOG.debug("Trying to bind with [{}]", operationUrl);
                final Channel channel = serverBootstrap.bind(operationUrl.host, operationUrl.port).syncUninterruptibly().channel();
                LOG.debug("Bound with [{}]", operationUrl);
                return tuple(channel, operationUrl);
            } catch (final Exception e) {
                if ((e instanceof BindException) || (e instanceof NativeIoException)) {
                    tries++;
                    if (tries == MAX_NO_OF_BIND_TRIES) {
                        throw new IllegalStateException("Did not manage to find port after " + MAX_NO_OF_BIND_TRIES + " tries", e);
                    }
                } else {
                    throw e;
                }
            }
        }
    }
    
    public void shutdown() {
        group.shutdownGracefully();
    }
    
}
