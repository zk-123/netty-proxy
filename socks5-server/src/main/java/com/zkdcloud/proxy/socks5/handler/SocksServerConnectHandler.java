package com.zkdcloud.proxy.socks5.handler;

import com.zkdcloud.proxy.socks5.ServerStart;
import com.zkdcloud.proxy.socks5.util.SocksServerUtils;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.codec.socksx.v5.DefaultSocks5CommandResponse;
import io.netty.handler.codec.socksx.v5.Socks5CommandRequest;
import io.netty.handler.codec.socksx.v5.Socks5CommandStatus;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.concurrent.TimeUnit;

import static com.zkdcloud.proxy.socks5.ServerStart.serverConfigure;
import static com.zkdcloud.proxy.socks5.context.ChannelContextConst.CLIENT_CHANNEL;
import static com.zkdcloud.proxy.socks5.context.ChannelContextConst.REMOTE_CHANNEL;

/**
 * build connect to remote
 */
public final class SocksServerConnectHandler extends SimpleChannelInboundHandler<Socks5CommandRequest> {
    /**
     * static logger
     */
    private static Logger logger = LoggerFactory.getLogger(SocksServerConnectHandler.class);

    private static EmptyHandler EMPTY_INBOUND_HANDLER = new EmptyHandler();
    private final Bootstrap b = new Bootstrap();

    @Override
    public void channelRead0(final ChannelHandlerContext ctx, final Socks5CommandRequest request) throws Exception {
        b.group(ServerStart.workGroup)
                .channel(NioSocketChannel.class)
                .option(ChannelOption.CONNECT_TIMEOUT_MILLIS, 10000)
                .option(ChannelOption.SO_KEEPALIVE, true)
                .handler(EMPTY_INBOUND_HANDLER);

        b.connect(request.dstAddr(), request.dstPort()).addListener(new ChannelFutureListener() {
            @Override
            public void operationComplete(final ChannelFuture future) throws Exception {
                if (future.isSuccess()) {
                    ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.SUCCESS, request.dstAddrType()))
                            .addListener(new ChannelFutureListener() {
                                @Override
                                public void operationComplete(ChannelFuture channelFuture) {
                                    ctx.channel().attr(REMOTE_CHANNEL).setIfAbsent(future.channel());
                                    future.channel().attr(CLIENT_CHANNEL).setIfAbsent(ctx.channel());

                                    //add remote handler
                                    future.channel().pipeline()
                                            .addLast("remote-idle", new IdleStateHandler(0, 0, serverConfigure.getSecondsRemoteIdle(), TimeUnit.SECONDS))
                                            .addLast("remote-reply-handler", new RelayHandler(ctx.channel()))
                                            .addLast("remote-exception-handler", new ExceptionDuplexHandler());
                                    //add client handler
                                    ctx.pipeline()
                                            .remove(SocksServerConnectHandler.this)
                                            .addBefore("client-exception-handler", "client-reply-handler", new RelayHandler(future.channel()));
                                }
                            });
                } else {
                    logger.error(future.cause().getMessage());
                    ctx.channel().writeAndFlush(new DefaultSocks5CommandResponse(Socks5CommandStatus.FAILURE, request.dstAddrType()));
                    SocksServerUtils.closeOnFlush(ctx.channel());
                }
            }
        });
    }

    @Override
    public void exceptionCaught(ChannelHandlerContext ctx, Throwable cause) throws Exception {
        SocksServerUtils.closeOnFlush(ctx.channel());
    }

    @Sharable
    private static class EmptyHandler extends ChannelInboundHandlerAdapter {
    }
}