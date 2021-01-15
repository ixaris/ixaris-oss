package com.ixaris.commons.netty.clustering;

import static com.ixaris.commons.netty.clustering.NettyBean.SO_BACKLOG;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.misc.lib.object.Tuple2;
import com.ixaris.commons.netty.clustering.NettyBean.HostAndPort;

import io.netty.bootstrap.ServerBootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.EpollServerSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioServerSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.flush.FlushConsolidationHandler;

public final class NettyServerChannel {
    
    private static final Logger LOG = LoggerFactory.getLogger(NettyServerChannel.class);
    
    private final HostAndPort url;
    private final Channel channel;
    private final AtomicBoolean active = new AtomicBoolean(true);
    
    public <T extends MessageLite> NettyServerChannel(final NettyBean nettyBean, final T defaultInstance, final BiConsumer<ChannelHandlerContext, T> messageConsumer) {
        final ServerBootstrap serverBootstrap = new ServerBootstrap()
            .group(nettyBean.getGroup())
            .channel(nettyBean.isLinux() ? EpollServerSocketChannel.class : NioServerSocketChannel.class)
            .childHandler(new ChannelInitializer<SocketChannel>() {
                
                @Override
                protected void initChannel(final SocketChannel ch) {
                    final ChannelPipeline pipeline = ch.pipeline();
                    
                    pipeline.addLast(new FlushConsolidationHandler());
                    pipeline.addLast(new ProtobufVarint32FrameDecoder());
                    pipeline.addLast(new ProtobufDecoder(defaultInstance));
                    pipeline.addLast(new ProtobufVarint32LengthFieldPrepender());
                    pipeline.addLast(new ProtobufEncoder());
                    pipeline.addLast(new SimpleChannelInboundHandler<T>() {
                        
                        @Override
                        protected void channelRead0(final ChannelHandlerContext ctx, final T message) {
                            messageConsumer.accept(ctx, message);
                        }
                        
                        @Override
                        public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable t) {
                            LOG.error("Exception in service channel " + url, t);
                        }
                        
                    });
                }
                
            })
            .option(ChannelOption.SO_BACKLOG, SO_BACKLOG)
            .childOption(ChannelOption.TCP_NODELAY, true)
            .childOption(ChannelOption.SO_KEEPALIVE, true)
            .childOption(ChannelOption.SO_RCVBUF, 5242880);
        
        final Tuple2<Channel, HostAndPort> channelAndUrl = nettyBean.bind(serverBootstrap);
        this.url = channelAndUrl.get2();
        this.channel = channelAndUrl.get1();
    }
    
    public HostAndPort getUrl() {
        return url;
    }
    
    public Channel getChannel() {
        return channel;
    }
    
    public void close() {
        if (active.compareAndSet(true, false)) {
            channel
                .close()
                .addListener(future -> {
                    if (future.isSuccess()) {
                        LOG.info("Unbound with [{}]", url);
                    }
                })
                .syncUninterruptibly();
        }
    }
    
}
