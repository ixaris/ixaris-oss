package com.ixaris.commons.netty.clustering;

import java.util.Optional;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.BiConsumer;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import com.google.protobuf.MessageLite;

import com.ixaris.commons.netty.clustering.NettyBean.HostAndPort;

import io.netty.bootstrap.Bootstrap;
import io.netty.channel.Channel;
import io.netty.channel.ChannelFuture;
import io.netty.channel.ChannelFutureListener;
import io.netty.channel.ChannelHandlerContext;
import io.netty.channel.ChannelInitializer;
import io.netty.channel.ChannelOption;
import io.netty.channel.ChannelOutboundInvoker;
import io.netty.channel.ChannelPipeline;
import io.netty.channel.SimpleChannelInboundHandler;
import io.netty.channel.epoll.EpollSocketChannel;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.protobuf.ProtobufDecoder;
import io.netty.handler.codec.protobuf.ProtobufEncoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32FrameDecoder;
import io.netty.handler.codec.protobuf.ProtobufVarint32LengthFieldPrepender;
import io.netty.handler.flush.FlushConsolidationHandler;

public final class NettyClientChannel implements ChannelFutureListener {
    
    private static final Logger LOG = LoggerFactory.getLogger(NettyClientChannel.class);
    
    private final HostAndPort url;
    private final ScheduledExecutorService executor;
    private final Bootstrap bootstrap;
    private final AtomicReference<Channel> channel = new AtomicReference<>();
    private final AtomicBoolean active = new AtomicBoolean(true);
    
    public <T extends MessageLite> NettyClientChannel(final ScheduledExecutorService executor,
                                                      final NettyBean nettyBean,
                                                      final HostAndPort url,
                                                      final T defaultInstance,
                                                      final BiConsumer<ChannelHandlerContext, T> messageConsumer) {
        this.url = url;
        this.executor = executor;
        this.bootstrap = new Bootstrap()
            .group(nettyBean.getGroup())
            .channel(nettyBean.isLinux() ? EpollSocketChannel.class : NioSocketChannel.class)
            .handler(new ChannelInitializer<SocketChannel>() {
                
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
                        public void channelUnregistered(final ChannelHandlerContext ctx) {
                            scheduleConnect();
                        }
                        
                        @Override
                        public void exceptionCaught(final ChannelHandlerContext ctx, final Throwable t) {
                            LOG.error("Exception in client channel " + url, t);
                            ctx.close();
                        }
                        
                    });
                }
                
            })
            .option(ChannelOption.SO_KEEPALIVE, true)
            .option(ChannelOption.TCP_NODELAY, true)
            .option(ChannelOption.SO_RCVBUF, 5242880);
        
        connect();
    }
    
    public Channel getChannel() {
        return channel.get();
    }
    
    @SuppressWarnings("squid:S1845")
    public void close() {
        if (active.compareAndSet(true, false)) {
            Optional.ofNullable(channel.getAndSet(null)).ifPresent(c -> c
                .close()
                .addListener(f -> {
                    if (f.isSuccess()) {
                        LOG.info("Disconnected with {}", url);
                    } else {
                        LOG.error("Disconnection with {} for {} failed", url, f.cause());
                    }
                })
                .syncUninterruptibly());
        }
    }
    
    @Override
    public void operationComplete(final ChannelFuture future) {
        final Channel newChannel = future.channel();
        if (!future.isSuccess()) {
            newChannel.close();
            connect();
        } else {
            channel.set(newChannel);
            if (active.get()) {
                // add a listener to detect the connection lost
                newChannel.closeFuture().addListener(f -> scheduleConnect());
            } else {
                Optional.ofNullable(channel.getAndSet(null)).ifPresent(ChannelOutboundInvoker::close);
            }
        }
    }
    
    private void connect() {
        if (active.get()) {
            try {
                bootstrap.connect(url.host, url.port).addListener(this);
            } catch (final RuntimeException e) {
                LOG.warn("Failed to connect to {}:{}. Scheduling connect", url.host, url.port, e);
                scheduleConnect();
            }
        }
    }
    
    private void scheduleConnect() {
        if (active.get()) {
            channel.set(null);
            executor.schedule(this::connect, 10L, TimeUnit.MILLISECONDS);
        }
    }
    
}
