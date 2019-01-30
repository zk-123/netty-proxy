package com.zkdcloud.proxy.http.util;

import com.zkdcloud.proxy.http.ServerStart;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateEvent;
import io.netty.handler.timeout.IdleStateHandler;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class ChannelUtil {

    public static ChannelFuture connect(InetSocketAddress dstAddress, EventLoopGroup eventExecutors, final ChannelHandler... channelHandlers) {
        Bootstrap bootstrap = new Bootstrap();
        return bootstrap.group(eventExecutors)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<Channel>() {
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addFirst("idle", new IdleStateHandler(0, 0, ServerStart.serverConfigure.getSecondsRemoteIdle(), TimeUnit.SECONDS){
                                    private Logger logger = LoggerFactory.getLogger("remote idle logger");
                                    @Override
                                    public void userEventTriggered(ChannelHandlerContext ctx, Object evt) throws Exception {
                                        if (evt instanceof IdleStateEvent) {
                                            ctx.channel().close();
                                            this.logger.warn("{} idle timeout, will be close", ctx.channel().id());
                                        }
                                    }
                                });

                        for (int i = 0; i < channelHandlers.length; i++) {
                            ch.pipeline().addLast(channelHandlers[i]);
                        }
                    }
                }).connect(dstAddress);

    }
}
