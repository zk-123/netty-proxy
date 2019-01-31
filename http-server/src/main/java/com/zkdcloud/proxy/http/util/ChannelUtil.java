package com.zkdcloud.proxy.http.util;

import com.zkdcloud.proxy.http.ServerStart;
import io.netty.bootstrap.Bootstrap;
import io.netty.channel.*;
import io.netty.channel.socket.nio.NioSocketChannel;
import io.netty.handler.timeout.IdleStateHandler;

import java.net.InetSocketAddress;
import java.util.concurrent.TimeUnit;

public class ChannelUtil {

    public static ChannelFuture connect(InetSocketAddress dstAddress, EventLoopGroup eventExecutors, final ChannelHandler... channelHandlers) {
        Bootstrap bootstrap = new Bootstrap();
        return bootstrap.group(eventExecutors)
                .channel(NioSocketChannel.class)
                .handler(new ChannelInitializer<Channel>() {
                    protected void initChannel(Channel ch) throws Exception {
                        ch.pipeline().addFirst("idle", new IdleStateHandler(0, 0, ServerStart.serverConfigure.getSecondsRemoteIdle(), TimeUnit.SECONDS));
                        for (int i = 0; i < channelHandlers.length; i++) {
                            ch.pipeline().addLast(channelHandlers[i]);
                        }
                    }
                }).connect(dstAddress);

    }
}
