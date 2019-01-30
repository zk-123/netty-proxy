package com.zkdcloud.proxy.socks5.handler;

import com.zkdcloud.proxy.socks5.ServerStart;
import com.zkdcloud.proxy.socks5.context.ChannelContextConst;
import io.netty.bootstrap.Bootstrap;
import io.netty.buffer.ByteBuf;
import io.netty.channel.*;
import io.netty.channel.nio.NioEventLoopGroup;
import io.netty.channel.socket.SocketChannel;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.*;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import io.netty.util.ReferenceCountUtil;
import io.netty.util.concurrent.DefaultThreadFactory;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

/**
 * Handle the socks connection that comes in, complete the socks5 handshake
 *
 * @author zk
 * @since 2018/10/9
 */
public class Socks5ServerDoorHandler extends ChannelInboundHandlerAdapter {
    /**
     * static logger
     */
    private static Logger logger = LoggerFactory.getLogger(Socks5ServerDoorHandler.class);
    private static EventLoopGroup connectExecutors = new NioEventLoopGroup(Math.min(Runtime.getRuntime().availableProcessors() + 1, 32),
            new DefaultThreadFactory("connect-executors"));

    enum ACCSTATE {UN_INIT, UN_CONNECT, FINISHED}

    private ACCSTATE state;

    private Channel clientChannel;

    private Bootstrap bootstrap;

    public Socks5ServerDoorHandler() {
        this.state = ACCSTATE.UN_INIT;
    }

    @Override
    public void channelRead(ChannelHandlerContext ctx, Object msg) throws Exception {
        switch (state) {
            case UN_INIT:
                Socks5InitialResponse response;
                if (msg instanceof Socks5InitialRequest) {
                    response = new DefaultSocks5InitialResponse(Socks5AuthMethod.NO_AUTH);
                    ctx.channel().writeAndFlush(response);

                    ctx.pipeline().addBefore(ctx.name(), "socks5-command", new Socks5CommandRequestDecoder());
                    this.state = ACCSTATE.UN_CONNECT;
                    logger.info("{} init success", ctx.channel().id());
                } else {
                    response = new DefaultSocks5InitialResponse(Socks5AuthMethod.UNACCEPTED);
                    ctx.writeAndFlush(response).addListener(ChannelFutureListener.CLOSE);
                    logger.warn("{} init is not socks5InitRequest", ctx.channel().id());
                }
                ReferenceCountUtil.release(msg);
                break;
            case UN_CONNECT:
                if (msg instanceof DefaultSocks5CommandRequest) {
                    DefaultSocks5CommandRequest commandRequest = (DefaultSocks5CommandRequest) msg;
                    InetSocketAddress dstAddress = new InetSocketAddress(commandRequest.dstAddr(), commandRequest.dstPort());

                    ctx.channel().attr(ChannelContextConst.DST_ADDRESS).setIfAbsent(dstAddress);
                    buildLocalConnect(dstAddress, ctx.channel());
                    this.state = ACCSTATE.FINISHED;
                } else {
                    ctx.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.IPv4))
                            .addListener(ChannelFutureListener.CLOSE);
                    logger.warn("{} is not a commanderRequest", ctx.channel().id());
                }

                ReferenceCountUtil.release(msg);
                break;
            case FINISHED:
                ctx.fireChannelRead(msg);
                break;
        }
    }

    private void buildLocalConnect(final InetSocketAddress dstAddress, final Channel clientChannel) {
        if (bootstrap == null) {
            bootstrap = new Bootstrap();
            bootstrap.group(connectExecutors)
                    .channel(NioSocketChannel.class)
                    .option(ChannelOption.SO_KEEPALIVE, true)
                    .handler(new ChannelInitializer<SocketChannel>() {
                        @Override
                        protected void initChannel(SocketChannel ch) throws Exception {
                            ch.pipeline().addFirst("idle", new IdleStateHandler(0, 0, ServerStart.serverConfigure.getSecondsRemoteIdle(), TimeUnit.SECONDS) {
                                        private Logger logger = LoggerFactory.getLogger("remote idle logger");

                                        @Override
                                        public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                                            if (evt instanceof IdleStateEvent) {
                                                ctx.channel().close();
                                                this.logger.warn("{} idle timeout, will be close", ctx.channel().id());
                                            }
                                        }
                                    });
                            ch.pipeline().addLast(new SimpleChannelInboundHandler<ByteBuf>() {
                                @Override
                                protected void channelRead0(ChannelHandlerContext ctx, ByteBuf msg) throws Exception {
                                    clientChannel.writeAndFlush(msg.retain());
                                }

                                @Override
                                public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
                                    logger.error("{} happen error ,will be close", ctx.channel().id());
                                    ctx.channel().close();
                                }
                            });
                        }
                    })
                    .connect(dstAddress)
                    .addListener(new ChannelFutureListener() {
                        @Override
                        public void operationComplete(ChannelFuture future) throws Exception {
                            if (future.isSuccess()) {
                                clientChannel.attr(ChannelContextConst.PROXY_CHANNEL).setIfAbsent(future.channel());
                                clientChannel.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, Socks5AddressType.IPv4));
                                clientChannel.pipeline().addLast("socks5-transfer", new TransferFlowHandler());


                                logger.info("{} remote connect success", clientChannel.id());
                            } else {
                                clientChannel.writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, Socks5AddressType.IPv4))
                                        .addListener(ChannelFutureListener.CLOSE);
                                logger.error("{} remote connect is fail.{}:{}", clientChannel.id(), dstAddress.getHostName(), dstAddress.getPort());
                            }
                        }
                    });
        }
    }

    @Override
    public void handlerAdded(ChannelHandlerContext ctx) throws Exception {
        super.handlerAdded(ctx);
        clientChannel = ctx.channel();
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        closeChannel();
        logger.error("{} happen error, will be close : {}", ctx.channel().id(), cause.getMessage());
    }

    private void closeChannel() {
        //close client channel
        if (clientChannel != null) {
            //close remote channel
            Channel proxyChannel = clientChannel.attr(ChannelContextConst.PROXY_CHANNEL).get();
            if (proxyChannel != null) {
                proxyChannel.close();
            }

            clientChannel.close();
            clientChannel = null;
        }
    }
}
